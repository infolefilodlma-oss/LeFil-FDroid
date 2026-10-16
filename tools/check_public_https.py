#!/usr/bin/env python3
"""Anonymous HTTPS diagnosis for CI; never proves authenticated message delivery."""
import datetime
import html
import json
import os
import pathlib
import re
import subprocess
import tempfile
import urllib.parse

ENDPOINTS = [
    ("Agora", "https://social.le-fil.net/message"),
    ("Le Fil home", "https://le-fil.net/"),
    ("Le Fil home WebView", "https://le-fil.net/"),
    ("Agora NodeInfo", "https://social.le-fil.net/.well-known/nodeinfo"),
    ("Talk inbox", "https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex"),
    ("Atrium home", "https://team.le-fil.net/"),
    ("Talk list PHP", "https://team.le-fil.net/index.php?r=mail%2Fmail%2Fnotification-list"),
    ("Talk count PHP", "https://team.le-fil.net/index.php?r=mail%2Fmail%2Fget-new-message-count-json"),
    ("Talk list", "https://team.le-fil.net/mail/mail/notification-list"),
    ("Talk count", "https://team.le-fil.net/mail/mail/get-new-message-count-json"),
]


def annotation(value):
    return str(value).replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def check(label, url):
    user_agent = "LeFil-Public-CI/1.0"
    if label == "Le Fil home WebView":
        user_agent = "Mozilla/5.0 (Linux; Android 15; sdk_gphone64_x86_64 Build/AP3A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/125.0.6422.165 Mobile Safari/537.36"
    with tempfile.TemporaryDirectory(prefix="lefil-public-") as directory:
        body = pathlib.Path(directory, "body")
        headers = pathlib.Path(directory, "headers")
        result = subprocess.run([
            "curl", "--disable", "--silent", "--show-error", "--location", "--max-redirs", "5",
            "--proto", "=https", "--proto-redir", "=https", "--connect-timeout", "10",
            "--max-time", "30", "--max-filesize", "2097152",
            "--user-agent", user_agent, "--header", "Accept: text/html,application/json",
            "--dump-header", str(headers), "--output", str(body),
            "--write-out", "%{http_code}\\n%{url_effective}", url,
        ], capture_output=True, text=True, check=False)
        # curl interprets the literal \\n in its write-out format.
        status, _, effective = result.stdout.strip().partition("\n")
        parsed = urllib.parse.urlsplit(effective)
        safe_destination = urllib.parse.urlunsplit((parsed.scheme, parsed.hostname or "", parsed.path, "", ""))
        contents = body.read_text(errors="replace") if body.exists() else ""
        header_text = headers.read_text(errors="replace") if headers.exists() else ""
        title = re.search(r"<title[^>]*>([\s\S]*?)</title>", contents, re.I)
        page_title = re.sub(r"\s+", " ", html.unescape(title.group(1))).strip()[:120] if title else ""
        chain = re.findall(r"^HTTP/\S+ (\d{3})", headers.read_text(errors="replace") if headers.exists() else "", re.M)
        record = {
            "label": label, "url": url, "curl_exit": result.returncode,
            "http_status": status, "http_chain": chain, "destination": safe_destination,
            "page_title": page_title,
            "body_bytes": body.stat().st_size if body.exists() else 0,
            "server": re.findall(r"^server:\s*([^\r\n]+)", header_text, re.I | re.M)[-1:],
            "login_form": bool(re.search(r"(?:type|name)\s*=\s*['\"]password['\"]", contents, re.I)),
            "friendica_marker": "friendica" in contents.lower(),
            "humhub_marker": "humhub" in contents.lower(),
            "tls_verified_response": result.returncode == 0 and status.isdigit() and status != "000",
        }
        # Store only public software metadata, never page bodies, cookies or tokens.
        if label in {"Agora NodeInfo", "Agora software"} and status == "200":
            try:
                payload = json.loads(contents)
                if not isinstance(payload, dict):
                    raise ValueError("Not an object")
                if label == "Agora software":
                    software = payload.get("software", {})
                    record["software"] = {key: str(software[key])[:100] for key in ["name", "version"] if key in software}
                record["nodeinfo_links"] = [link["href"] for link in payload.get("links", [])
                    if urllib.parse.urlsplit(link.get("href", "")).hostname == "social.le-fil.net"
                    and urllib.parse.urlsplit(link.get("href", "")).scheme == "https"]
            except (ValueError, TypeError, KeyError):
                record["nodeinfo_json"] = False
        message = (f"curl_exit={record['curl_exit']}; HTTP={status}; chain={','.join(chain)}; "
                   f"destination={safe_destination}; login_form={record['login_form']}; "
                   f"Friendica={record['friendica_marker']}; HumHub={record['humhub_marker']}; "
                   f"software={record.get('software', {})}; title={page_title}; server={record['server']}; bytes={record['body_bytes']}")
        print(f"::notice title={label} HTTPS public::{annotation(message)}", flush=True)
        return record


def main():
    records = [check(label, url) for label, url in ENDPOINTS]
    nodeinfo = next(record for record in records if record["label"] == "Agora NodeInfo")
    links = nodeinfo.get("nodeinfo_links", [])
    if links:
        uri = urllib.parse.urlsplit(links[0])
        if uri.username is None and uri.port in (None, 443) and not uri.query and not uri.fragment:
            records.append(check("Agora software", links[0]))
    report = {"checked_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "authenticated": False, "notification_delivery_validated": False, "checks": records}
    output = pathlib.Path("public-https-report.json")
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary:
            summary.write("## Anonymous HTTPS checks\n\nNo accounts or cookies supplied. This does not validate message detection or notifications.\n\n")
            summary.write("| Endpoint | curl exit | HTTP | Login form | Destination |\n|---|---:|---|---|---|\n")
            for record in records:
                summary.write(f"| {html.escape(record['label'])} | {record['curl_exit']} | {record['http_status']} | {record['login_form']} | {html.escape(record['destination'])} |\n")
    # Authentication refusals are HTTP responses, not proof of an authenticated session.
    # Transport errors and missing/broken required routes must remain visible failures.
    def available(record):
        return record["tls_verified_response"] and record["http_status"] in {"200", "401", "403"}
    # The Android reader already supports both pretty URLs and index.php routing.
    return 0 if (available(records[0]) and
                 all(record["http_status"] == "200" and record["body_bytes"] > 0 for record in records if record["label"].startswith("Le Fil home")) and
                 any(available(record) for record in records if record["label"] == "Talk inbox") and
                 any(available(record) for record in records if record["label"] in {"Talk list", "Talk list PHP"}) and
                 any(available(record) for record in records if record["label"] in {"Talk count", "Talk count PHP"})) else 1



if __name__ == "__main__":
    raise SystemExit(main())
