# Compilation et mises à jour F-Droid

Application : `net.lefil.fdroid`, version candidate `0.2.1` / `3`, minimum Android 7 (API 24), cible et compilation API 35. Licence du dépôt : MIT (`LICENSE`). Les ressources du dépôt sont couvertes par cette licence sauf indication contraire ; la provenance et les droits du logo officiel doivent être confirmés pour le dossier d'admission.

## Reproduire les contrôles

Outils : OpenJDK 17, Gradle 8.7, Android SDK platform 35 et build-tools 34.0.0. Le projet ne fournit pas de wrapper Gradle ; la CI installe explicitement Gradle 8.7. Utiliser un JDK libre (pas Oracle JDK).

```sh
cd LeFil-FDroid
# ANDROID_HOME doit pointer sur le SDK ; Java 17 et Gradle 8.7 doivent être dans PATH.
gradle --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Dans l'environnement cloud préparé, commencer par `source /workspace/.cloud-setup/env.sh`. Les paramètres locaux du proxy et de confiance TLS se trouvent hors du dépôt. Ne jamais désactiver TLS ni incorporer des secrets dans Gradle.

Résultats :

- Tests JUnit : `app/build/test-results/testDebugUnitTest/TEST-*.xml` et rapport HTML `app/build/reports/tests/testDebugUnitTest/index.html`.
- Lint : `app/build/reports/lint-results-debug.xml` et `.html`.
- Essai installable : `app/build/outputs/apk/debug/app-debug.apk`, signé avec une clé **de débogage**, uniquement pour essais.
- Candidat de compilation F-Droid : `app/build/outputs/apk/release/app-release-unsigned.apk`, **non signé et non distribuable comme version officielle**.

La signature de débogage peut empêcher le remplacement par un paquet F-Droid de même identifiant. Utiliser un profil d'essai ; désinstaller une application efface ses données locales et ses sessions. Ne jamais publier la clé de débogage comme clé officielle.

## Dépendances

Production : AndroidX Core 1.13.1, AppCompat 1.7.0, Fragment 1.8.2, Material Components 1.12.0 et leurs dépendances transitives. Kotlin 1.9.24 et Android Gradle Plugin 8.6.1 pour la compilation. AndroidX / Material / outils Android sont sous licences libres ; consulter leurs fichiers LICENSE/NOTICE et l'arbre transitif avant soumission. Aucun ajout de Firebase, GMS, publicité ou SDK de télémétrie dans le code natif.

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

Après validation sur téléphone et comptes réels, intégrer les changements contrôlés, puis créer une version/tag stable et unique correspondant à `versionName`. Ne pas réutiliser un `versionCode` déjà distribué. Aucun tag de publication n'a été créé par cette mission.

La recette `net.lefil.fdroid.yml` épingle un commit de candidature. Avant soumission, remplacer ce commit par celui de la version validée, maintenir `CurrentVersion`/`CurrentVersionCode`, puis exécuter `fdroid lint`, `fdroid readmeta`, `fdroid build --server net.lefil.fdroid:3` dans un environnement F-Droid approprié et le scanner. Un build Gradle local ne remplace pas ces contrôles.

Le dépôt officiel compile les sources et gère sa signature. Le premier paquet accepté fixe l'identité de signature pour les mises à jour ; conserver le même identifiant et augmenter les codes de version. Aucun mécanisme d'auto-installation/auto-mise à jour n'est ajouté à l'application. Les mises à jour seront disponibles via le client F-Droid seulement après admission et publication effectives.

Références officielles :

- https://f-droid.org/docs/Inclusion_Policy/
- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/Build_Metadata_Reference/
- https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/

Leurs sources officielles ont été consultées dans https://github.com/f-droid/fdroid-website/tree/master/_docs pendant cette mission (le domaine du site documentaire était bloqué par le proxy).
