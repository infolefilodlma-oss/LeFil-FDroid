# Compilation et mises à jour F-Droid

Application : `net.lefil.fdroid`, version candidate `0.2.1` / `3`, minimum Android 7 (API 24), cible et compilation API 35. Licence déclarée du dépôt : MIT (`LICENSE`), ressources comprises sauf indication contraire. Aucun avis distinct restreignant le logo n'a été relevé dans les pièces examinées.

La candidature officielle utilise [le dossier de soumission](SUBMISSION.md). Les 70 tests JVM et le test natif acquis sont conservés. Les ressources officielles et le logo de la barre native ont déjà été intégrés et contrôlés. Cette reprise corrige seulement les déclarations et pièces de candidature : l'arbre `app`, les tests et les entrées Gradle restent identiques, donc les tests Android ne sont pas recommencés. Aucun essai sur téléphone ni réception réelle ne sont déduits de ces résultats.

## Contrôler la recette de soumission

Le commit de candidature `25189d316d8345a601b4dae2fa9987d286831d1e` contient les descriptions réseau corrigées, les originaux graphiques officiels, les icônes et deux captures réelles dans Fastlane. La recette y est épinglée pour que F-Droid récupère ces fichiers. Le [bilan](CHECKS.json) relie les rapports JUnit, le contrôle natif, les images et le build F-Droid à cette même arborescence applicative. `TetheredNet`, `AuthorName` et les notes sur les ressources Web tierces figurent dans les métadonnées officielles.

Avec fdroidserver 2.4.5 dans un environnement virtuel Python, vérifier uniquement les métadonnées dans un répertoire de travail vide :

```sh
python tools/check_fdroid_submission.py --work-dir /tmp/lefil-fdroid-metadata
```

Pour construire cette recette précise et scanner ses sources et son APK, ajouter `--build` avec JDK 17, Gradle 8.7 et le SDK requis. Le workflow `.github/workflows/fdroid-submission.yml` prépare cet environnement, exécute `readmeta`, `lint -f` et `build --test --scan-binary --no-tarball`, puis conserve seulement les rapports. Il ne publie pas de paquet et ne relance pas les tests Android inchangés.

Le script vérifie le commit réellement construit, l'existence du nouvel artefact et le résultat `1 build succeeded` : le code de sortie de fdroidserver seul ne suffit pas. Ses logs et `result.json` rendent les contrôles vérifiables. Les résultats sont distincts de la CI de la future MR GitLab et d'une admission F-Droid.

## Reproduire les contrôles

Outils : OpenJDK 17, Gradle 8.7, Android SDK platform 35 et build-tools 34.0.0. Le projet ne fournit pas de wrapper Gradle ; la CI installe explicitement Gradle 8.7. Utiliser un JDK libre (pas Oracle JDK).

```sh
cd LeFil-FDroid
# ANDROID_HOME doit pointer sur le SDK ; Java 17 et Gradle 8.7 doivent être dans PATH.
gradle --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Le workflow de candidature installe ces outils avec leurs versions explicites. Les paramètres locaux du proxy et de confiance TLS restent hors du dépôt. Ne jamais désactiver TLS ni incorporer des secrets dans Gradle.

Résultats :

- Tests JUnit : `app/build/test-results/testDebugUnitTest/TEST-*.xml` et rapport HTML `app/build/reports/tests/testDebugUnitTest/index.html`.
- Lint : `app/build/reports/lint-results-debug.xml` et `.html`.
- Essai installable : `app/build/outputs/apk/debug/app-debug.apk`, signé avec une clé **de débogage**, uniquement pour essais.
- Candidat de compilation F-Droid : `app/build/outputs/apk/release/app-release-unsigned.apk`, **non signé et non distribuable comme version officielle**.

La signature de débogage peut empêcher le remplacement par un paquet F-Droid de même identifiant. Utiliser un profil d'essai ; désinstaller une application efface ses données locales et ses sessions. Ne jamais publier la clé de débogage comme clé officielle.

## Dépendances

Production : AndroidX Core 1.13.1, AppCompat 1.7.0, Fragment 1.8.2, Material Components 1.12.0 et leurs dépendances transitives. Kotlin 1.9.24 et Android Gradle Plugin 8.6.1 pour la compilation. AndroidX / Material / outils Android sont sous licences libres ; consulter leurs fichiers LICENSE/NOTICE et l'arbre transitif avant soumission. Aucun ajout de Firebase, GMS, publicité ou SDK de télémétrie dans le code natif.

Le [relevé Gradle réel](../validation/fdroid-finalization/runtime-dependencies.json) comprend 68 modules sélectionnés de production, leurs versions, les empreintes des artefacts et les POM officiels : tous déclarent Apache 2.0, zéro déclaration manquante. [NETWORK-DEPENDENCIES.md](NETWORK-DEPENDENCIES.md) distingue ces bibliothèques embarquées des ressources Web Google Fonts/WordPress et des sessions réseau. Ces relevés n'impliquent pas un audit de chaque contenu distant.

Tests uniquement : JUnit 4.13.2, JSON-java 20240303, Robolectric 4.14.1 et AndroidX Test (runner 1.6.2, core 1.6.1, ext-junit 1.2.1). Robolectric télécharge ses images Android de test depuis Maven ; elles ne sont pas intégrées à l'APK. Les tests Robolectric simulent les API Android et ne prouvent pas le comportement d'un téléphone.

## Test du moteur d'affichage dans Android complet

`RendererRecoveryInstrumentedTest` utilise l'API publique `WebViewRenderProcess.terminate()` pour provoquer deux arrêts réels du moteur, puis vérifie la reprise par le bouton natif et le menu Talk. Il charge une page locale et exige que l'API confirme la terminaison ; il ne dépend ni du réseau ni du générateur de rapports de crash du moteur. Il nécessite Android 10+ (API 29), refuse les appareils physiques et doit être exécuté exclusivement dans un émulateur jetable sans comptes. Il désactive la surveillance dans ce profil d'essai. Le rappel de reprise existe depuis Android 8 ; Android 7 reste couvert par un test de démarrage Robolectric.

Avec cet émulateur déjà démarré, et **uniquement** son identifiant ADB :

```sh
gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r net.lefil.fdroid.test/androidx.test.runner.AndroidJUnitRunner
```

Vérifier le résultat `OK (1 test)` et l'absence de crash du processus : le code de sortie ADB seul ne suffit pas. Ce test n'utilise aucun compte et ne valide ni le chargement des services distants, ni la réception des notifications, ni les restrictions d'un téléphone réel.

Dépôts : Google Maven, Maven Central et portail des plugins Gradle, définis dans `settings.gradle`. Les pages des services distants et leurs scripts ne sont pas audités ici : ne pas assimiler l'absence de SDK natif de suivi à une garantie sur les sites affichés.

## Publication et signature

Après acceptation fonctionnelle de la version retenue, intégrer les changements contrôlés, puis créer une version/tag stable et unique correspondant à `versionName`. Les essais de communication sont préparés pour une équipe technique ; l'absence d'essai sur téléphone physique n'est pas présentée comme une interdiction générale F-Droid. Ne pas réutiliser un `versionCode` déjà distribué. Aucun tag de publication n'a été créé par cette mission.

La recette `net.lefil.fdroid.yml` épingle un commit de candidature. Maintenir `CurrentVersion`/`CurrentVersionCode`, contrôler les métadonnées et la compilation de cette recette, puis suivre la CI de contribution et la revue officielles. Un build serveur F-Droid n'a pas été exécuté dans cette mission ; il n'est pas annoncé comme une condition déjà satisfaite. La comparaison binaire reproductible indépendante reste recommandée par le guide officiel, sans être un préalable universel d'admission.

Le dépôt officiel compile les sources et gère sa signature. Le premier paquet accepté fixe l'identité de signature pour les mises à jour ; conserver le même identifiant et augmenter les codes de version. Aucun mécanisme d'auto-installation/auto-mise à jour n'est ajouté à l'application. Les mises à jour seront disponibles via le client F-Droid seulement après admission et publication effectives.

Références officielles :

- https://f-droid.org/docs/Inclusion_Policy/
- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/Build_Metadata_Reference/
- https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/

Leurs sources officielles ont été consultées dans https://github.com/f-droid/fdroid-website/tree/master/_docs pendant cette mission (le domaine du site documentaire était bloqué par le proxy).
