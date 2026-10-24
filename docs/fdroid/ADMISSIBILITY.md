# Admission F-Droid — vérification du 17 octobre 2026

> **Candidat dépassé (2026-10-24).** Ce relevé porte sur le candidat
> 0.2.1 / versionCode 3 (commit `25189d316d8345a601b4dae2fa9987d286831d1e`).
> Le candidat actuel est 0.2.7-test / versionCode 11 (commit
> `85cbe966f3948a6fac9b690c82a21e2531cc25e1`), qui remplace le sondage
> Gotify par WorkManager (~15 min, notification unique par cycle) et
> ajoute Radio au menu ; voir `net.lefil.fdroid.yml` pour la recette
> actualisée. La politique F-Droid citée ci-dessous (règle des trois
> mois, exigence de fonctions natives pour les enveloppes WebView) a
> été reconfirmée le 2026-10-24 sur le dépôt officiel et reste
> d'application ; les preuves techniques détaillées (tests, scans,
> exécutions CI) ci-dessous n'ont pas été recommencées pour le nouveau
> candidat et ne doivent pas être lues comme le validant. Décision du
> 2026-10-24 : signature standard F-Droid retenue, clé v2 réservée à la
> distribution directe (`docs/release/SIGNING-KEY-V2-DECISION.md`) ;
> compilation depuis les sources du candidat actuel vérifiée ce même
> jour par un build Gradle propre réussi (`CHECKS.json`), sans preuve de
> reproductibilité binaire.

Le dossier de Le Fil 0.2.1 est techniquement prêt à être proposé pour examen, avec demande explicite d'examen de l'exception d'ancienneté et déclaration des notifications non validées. Cela ne certifie pas son admission. La politique fixe des critères pour l'ajout au catalogue ; le guide prévoit que les mainteneurs examinent la proposition et déterminent les écarts à résoudre. Les limites connues restent ouvertes dans le texte de MR, sans être transformées en interdiction générale de déposer une proposition.

## Critères et preuves

| Point examiné | État réel | Preuve / portée |
|---|---|---|
| Licence du projet | MIT déclarée | `LICENSE`, copyright `infolefilodlma-oss`. Aucun avis distinct restreignant les ressources du dépôt n'a été relevé dans les pièces examinées. |
| Dépendances | 68 modules de production, licences Apache 2.0 déclarées | Graphe Gradle réel `releaseRuntimeClasspath`, versions, empreintes des artefacts et POM officiels : [relevé complet](../validation/fdroid-finalization/runtime-dependencies.json). Zéro déclaration de licence manquante. Les scans sont un contrôle distinct ; la revue finale F-Droid reste à effectuer. |
| Sources publiques | Présentes et épinglées | Dépôt existant public ; recette visant `25189d316d8345a601b4dae2fa9987d286831d1e`, incluant les déclarations réseau corrigées, les originaux officiels et les captures Fastlane. |
| Compilation depuis les sources | Recette actualisée construite et scannée avec succès | [GitHub Actions](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39014443718) construit `25189d3…` avec JDK libre 17, Gradle 8.7, SDK 35 / build-tools 34.0.0. Métadonnées, Lint et scans source/binaire réussis. [Résultat original](../validation/fdroid-finalization/fdroid-result.json) et [CHECKS.json](CHECKS.json). |
| Reproductibilité binaire indépendante | Non effectuée ; recommandée | Le guide officiel indique explicitement qu'elle n'est pas obligatoire. La recette utilise la compilation et la signature F-Droid, sans `Binaries` ni clé de débogage officielle. |
| Google obligatoire | Aucune dépendance native GMS/Firebase ; ressources Google Fonts distantes présentes | Google Fonts sur l'accueil et la connexion Atrium, emoji WordPress sur l'accueil, cookies tiers autorisés. Déclarations corrigées en français et en anglais. Le blocage des hôtes Google laisse les contrôles publics visibles ; aucun fonctionnement authentifié n'en est déduit. [Périmètre et classification](NETWORK-DEPENDENCIES.md). |
| Logiciels serveur | Licences amont libres disponibles | WordPress GPL, Friendica AGPL, HumHub avec option AGPL et Mail AGPL v3 selon sa fiche officielle. Ce relevé ne certifie pas les versions, thèmes ou extensions effectivement déployés. |
| Valeur native d'une interface Web | Présente, appréciation finale F-Droid | Menu de services et reprise native après perte du moteur, testée dans Android complet. Sélecteur de fichiers et couche de surveillance présents dans le code ; aucune preuve de notification réelle n'en est déduite. |
| Fonctions effectivement opérationnelles | Validation partielle | Résultats acquis de 70 tests JVM et d'un test natif Android 15 réutilisés, rendus du lanceur et de la barre native examinés. Les messages authentifiés et notifications réelles restent non validés. |
| Notifications décrites comme fiables | Écart corrigé dans les textes | Résumés recentrés sur l'accès aux services ; surveillance explicitement expérimentale, désactivée par défaut et non validée de bout en bout. |
| Dépendance aux instances du Fil | `TetheredNet` ajouté | Les origines sont fixes dans `MainActivity` et `MessageSources`, sans réglage d'instance. Cet Anti-Feature est une information utilisateur, pas un motif automatique d'exclusion. |
| Identification / contacts | Renseignés | `net.lefil.fdroid`, version `0.2.1`, code `3`, `AuthorName: infolefilodlma-oss`, sources et suivi GitHub. Le fichier de cet identifiant est absent de `fdroiddata` lors du contrôle. |
| Accord de l'auteur | Demande publique présente | L'[issue amont nº 1](https://github.com/infolefilodlma-oss/LeFil-FDroid/issues/1), ouverte par `infolefilodlma-oss`, demande explicitement la publication au catalogue officiel F-Droid. |
| Métadonnées de publication | Préparées | Résumé, description et changelog `3.txt` en `en-US` et `fr-FR`, recette canonique et texte de MR. Les descriptions restent dans le dépôt applicatif, conformément au modèle de MR. |
| Icône officielle et captures | Fournies et examinées | PNG bleu sur blanc copié à l’identique ; deux captures Android 15 réelles et non retouchées dans Fastlane en/fr. [Provenance](../validation/branding/fastlane-screenshots.json). Fiche publiée non vérifiée. |
| Identité visuelle demandée par le projet | Intégrée et contrôlée sur Android 15 | Logo bleu sur blanc dans le lanceur et la barre claire ; variante transparente existante sur fond sombre. [Rendus et limites](../BRANDING.md). Autres lanceurs, téléphone et fiche publiée non validés. |
| Versions / maintenance | Activité récente visible ; aucun tag stable | Le guide recommande un tag par version officielle. La recette peut construire un commit ; le suivi des futurs tags est configuré. Aucun tag stable n'est créé pour faire passer une candidature pour une version validée. |
| Ancienneté / exception | Trois mois non démontrés ; examen de l'exception « utilisateurs » demandé | Historique public à partir du 14 octobre 2026. Le chef de projet a rapporté un essai du candidat Android sur téléphone ; ce retour est présenté comme témoignage d'essai, sans nombre d'utilisateurs inventé ni exception annoncée comme acceptée. Les deux autres exceptions ne sont pas invoquées. |
| Accès de soumission | Non authentifié sur GitLab | `GET https://gitlab.com/api/v4/user` retourne HTTP 401 ; aucune identité ni liaison GitLab utilisable n'est configurée dans cet environnement. Les accès GitHub ne donnent pas accès au compte GitLab. |

## La règle des trois mois existe

Source officielle vérifiée : [Inclusion Policy, Quality Control](https://gitlab.com/fdroid/fdroid-website/-/blob/a9c6e1c591bfa4a36d4da0ce9ad15d1b7463c244/_docs/Inclusion_Policy.md#quality-control). Le contenu de cette révision correspond à celui de `master` consulté le 17 octobre 2026.

> The application should have been actively developed for at least 3 months, unless:
> 1. The application has been proven to have some users.
> 1. The author has been maintaining other apps on F-Droid.
> 1. The application is developed only for an event.

Il s'agit d'un critère officiel formulé avec « should », assorti d'exceptions et d'une revue humaine. L'ancienneté de ce seul dépôt ne démontre pas un historique antérieur de l'application. Des utilisateurs du site Agora/Atrium ne prouvent pas, à eux seuls, des utilisateurs de cette application Android. Une attestation ou des éléments d'utilisation peuvent permettre de documenter l'exception ; aucune n'est supposée acquise.

Le texte ne fixe pas de nombre minimal d'utilisateurs et n'exclut pas explicitement les versions d'essai. Le retour déjà rapporté d'un candidat se fermant ou restant vide constitue un élément de test utilisateur à documenter, sans inventer une base d'utilisateurs ou l'acceptation de cette exception par F-Droid. L'examen ne se résume donc pas à une attente automatique de trois mois.

**Application au Fil :** la voie ordinaire des trois mois n'est pas démontrée par l'historique disponible. L'exception pertinente à demander est celle des utilisateurs, à partir du retour d'essai Android déjà rapporté. Il n'existe pas de preuve examinée que l'auteur maintienne une autre application F-Droid ; Le Fil n'est pas présenté comme une application événementielle. Aucun de ces deux cas n'est donc revendiqué. Le témoignage d'essai n'est ni une validation du candidat corrigé ni une décision d'exception prise par les mainteneurs. La demande de revue l'expose explicitement.

## Fonctionnalité et valeur native

La même politique demande que l'application soit fonctionnelle et implémente les fonctions décrites, et qu'une application enveloppant un site fournisse des fonctions natives et des améliorations. Elle n'impose pas une fonction de notifications à toutes les applications ni un certificat d'essai sur téléphone physique.

Le menu et la reprise du moteur sont des fonctions natives identifiables. La surveillance des messages apporte une valeur supplémentaire prévue, mais ses échanges authentifiés et sa réception réelle ne sont pas validés. Les textes corrigés rendent cette limite explicite ; ils ne remplacent pas une preuve fonctionnelle. La [recette de communication](COMMUNICATION-ACCEPTANCE.md) prépare les contrôles avec une équipe technique et un émulateur, sans installation demandée au chef de projet.

Le test natif acquis reste un contrôle de reprise du moteur. Les messages identiques, la pagination, les conversations ouvertes depuis une vraie alerte, la veille et le redémarrage restent à renseigner. La décision d'admission et l'appréciation de la valeur native appartiennent aux mainteneurs F-Droid.

## Dépôt d'une proposition et admission

Référence exacte : [Quick Start Guide, Application Review Process](https://gitlab.com/fdroid/fdroid-website/-/blob/master/_docs/Submitting_to_F-Droid_Quick_Start_Guide.md#application-review-process).

> Once the inclusion proposal is filed, the application will enter a reviewing process where F-Droid staff look into the applications source code and determine whether it fits for inclusion (and when it's not, determine all necessary steps to make it so).

Le [CONTRIBUTING officiel, Building it](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md#building-it) exige une recette avec au moins un build fonctionnel et ses contrôles de métadonnées, déjà acquis pour la source épinglée. Aucun des textes examinés ne demande un rapport de test authentifié des notifications avant de pouvoir déposer une proposition. L'absence de ce test est donc une **limitation déclarée à examiner**, pas un motif automatique interdisant le dépôt. Cela ne prouve pas que la surveillance fonctionne et n'autorise pas à en promettre la fiabilité. Un défaut fonctionnel établi ou une description trompeuse devra être corrigé avant l'admission ; les mainteneurs peuvent demander ces essais pendant l'examen.

La proposition s'appuie sur l'accès aux services, le menu natif et la reprise native testée, sans utiliser les notifications non validées comme preuve de fonctionnement. La case attestant l'ensemble des critères reste ouverte, avec demande de revue de l'exception. Une MR en brouillon peut présenter ces points sans revendiquer une admission acquise ; [GitLab documente](https://gitlab.com/gitlab-org/gitlab/-/blob/master/doc/user/project/merge_requests/drafts.md) que le préfixe `Draft:` interdit sa fusion tant qu'il n'est pas retiré.

Le [modèle officiel, Metadata](https://gitlab.com/fdroid/fdroiddata/-/blob/master/.gitlab/merge_request_templates/App%20inclusion.md#metadata) demande des versions taguées « unless there is a special reason » et une révision `commit` sous forme de hash complet. Le motif exposé ici est l'examen du candidat 0.2.1 à une révision publique immuable, avant de le déclarer version stable. Le suivi des futurs tags est configuré. Cette explication reste soumise à la revue ; aucun tag ni aucune publication de version stable n'est inventé.

Le seul prérequis extérieur à la poursuite de cette contribution est maintenant l'accès d'un compte GitLab autorisé, pour le fork de contribution, sa CI et la MR vers `fdroid/fdroiddata`. Un jeton nouvellement généré n'est pas un critère d'admission ; utiliser un accès existant s'il devient disponible. La CI GitLab et la décision des mainteneurs seront des étapes de la contribution, pas des résultats déjà obtenus.

## Obligations et recommandations séparées

- **Sources libres et build depuis les sources : requis.** Un build local et ses scans constituent des preuves techniques ; la revue et la CI F-Droid restent distinctes.
- **Comparaison binaire reproductible avec une signature amont : recommandée.** Le [guide officiel, Setup Reproducible Build](https://gitlab.com/fdroid/fdroid-website/-/blob/master/_docs/Submitting_to_F-Droid_Quick_Start_Guide.md#setup-reproducible-build) dit : « Reproducible builds are not a requirement for apps being on F-Droid. »
- **Résumés et descriptions : requis.** La [documentation graphique](https://gitlab.com/fdroid/fdroid-website/-/blob/master/_docs/All_About_Descriptions_Graphics_and_Screenshots.md) dit : « All metadata files are completely optional, except for the short summary description as well as the longer full description. » Les captures sont recommandées ; elles ne doivent pas être ajoutées à `fdroiddata`.
- **Licence valide des ressources : requise.** Le dépôt déclare MIT. Aucune autorisation additionnelle non prévue par cette licence n'est inventée comme condition générale.
- **CI officielle / revue : étapes du processus de contribution.** Elles ne sont pas annoncées comme déjà exécutées. Une comparaison indépendante des binaires n'est pas ajoutée comme préalable universel au dépôt d'une MR.

Les URL consultées, leurs empreintes et les contrôles d'accès sont conservés dans [OFFICIAL-SOURCES.json](OFFICIAL-SOURCES.json).

## Compléments de cette reprise

Les résultats acquis de 70 tests JVM, du test natif et de Lint sont conservés, sans nouvelle exécution : `app`, les tests et les entrées Gradle sont identiques à la candidature déjà validée. Le premier écart supplémentaire reproduit est une déclaration réseau incomplète : les descriptions pouvaient laisser croire que seules trois origines étaient contactées. Elles exposent maintenant Google Fonts, les emoji WordPress et les cookies tiers. Aucun dessin ni fichier graphique n'a changé.

Les observations publiques, le contrôle ciblé du repli des polices et les licences de tous les modules sélectionnés sont [conservés avec provenance](../validation/fdroid-finalization/README.md). La classification `TetheredNet` est établie. Le dossier expose aussi les chargements tiers et le marqueur Site Kit à la revue, sans inventer une dépendance totale à Google ou un suivi démontré. La recette actualisée fait l'objet de `readmeta`, `lint -f`, d'une compilation et de scans de la nouvelle source épinglée ; leur résultat exact figure dans [CHECKS.json](CHECKS.json).
