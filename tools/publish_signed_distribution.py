#!/usr/bin/env python3
"""Publish only the exact APK that passed the final certificate/install checks."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request

from check_signed_distribution import CERTIFICATE, ROOT, SHA256, SOURCE


REPOSITORY = "infolefilodlma-oss/LeFil-FDroid"
TAG = "v0.2.1"
VERSION_URL = f"https://github.com/{REPOSITORY}/releases/download/{TAG}/LeFil.apk"
LATEST_URL = f"https://github.com/{REPOSITORY}/releases/latest/download/LeFil.apk"
OUTPUT = ROOT / "docs/validation/signed-distribution/publication.json"


def gh(*args, check=True):
    result = subprocess.run(["gh", *args], capture_output=True, text=True, timeout=180)
    if check and result.returncode:
        raise RuntimeError(result.stderr.strip() or "GitHub operation failed")
    return result


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "LeFil-release-verification",
                                                   "Cache-Control": "no-cache"})
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
        assert response.status == 200
    digest = hashlib.sha256(data).hexdigest()
    assert digest == SHA256, "Downloaded APK differs from the tested signed APK"
    return {"url": url, "http_status": 200, "bytes": len(data), "sha256": digest}


def main():
    assert os.environ["GITHUB_REPOSITORY"] == REPOSITORY
    assert os.environ["GITHUB_REF"] == "refs/heads/codex/direct-release-0.2.1"
    validation = json.loads((ROOT / "docs/validation/signed-distribution/result.json").read_text())
    assert validation["outcome"] == "final_signed_apk_installation_checks_passed"
    assert validation["installed_apk_sha256"] == validation["sha256"] == SHA256
    assert validation["certificate_sha256"] == CERTIFICATE
    assert validation["source_commit"] == SOURCE
    assert validation["fresh_install"] == validation["same_certificate_reinstall"] == "passed"
    apk = ROOT / "distribution/LeFil.apk"
    assert hashlib.sha256(apk.read_bytes()).hexdigest() == SHA256
    # Refuse a mismatched pre-existing tag; never alter a release or tag belonging to another build.
    tags = subprocess.check_output(["git", "ls-remote", "origin", f"refs/tags/{TAG}",
                                    f"refs/tags/{TAG}^{{}}"], text=True).splitlines()
    if tags:
        resolved = next((line.split()[0] for line in tags if line.endswith("^{}")), tags[0].split()[0])
        assert resolved == SOURCE, "Existing tag points to a different source"
    notes = Path(os.environ["RUNNER_TEMP"]) / "lefil-release-notes.md"
    notes.write_text("""Application Android Le Fil 0.2.1, pour Agora, Atrium/Talk et Radio.

Téléchargement Android 7.0 et versions suivantes : **LeFil.apk** ci-dessous.

APK de diffusion non débogable, signé avec le certificat de diffusion Le Fil.
Le fichier exact a passé les contrôles de signature, l'installation, la réinstallation
et deux démarrages dans un émulateur Android 15. Le contenu avant signature correspond
exactement aux deux compilations déjà contrôlées. Les 70 tests JVM antérieurs sont conservés ;
la création du tag a également déclenché la CI standard existante.

Limites déclarées : notifications réelles Agora/Talk, comportement en veille/après
redémarrage et téléphone physique non validés. La candidature F-Droid !52965 reste
distincte de cette diffusion ; cette Release ne constitue pas une admission F-Droid.
Une mise à jour automatique depuis une autre signature (ancien APK de débogage,
ou future signature F-Droid différente) n'est pas garantie.

Sources de cette version : commit 25189d316d8345a601b4dae2fa9987d286831d1e, licence MIT.
Le certificat fourni est public. Aucune clé privée ni aucun mot de passe n'est publié.
La clé privée chiffrée reste dans l'environnement privé du projet ; sa sauvegarde
indépendante hors de cet environnement n'est pas encore vérifiée.
""")
    report = {"source_commit": SOURCE, "tag": TAG, "apk_sha256": SHA256,
              "certificate_sha256": CERTIFICATE, "installation_checks": validation["run_url"],
              "private_key_uploaded": False, "external_durable_key_retention_verified": False,
              "published": False, "outcome": "not_completed"}
    was_published = False
    try:
        report["phase"] = "read_release_metadata"
        # The list endpoint also returns drafts to this authorized repository token.
        releases = json.loads(gh("api", f"repos/{REPOSITORY}/releases?per_page=100").stdout)
        release = next((r for r in releases if r["tag_name"] == TAG), None)
        if release is not None:
            # The resolved existing tag checked above determines the source.
            # target_commitish is ignored by GitHub when that tag already exists.
            assert tags, "Existing release has no verified source tag"
            assert not release["prerelease"], "Refuse to alter another release configuration"
            was_published = not release["draft"]
        else:
            report["phase"] = "create_draft_from_existing_tag"
            assert tags, "The source tag must exist before creating a release"
            gh("release", "create", TAG, "--repo", REPOSITORY,
               "--verify-tag", "--draft", "--title", "Le Fil 0.2.1", "--notes-file", str(notes))
            releases = json.loads(gh("api", f"repos/{REPOSITORY}/releases?per_page=100").stdout)
            release = next(r for r in releases if r["tag_name"] == TAG)
        assets = [apk, ROOT / "distribution/LeFil.apk.sha256",
                  ROOT / "docs/release/LeFil-signing-certificate.pem",
                  ROOT / "docs/validation/signed-distribution/result.json"]
        assert all(p.is_file() for p in assets)
        existing_names = {asset["name"] for asset in release["assets"]}
        missing = [str(p) for p in assets if p.name not in existing_names]
        if missing:
            assert release["draft"], "Never alter assets of an already published release"
            report["phase"] = "upload_signed_assets_to_draft"
            gh("release", "upload", TAG, *missing, "--repo", REPOSITORY)
        downloaded = Path(os.environ["RUNNER_TEMP"]) / "lefil-draft-asset"
        downloaded.mkdir(exist_ok=False)
        report["phase"] = "download_and_verify_draft_asset"
        gh("release", "download", TAG, "--repo", REPOSITORY, "--pattern", "LeFil.apk",
           "--dir", str(downloaded))
        assert hashlib.sha256((downloaded / "LeFil.apk").read_bytes()).hexdigest() == SHA256
        report["draft_asset_download_sha256_verified"] = True
        if release["draft"]:
            report["phase"] = "publish_verified_release"
            gh("release", "edit", TAG, "--repo", REPOSITORY, "--draft=false", "--latest")
        report["phase"] = "verify_anonymous_public_downloads"
        report["downloads"] = [fetch(VERSION_URL), fetch(LATEST_URL)]
        final = json.loads(gh("api", f"repos/{REPOSITORY}/releases/tags/{TAG}").stdout)
        assert not final["draft"] and not final["prerelease"]
        final_tags = subprocess.check_output(["git", "ls-remote", "origin", f"refs/tags/{TAG}",
                                             f"refs/tags/{TAG}^{{}}"], text=True).splitlines()
        resolved = next((line.split()[0] for line in final_tags if line.endswith("^{}")),
                        final_tags[0].split()[0])
        assert resolved == SOURCE, "Published tag does not match the built commit"
        report.update(published=True, release_url=final["html_url"],
                      direct_download_url=VERSION_URL, latest_download_url=LATEST_URL,
                      outcome="published_and_anonymously_download_verified", phase="verified")
    except Exception as error:
        report.update(outcome="failed", failure=str(error))
        if not was_published:
            rollback = gh("release", "edit", TAG, "--repo", REPOSITORY, "--draft=true", check=False)
            report["rollback_to_draft_succeeded"] = rollback.returncode == 0
        raise
    finally:
        OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
