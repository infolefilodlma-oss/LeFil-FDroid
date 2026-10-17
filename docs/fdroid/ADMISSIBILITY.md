# Admission F-Droid — vérification du 17 octobre 2026

L'admissibilité de Le Fil 0.2.1 n'est pas encore entièrement établie. Les sources libres et la compilation sont documentées ; l'ancienneté ou une exception, ainsi que le fonctionnement des communications annoncées, restent à établir. Le dossier technique et une MR préparée ne constituent ni une soumission, ni une acceptation, ni une publication.

## Critères et preuves

| Point examiné | État réel | Preuve / portée |
|---|---|---|
| Licence du projet | MIT déclarée | `LICENSE`, copyright `infolefilodlma-oss`. Aucun avis distinct restreignant les ressources du dépôt n'a été relevé dans les pièces examinées. |
| Dépendances | Dépendances directes Apache-2.0 | POM officiels des quatre bibliothèques AndroidX/Material vérifiés ; [relevé](dependency-licenses.json). Scans source/binaire de la candidature déjà réussis. Cela ne remplace pas la revue finale des dépendances par F-Droid. |
| Sources publiques | Présentes et épinglées | Dépôt GitHub existant ; la nouvelle recette vise `ccb61022e84a242a5e5989b9a629ac7c90a47a56`, qui corrige seulement les textes Fastlane. |
| Compilation depuis les sources | Nouvelle recette construite et scannée avec succès | [GitHub Actions](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39007522257) construit `ccb6102` avec JDK libre 17, Gradle 8.7, SDK 35 et build-tools 34.0.0. Le code Android et les entrées Gradle restent identiques à `c85f5a3`. [CHECKS.json](CHECKS.json) décrit la portée des preuves inspectées. |
| Reproductibilité binaire indépendante | Non effectuée ; recommandée | Le guide officiel indique explicitement qu'elle n'est pas obligatoire. La recette utilise la compilation et la signature F-Droid, sans `Binaries` ni clé de débogage officielle. |
| Google obligatoire | Aucune dépendance native GMS/Firebase déclarée | AndroidX/Material sont des bibliothèques libres ; le moteur système AOSP a été utilisé dans le test natif Android 15. Les scans ne valent pas audit de tout contenu Web distant. |
| Valeur native d'une interface Web | Présente, appréciation finale F-Droid | Menu de services et reprise native après perte du moteur, testée dans Android complet. Sélecteur de fichiers et couche de surveillance présents dans le code ; aucune preuve de notification réelle n'en est déduite. |
| Fonctions effectivement opérationnelles | Validation partielle | 70 tests JVM et un test natif acquis. Le test natif utilisait une page locale : il ne valide ni session authentifiée, ni messages privés, ni réception d'alertes. |
| Notifications décrites comme fiables | Écart corrigé dans les textes | Résumés recentrés sur l'accès aux services ; surveillance explicitement expérimentale, désactivée par défaut et non validée de bout en bout. |
| Dépendance aux instances du Fil | `TetheredNet` ajouté | Les origines sont fixes dans `MainActivity` et `MessageSources`, sans réglage d'instance. Cet Anti-Feature est une information utilisateur, pas un motif automatique d'exclusion. |
| Identification / contacts | Renseignés | `net.lefil.fdroid`, version `0.2.1`, code `3`, `AuthorName: infolefilodlma-oss`, sources et suivi GitHub. Le fichier de cet identifiant est absent de `fdroiddata` lors du contrôle. |
| Accord de l'auteur | Demande publique présente | L'[issue amont nº 1](https://github.com/infolefilodlma-oss/LeFil-FDroid/issues/1), ouverte par `infolefilodlma-oss`, demande explicitement la publication au catalogue officiel F-Droid. |
| Métadonnées de publication | Préparées | Résumé, description et changelog `3.txt` en `en-US` et `fr-FR`, recette canonique et texte de MR. Les descriptions restent dans le dépôt applicatif, conformément au modèle de MR. |
| Icône / captures | Icône intégrée ; captures absentes | Les captures sont recommandées et demandées dans le modèle de MR. La documentation générale les rend facultatives pour l'admission ; elles interviennent notamment pour l'onglet « Latest ». Aucune capture ni notification n'est fabriquée. |
| Identité visuelle demandée par le projet | Originaux reçus et intégrés sans retouche | PNG officiel bleu sur blanc pour Android/Fastlane ; variante transparente existante sur fond sombre. [Intégration et contrôles](../BRANDING.md). Nouveaux rendus en attente de contrôle, fiche publiée non vérifiée. Il s'agit d'une demande du projet, pas d'une couleur obligatoire du catalogue. |
| Versions / maintenance | Activité récente visible ; aucun tag stable | Le guide recommande un tag par version officielle. La recette peut construire un commit ; le suivi des futurs tags est configuré. Aucun tag stable n'est créé pour faire passer une candidature pour une version validée. |
| Ancienneté / exception | Non établie | Historique public vérifié à partir du 14 octobre 2026 ; aucun élément reçu ne démontre trois mois de développement ou une exception. Un historique antérieur peut être apporté sans être inventé. |
| Accès de soumission | Non authentifié sur GitLab | `GET https://gitlab.com/api/v4/user` retourne HTTP 401 ; aucune identité ni liaison GitLab utilisable n'est configurée dans cet environnement. Les accès GitHub ne donnent pas accès au compte GitLab. |

## La règle des trois mois existe

Source officielle vérifiée : [Inclusion Policy, Quality Control](https://gitlab.com/fdroid/fdroid-website/-/blob/a9c6e1c591bfa4a36d4da0ce9ad15d1b7463c244/_docs/Inclusion_Policy.md#quality-control). Le contenu de cette révision correspond à celui de `master` consulté le 17 octobre 2026.

> The application should have been actively developed for at least 3 months, unless:
> 1. The application has been proven to have some users.
> 1. The author has been maintaining other apps on F-Droid.
> 1. The application is developed only for an event.

Il s'agit d'un critère officiel formulé avec « should », assorti d'exceptions et d'une revue humaine. L'ancienneté de ce seul dépôt ne démontre pas un historique antérieur de l'application. Des utilisateurs du site Agora/Atrium ne prouvent pas, à eux seuls, des utilisateurs de cette application Android. Une attestation ou des éléments d'utilisation peuvent permettre de documenter l'exception ; aucune n'est supposée acquise.

Le texte ne fixe pas de nombre minimal d'utilisateurs et n'exclut pas explicitement les versions d'essai. Le retour déjà rapporté d'un candidat se fermant ou restant vide constitue un élément de test utilisateur à documenter, sans inventer une base d'utilisateurs ou l'acceptation de cette exception par F-Droid. L'examen ne se résume donc pas à une attente automatique de trois mois.

## Fonctionnalité et valeur native

La même politique demande que l'application soit fonctionnelle et implémente les fonctions décrites, et qu'une application enveloppant un site fournisse des fonctions natives et des améliorations. Elle n'impose pas une fonction de notifications à toutes les applications ni un certificat d'essai sur téléphone physique.

Le menu et la reprise du moteur sont des fonctions natives identifiables. La surveillance des messages apporte une valeur supplémentaire prévue, mais ses échanges authentifiés et sa réception réelle ne sont pas validés. Les textes corrigés rendent cette limite explicite ; ils ne remplacent pas une preuve fonctionnelle. La [recette de communication](COMMUNICATION-ACCEPTANCE.md) prépare les contrôles avec une équipe technique et un émulateur, sans installation demandée au chef de projet.

Le test natif acquis reste un contrôle de reprise du moteur. Les messages identiques, la pagination, les conversations ouvertes depuis une vraie alerte, la veille et le redémarrage restent à renseigner. La décision d'admission et l'appréciation de la valeur native appartiennent aux mainteneurs F-Droid.

## Obligations et recommandations séparées

- **Sources libres et build depuis les sources : requis.** Un build local et ses scans constituent des preuves techniques ; la revue et la CI F-Droid restent distinctes.
- **Comparaison binaire reproductible avec une signature amont : recommandée.** Le [guide officiel, Setup Reproducible Build](https://gitlab.com/fdroid/fdroid-website/-/blob/master/_docs/Submitting_to_F-Droid_Quick_Start_Guide.md#setup-reproducible-build) dit : « Reproducible builds are not a requirement for apps being on F-Droid. »
- **Résumés et descriptions : requis.** La [documentation graphique](https://gitlab.com/fdroid/fdroid-website/-/blob/master/_docs/All_About_Descriptions_Graphics_and_Screenshots.md) dit : « All metadata files are completely optional, except for the short summary description as well as the longer full description. » Les captures sont recommandées ; elles ne doivent pas être ajoutées à `fdroiddata`.
- **Licence valide des ressources : requise.** Le dépôt déclare MIT. Aucune autorisation additionnelle non prévue par cette licence n'est inventée comme condition générale.
- **CI officielle / revue : étapes du processus de contribution.** Elles ne sont pas annoncées comme déjà exécutées. Une comparaison indépendante des binaires n'est pas ajoutée comme préalable universel au dépôt d'une MR.

Les URL consultées, leurs empreintes et les contrôles d'accès sont conservés dans [OFFICIAL-SOURCES.json](OFFICIAL-SOURCES.json).
