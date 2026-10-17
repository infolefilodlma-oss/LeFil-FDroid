#!/usr/bin/env python3
"""Verify and install the already signed APK; no signing key is used in CI."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / "distribution/LeFil.apk"
OUTPUT = ROOT / "docs/validation/signed-distribution"
REPORT = OUTPUT / "result.json"
PACKAGE = "net.lefil.fdroid"
SOURCE = "25189d316d8345a601b4dae2fa9987d286831d1e"
SHA256 = "b0141391fad9fe1cfea638fd33a69d4fee1839f14747320820856577f57d4cb2"
CERTIFICATE = "d0755310dbd84319bbd0cb658475168cb9234cd1b60f9893a092afa19c31b902"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def execute(args, name, timeout=90):
    result = subprocess.run(list(map(str, args)), capture_output=True, timeout=timeout)
    (OUTPUT / (name + ".log")).write_bytes(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(f"{name} failed with exit code {result.returncode}")
    return result.stdout.decode(errors="replace")


def adb(*args, name, timeout=90):
    return execute(["adb", *args], name, timeout)


def verify(report):
    assert digest(APK) == SHA256, "Unexpected signed APK"
    tools = Path(os.environ["ANDROID_HOME"]) / "build-tools/34.0.0"
    output = execute([tools / "apksigner", "verify", "--verbose", "--print-certs",
                      "--min-sdk-version", "24", "--max-sdk-version", "35", APK], "signature")
    assert f"Signer #1 certificate SHA-256 digest: {CERTIFICATE}" in output
    assert "Verified using v2 scheme (APK Signature Scheme v2): true" in output
    assert "Verified using v3 scheme (APK Signature Scheme v3): true" in output
    assert "Number of signers: 1" in output
    badging = execute([tools / "aapt", "dump", "badging", APK], "manifest")
    assert "name='net.lefil.fdroid' versionCode='3' versionName='0.2.1'" in badging
    assert "application-debuggable" not in badging
    assert "sdkVersion:'24'" in badging and "targetSdkVersion:'35'" in badging
    execute([tools / "zipalign", "-c", "4", APK], "alignment")
    report.update(signature_verification="passed", certificate_sha256=CERTIFICATE,
                  sha256=SHA256, debuggable=False, alignment="passed")


def hierarchy(label, report):
    # Some Android 15 dumps finish writing XML before their app_process exits 137.
    # Remove the previous file so no stale dump can satisfy the UI assertions.
    destination = "/sdcard/lefil-signed.xml"
    adb("shell", "rm", "-f", destination, name=label + "-remove-old-hierarchy")
    result = subprocess.run(["adb", "shell", "uiautomator", "dump", destination],
                            capture_output=True, timeout=45)
    (OUTPUT / (label + "-dump.log")).write_bytes(result.stdout + result.stderr)
    report.setdefault("ui_export_exit_codes", {})[label] = result.returncode
    if result.returncode not in (0, 137):
        raise RuntimeError(f"{label} UI export failed with exit code {result.returncode}")
    if result.returncode == 137:
        assert b"UI hierchary dumped to:" in result.stdout
        adb("logcat", "-d", "-b", "crash", name=label + "-export-crash-diagnostic")
        adb("shell", "dumpsys", "meminfo", PACKAGE, name=label + "-export-memory-diagnostic")
    xml = adb("exec-out", "cat", destination, name=label + "-hierarchy")
    return ET.fromstring(xml)


def start(label, report):
    adb("shell", "am", "force-stop", PACKAGE, name=label + "-stop")
    launch = adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity",
                 name=label + "-start")
    assert "Status: ok" in launch
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        nodes = list(hierarchy(label, report).iter("node"))
        native_logo = any(n.get("class") == "android.widget.ImageView"
                          and n.get("content-desc") == "Le Fil" for n in nodes)
        webview = any(n.get("class") == "android.webkit.WebView" for n in nodes)
        if native_logo and webview:
            pid = adb("shell", "pidof", PACKAGE, name=label + "-pid").strip()
            assert pid
            time.sleep(5)
            assert adb("shell", "pidof", PACKAGE, name=label + "-pid-stable").strip() == pid
            capture = subprocess.run(["adb", "exec-out", "screencap", "-p"],
                                     capture_output=True, check=True, timeout=45).stdout
            assert capture.startswith(b"\x89PNG\r\n\x1a\n")
            (OUTPUT / (label + ".png")).write_bytes(capture)
            return {"status": "passed", "native_logo_and_webview_visible": True,
                    "stable_process": True, "authenticated_services_validated": False}
        time.sleep(1)
    raise RuntimeError("Expected native UI did not appear")


def install(report):
    assert report.get("signature_verification") == "passed" and digest(APK) == SHA256
    sdk = adb("shell", "getprop", "ro.build.version.sdk", name="emulator-sdk").strip()
    hardware = adb("shell", "getprop", "ro.hardware", name="emulator-hardware").strip()
    assert sdk == "35" and hardware in ("ranchu", "goldfish"), "Use a disposable Android 15 emulator"
    adb("logcat", "-c", name="clear-logcat")
    assert "Success" in adb("install", APK, name="fresh-install")
    report["fresh_install"] = "passed"
    report["first_start"] = start("first", report)
    assert "Success" in adb("install", "-r", APK, name="same-certificate-reinstall")
    report["same_certificate_reinstall"] = "passed"
    report["second_start"] = start("second", report)
    installed = adb("shell", "pm", "path", PACKAGE, name="installed-path").strip()
    assert installed.startswith("package:") and "\n" not in installed
    installed_apk = Path(os.environ["RUNNER_TEMP"]) / "lefil-installed.apk"
    adb("pull", installed.removeprefix("package:"), installed_apk, name="read-installed-apk")
    assert digest(installed_apk) == SHA256, "Installed APK differs from the release asset"
    report["installed_apk_sha256"] = digest(installed_apk)
    crash = adb("logcat", "-d", "-b", "crash", name="crash-buffer")
    assert not re.search(r"Process:\s*net\.lefil\.fdroid\b", crash), "Application crashed"
    report.update(android_api=35, hardware=hardware, application_crash_detected=False,
                  outcome="final_signed_apk_installation_checks_passed")


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    phase = sys.argv[1]
    report = json.loads(REPORT.read_text()) if phase == "install" else {
        "source_commit": SOURCE, "version_name": "0.2.1", "version_code": 3,
        "run_url": "https://github.com/" + os.environ["GITHUB_REPOSITORY"]
                   + "/actions/runs/" + os.environ["GITHUB_RUN_ID"],
        "workflow_commit": os.environ["GITHUB_SHA"], "outcome": "not_completed",
        "production_private_key_available_to_ci": False,
        "previous_70_jvm_tests_rerun": False, "application_rebuilt_in_ci": False,
        "physical_phone_validated": False, "real_notifications_validated": False,
        "authenticated_services_validated": False,
        "official_fdroid_signature_compatibility_validated": False,
        "external_durable_key_retention_verified": False,
    }
    try:
        {"verify": verify, "install": install}[phase](report)
    except Exception as error:
        report.update(outcome="failed", failure_phase=phase, failure=str(error))
        raise
    finally:
        REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
