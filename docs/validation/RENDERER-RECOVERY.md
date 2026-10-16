# Reprise après perte du moteur — candidat 0.2.1

## Défaut reproduit et portée

Le candidat `0.2` a été signalé comme se fermant ou restant vide. Dans l'émulateur Android 15 AOSP, avec WebView `124.0.6367.219`, l'APK livré (`ae7cf28`, SHA-256 `63ae4213f564416ab90e839508601696fff29bfb78ac91348f929997f5a6c0c5`) a effectivement terminé son processus après la perte du moteur :

```text
Renderer process (2049) crash detected (code 5).
Render process (2049)'s crash wasn't handled by all associated webviews, triggering application crash.
```

[Extrait original horodaté](renderer-original-crash.log). Le comportement par défaut de `WebViewClient.onRenderProcessGone` retourne `false`, ce qui laisse Android fermer l'application. Le test de régression ajouté échouait sur les deux API simulées 34/35 avant le correctif.

Cela démontre un défaut de reprise dans Le Fil. Cela n'établit ni la cause initiale de l'arrêt du moteur, ni que le téléphone de l'utilisateur rencontre exactement ce défaut : modèle, version Android et journal du téléphone ne sont pas disponibles. L'émulation sans KVM est lente ; des ANR de Launcher/SystemUI ont aussi été observés et ne sont pas attribués à Le Fil.

## Correction minimale

Le commit `0d68b843504f98e86e1471e887d8a73a5c1fe016` prend en charge la perte du moteur sous Android 8+ : retirer et détruire le WebView perdu, afficher un message natif et conserver l'URL à rouvrir. Le bouton « Recharger » recrée le WebView ; le menu permet aussi de choisir un service. Les callbacks provenant de l'ancien WebView sont ignorés. Une notification reçue et la sauvegarde d'état restent utilisables après la perte du moteur.

Les sessions ne sont pas effacées et aucun rechargement automatique en boucle n'est ajouté. Android 7 ne possède pas ce rappel : son démarrage est couvert par un test séparé, sans promesse de récupération équivalente.

Références Android officielles et contrat de nettoyage : [mécanismes retenus](../ANDROID-MECHANISMS.md).

## Contrôles

Les 61 tests précédents restent présents. Huit cas de reprise sur Android simulé 34/35 et un cas de démarrage Android 7 portent le total à **70 tests, zéro échec, erreur ou ignoré**. Lint conserve **zéro erreur et 19 avertissements**. Les variantes debug, release non signée et APK de test instrumenté compilent.

Un premier test natif par `chrome://crash` a échoué : le panneau n'était pas disponible dans les 60 secondes. [Résultat conservé](renderer-crash-probe-failed.log). Ce test n'est pas déclaré réussi. Le test instrumenté utilise maintenant l'API publique `WebViewRenderProcess.terminate()` après chargement d'une page locale ; il exige une confirmation réelle de terminaison avant de vérifier le panneau, le nouveau WebView et le menu. Cela évite de dépendre de services distants ou d'un rapport de crash généré par le moteur. Le code de production ne provoque jamais ces arrêts.

Le test natif sur Android 15 complet réussit : **`OK (1 test)`, 47,022 s**, avec deux terminaisons confirmées, panneau natif disponible, bouton Recharger créant un autre WebView et menu Talk utilisable après la seconde terminaison. [Sortie brute](renderer-termination-passed.log), [conditions et empreintes des APK effectivement testés](renderer-native-result.json). Les scénarios de crash restent couverts par Robolectric ; cette réussite native prouve la reprise après terminaison du moteur, pas l'absence de toute cause de crash sur téléphone.

La recette F-Droid épingle le code de candidature `c85f5a3cbb78ab58e6207f770cd9f6df60fb818d`, version `0.2.1` / code `3`. `readmeta`, `lint`, scan des sources, compilation et scan binaire locaux réussissent : [**1 build succeeded**](renderer-fdroid-build.log). Les signatures de scan sont celles embarquées dans fdroidserver 2.4.5 ; le serveur officiel F-Droid et la reproductibilité indépendante ne sont pas validés. Le [dossier de candidature](../fdroid/SUBMISSION.md) conserve ses conditions restantes.

[GitHub Actions nº 53](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/38970827650), source `c85f5a3`, termine avec **Success**, jobs `public-https` et `build`, quatre artefacts. Le workflow exécute les tests JVM, Lint et les compilations ; le test natif ci-dessus a été exécuté localement. Les réponses anonymes d'accueil Le Fil, Agora et Atrium/Talk sont à nouveau observées en HTTPS sur ce runner. Les pages de connexion et HTTP 200 ne prouvent pas un rendu complet, une session authentifiée ou une notification.

## Essai téléphone et retour arrière

Le paquet `app/build/phone-candidate/LeFil-0.2.1-c85f5a3-debug.apk`, `0.2.1` / code `3`, est réservé aux essais. Il est identique à l'APK ayant passé ce test natif et signé avec la même clé de débogage locale que le paquet précédent. Une installation par-dessus celui-ci doit conserver les données. En cas d'incompatibilité de signature avec un autre APK, interrompre l'installation et relever sa provenance ; ne pas désinstaller pour forcer la mise à jour. Le reçu local et les [résultats structurés](automated-results.json) donnent ses empreintes.

Le premier essai demandé est le démarrage sur le téléphone ayant signalé la panne, puis l'accès aux trois services. La [recette complète](../PHONE-ACCEPTANCE.md) reste requise pour les sessions, messages, pagination, messages identiques, notifications en veille et redémarrage. Aucun de ces essais réels n'est annoncé comme réussi.

Le correctif est isolé sur la branche de développement. Un revert de `0d68b84` permet de revenir sur sa logique sans réinitialiser l'historique ; si un APK de remplacement était créé, conserver un `versionCode` supérieur pour permettre l'installation. Ne pas revenir à l'ancien APK défaillant sur le téléphone sans diagnostic. Aucune fusion dans `main` ou PR nº 2, modification serveur, publication officielle, soumission ou admission F-Droid.
