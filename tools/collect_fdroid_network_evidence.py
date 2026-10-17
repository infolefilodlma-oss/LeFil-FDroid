#!/usr/bin/env python3
"""Observe anonymous web dependencies and resolve license declarations; never log secrets."""
import concurrent.futures
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

PIN = '99789aa0f9c82d90cef020b33444698a05aafc47'
OUT = Path('docs/validation/fdroid-finalization')


def safe_url(url):
    p = urllib.parse.urlsplit(url)
    query = urllib.parse.parse_qs(p.query)
    record = {'url': urllib.parse.urlunsplit((p.scheme, p.hostname or '', p.path, '', '')),
              'query_keys': sorted(query)}
    route = query.get('r', [''])[0]
    if re.fullmatch(r'[a-zA-Z0-9_/-]{1,80}', route):
        record['route'] = route
    return record


def read_pom(group, name, version, depth=0):
    if depth > 3 or any('${' in x for x in (group, name, version)):
        raise ValueError('Unresolved POM inheritance')
    relative = '/'.join((group.replace('.', '/'), name, version, name + '-' + version + '.pom'))
    repos = ['https://dl.google.com/dl/android/maven2/', 'https://repo.maven.apache.org/maven2/']
    if not group.startswith(('androidx.', 'com.google.android.')):
        repos.reverse()
    for repo in repos:
        try:
            with urllib.request.urlopen(repo + relative, timeout=30) as response:
                data = response.read(2_000_001)
            if len(data) > 2_000_000:
                raise ValueError('POM exceeds limit')
            break
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
    else:
        raise ValueError('No POM in the declared trusted repositories')
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    root = ET.fromstring(data)
    licenses = [{'name': e.findtext('m:name', default='', namespaces=ns),
                 'url': e.findtext('m:url', default='', namespaces=ns)}
                for e in root.findall('m:licenses/m:license', ns)]
    evidence = {'url': repo + relative, 'sha256': hashlib.sha256(data).hexdigest()}
    if not licenses:
        parent = root.find('m:parent', ns)
        if parent is not None:
            inherited = read_pom(*(parent.findtext('m:' + key, namespaces=ns)
                                   for key in ('groupId', 'artifactId', 'version')), depth=depth + 1)
            licenses = inherited['licenses']
            evidence['inherited_from'] = inherited['pom']
    return {'licenses': licenses, 'pom': evidence}


def library_evidence():
    modules = json.loads(Path(os.environ['LEFIL_RUNTIME_REPORT']).read_text())
    assert len(modules) > 4, 'The full runtime graph, not only direct libraries, is required'
    def license_for(module):
        result = dict(module)
        try:
            result.update(read_pom(module['group'], module['name'], module['version']))
        except Exception as error:
            result.update(licenses=[], error=str(error))
        return result
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        libraries = list(pool.map(license_for, modules))
    result = {'configuration': 'releaseRuntimeClasspath', 'modules': libraries,
              'module_count': len(libraries), 'missing_license_count': sum(not x['licenses'] for x in libraries),
              'scope': 'Selected production runtime modules and artifacts; test dependencies and Android platform are separate. License declarations are evidence, not an independent code audit.'}
    (OUT / 'runtime-dependencies.json').write_text(json.dumps(result, indent=2) + '\n')
    if result['missing_license_count']:
        raise RuntimeError('Some runtime POM licenses remain unresolved; see report')


def browser_evidence():
    from playwright.sync_api import sync_playwright
    pages = []
    inputs = [('Le Fil', 'https://le-fil.net/'),
              ('Agora', 'https://social.le-fil.net/message'),
              ('Atrium Talk', 'https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex')]
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch()
        for label, url in inputs:
            context = browser.new_context(viewport={'width': 393, 'height': 851}, is_mobile=True,
                                          service_workers='block')
            page = context.new_page()
            requests = []
            def intercept(route):
                request = route.request
                blocked = request.method not in ('GET', 'HEAD', 'OPTIONS') or request.resource_type == 'media'
                entry = dict(safe_url(request.url), method=request.method,
                             resource_type=request.resource_type, blocked_by_observer=blocked)
                requests.append(entry)
                if len(requests) > 300 or blocked:
                    route.abort()
                else:
                    route.continue_()
            def response_seen(response):
                key = safe_url(response.url)
                for entry in reversed(requests):
                    if entry['url'] == key['url'] and entry['query_keys'] == key['query_keys']:
                        entry['status'] = response.status
                        break
            page.route('**/*', intercept)
            page.on('response', response_seen)
            item = {'label': label, 'input': url, 'requests': requests}
            try:
                response = page.goto(url, wait_until='domcontentloaded', timeout=30000)
                page.wait_for_timeout(5000)
                item.update(final=safe_url(page.url), http_status=response.status if response else None,
                            title=page.title(), dom_sha256=hashlib.sha256(page.content().encode()).hexdigest())
                item['forms'] = page.locator('form').evaluate_all("""forms => forms.map(form => ({
                    method: form.method, action: new URL(form.action, document.baseURI).origin + new URL(form.action, document.baseURI).pathname,
                    password_fields: form.querySelectorAll('input[type=password]').length,
                    field_names: Array.from(form.querySelectorAll('input[name]')).map(input => input.name)
                }))""")
                item['generator'] = page.locator('meta[name=generator]').evaluate_all('items => items.map(item => item.content)')
            except Exception as error:
                item['error'] = str(error).split('Call log:')[0][:1000]
            item['observed_hosts'] = sorted({urllib.parse.urlsplit(x['url']).hostname for x in requests
                                              if x['url'].startswith(('https://', 'http://'))})
            pages.append(item)
            context.close()
        browser.close()
    result = {'authenticated': False, 'browser': 'Playwright Chromium, fresh mobile-sized anonymous contexts',
              'limitations': 'Bounded public-page observation, 5 seconds after DOM load; no forms submitted. Non-read requests and media are recorded then blocked. Service workers blocked. Authenticated, radio playback and user-generated content may introduce other hosts. Not an Android WebView or phone acceptance test.',
              'cookies_headers_tokens_or_bodies_stored': False, 'pages': pages}
    (OUT / 'public-network.json').write_text(json.dumps(result, indent=2) + '\n')
    if any('error' in p or not p.get('http_status') for p in pages):
        raise RuntimeError('Public browser observation incomplete; see report')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    sha = os.environ['GITHUB_SHA']
    app_tree = subprocess.check_output(['git', 'rev-parse', sha + ':app'], text=True).strip()
    assert app_tree == subprocess.check_output(['git', 'rev-parse', PIN + ':app'], text=True).strip()
    provenance = {'checked_at_utc': datetime.now(timezone.utc).isoformat(), 'candidate_commit': PIN,
                  'observer_commit': sha, 'application_tree': app_tree,
                  'run_url': 'https://github.com/' + os.environ['GITHUB_REPOSITORY'] + '/actions/runs/' + os.environ['GITHUB_RUN_ID'],
                  'jvm_tests_rerun': False, 'native_tests_rerun': False, 'apk_rebuilt': False,
                  'authenticated_communications_validated': False}
    (OUT / 'provenance.json').write_text(json.dumps(provenance, indent=2) + '\n')
    library_evidence()
    browser_evidence()
    print('::notice::Full production runtime POM declarations and bounded anonymous network observation saved; no APK build, test rerun or authenticated validation')


if __name__ == '__main__':
    main()
