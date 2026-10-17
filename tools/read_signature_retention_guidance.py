#!/usr/bin/env python3
"""Read public official guidance only; no keystore or application session access."""
from datetime import datetime, timezone
import hashlib
from html.parser import HTMLParser
import json
import os
from pathlib import Path
import re
import urllib.request


class Paragraphs(HTMLParser):
    def __init__(self):
        super().__init__()
        self.parts = []
        self.skip = 0

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style"):
            self.skip += 1
        if tag in ("p", "li", "h2", "h3", "br"):
            self.parts.append("\n")

    def handle_endtag(self, tag):
        if tag in ("script", "style"):
            self.skip = max(0, self.skip - 1)
        if tag in ("p", "li", "h2", "h3"):
            self.parts.append("\n")

    def handle_data(self, data):
        if not self.skip:
            self.parts.append(data)


urls = {
    "android_app_signing": "https://developer.android.com/studio/publish/app-signing",
    "owasp_key_management": "https://cheatsheetseries.owasp.org/cheatsheets/Key_Management_Cheat_Sheet.html",
    "fdroid_developer_signatures": "https://f-droid.org/docs/Reproducible_Builds/",
}
report = {"retrieved_at_utc": datetime.now(timezone.utc).isoformat(),
          "run_url": f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
          "private_signing_material_used": False, "application_tests_rerun": False, "sources": {}}
for name, url in urls.items():
    request = urllib.request.Request(url, headers={"User-Agent": "LeFil-key-retention-review"})
    with urllib.request.urlopen(request, timeout=45) as response:
        data = response.read()
        assert response.status == 200
    parser = Paragraphs()
    parser.feed(data.decode(errors="replace"))
    paragraphs = [re.sub(r"\s+", " ", p).strip() for p in "".join(parser.parts).split("\n")]
    terms = r"lost|lose|backup|back up|recover|same.{0,30}(key|certificate)|AllowedAPKSigningKeys|Binaries|secure location|protect.{0,40}key"
    excerpts = list(dict.fromkeys(p for p in paragraphs if re.search(terms, p, re.I) and 20 < len(p) < 2200))
    report["sources"][name] = {"url": url, "http_status": 200,
                              "document_sha256": hashlib.sha256(data).hexdigest(),
                              "relevant_paragraphs": excerpts[:30]}
output = Path("docs/validation/distribution-assurance-0.2.1/official-guidance.json")
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
