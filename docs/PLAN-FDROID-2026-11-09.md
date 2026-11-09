# Plan de livraison vérifiable — Le Fil sur F-Droid officiel
Date : 9 novembre 2026. Responsable de coordination technique : assistant, interventions sur GitHub contrôlées.
Objectif prioritaire : que **Le Fil** soit accepté puis publié dans le catalogue officiel F-Droid. **Le Fil Talk** est un chantier séparé, sans conditionnement de la publication de Le Fil. Ne pas confondre F-Droid, Google Play et téléchargement APK direct.

## État établi, sans extrapolation
- Le code Android de Le Fil est dans `infolefilodlma-oss/LeFil-FDroid`; la branche `distribution-directe` a compilé avec tests unitaires/lint et réussi une installation/démarrage sur émulateur Android 15 (run GitHub Actions 42578954242). C'est **un test technique**, pas un test complet en usage réel ni une publication.
- Issue GitHub #1 (15 octobre) : l'application générale est un WebView du site existant ; les notifications Android de messages réels Agora/Atrium ne sont **pas implémentées** à la date de ce diagnostic. Ne pas fusionner de simulations en prétendant avoir les notifications opérationnelles.
- La MR officielle F-Droid `fdroiddata!52965` apparaît **Closed** sur la capture fournie par le propriétaire. Son motif, la chronologie des commentaires et les pipelines ne sont **pas encore vérifiés** ; ne pas affirmer qu'elle a été rejetée pour une cause déterminée.
- Le site WordPress `le-fil.net` et son accueil validé sont un **chantier séparé** : ne rien publier sur LWS pour corriger une MR F-Droid. Les APK de debug ne doivent pas être présentés comme livrables F-Droid.

## Ordre strict — un chantier à la fois
### Jalon A. Comprendre la décision de F-Droid (priorité immédiate)
1. Lire `fdroiddata!52965` : auteur de la fermeture, tous les commentaires, erreurs des 2 pipelines, diff du fichier `metadata/net.lefil.fdroid.yml`, éventuelle autre MR ou fusion.
2. Produire un diagnostic sourcé : (a) fusionnée, (b) fermée à corriger puis rouvrir/ressoumettre, (c) refus de politique ; **ne choisir qu'après vérification**.
**Preuve de sortie** : liens vers commentaires/pipelines et texte du motif exact. **Blocage actuel** : consultation de GitLab non disponible depuis les outils de cette session ; n'inventer ni preuve ni motif.

### Jalon B. Stabiliser l'application réellement attendue
1. Comparer `main`, la branche de test et l'issue #1 ; préserver application unique, source WebView, liens, connexion et fichiers.
2. Vérifier le parcours sur appareil Android : ouvrir accueil, Agora, Atrium, retour, authentification, téléchargement/fichiers. Ne toucher aux fonctions du site que sur défaut constaté.
3. Clarifier l'exigence notifications de messages existants (pas de notifications fictives ni promesse d'interrogation toutes les 10–15 s en arrière-plan sans validation des limites Android). Implémenter seulement le mécanisme compatible et réalisable, puis prouver une notification réelle de bout en bout, y compris appli fermée, si cette fonction reste un critère de livraison.
**Preuve de sortie** : tests ciblés, logs horodatés, capture fonctionnelle, aucune régression de navigation.

### Jalon C. Préparer la publication F-Droid officielle
1. Vérifier licence du code et des médias, dépendances libres, absence de code précompilé/interdictions, conformité à la politique officielle F-Droid.
2. Ajouter/contrôler les métadonnées Fastlane (français puis anglais si pertinent), icône, captures réelles, changements, versionCode/versionName.
3. Assurer tag Git stable, source reproductible et test `fdroid build` sur les métadonnées finales.
**Preuve de sortie** : logs de build F-Droid réussis et manifeste, code et métadonnées cohérents.

### Jalon D. Faire accepter puis vérifier la publication
1. À partir du constat A, corriger la MR existante si possible, sinon ouvrir une nouvelle MR justifiée, en liant l'ancienne.
2. Traiter les retours des mainteneurs et les pipelines jusqu'à merge **effectif**.
3. Vérifier page publique `f-droid.org/packages/net.lefil.fdroid/`, version distribuée et installation + mise à jour depuis le client officiel.
**Preuve finale** : page F-Droid accessible + installation/mise à jour réussies. **Date externe non garantie** : les mainteneurs de F-Droid décident de l'acceptation et du délai.

## Attribution et garde-fous
- **Assistant** : pilotage, inspection GitHub, modifications limitées aux fichiers nécessaires, tests, préparation des preuves. Ne pas déclarer les étapes externes achevées sans contrôle.
- **GitHub Actions** : exécution automatisée des builds et tests, pas « développeur » autonome.
- **F-Droid** : examen et publication officielle, délai hors de notre contrôle.
- **Utilisateur** : intervenir seulement si une permission/connexion ou décision impossible à déduire est démontrée ; demander alors **une seule action exacte**.
- Avant écriture : vérifier fichiers existants, historiques, branche, sauvegarde/retour arrière. Pas de modification de `main`, de production VPS ou du site LWS par ce plan. Aucune signature personnelle à générer pour remplacer la signature de publication officielle F-Droid.
- **Le Fil Talk** : application différente, builds de diagnostic et tests distincts ; ne pas retarder ni mélanger le dossier de Le Fil.

Références :
- https://github.com/infolefilodlma-oss/LeFil-FDroid/issues/1
- https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/42578954242
- https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52965
- https://f-droid.org/docs/Inclusion_Policy/
- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
