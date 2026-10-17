# Candidature officielle F-Droid — préparée, non déposée

Le Fil reste la même application Android `net.lefil.fdroid`, version candidate `0.2.1` / code `3`. La finalisation du dossier est isolée sur `codex/fdroid-submission-readiness`, à partir des travaux validés de `codex/notifications-fdroid-validation`. `main`, `notifications-reelles` et la PR nº 2 sont préservées.

## État réel

Les pièces techniques sont préparées : licence MIT, sources publiques, recette de build, descriptions `en-US` et `fr-FR`, changelogs, procédure de signature et de mises à jour, preuves existantes et contrôle de la nouvelle recette. La source épinglée est `25189d316d8345a601b4dae2fa9987d286831d1e` ; elle inclut les déclarations réseau corrigées, les logos officiels et les captures réelles. L'arbre applicatif complet, les tests, Gradle et les fichiers graphiques sont identiques à la candidature déjà contrôlée. Les tests Android concluants ne sont pas recommencés.

Les premières corrections du dossier répondent à trois écarts établis : suppression des promesses non validées de notifications, déclaration `TetheredNet` pour les instances fixes du Fil et ajout de l'auteur `infolefilodlma-oss`. Les descriptions sont dans le dépôt applicatif, conformément au modèle officiel de MR ; elles ne sont pas dupliquées dans `fdroiddata`.

Cette reprise termine le relevé des **68 modules de production** et de leurs licences POM, ainsi que la [déclaration des communications](NETWORK-DEPENDENCIES.md). Google Fonts et des emoji WordPress sont effectivement chargés depuis des domaines tiers ; les descriptions et notes de maintenance le déclarent désormais. Les contrôles publics restent visibles lorsque Google Fonts est bloqué. Ce test n'authentifie personne et ne valide pas les messages. Les [preuves complémentaires](../validation/fdroid-finalization/README.md) séparent ces observations des résultats Android acquis.

Le dossier est techniquement prêt pour une proposition en examen, avec ses limites déclarées ; l'admission complète reste à décider par F-Droid. La [vérification des critères](ADMISSIBILITY.md) distingue désormais les exigences d'admission des prérequis de dépôt. Le [bilan des contrôles](CHECKS.json) conserve les preuves acquises sans relancer les tests.

## Identité visuelle contrôlée

**Identité visuelle demandée par le projet :** les deux originaux officiels fournis ont été récupérés avec HTTP 200. Le PNG bleu sur blanc est intégré sans réencodage dans Android et Fastlane ; la variante transparente existante est conservée pour la barre native sombre. Les [nouveaux rendus](../BRANDING.md) de l'accueil, de la liste des applications et des thèmes clair/sombre sont contrôlés sur émulateur Android 15 ; les deux captures Fastlane sont réelles et non retouchées. Aucune validation de la fiche publiée n'est annoncée.

## Points à exposer à l'examen

1. **Ancienneté ou exception.** La politique officielle actuelle contient le critère de trois mois de développement actif, avec exceptions. L'historique public vérifié commence le 14 octobre 2026. La MR demande l'examen de l'exception « utilisateurs » sur la base de l'essai du candidat Android rapporté par le chef de projet ; elle n'affirme pas que cette exception est déjà acceptée. Les autres exceptions ne sont pas invoquées. Le texte exact et sa révision officielle figurent dans le bilan d'admissibilité. Ce n'est pas une attente automatique de trois mois avant toute proposition.
2. **Fonctionnement des communications.** Les 70 tests JVM et le test natif acquis ne valident pas les messages authentifiés, alertes, textes identiques, pagination ou veille sur les services réels. La surveillance est expérimentale et désactivée par défaut ; sa validation est une limitation déclarée à traiter pendant l'examen, sans inventer une exigence universelle de test de notifications préalable au dépôt. La [recette préparée pour l'équipe technique](COMMUNICATION-ACCEPTANCE.md) reste disponible si les mainteneurs la demandent avant admission ou pour achever cette fonction. Aucun résultat de téléphone n'est revendiqué.

## Seul accès extérieur nécessaire pour poursuivre

La consultation publique de GitLab fonctionne, mais aucune identité GitLab active n'est fournie à l'environnement. L'accès d'un compte autorisé est nécessaire pour créer le fork de contribution, exécuter sa CI puis ouvrir la MR officielle. Il n'est pas nécessaire de générer un nouveau jeton pour préparer ou évaluer ce dossier ; aucun nouveau secret n'est demandé ni ajouté par ce dernier contrôle. Réutiliser un accès existant lorsqu'il est autorisé. La préparation de l'accès ne vaut pas dépôt, succès de CI ou admission.

Deux captures d'émulateur sont maintenant fournies en amont dans Fastlane, avec leurs empreintes. Les captures sont recommandées et demandées par le modèle de MR ; la documentation générale les rend facultatives pour l'admission. La comparaison binaire indépendante est également recommandée. Elles ne sont pas ajoutées comme obligations générales inventées. Le dépôt déclare MIT pour ses ressources ; aucun avis contradictoire n'a été relevé dans les pièces examinées.

## Contribution prête à relire

- Recette canonique : [net.lefil.fdroid.yml](net.lefil.fdroid.yml).
- Fichier à ajouter dans le dépôt officiel : `metadata/net.lefil.fdroid.yml`.
- Patch de cette seule addition : [net.lefil.fdroid.patch](submission/net.lefil.fdroid.patch).
- Titre pour l'examen initial en brouillon : **Draft: New app: Le Fil** ; titre final **New app: Le Fil** après résolution des points de revue.
- Corps suivant le modèle officiel, avec cases non acquises laissées ouvertes : [MERGE_REQUEST.md](submission/MERGE_REQUEST.md).
- Sources documentaires, empreintes et contrôles d'accès : [OFFICIAL-SOURCES.json](OFFICIAL-SOURCES.json).

Le patch est contrôlé par application dans un répertoire vide et comparaison exacte avec la recette. Ce test prépare une contribution ; il ne crée pas de dépôt de distribution personnel et ne publie aucun paquet.

## Sauvegarde et retour arrière

Les modifications et leurs preuves sont enregistrées uniquement sur la branche dédiée. Le candidat antérieur `99789aa0f9c82d90cef020b33444698a05aafc47` et sa recette réussie restent accessibles dans l'historique ; [CHECKS.json](CHECKS.json) conserve aussi le précédent résultat de build. L'arbre `app` complet n'a pas changé pendant cette reprise. En cas de régression des pièces de candidature, révoquer les commits concernés par `git revert` sur cette branche et restaurer ensemble recette et patch, puis relancer seulement leurs contrôles. Aucun reset, force-push, changement de `main` ou de PR nº 2 n'est nécessaire.

## Contribution après autorisation de l'accès GitLab

Suivre [CONTRIBUTING de fdroiddata](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md) : utiliser un fork public de contribution, créer une branche non protégée, y ajouter la recette, puis examiner sa CI GitLab et ouvrir la MR vers `fdroid/fdroiddata`. Le corps préparé demande explicitement la revue de l'exception et expose les notifications non validées. Le brouillon ne certifie pas tous les critères et ne demande pas une fusion sans revue. Ce fork sert à la contribution au catalogue officiel.

La CI GitLab officielle n'a pas été exécutée ici. Les builds GitHub ou locaux, le résultat `fdroid lint` et les scans ne valent pas acceptation. Si GitLab demande une carte bancaire ou un numéro pour activer les runners, le modèle officiel demande de laisser les mainteneurs déclencher la CI ; ne pas acheter de temps de calcul.

Un tag stable doit correspondre à la version réellement retenue après les contrôles ; aucun tag n'est créé pour présenter cette candidature comme une version finale. Les futures mises à jour conserveront l'identifiant, augmenteront `versionCode` et pourront être suivies via les tags configurés dans la recette. Le catalogue officiel gère sa compilation et sa signature ; aucune clé de débogage n'est déclarée comme clé officielle.

**Statut : préparation effectuée ; candidature non déposée ; application non acceptée et non publiée sur F-Droid.** Aucun lien de MR officielle ne peut être fourni avant sa création effective.
