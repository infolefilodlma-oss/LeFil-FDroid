# Décision de signature — clé v2 officielle (2026-10-24)

## Décision

À partir du 2026-10-24, la clé **v2** est la clé de signature officielle et
définitive de Le Fil pour toutes les futures versions. La clé **v1** est
abandonnée pour toute nouvelle version. Aucun fichier ni aucune clé n'a été
supprimé : v1 et son historique restent disponibles pour archive.

| | v1 (abandonnée pour les nouvelles versions) | v2 (officielle) |
|---|---|---|
| Certificat | `CN=Le Fil` | `CN=Le Fil, OU=infolefilodlma-oss, O=Le Fil, C=FR` |
| SHA-256 | `d0755310dbd84319bbd0cb658475168cb9234cd1b60f9893a092afa19c31b902` | `ec6b47258ca1acd76a5855ce06b533017eb893b544fb95b1db867c57d71efccc` |
| Créée le | antérieure, non datée dans ce dépôt | 2026-10-23 |
| Utilisée pour | `distribution/LeFil.apk` (0.2.1, release GitHub) | 0.2.2 et versions suivantes, dont 0.2.7-test |
| Référence publique | `docs/release/LeFil-signing-certificate.pem` (conservé tel quel, valeur historique) | `/root/secure-lefil-signing-v2/NOTICE.txt` (hors dépôt, matériel privé) |

## Conséquence pour les installations existantes

Android interdit la mise à jour d'une application avec une clé de signature
différente de celle déjà installée. Toute personne ayant installé l'APK
0.2.1 publié sur la release GitHub (signé v1) **ne pourra pas mettre à jour
en place** vers une version signée v2 (0.2.2 et suivantes). La mise à jour
exige une **désinstallation puis réinstallation**, avec perte des données
locales de l'application (sessions WebView, préférences). Il n'existe pas
de mécanisme permettant d'éviter cette rupture : elle est inhérente au
changement de clé.

Cette rupture ne concerne que les appareils ayant installé l'APK 0.2.1
GitHub. Les appareils n'ayant encore rien installé n'ont aucune action à
faire : ils reçoivent directement une version signée v2.

## Canal de distribution F-Droid (décision du 2026-10-24)

La soumission au catalogue officiel F-Droid utilise la **signature
standard F-Droid** : F-Droid compile et signe lui-même l'APK distribué
via son dépôt, avec sa propre clé, comme c'est l'usage pour la majorité
des applications du catalogue. La clé **v2** reste réservée exclusivement
à la distribution directe (`le-fil.net` / `aide.le-fil.net`) et n'est
pas transmise à F-Droid.

Conséquence déjà documentée plus haut : un appareil installé depuis
F-Droid et un appareil installé directement depuis le-fil.net portent
des signatures différentes et ne peuvent pas se mettre à jour l'un vers
l'autre sans désinstallation. Ce sont deux canaux de distribution
distincts et non interchangeables, par choix.

La variante de recette « developer-signed »
(`docs/release/net.lefil.fdroid.developer-signed.yml`, conservation de
la signature v2 via vérification de reproductibilité binaire et un
APK publié en `Binaries`) n'est **pas retenue** pour la soumission
actuelle. Elle reste dans le dépôt comme option documentée pour une
éventuelle évolution future, non comme une soumission en cours.

## Conservation

- `docs/release/LeFil-signing-certificate.pem` (certificat public v1) reste
  dans le dépôt comme référence historique de l'APK 0.2.1 déjà distribué.
  Il ne doit plus être présenté comme la clé officielle courante.
- Le matériel privé v2 (`/root/secure-lefil-signing-v2/`) n'est pas et ne
  doit pas être versionné dans ce dépôt Git.
