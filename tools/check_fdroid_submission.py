#!/usr/bin/env python3
"""Check the official build recipe without publishing an APK or using accounts."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import urllib.request

import yaml


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--work-dir", required=True, type=Path)
    parser.add_argument("--build", action="store_true")
    args = parser.parse_args()
    work = args.work_dir.resolve()
    if work.exists() and any(work.iterdir()):
        parser.error("Use an empty work directory so stale results cannot pass")
    root = Path(__file__).resolve().parents[1]
    recipe = root / "docs/fdroid/net.lefil.fdroid.yml"
    app = yaml.safe_load(recipe.read_text())
    build = app["Builds"][0]
    appid = recipe.stem
    commit = build["commit"]
    assert re.fullmatch(r"[0-9a-f]{40}", commit), "Pin a complete source commit"
    assert app["Repo"] == "https://github.com/infolefilodlma-oss/LeFil-FDroid.git"
    work.mkdir(parents=True, exist_ok=True)
    (work / "metadata").mkdir()
    (work / "config").mkdir()
    shutil.copyfile(recipe, work / "metadata" / recipe.name)
    config = {}
    if args.build:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        assert sdk and Path(sdk).is_dir(), "An Android SDK is required for --build"
        gradle = shutil.which("gradle")
        assert gradle, "Gradle 8.7 must be on PATH"
        config = {"sdk_path": sdk, "gradle": gradle,
                  "java_paths": {"17": os.environ["JAVA_HOME"]}}
    (work / "config.yml").write_text(yaml.safe_dump(config))
    report = {"source_commit": commit, "versionName": str(build["versionName"]),
              "versionCode": build["versionCode"], "outcome": "failed",
              "apk_build": "not run", "submitted": False, "accepted": False,
              "config_sha256": {}}

    def fetch_config(name):
        assert name == Path(name).name, "Config assets must be plain filenames"
        url = "https://gitlab.com/fdroid/fdroiddata/-/raw/master/config/" + name
        with urllib.request.urlopen(url, timeout=45) as response:
            data = response.read()
        (work / "config" / name).write_bytes(data)
        return name, hashlib.sha256(data).hexdigest()

    fdroid = str(Path(sys.executable).with_name("fdroid"))

    def run(name, arguments):
        log = work / (name + ".log")
        with log.open("w") as stream:
            result = subprocess.run(arguments, cwd=work, stdout=stream,
                                    stderr=subprocess.STDOUT, check=False)
        if result.returncode:
            print(log.read_text()[-12000:], file=sys.stderr)
            raise RuntimeError(f"{name} exited with {result.returncode}")
        return log.read_text()

    try:
        icons = set()
        for name in ("categories.yml", "antiFeatures.yml"):
            key, digest = fetch_config(name)
            report["config_sha256"][key] = digest
            for item in yaml.safe_load((work / "config" / name).read_text()).values():
                if isinstance(item, dict) and item.get("icon"):
                    icons.add(item["icon"])
        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
            report["config_sha256"].update(pool.map(fetch_config, sorted(icons)))
        run("readmeta", [fdroid, "readmeta"])
        report["readmeta"] = "passed"
        run("lint", [fdroid, "lint", "-f", appid])
        report["lint"] = "passed"
        if args.build:
            source = work / "build" / appid
            source.parent.mkdir()
            run("clone", ["git", "clone", "--quiet", app["Repo"], str(source)])
            run("checkout", ["git", "-C", str(source), "checkout", "--detach", commit])
            # fdroidserver 2.4.5 reads SOURCE_DATE_EPOCH before its clone step.
            # Seed its normal build cache with the selected revision first.
            (source.parent / (".fdroidvcs-" + appid)).write_text("git " + app["Repo"])
            log = run("build", [fdroid, "build", "--test", "--scan-binary",
                                "--no-tarball", f"{appid}:{build['versionCode']}"])
            apk = work / "tmp" / f"{appid}_{build['versionCode']}.apk"
            # Some build failures are reported with exit code zero by this tool.
            assert "1 build succeeded" in log and f"from {commit}" in log
            assert apk.is_file() and apk.stat().st_size > 0
            actual = subprocess.check_output(
                ["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
            assert actual == commit, "The build did not use the pinned source"
            report.update(apk_build="passed", source_scan="passed",
                          binary_scan="passed", apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest())
        report["outcome"] = "passed"
        print(f"::notice title=Le Fil F-Droid checks::source={commit}; "
              f"readmeta=passed; lint=passed; build={report['apk_build']}; "
              "official_submission=false")
    finally:
        (work / "result.json").write_text(json.dumps(report, indent=2) + "\n")


if __name__ == "__main__":
    main()
