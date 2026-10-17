#!/usr/bin/env python3
"""Read release prerequisites in GitHub Actions; never publishes signing material."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
from html.parser import HTMLParser
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.request


SOURCE = "25189d316d8345a601b4dae2fa9987d286831d1e"
REPOSITORY = "infolefilodlma-oss/LeFil-FDroid"
OUTPUT = Path("docs/validation/direct-release/preflight.json")
DOCUMENTS = {
    "android_signing": "https://developer.android.com/studio/publish/app-signing",
    "android_distribution": "https://developer.android.com/distribute/marketing-tools/alternative-distribution",
    "github_releases": "https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases",
    "fdroid_developer_signing": "https://f-droid.org/docs/Reproducible_Builds/",
}


class VisibleText(HTMLParser):
    def __init__(self):
        super().__init__()
        self.hidden = 0
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style"):
            self.hidden += 1

    def handle_endtag(self, tag):
        if tag in ("script", "style"):
            self.hidden -= 1
        if tag in ("p", "li", "h1", "h2", "h3"):
            self.parts.append("\n")

    def handle_data(self, data):
        if not self.hidden:
            self.parts.append(data)


def document(item):
    name, url = item
    result = {"url": url}
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "LeFil-release-readiness"})
        with urllib.request.urlopen(request, timeout=45) as response:
            body = response.read()
            result.update(http_status=response.status, resolved_url=response.url,
                          sha256=hashlib.sha256(body).hexdigest())
        parser = VisibleText()
        parser.feed(body.decode("utf-8", errors="replace"))
        paragraphs = [re.sub(r"\s+", " ", p).strip()
                      for p in "".join(parser.parts).splitlines()]
        relevant = re.compile(
            r"same (?:signing )?key|same certificate|secure|back.?up|25 years|"
            r"Binaries|AllowedAPKSigningKeys|upstream developer|signature|"
            r"website|release-ready|unknown sources|write permission", re.I)
        result["relevant_passages"] = [p for p in paragraphs if relevant.search(p)][:20]
    except (urllib.error.URLError, TimeoutError) as error:
        result["error"] = str(error)
    return name, result


def api(path):
    # Native CLI authentication; only selected public fields are retained below.
    process = subprocess.run(
        ["gh", "api", path], capture_output=True, text=True, timeout=45)
    if process.returncode:
        # gh errors contain status/operation, not request authorization headers.
        return {"error": process.stderr.strip()[:600], "exit_code": process.returncode}
    return json.loads(process.stdout)


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def main():
    assert os.environ.get("GITHUB_REPOSITORY") == REPOSITORY
    assert os.environ.get("GITHUB_REF") == "refs/heads/codex/direct-release-0.2.1"
    # Actions evaluates only presence. No signing secret value enters this job.
    bindings = {
        "LEFIL_SIGNING_KEYSTORE_BASE64": os.environ.pop("LEFIL_KEYSTORE_CONFIGURED") == "true",
        "LEFIL_SIGNING_STORE_PASSWORD": os.environ.pop("LEFIL_STORE_PASSWORD_CONFIGURED") == "true",
        "LEFIL_SIGNING_KEY_PASSWORD": os.environ.pop("LEFIL_KEY_PASSWORD_CONFIGURED") == "true",
    }
    app_inputs = ["app", "build.gradle", "settings.gradle", "gradle.properties"]
    same_inputs = all(git("rev-parse", f"HEAD:{p}") == git("rev-parse", f"{SOURCE}:{p}")
                      for p in app_inputs)
    report = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "run_url": f"https://github.com/{REPOSITORY}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
        "workflow_commit": os.environ["GITHUB_SHA"],
        "build_source_commit": SOURCE,
        "applicationId": "net.lefil.fdroid", "versionName": "0.2.1", "versionCode": 3,
        "app_build_inputs_unchanged": same_inputs,
        "expected_signing_bindings_present": bindings,
        "production_signing_key_created": False,
        "production_signature_validated": False,
        "production_release_published": False,
        "tests_or_builds_rerun": False,
        "notifications_validated": False,
        "physical_phone_validated": False,
    }
    endpoints = {
        "repository": f"repos/{REPOSITORY}",
        "releases": f"repos/{REPOSITORY}/releases?per_page=100",
        "repository_secret_metadata": f"repos/{REPOSITORY}/actions/secrets?per_page=100",
        "secret_encryption_key": f"repos/{REPOSITORY}/actions/secrets/public-key",
        "environments": f"repos/{REPOSITORY}/environments?per_page=100",
    }
    with ThreadPoolExecutor(max_workers=5) as pool:
        report["official_documentation"] = dict(pool.map(document, DOCUMENTS.items()))
        answers = dict(zip(endpoints, pool.map(api, endpoints.values())))
    repository = answers["repository"]
    report["github_repository_access"] = ({"error": repository["error"]} if "error" in repository
        else {k: repository.get(k) for k in ("full_name", "private", "permissions")})
    releases = answers["releases"]
    report["releases"] = releases if isinstance(releases, dict) else [
        {k: release.get(k) for k in ("tag_name", "draft", "prerelease", "html_url")}
        for release in releases]
    metadata = answers["repository_secret_metadata"]
    report["repository_secret_metadata"] = ({"error": metadata["error"]} if "error" in metadata
        else {"names": sorted(secret["name"] for secret in metadata.get("secrets", [])),
              "total_count": metadata.get("total_count")})
    encryption_key = answers["secret_encryption_key"]
    report["secret_public_key_api"] = ({"error": encryption_key["error"]}
        if "error" in encryption_key else {"readable": True})
    environments = answers["environments"]
    report["environments"] = ({"error": environments["error"]} if "error" in environments
        else {"names": [env["name"] for env in environments.get("environments", [])],
              "total_count": environments.get("total_count")})
    recipe = Path("docs/fdroid/net.lefil.fdroid.yml").read_text()
    report["fdroid_developer_signature_configured"] = {
        "Binaries": bool(re.search(r"^Binaries:", recipe, re.M)),
        "AllowedAPKSigningKeys": bool(re.search(r"^AllowedAPKSigningKeys:", recipe, re.M)),
        "cross_channel_updates_validated": False,
    }
    required = ("LEFIL_SIGNING_KEYSTORE_BASE64", "LEFIL_SIGNING_STORE_PASSWORD")
    report["required_signing_bindings"] = list(required)
    report["optional_signing_binding"] = "LEFIL_SIGNING_KEY_PASSWORD"
    report["state"] = ("requires_signing_validation" if all(bindings[name] for name in required)
                       else "expected_signing_bindings_not_configured")
    report["other_secret_names"] = (
        "Only metadata returned by the official secrets API can establish other binding names; "
        "this workflow does not enumerate the Actions secrets context.")
    report["github_actions_token_limit"] = (
        "A release-capable workflow token does not establish permission to store repository secrets.")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(f"::notice::Release prerequisites recorded; signing material created=false; published=false")
    assert same_inputs, "Release preflight must not alter the validated Android candidate"


if __name__ == "__main__":
    main()
