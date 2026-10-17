#!/usr/bin/env python3
"""Check only the newly observed Google font dependency, without accounts or app changes."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import urllib.parse
from playwright.sync_api import sync_playwright

records = []
with sync_playwright() as playwright:
    browser = playwright.chromium.launch()
    for label, url in (
        ('Le Fil', 'https://le-fil.net/'),
        ('Atrium Talk login', 'https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex'),
    ):
        blocked = set()
        context = browser.new_context(viewport={'width': 393, 'height': 851}, service_workers='block')
        page = context.new_page()
        def intercept(route):
            request = route.request
            host = urllib.parse.urlsplit(request.url).hostname or ''
            google = any(host == domain or host.endswith('.' + domain) for domain in
                         ('google.com', 'googleapis.com', 'gstatic.com', 'googleusercontent.com'))
            if google:
                blocked.add(host)
            if google or request.method not in ('GET', 'HEAD', 'OPTIONS') or request.resource_type == 'media':
                route.abort()
            else:
                route.continue_()
        page.route('**/*', intercept)
        response = page.goto(url, wait_until='domcontentloaded', timeout=30000)
        page.wait_for_timeout(5000)
        if label == 'Le Fil':
            controls = {word: page.get_by_text(word, exact=True).first.is_visible()
                        for word in ('AGORA', 'ATRIUM', 'TALK')}
        else:
            controls = {'password_input_visible': page.locator('input[type=password]').first.is_visible()}
        record = {'label': label, 'input_url': url, 'http_status': response.status,
                  'google_hosts_blocked': sorted(blocked), 'visible_controls': controls,
                  'public_controls_remain_visible': bool(controls) and all(controls.values())}
        records.append(record)
        context.close()
    browser.close()
result = {'checked_at_utc': datetime.now(timezone.utc).isoformat(),
          'run_url': 'https://github.com/' + os.environ['GITHUB_REPOSITORY'] + '/actions/runs/' + os.environ['GITHUB_RUN_ID'],
          'method': 'Anonymous Chromium; block Google hosts at request level; no CSS injection, form submission or account. Observe existing public controls.',
          'checks': records,
          'limits': 'Public home and login appearance only. Does not validate authenticated sessions, challenge/2FA providers, messages, Android notifications or phone behavior.'}
out = Path('docs/validation/fdroid-finalization')
out.mkdir(parents=True, exist_ok=True)
(out / 'google-fonts-fallback.json').write_text(json.dumps(result, indent=2) + '\n')
assert all(r['google_hosts_blocked'] and r['http_status'] == 200 and r['public_controls_remain_visible'] for r in records), records
print('::notice::Public home and Talk login controls remain visible with Google font hosts blocked; no authenticated functionality validated')
