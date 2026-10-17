# Preuves complémentaires de finalisation

Ces contrôles complètent les preuves Android existantes ; ils ne les réexécutent pas. Aucun compte ni donnée privée n'a été utilisé.

| Pièce | Origine vérifiable | Résultat / limite |
|---|---|---|
| `public-https-report.json` | [Contrôle HTTPS acquis, run 39010816563](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39010816563) ; artefact récupéré par le run ci-dessous | 11 réponses vérifiées avec TLS. Formulaires publics et redirections observés ; aucun message authentifié. Pas de nouvelle exécution de cette suite. |
| `runtime-dependencies.json` | [Run 39013402324](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39013402324) | Résolution Gradle de `releaseRuntimeClasspath` uniquement ; 68 modules, zéro licence manquante, déclarations Apache 2.0 dans leurs POM officiels. Aucun test Android ni APK reconstruit. |
| `public-network.json`, `provenance.json` | Même run, observateur `dbe836eddbcd8c1aece06f43b6a3b86ef99d36e5` | Trois pages dans un navigateur anonyme ; ressources Google Fonts et WordPress emoji déclarées. Arbre `app` identique au candidat déjà construit. Observation bornée, sans authentification. |
| `humhub-mail-upstream.json` | [Lecture de la fiche officielle, run 39013291055](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39013291055) | Fiche publique du module Mail ; son lien de licence est ensuite suivi. La version publiée amont ne prouve pas celle de l'instance Atrium. |
| `humhub-mail-license.json` | [Run 39013402328](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39013402328) | Page officielle de licence Mail, HTTP 200, AGPL v3. Aucun changement du serveur. |
| `google-fonts-fallback.json` | [Run 39014068125](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39014068125) | Hôtes Google bloqués au niveau requête : trois liens de services et formulaire Atrium restent visibles. Deux pages HTTP 200. Aucun test de connexion ou de messages. |
| `server-licenses.json`, `official-access.json`, `public-source-access.json` | URL, statuts HTTP et empreintes enregistrés | Licences des logiciels amont, documentation officielle courante et accessibilité publique des sources ; pas d'audit du déploiement distant. |

La première version du contrôle de repli des polices échouait sur un sélecteur de texte sensible à la casse, susceptible de viser un élément caché ([run 39013809801](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39013809801)). La correction porte uniquement sur le test : les liens visibles sont identifiés par leur nom accessible et leur domaine de destination. Le résultat réussi et ses correspondances sont conservés ; aucun changement de site ou d'application n'a été nécessaire.

Une première déclaration invalide d'environnement de workflow a également été corrigée avant la collecte réussie ; elle n'avait lancé aucun contrôle Android. Les échecs CI restent dans l'historique, sans être présentés comme des réussites.

Les sources Java/Kotlin, tests, configuration Gradle et fichiers graphiques sont inchangés. Les descriptions de publication et notes de candidature sont les seules entrées de publication corrigées après ces constats. Les preuves Android antérieures restent liées à leur révision exacte dans [CHECKS.json](../../fdroid/CHECKS.json). Aucun de ces relevés ne valide les notifications, un téléphone physique, la CI GitLab ou l'admission F-Droid.
