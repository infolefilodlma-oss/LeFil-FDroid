# Candidature officielle F-Droid — préparée, non déposée

Le Fil reste la même application Android `net.lefil.fdroid`, version candidate `0.2.1` / code `3`. La finalisation du dossier est isolée sur `codex/fdroid-submission-readiness`, à partir des travaux validés de `codex/notifications-fdroid-validation`. `main`, `notifications-reelles` et la PR nº 2 sont préservées.

## État réel

Les pièces techniques sont préparées : licence MIT, sources publiques, recette de build, descriptions `en-US` et `fr-FR`, changelogs, procédure de signature et de mises à jour, preuves existantes et contrôle de la nouvelle recette. La nouvelle source épinglée est `ccb61022e84a242a5e5989b9a629ac7c90a47a56` ; seul le texte Fastlane change par rapport au code Android validé.

Les corrections du dossier sont limitées à trois écarts établis : suppression des promesses non validées de notifications, déclaration `TetheredNet` pour les instances fixes du Fil et ajout de l'auteur `infolefilodlma-oss`. Les descriptions sont dans le dépôt applicatif, conformément au modèle officiel de MR ; elles ne sont pas dupliquées dans `fdroiddata`.

L'admissibilité complète reste à établir. La [vérification des critères](ADMISSIBILITY.md) sépare exigences, recommandations et résultats observés. Le [bilan des contrôles](CHECKS.json) distingue les tests acquis de ceux exécutés pour cette préparation.

## Points externes encore ouverts

1. **Ancienneté ou exception.** La politique officielle actuelle contient bien le critère de trois mois de développement actif, avec exceptions pour une application ayant des utilisateurs, un auteur maintenant d'autres applications F-Droid ou une application événementielle. L'historique public vérifié commence le 14 octobre 2026 ; aucun historique antérieur ou élément d'exception n'est reçu. Le texte exact et sa révision officielle figurent dans le bilan d'admissibilité.
2. **Fonctionnement des communications.** Les 70 tests JVM et le test natif acquis ne valident pas les messages authentifiés, les alertes reçues, les textes identiques, la pagination ni la veille réelle. Les notifications sont désormais décrites comme expérimentales. La [recette préparée pour l'équipe technique](COMMUNICATION-ACCEPTANCE.md) peut commencer sur émulateur avec des comptes d'essai autorisés ; aucune installation manuelle n'est demandée au chef de projet.
3. **Compte GitLab.** La lecture publique des documents et de `fdroiddata` fonctionne. L'API du compte répond HTTP 401 et aucun accès GitLab authentifié n'est configuré. Un compte autorisé à contribuer à `fdroid/fdroiddata` sera indispensable pour le dépôt, une fois les conditions de candidature établies. Les identifiants ne doivent pas être envoyés dans le chat.

Les captures sont recommandées et demandées par le modèle de MR ; la documentation générale les rend facultatives pour l'admission. La comparaison binaire indépendante est également recommandée. Elles ne sont pas ajoutées comme obligations générales inventées. Le dépôt déclare MIT pour ses ressources ; aucun avis contradictoire n'a été relevé dans les pièces examinées.

## Contribution prête à relire

- Recette canonique : [net.lefil.fdroid.yml](net.lefil.fdroid.yml).
- Fichier à ajouter dans le dépôt officiel : `metadata/net.lefil.fdroid.yml`.
- Patch de cette seule addition : [net.lefil.fdroid.patch](submission/net.lefil.fdroid.patch).
- Titre de MR : **New app: Le Fil**.
- Corps suivant le modèle officiel, avec cases non acquises laissées ouvertes : [MERGE_REQUEST.md](submission/MERGE_REQUEST.md).
- Sources documentaires, empreintes et contrôles d'accès : [OFFICIAL-SOURCES.json](OFFICIAL-SOURCES.json).

Le patch est contrôlé par application dans un répertoire vide et comparaison exacte avec la recette. Ce test prépare une contribution ; il ne crée pas de dépôt de distribution personnel et ne publie aucun paquet.

## Dépôt après levée des points ouverts

Suivre [CONTRIBUTING de fdroiddata](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md) : utiliser un fork public de contribution, créer une branche non protégée, y ajouter la recette, puis examiner sa CI GitLab et ouvrir la MR vers `fdroid/fdroiddata`. Ce fork sert à la contribution au catalogue officiel.

La CI GitLab officielle n'a pas été exécutée ici. Les builds GitHub ou locaux, le résultat `fdroid lint` et les scans ne valent pas acceptation. Si GitLab demande une carte bancaire ou un numéro pour activer les runners, le modèle officiel demande de laisser les mainteneurs déclencher la CI ; ne pas acheter de temps de calcul.

Un tag stable doit correspondre à la version réellement retenue après les contrôles ; aucun tag n'est créé pour présenter cette candidature comme une version finale. Les futures mises à jour conserveront l'identifiant, augmenteront `versionCode` et pourront être suivies via les tags configurés dans la recette. Le catalogue officiel gère sa compilation et sa signature ; aucune clé de débogage n'est déclarée comme clé officielle.

**Statut : préparation effectuée ; candidature non déposée ; application non acceptée et non publiée sur F-Droid.** Aucun lien de MR officielle ne peut être fourni avant sa création effective.
