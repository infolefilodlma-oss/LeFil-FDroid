# Recette réelle — à exécuter, pas encore validée

Utiliser deux comptes d'essai et deux appareils ou un appareil + navigateur. Aucun mot de passe ni cookie ne doit être ajouté au dépôt ou aux rapports. Il n'est pas nécessaire de modifier le VPS pour commencer cette recette.

Tester au minimum Android 7/8, 13, 14 et 15 ; inclure Android 16 et un appareil avec restrictions constructeur avant de prétendre couvrir les versions Android récentes. Un émulateur peut compléter ces essais ; il ne remplace pas un appareil pour la veille et les restrictions constructeur.

| Étape | Manipulation | Résultat attendu | État |
|---|---|---|---|
| Démarrage | Installer le candidat `0.2.1` / code `3` en conservant les données, puis ouvrir et refermer l'app | L'app reste ouverte et affiche l'accueil ; relever immédiatement modèle, Android et symptôme en cas d'échec | À faire sur le téléphone concerné |
| Accès | Ouvrir le menu et les entrées Agora, Atrium, Talk | Friendica, HumHub puis sa messagerie s'ouvrent ; aucun Nextcloud Talk | À faire |
| Connexion | Se connecter séparément à Agora et Atrium, fermer puis rouvrir l'app | Sessions conservées selon les réglages serveur ; aucun secret dans les logs | À faire |
| Activation | Activer les notifications depuis le menu | Explication puis permission Android 13+, notification permanente si autorisé | À faire |
| Refus | Refuser la permission, puis l'autoriser dans les réglages | Pas de crash ; activer la surveillance ensuite et recevoir le message encore non lu | À faire |
| Messages | Envoyer un message depuis l'autre compte, sur chacun des deux services | Une alerte par conversation/version, pas par simple ouverture du WebView | À faire |
| Conversation déjà non lue | Envoyer un second message, puis deux messages de texte identique | Nouvelle alerte attendue ; relever si l'empreinte HTML ne distingue pas les messages identiques | À faire |
| Routage | Toucher deux alertes de conversations différentes, app fermée puis ouverte | Chaque alerte ouvre la bonne conversation et le bon service | À faire |
| Lu ailleurs | Lire un message dans le navigateur, puis en envoyer un autre | L'état de lecture et la nouvelle version sont correctement détectés | À faire |
| Pagination | Dépasser cinq conversations non lues Talk et une page Agora ; inclure les conversations épinglées | Documenter toute conversation manquée ; bloquant pour une promesse de réception exhaustive | À faire |
| Arrière-plan | Quitter l'interface, verrouiller 30 min puis plusieurs heures | Réception observée et délai mesuré ; ne pas déduire le succès d'un PID | À faire |
| Doze | Tester écran éteint, économie batterie puis réseau coupé/rétabli | Pas de crash ni perte d'historique ; reprise après restauration réseau/veille | À faire |
| Session expirée | Révoquer la session serveur puis se reconnecter dans l'app | Statut de reconnexion, reprise sans notifications répétées pour les mêmes empreintes | À faire |
| Redémarrage | Redémarrer et déverrouiller le téléphone avec surveillance activée | Reprise autorisée par Android ; vérifier par réception d'un nouveau message | À faire |
| Désactivation | Désactiver, puis redémarrer le téléphone | Aucune surveillance relancée ; les services restent accessibles | À faire |
| Arrêt forcé | Forcer l'arrêt depuis Android | Pas de promesse de relance automatique ; rouvrir l'app rétablit la surveillance activée | À faire |
| Canal bloqué | Désactiver uniquement le canal Messages, puis le réactiver | Messages non marqués comme notifiés tant que le canal est bloqué | À faire |
| Smartphone | Tester grande police, mode sombre, clavier, rotation et découpes/barres Android 15 | Menu, formulaire et conversation utilisables | À faire |
| Pièce jointe | Choisir puis annuler un fichier et ouvrir un téléchargement | Choix/annulation sans blocage du prochain essai | À faire |
| Perte du moteur | Si l'affichage s'interrompt sous Android 8+, toucher Recharger puis changer de service dans le menu | Un panneau explicite remplace le WebView perdu ; reprise sans fermeture ni effacement des sessions. Ne pas provoquer de crash sur un téléphone personnel | À faire sur téléphone ; test automatique réservé à un émulateur sans comptes |

Pour chaque essai, noter : commit, SHA-256 de l'APK, modèle/API Android, heure, réglages batterie, serveur/version, cas, résultat réel, délai et preuve anonymisée. Ne pas conserver le corps des messages privés ni les cookies.

## Limites développées / limites non résolues

- Le service `specialUse` s'appuie sur `START_STICKY` et les broadcasts de reboot/mise à jour. Ce n'est pas une garantie de fonctionnement continu. Aucun contournement de l'arrêt forcé ou des restrictions Android n'est ajouté.
- Le polling utilise les pages authentifiées existantes, pas une API de push. Agora annonce publiquement Friendica 2026.05-1595 ; la version HumHub et les sessions natives restent non vérifiées. Les URL simplifiées Atrium renvoient 404 : utiliser les liens `index.php?r=...` du candidat corrigé.
- HumHub limite `notification-list` à cinq conversations ; Friendica pagine les messages. Le client conserve l'historique lorsqu'une liste est partielle mais cela ne récupère pas toutes les pages.
- Une empreinte fondée sur le nombre de messages Agora ou l'aperçu Talk ne constitue pas un identifiant serveur de message universel. Les aperçus identiques et changements de format doivent être testés et peuvent nécessiter un endpoint serveur documenté fournissant un identifiant stable.
- Aucun accès serveur authentifié ni aucune réception sur téléphone n'est attesté par les tests automatisés.

[Mécanismes et références officielles Android](ANDROID-MECHANISMS.md).
