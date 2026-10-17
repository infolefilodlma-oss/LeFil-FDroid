# New app: Le Fil

DRAFT PREPARED UPSTREAM — NOT SUBMITTED. Policy eligibility and functional acceptance below remain open. This text must not be posted as a claim that the candidate has passed them.

## Abstract

Le Fil 0.2.1 (versionCode 3, `net.lefil.fdroid`) provides an Android interface to the Le Fil services: Agora (Friendica), Atrium (HumHub) and its Mail/Talk module. Talk is HumHub messaging. The application contains a native service menu and native recovery after WebView renderer loss. A real Android 15 emulator test checked two renderer terminations, recovery, retry and service selection using local HTML.

Optional background message polling is present, disabled by default and explicitly experimental in the corrected upstream descriptions. Authenticated message detection, notification delivery and opening private conversations from actual alerts remain unvalidated on the live services. The application is tied to the Le Fil instances; `TetheredNet` is declared. The native application has no GMS/Firebase dependency.

The intended value over browsing the sites is direct native service navigation and renderer recovery, plus an optional native communication layer whose actual behavior is still awaiting acceptance. These features and the usefulness of this website wrapper remain subject to F-Droid review. Two actual, unedited Android 15 emulator screenshots are now included in upstream Fastlane, with recorded provenance. They show the native light/dark toolbar and public homepage, without authenticated accounts.

Sources: https://github.com/infolefilodlma-oss/LeFil-FDroid
Build revision: `25189d316d8345a601b4dae2fa9987d286831d1e`.
Issue tracker/contact: https://github.com/infolefilodlma-oss/LeFil-FDroid/issues

## Checklist

### Policy

- [ ] The app complies with all inclusion criteria. **Open:** the public history checked starts on 2026-10-14. The three-month criterion or an applicable exception needs review; a project representative has reported testing a failing candidate, which may support examining the user exception but does not establish an independently verified user base. The policy gives no numerical user threshold and does not explicitly exclude test versions. Live communication acceptance is also outstanding.
- [x] The original author is notified and requests inclusion: [upstream issue #1](https://github.com/infolefilodlma-oss/LeFil-FDroid/issues/1), opened by `infolefilodlma-oss`, explicitly requests publication in the official F-Droid catalog. This is also the copyright holder declared in `LICENSE`.
- [x] Upstream Fastlane metadata contains summaries, descriptions and versionCode 3 changelogs in `en-US` and `fr-FR`.

### Docs

- [x] Reviewed the official `fdroiddata/CONTRIBUTING.md` and App inclusion MR template.
- [x] Reviewed Inclusion Policy, Build Metadata Reference, Quick Start Guide and description/graphics guidance.
- [x] Metadata parsed with `fdroid readmeta` and checked with `fdroid lint -f`.

### Merge Request Setup

- [x] Proposed title: `New app: Le Fil`.
- [ ] A public fdroiddata contribution fork and an unprotected source branch are available. **Open:** no authenticated GitLab account is available in the execution environment.
- [x] Only one app and one file are proposed: `metadata/net.lefil.fdroid.yml`.
- [x] Searches by exact package ID found no fdroiddata metadata, MR or RFP entry at the time of this preparation. These searches do not claim exhaustive name-based matching.
- [ ] CI on the GitLab contribution fork has passed; an actual MR is open. **Not run / not created.** Upstream GitHub checks are separate evidence.

### Metadata

- [x] Canonical YAML with LF line endings, including `AuthorName`, public sources and issue tracker.
- [x] Summaries, full descriptions and changelogs remain upstream. No screenshots or other publication assets are added to fdroiddata.
- [ ] Stable release is tagged. **Open:** version 0.2.1 is still a candidate. Tag-based automatic update settings are prepared.
- [x] Only the current candidate build is proposed, pinned to a full public commit.
- [x] `TetheredNet` has localized explanations for the fixed server instances.
- [x] Automatic Google Fonts requests, WordPress emoji resources and enabled third-party cookies are disclosed in the upstream descriptions and recipe notes. The native library graph has 68 selected modules, each with an Apache 2.0 POM declaration. Remote web content is a separate scope.
- [x] No scanner bypasses, disabled builds, bundled proprietary SDKs or external source repositories are introduced by this metadata.
- [ ] Independent reproducible binary comparison with an upstream release signature is enabled. **Not performed:** there is no official signed upstream release to compare. F-Droid signing is intended; the official Quick Start Guide treats independent reproducible builds as recommended, not required. A debug certificate is not proposed for official signing.
- [x] ABI splits are unnecessary for this small Java/Kotlin application using the system WebView; no app-specific native ABI variants are introduced.

## Evidence and open acceptance

The already validated Android implementation is preserved: 70 JVM tests, one native Android 15 emulator test, Lint with zero errors, and prior F-Droid source/binary scans. The new application resources use the official logo and select its light/dark variant in the native toolbar. Java/Kotlin production sources, test sources and Gradle declarations remain identical to validated revision `c85f5a3cbb78ab58e6207f770cd9f6df60fb818d`. The 70-test JVM suite and native test have passed again; the exact pinned F-Droid source was built and scanned successfully. Current recipe check results and official documentary references are maintained in the upstream `docs/fdroid/` directory on `codex/fdroid-submission-readiness`.

The native test used local HTML and is not evidence of live account sessions or notifications. A separate acceptance recipe is prepared for authorized test accounts and an Android emulator; the project representative is not asked to install a manual APK. Identical messages, pagination, actual alert routing, background delivery and restart recovery remain unvalidated.

The existing HTTPS artifact was recovered without rerunning that check. An anonymous browser observation confirms the public Friendica and HumHub login gates and records automatic requests to fonts.googleapis.com, fonts.gstatic.com and s.w.org. A separate request-blocking check leaves the public service links and Atrium password field visible without Google Fonts; it does not prove authenticated independence. The public homepage contains a Site Kit generator marker, but no analytics request was identified within the bounded observation. These facts and limits are exposed for Anti-Feature review; neither the marker alone nor the native dependency graph establishes whether authenticated web content tracks users. Official upstream licenses for WordPress, Friendica, HumHub and Mail were recorded without claiming an audit of the deployed servers. See upstream docs/fdroid/NETWORK-DEPENDENCIES.md and docs/validation/fdroid-finalization/.

This finalization changes publication descriptions and evidence only. The complete app tree, build inputs and existing tests match the already validated candidate. The 70 JVM tests and native Android test were reused, not rerun. The updated metadata and pinned publication source are checked separately by the F-Droid recipe workflow.

The official blue-on-white PNG supplied by the project is now included byte-for-byte in the Android launcher resources and Fastlane icons. The existing transparent light-lettered variant is preserved for the dark native toolbar. Actual Android 15 home, app drawer and native light/dark captures have been inspected. Other launchers and themed icon modes are untested; the published listing cannot be checked before publication. No logo has been recreated or recolored.

No claim is made that GitLab CI, inclusion, publication or phone notification acceptance have passed. Resolve the policy and functionality items before marking the inclusion checklist complete or posting this as an eligible submission.
