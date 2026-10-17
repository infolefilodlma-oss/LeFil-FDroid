#!/usr/bin/env python3
"""Check the release build using a disposable key; never makes a distributable APK."""
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zipfile


SOURCE = "25189d316d8345a601b4dae2fa9987d286831d1e"
PACKAGE = "net.lefil.fdroid"
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs/validation/direct-release"
WORK = Path(os.environ["RUNNER_TEMP"]) / "lefil-release-check"
PRIVATE = WORK / "private"
REPORT = OUTPUT / "release-checks.json"
TEST_APK = WORK / "INSTALLATION-CHECK-ONLY-NOT-FOR-DISTRIBUTION.apk"
TOOLS = Path(os.environ["ANDROID_HOME"]) / "build-tools/34.0.0"


def execute(args, cwd=None, log=None, timeout=600):
    result = subprocess.run(list(map(str, args)), cwd=cwd, capture_output=True, timeout=timeout)
    if log:
        (OUTPUT / log).write_bytes(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(f"{args[0]} failed ({result.returncode}); see {log or 'installation diagnostics'}")
    return result.stdout


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(report):
    assert not WORK.exists(), "Use a fresh runner directory"
    WORK.mkdir(mode=0o700)
    PRIVATE.mkdir(mode=0o700)
    paths = []
    for label in ("a", "b"):
        source = WORK / f"source-{label}"
        execute(["git", "clone", "--quiet", "--shared", ROOT, source])
        execute(["git", "checkout", "--quiet", "--detach", SOURCE], cwd=source)
        assert execute(["git", "rev-parse", "HEAD"], cwd=source).decode().strip() == SOURCE
        assert not execute(["git", "status", "--porcelain"], cwd=source).strip()
        execute(["gradle", "--no-daemon", "--max-workers=2", "--console=plain", ":app:assembleRelease"],
                cwd=source, log=f"release-build-{label}.log", timeout=900)
        apk = source / "app/build/outputs/apk/release/app-release-unsigned.apk"
        assert apk.is_file() and apk.stat().st_size > 0
        paths.append(apk)
    report["unsigned_apk_sha256"] = [digest(apk) for apk in paths]
    report["reproducibility"] = {
        "method": "Two clean checkouts of the identical pinned commit in different directories on one CI runner",
        "exact_unsigned_match": digest(paths[0]) == digest(paths[1]),
        "official_fdroid_server_verified": False,
    }
    if digest(paths[0]) != digest(paths[1]):
        with zipfile.ZipFile(paths[0]) as a, zipfile.ZipFile(paths[1]) as b:
            names = set(a.namelist()) | set(b.namelist())
            report["differing_zip_entries"] = sorted(n for n in names if n not in a.namelist()
                or n not in b.namelist() or a.read(n) != b.read(n))
        raise RuntimeError("Release APKs differ; no production signing or publication is allowed")
    badging = execute([TOOLS / "aapt", "dump", "badging", paths[0]], log="release-manifest.log").decode()
    assert re.search(r"package: name='net\.lefil\.fdroid' versionCode='3' versionName='0\.2\.1'", badging)
    assert "application-debuggable" not in badging
    execute([TOOLS / "zipalign", "-c", "4", paths[0]], log="release-alignment.log")
    report["manifest"] = {"applicationId": PACKAGE, "versionName": "0.2.1", "versionCode": 3,
                          "debuggable": False, "zip_alignment": "passed"}
    password = PRIVATE / "password"
    password.write_text(secrets.token_urlsafe(48))
    password.chmod(0o600)
    keystore = PRIVATE / "installation-only.p12"
    execute(["keytool", "-genkeypair", "-alias", "installation-check-only", "-keyalg", "RSA", "-keysize", "4096",
             "-validity", "2", "-storetype", "PKCS12", "-keystore", keystore, "-storepass:file", password,
             "-dname", "CN=Le Fil INSTALLATION TEST ONLY,OU=Disposable CI,O=Le Fil"], timeout=60)
    keystore.chmod(0o600)
    execute([TOOLS / "apksigner", "sign", "--ks", keystore, "--ks-key-alias", "installation-check-only",
             "--ks-pass", f"file:{password}", "--v1-signing-enabled", "true", "--v2-signing-enabled", "true",
             "--v3-signing-enabled", "true", "--v4-signing-enabled", "false", "--out", TEST_APK, paths[0]])
    certificates = execute([TOOLS / "apksigner", "verify", "--verbose", "--print-certs", "--min-sdk-version", "24", TEST_APK],
                           log="release-test-signature.log").decode()
    fingerprint = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]+)", certificates)
    assert fingerprint and "INSTALLATION TEST ONLY" in certificates
    report["installation_test_apk"] = {
        "sha256": digest(TEST_APK), "signer_sha256": fingerprint.group(1),
        "signature_verified": True, "key_type": "Ephemeral CI installation test key; valid two days; never for distribution",
        "production_signature_validated": False, "uploaded_or_published": False,
    }


def adb(*args, **kwargs):
    return execute(["adb", *args], timeout=kwargs.pop("timeout", 45), **kwargs)


def startup(label):
    adb("shell", "am", "force-stop", PACKAGE)
    launch = adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity",
                 log=f"release-{label}-start.log").decode()
    assert "Status: ok" in launch
    deadline = time.monotonic() + 50
    while time.monotonic() < deadline:
        adb("shell", "uiautomator", "dump", "/sdcard/lefil-release.xml")
        hierarchy = adb("exec-out", "cat", "/sdcard/lefil-release.xml")
        tree = ET.fromstring(hierarchy)
        nodes = list(tree.iter("node"))
        logo = any(n.get("class") == "android.widget.ImageView" and n.get("content-desc") == "Le Fil" for n in nodes)
        webview = any(n.get("class") == "android.webkit.WebView" for n in nodes)
        if logo and webview:
            pid = adb("shell", "pidof", PACKAGE).strip()
            assert pid
            time.sleep(3)
            assert adb("shell", "pidof", PACKAGE).strip() == pid
            (OUTPUT / f"release-{label}-hierarchy.xml").write_bytes(hierarchy)
            png = adb("exec-out", "screencap", "-p")
            assert png.startswith(b"\x89PNG\r\n\x1a\n")
            (OUTPUT / f"release-{label}.png").write_bytes(png)
            return {"status": "passed", "native_logo_and_webview_visible": True,
                    "pid_stable_during_check": True, "authenticated_services_validated": False}
        time.sleep(1)
    raise RuntimeError("Native release UI did not appear; no installation/startup validation claimed")


def install(report):
    hardware = adb("shell", "getprop", "ro.hardware").decode().strip()
    api = adb("shell", "getprop", "ro.build.version.sdk").decode().strip()
    assert hardware in ("ranchu", "goldfish") and api == "35", "Only a disposable Android 15 emulator may be used"
    assert digest(TEST_APK) == report["installation_test_apk"]["sha256"]
    adb("logcat", "-c")
    assert b"Success" in adb("install", str(TEST_APK), log="release-install.log", timeout=90)
    report["emulator"] = {"api": 35, "hardware": hardware, "fresh_install": "passed"}
    report["emulator"]["first_start"] = startup("first-start")
    assert b"Success" in adb("install", "-r", str(TEST_APK), log="release-reinstall.log", timeout=90)
    report["emulator"]["same_signature_reinstall"] = "passed"
    report["emulator"]["second_start"] = startup("second-start")
    adb("logcat", "-d", "-b", "crash", log="release-crash-buffer.log")
    report["outcome"] = "release_build_and_disposable_key_installation_checks_passed"


def main():
    phase = sys.argv[1]
    assert os.environ["GITHUB_REPOSITORY"] == "infolefilodlma-oss/LeFil-FDroid"
    if phase == "cleanup":
        if PRIVATE.is_dir():
            shutil.rmtree(PRIVATE)
        return
    OUTPUT.mkdir(parents=True, exist_ok=True)
    report = json.loads(REPORT.read_text()) if phase == "install" else {
        "build_source_commit": SOURCE, "workflow_commit": os.environ["GITHUB_SHA"],
        "run_url": "https://github.com/" + os.environ["GITHUB_REPOSITORY"] + "/actions/runs/" + os.environ["GITHUB_RUN_ID"],
        "outcome": "not_completed", "production_key_generated": False,
        "production_release_published": False, "physical_phone_validated": False,
        "real_notifications_validated": False, "existing_70_jvm_tests_rerun": False,
        "official_fdroid_signature_compatibility_validated": False,
    }
    try:
        {"build": build, "install": install}[phase](report)
    except Exception as error:
        report["outcome"] = "failed"
        report["failure_phase"] = phase
        report["failure"] = str(error)
        raise
    finally:
        REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
