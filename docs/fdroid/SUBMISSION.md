# Candidature au catalogue officiel F-Droid — préparation, non soumise

## État

Le code de candidature prolonge `notifications-reelles` et la PR nº 2 du dépôt public `infolefilodlma-oss/LeFil-FDroid`. Il ne remplace pas les travaux existants par une TWA. Les corrections sont isolées sur `codex/notifications-fdroid-validation` ; aucune fusion dans `main`, publication officielle ou acceptation F-Droid n'est annoncée.

Éléments préparés : licence MIT, sources Android/Kotlin, recette de compilation épinglée, métadonnées françaises/anglaises Fastlane, changelog, procédure de compilation et de signature/mise à jour, recette d'acceptation et bilan des contrôles. Le nom de paquet reste `net.lefil.fdroid`.

## Conditions restantes avant dépôt

1. Exécuter et signer le bilan de la [recette réelle](../PHONE-ACCEPTANCE.md) : connexion, lecture des deux services, alertes, conversations, veille et reboot. Les fixtures et Robolectric ne remplacent pas ces preuves.
2. Vérifier les versions réellement déployées et la pagination. Si la détection par aperçu manque deux messages identiques ou une conversation hors des premières pages, corriger avec les mécanismes documentés réellement disponibles ; ne pas inventer d'API ni modifier le VPS sans diagnostic.
3. Vérifier la provenance et le droit de redistribution des logos, ainsi que les licences/conditions des services et scripts Web affichés. L'application native n'embarque pas GMS/Firebase, mais l'audit des sites distants reste distinct.
4. La politique d'inclusion consultée exige normalement **trois mois de développement actif**, sauf utilisateurs déjà établis, auteur maintenant d'autres applications F-Droid, ou application événementielle. Le premier commit de ce dépôt date du **14 octobre 2026**. Justifier une exception avec des faits publics si elle existe ; sinon attendre l'ancienneté requise. Ne pas inventer un historique antérieur.
5. Finaliser l'intégration seulement après les contrôles concluants ; fixer version et tag stable. La version `0.2` / code `2` est actuellement une candidature, sans tag de publication créé ici.
6. Exécuter la recette dans l'infrastructure F-Droid (`fdroid build --server`) et le scanner actualisé. Les outils locaux `readmeta`, `lint` ou un scan avec signatures embarquées ne valent pas admission ni validation de reproductibilité sur leur serveur.
7. Ajouter de vraies captures d'écran de téléphone (sans données privées). Aucune image de faux écran ou preuve de notification n'est produite.

## Proposition de texte de soumission

> Le Fil (`net.lefil.fdroid`) est une application Android sous licence MIT donnant accès à Agora (Friendica), Atrium (HumHub) et Talk, la messagerie HumHub. Une couche native assure une surveillance optionnelle des conversations et l'ouverture des messages depuis les notifications, sans services Google. Les limites du polling et de la veille sont exposées dans l'interface et les descriptions. Sources : https://github.com/infolefilodlma-oss/LeFil-FDroid. Les métadonnées et instructions de compilation figurent dans le dépôt. Joindre ici, **après validation réelle**, les preuves d'essai, le tag stable, la justification d'éligibilité et la recette de build finale.

Ce texte reste une proposition ; il n'a pas été envoyé à F-Droid.

## Procédure de soumission

Suivre le guide officiel : https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/. Préparer une MR sur `fdroid/fdroiddata` avec `metadata/net.lefil.fdroid.yml`, puis répondre aux contrôles et demandes des mainteneurs. Les descriptions Fastlane seront lues depuis les sources validées. En cas d'acceptation, les versions ultérieures conserveront l'identifiant et augmenteront `versionCode` ; la vérification des tags permettra leur prise en charge par F-Droid.

Ne pas soumettre le paquet de débogage comme version officielle. Aucun APK n'a été publié, aucune demande d'inclusion n'a été envoyée et aucune admission n'a été obtenue au cours de cette mission.
