# Le Fil Talk — programme d'exécution (11 novembre 2026)

## Périmètre validé
Deux applications distinctes : Atrium (HumHub complet) et Le Fil Talk (messagerie HumHub seule). Même serveur HumHub, mêmes profils, conversations, fichiers et groupes. Conserver le module `messenger-contacts` et le Messenger HumHub natif. Ne pas modifier Agora, WordPress, Radio ou les projets Matrix/XMPP. Logo rond fourni par le propriétaire, fond opaque, Poppins si disponible. Pas de remplacement des dépôts/branches existants.

## Règles de protection
Avant toute intervention serveur : lire `CLAUDE.md`, `ETAT-AGORA.md`, les références HumHub/Talk et REX disponibles, établir un état de référence, sauvegarder les fichiers et données concernées et fournir un retour arrière vérifiable. Aucun changement en production avant validation sur un compte pilote. Toute régression : arrêt et retour arrière. Ne jamais affirmer qu'une notification, une connexion ou un appel fonctionne sans essai réel sur Android.

## Étapes / critères de sortie

1. **Inventaire et récupération (lecture seule).**
   - Retrouver `REFERENCE_TECHNIQUE_TALK_LE_FIL.md`, les fichiers du module `messenger-contacts` et leur version active, le statut `mail`, versions HumHub, mécanismes de connexion actuels (LDAP/SSO ou local), configuration de l'auto-inscription et des e-mails.
   - Vérifier le comportement actuel de `https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex` sous authentification.
   - Sortie : inventaire précis, sauvegarde/référence prête, aucun accès utilisateur perdu.

2. **APK Talk isolée — prototype sans toucher au serveur.**
   - Branche existante : `prototype-talk-humhub-20261111` du dépôt LeFil-FDroid; identifiant Android `net.lefil.talk.humhub`.
   - Charger uniquement le Messenger Atrium dans la WebView; préserver cookies, pièces jointes et retour Android; liens hors Atrium dans navigateur.
   - Intégrer le logo fourni comme icône Android, valider les ressources avant compilation.
   - Sortie : `assembleDebug` + lint OK, APK identifiable et installable à côté des autres applications; test réel connexion/lecture/envoi/photo sur smartphone.

3. **Comptes HumHub autonomes (projet de migration séparé).**
   - Identifier l'authentification réellement utilisée pour chaque utilisateur; **ne jamais créer de doublon ni supprimer les comptes existants**.
   - Concevoir la transition des comptes SSO/LDAP YunoHost vers l'authentification locale HumHub, préserver leurs IDs, profils, messages, pièces jointes, contacts et groupes.
   - Tester la réinitialisation du mot de passe (e-mail expédié et reçu), connexion Atrium et Talk, puis nouvelle auto-inscription avec protections anti-spam et règles d'accès privés.
   - Sortie : pilote vérifié, procédure reproductible, rollback et validation avant généralisation. YunoHost peut rester hébergeur sans gérer l'identité.

4. **Notifications Android.**
   - Examiner les possibilités exactes du module Messenger, `fcm-push`, la configuration Firebase et une passerelle d'APK native; **une WebView seule ne suffit pas pour supposer des push fiables**.
   - Implémenter le minimum nécessaire, sans modifier Atrium hors périmètre.
   - Sortie : réception du message notification téléphone verrouillé / application fermée, clic ouvrant la bonne conversation, absence de doublon et de fuite de données de conversations privées.

5. **Appels audio/vidéo.**
   - Vérifier séparément l'intégration Jitsi existante, les permissions micro/caméra et la navigation dans l'APK.
   - Sortie : tests appels sortants puis entrants avec sonnerie téléphone verrouillé; faute de preuve, afficher « non disponible », sans déclarer de parité WhatsApp.

6. **Publication sûre.**
   - Signature de distribution durable, versionCode, install/upgrade sans perte de données, contrôle des permissions et qualité.
   - Sortie : APK téléchargeable directement, vérification de l'empreinte SHA-256, instructions de mise à jour, validation utilisateur.

## État constaté lors de la création du programme
- Branche prototype créée; trois modifications enregistrées et relues (URL du Messenger, applicationId distinct, label Le Fil Talk).
- Le workflow GitHub Android du dépôt source existe et compile `assembleDebug` + `lintDebug`, mais aucun run de la branche n'a été prouvé et aucun APK n'est validé.
- Aucun réglage du serveur HumHub/YunoHost, utilisateur ou notification n'a été changé.
- **Étape courante : 1 (inventaire) et 2 (prototype partiellement préparé).** Ne pas ouvrir une phase suivante comme « terminée » sur seule présence du code.
