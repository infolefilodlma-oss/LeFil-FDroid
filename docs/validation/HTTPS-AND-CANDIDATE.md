# Vérification réelle HTTPS et candidat corrigé — 16 octobre 2026

## Résultats récupérés sur GitHub Actions

- [Exécution nº 45](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/38962818582), commit `1f7b55d`, branche candidate : **réussie**, trois artefacts (APK debug, release non signé, rapports). Ce résultat vérifie les contrôles existants ; aucun besoin de recommencer leur implémentation.
- [Exécution nº 46](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/38963726422), commit `f3770bf`, branche `notifications-reelles` : **échec Lint**, deux erreurs de permission déjà corrigées sur la candidate. Le pas `curl ... || true` ne prouve pas à lui seul la réussite HTTPS ; les journaux sont masqués aux visiteurs non authentifiés.
- Le diagnostic a été rendu indépendant du build Android et publie désormais ses codes HTTP en annotations, résumé et artefact JSON. Les requêtes utilisent le réseau du runner GitHub, la validation TLS, aucun compte et aucun cookie fourni. Les pages complètes, cookies et jetons ne sont pas conservés dans le rapport.

## Réponses anonymes observées

[Preuve : diagnostic de l'exécution nº 49](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/38964668550), source `ae7cf28` :

| Requête | Réponse et observation |
|---|---|
| `https://social.le-fil.net/message` | HTTPS vérifié, HTTP 200, page Friendica avec champ mot de passe |
| `https://social.le-fil.net/.well-known/nodeinfo` | HTTP 200, document de découverte NodeInfo |
| `https://social.le-fil.net/nodeinfo/1.0` | HTTP 200, logiciel annoncé `friendica`, version `2026.05-1595` |
| `https://team.le-fil.net/` | HTTP 302 puis 200, page d'accueil HumHub « Le Fil Team » |
| `https://team.le-fil.net/mail/mail/notification-list` | HTTP 404, page nginx « 404 Not Found » |
| `https://team.le-fil.net/mail/mail/get-new-message-count-json` | HTTP 404, même résultat |
| `https://team.le-fil.net/index.php?r=mail%2Fmail%2Fnotification-list` | HTTP 302 puis 200, formulaire HumHub « Identifiant - Le Fil Team » |
| `https://team.le-fil.net/index.php?r=mail%2Fmail%2Fget-new-message-count-json` | HTTP 302 puis 200, même formulaire de connexion |
| `https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex` | HTTP 302 puis 200, formulaire de connexion de la messagerie |

Le diagnostic HTTPS public est réussi avec les routes du contrôleur frontal. Les 404 des URL simplifiées restent visibles ; ils ne signifient ni serveur hors ligne ni panne de TLS. Aucun réglage serveur n'a été modifié. Le proxy de la machine Codex n'a pas été contourné ; les contrôles sont exécutés par le workflow GitHub autorisé.

## Correction minimale

Le raccourci natif Talk utilisait l'URL simplifiée qui répond 404. Il utilise désormais la route `index.php?r=mail%2Fmail%2Findex`. Le lien de secours généré en l'absence de `href` emploie la même route avec l'identifiant de conversation. Le lecteur Atrium possédait déjà le repli `index.php` pour ses deux requêtes ; il est conservé.

Le test existant de navigation a été ajusté à cette URL réellement accessible, et un test de régression couvre le lien de secours. Les 60 tests précédents sont conservés, pour **61 tests exécutés avec zéro échec/erreur/ignoré**, dont Android simulé API 34/35. Lint : zéro erreur et 19 avertissements. Les deux APK sont générés et la signature debug vérifiée.

La recette F-Droid épingle maintenant `ae7cf28e5429ed9618b14040a1b24b0c038113b8`. `readmeta`, `lint`, compilation locale F-Droid et scan binaire réussissent : **1 build succeeded**. Le serveur officiel et une vérification de reproductibilité indépendante ne sont pas exécutés. [Résultats et SHA-256](automated-results.json).

## Lecture des messages et exigences F-Droid restantes

Les sources officielles Friendica du tag `2026.05`, correspondant à la version annoncée par le serveur, confirment les marqueurs de lecture Frio, le compteur des messages et la pagination. Cela ne prouve pas que le serveur utilise ce thème sans modification, ni que des sessions natives sont valides. HumHub est reconnaissable publiquement, mais sa version et celle du module Mail ne sont pas établies.

Une réponse de connexion anonyme ne valide pas un compteur authentifié, une conversation privée, la pagination, les messages identiques ou une notification reçue. Ces cas restent **à effectuer avec comptes d'essai autorisés et téléphone**, suivant la [recette](../PHONE-ACCEPTANCE.md). Aucune utilisation de compte réel n'a été tentée.

La documentation officielle F-Droid a été revérifiée dans les sources de son site, notamment [Inclusion Policy](https://github.com/f-droid/fdroid-website/blob/master/_docs/Inclusion_Policy.md) et [guide de soumission](https://github.com/f-droid/fdroid-website/blob/master/_docs/Submitting_to_F-Droid_Quick_Start_Guide.md). Les conditions restantes n'ont pas été effacées : fonctions réellement validées, droits des visuels, captures réelles, éligibilité à la règle des trois mois ou exception documentée, version/tag stable et revue F-Droid. Aucun tag ni soumission prématurée.

## Essais et retour arrière

Le paquet de téléphone se trouve dans `app/build/phone-candidate/LeFil-0.2-ae7cf28-debug.apk`, accompagné d'un reçu SHA-256 et des instructions d'installation. Il n'est pas publié comme APK officiel.

Le commit `ae7cf28` isole la correction Android et son test ; son parent est `3b647e2`. En cas de régression avérée, revenir sur ce seul commit par un revert sur la branche de développement préserve l'historique. Aucune réinitialisation, fusion dans `main`, modification de PR nº 2 ou intervention VPS n'est réalisée.
