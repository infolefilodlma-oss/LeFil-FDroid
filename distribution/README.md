# Distribution indépendante — Le Fil

Objectif : distribuer Le Fil et Le Fil Talk sans dépendre de l'admission au catalogue officiel F-Droid.

## Étapes de livraison (bloquées tant que les preuves manquent)

1. Identifier les APK produits par les compilations GitHub et leur provenance exacte.
2. Vérifier pour chaque APK le nom de paquet, la version, la signature, la clé de signature conservée et la somme SHA-256.
3. Publier une première version téléchargeable sur GitHub Releases, sans modifier l'APK.
4. Préparer une page d'installation accessible sur le-fil.net, sans publier de lien avant la présence effective du fichier.
5. Préparer un dépôt F-Droid indépendant pour les mises à jour, avec clés et sauvegardes sécurisées ; vérifier une mise à jour réelle avant annonce.
6. Refaire les étapes 1 à 5 pour Le Fil Talk, dans son propre dépôt source si distinct.

Ne pas fusionner la PR notifications #4 sans validation réelle. Ne pas toucher à Google Play, au VPS ou à la branche main pour cette préparation.
