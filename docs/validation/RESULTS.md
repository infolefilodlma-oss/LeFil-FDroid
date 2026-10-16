# Résultats de validation — 16 octobre 2026

## Objectif et historique

Prolonger la branche `notifications-reelles` (`b18b592d1eb4a07a6290825a9381062147ccc87b`), identique à la tête de la PR nº 2 lors de la récupération. `main` distante était `89f73297a3ee807eb79bd17f76b24d154a51e287`. La branche de correction est `codex/notifications-fdroid-validation` ; elle préserve l'historique existant. Pas de TWA réintroduite, pas de modification du VPS.

## Première rupture et correction

La référence exécute trois tests avec succès, mais Android Lint échoue sur deux erreurs `MissingPermission`. Trois tests ajoutés reproduisent ensuite les empreintes vides Agora/Frio, Agora/standard et Talk : **6 tests exécutés, 3 échecs**, avant correction. Les expressions ont été corrigées, la version du thème standard reconnue, et la révocation de permission traitée explicitement.

Les corrections suivantes rendent les transports testables, conservent l'historique sur panne ou liste tronquée, permettent l'activation/désactivation native, limitent les liens de notification aux bonnes origines, vérifient les canaux bloqués et adaptent les marges de l'interface. Les cookies reçus sont répercutés dans le CookieManager ; ce comportement n'a pas été validé sur les serveurs réels.

## Preuves automatisées

Le fichier [automated-results.json](automated-results.json) contient le commit source exact, les nombres par suite, les horodatages du runner, la commande et les SHA-256 des deux APK. Les horodatages JUnit sont en UTC.

- **60 tests exécutés, 0 échec, 0 erreur, 0 ignoré** : 32 tests de logique/lecteurs et 28 tests Android simulés (API 34 et 35).
- **Lint : 0 erreur, 19 avertissements.** Les avertissements restants concernent notamment JavaScript requis par les services, sauvegarde Android et ressources/icônes. Ils ne sont ni désactivés ni masqués par une baseline.
- `assembleDebug` et `assembleRelease` : réussis.
- Signature du paquet de débogage vérifiée par `apksigner` ; le paquet release est non signé, identifiant `net.lefil.fdroid`, version `0.2` / `2`, minSdk 24, targetSdk 35.
- Arbre `releaseRuntimeClasspath` résolu. Scans locaux F-Droid des sources épinglées et de l'APK release : aucun problème fatal signalé avec les règles embarquées de fdroidserver 2.4.5. Ce n'est pas un audit exhaustif ni un scan de règles actualisées.
- **Build local via F-Droid réussi**, depuis le commit épinglé récupéré via Git : `fdroid build --test --scan-binary --no-tarball net.lefil.fdroid:2` termine avec `1 build succeeded`. Le paquet produit est contrôlé (identifiant/version/API attendus), et son SHA-256 est conservé dans le bilan JSON. Il s’agit du mode test local, pas du serveur officiel F-Droid.
- Recette de métadonnées lue par `fdroid readmeta` et vérifiée par `fdroid lint -f`, avec les catégories officielles de `fdroiddata`.

Les tests des lecteurs utilisent un transport simulé et des fragments synthétiques correspondant aux sources amont. Les tests Robolectric vérifient notamment canaux, absence d'envoi après désactivation, canaux refusés, intents distincts, bonne URL à froid/à chaud, menu Talk HumHub et sélection du redémarrage. Ils **ne prouvent pas** la réception en arrière-plan, Doze, une connexion réelle ou une reprise de téléphone après reboot.

## Blocages et suite

Le proxy a renvoyé `CONNECT 403` pour `social.le-fil.net`, `team.le-fil.net`, `developer.android.com` et `f-droid.org`. La requête GitHub CLI vers `https://api.github.com/graphql` est également refusée. Les domaines nécessaires ont été ajoutés au brouillon de configuration ; leur enregistrement ne prouve pas leur application au réseau courant. Les sources officielles accessibles sur GitHub/GitLab ont été consultées, sans contourner le proxy.

L'accès Git HTTPS au dépôt fonctionne, y compris la publication de la branche dédiée. Le statut de CI GitHub et la création d'une nouvelle PR ne sont pas attestés via l'API bloquée. La PR nº 2 n'est pas fusionnée. Les droits Git ne sont pas assimilés à des droits API.

Aucun appareil ni compte de test authentifié n'est disponible/configuré dans cette session ; les seuls noms de variables et métadonnées de configuration ont été examinés, jamais leurs valeurs. Ne fournir aucun mot de passe ni cookie dans le chat. Utiliser les comptes d'essai dans l'application sur le téléphone de recette.

Il faut appliquer les paramètres réseau via les réglages de l'environnement, puis reprendre les vérifications des routes déployées et la [recette réelle](../PHONE-ACCEPTANCE.md). La pagination des services et les aperçus de messages identiques constituent des limites connues à résoudre avant une promesse de réception exhaustive. L'ancienneté F-Droid et la provenance des logos sont décrites dans le [dossier de candidature](../fdroid/SUBMISSION.md).

**Développé et testé automatiquement : oui. Validation réelle : non. Fusion principale : non. Soumission F-Droid : non. Acceptation F-Droid : non.**
