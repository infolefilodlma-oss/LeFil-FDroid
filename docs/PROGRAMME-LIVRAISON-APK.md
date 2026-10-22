# Livraison APK Le Fil — programme fixe (2026-10-22)

**Objectif unique :** APK Android signée, mise à jour sans désinstallation et téléchargée sur https://le-fil.net/, avec notifications natives Agora, Atrium et Talk reçues en arrière-plan et téléphone verrouillé, sans Google ni deuxième application obligatoire. Radio accessible depuis le menu du site https://radio.le-fil.net/.

## Ordre imposé

1. **Contrat de notifications** — confirmer le format Gotify officiel et refuser dans l'APK tout événement malformé ou tout lien externe. Preuve : tests unitaires. État : **VALIDÉ** — tests, analyse et compilation réussis sur le commit `b43e96c2` ([contrôle GitHub](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39738794588)).
2. **Réception native** — réutiliser OkHttp/WebSocket comme le client libre Gotify Android ; authentification par en-tête `X-Gotify-Key`, jamais par URL contenant une clé ; reconnexion automatique, rattrapage des messages manqués, aucun doublon ; raccordement au service Android existant et fonctionnement sans deuxième application. Aucun démarrage réseau tant que le VPS et les comptes ne sont pas raccordés. Preuve : tests de connexion, reconnexion, rattrapage et dédoublonnage. État : **à faire**.
3. **Émission sur VPS** — vérifier l'état réel des serveurs en lecture seule, ressources disponibles et mécanismes natifs Friendica/HumHub. Raccorder les événements Agora, Atrium et Talk à un serveur Gotify privé et aux bons destinataires, avec une seule identité par membre et sans exposer les messages. Sauvegarde/rollback obligatoire. Preuve : message test serveur → téléphone. **Aucune installation ou modification du VPS autorisée sans validation du diagnostic ciblé.** État : **à faire**.
4. **Validation Android réel** — APK candidate sur téléphone verrouillé (écran éteint, veille longue, réseau interrompu/repris), vrais messages des trois services, sans doublons ni perte de confidentialité. Les essais émulateur **ne suffisent pas**. État : **à faire**.
5. **Signature officielle et mise à jour** — reprendre les deux fichiers privés de la clé 0.2.1 dans l'ancien espace Codex ; exporter hors de Codex en stockage chiffré sans publication ; produire 0.2.2 avec **la même clé** ; vérifier empreinte du certificat et installation par-dessus 0.2.1. **Ne jamais générer une autre clé.** État : clé retrouvée et mot de passe vérifié ; accès depuis la chaîne de compilation **non établi**.
6. **Publication finale** — publier l'APK signée officielle sur GitHub Releases, ajouter/vérifier un bouton de téléchargement direct sur https://le-fil.net/ en affichage smartphone et contrôler le lien réellement téléchargé. État : **à faire**.

## Registre anti-dérive

- Dépôt : `infolefilodlma-oss/LeFil-FDroid`, branche `fix/direct-apk-0.2.2`, base `codex/direct-release-0.2.1`; ne pas réécrire `main`.
- Dernières preuves avant travaux : tests/compilation et démarrage émulateur Android 15 OK ; pas de preuve de notifications réelles en veille ; clé historique 0.2.1 conservée dans l'espace privé Codex, absente de GitHub Actions.
- Première rupture à résoudre à chaque étape, correction minimale, tests dédiés, contrôle des régressions, et STOP. Aucune refonte du site et aucune modification serveur implicite.
- La configuration actuelle qui interroge les messages périodiquement reste provisoire jusqu'au remplacement validé par Gotify ; ne pas la présenter comme une solution définitive.
- Documentation protocole utilisée : `gotify/server/model/message.go` (id, appid, extras) et `gotify/server/api/stream/stream.go` (WebSocket /stream) ; `gotify/android/README.md` (limitations Android batterie/veille).
