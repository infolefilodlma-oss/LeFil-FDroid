# Contrôle des communications pour la candidature

État : **préparé, non exécuté**. Ce contrôle est destiné à l'équipe technique ou de recette. Le chef de projet n'a pas à télécharger ou installer manuellement un APK. Les tests existants ne sont pas recommencés.

## Préconditions externes

- Deux comptes d'essai autorisés sur Agora et deux sur Atrium/Talk, réservés aux conversations de recette. Les secrets sont fournis par un canal sécurisé à l'opérateur ; aucun mot de passe, cookie ou jeton n'entre dans le dépôt, la MR ou un artefact.
- Autorisation d'envoyer des messages uniquement entre ces comptes. Sans celle-ci, limiter les contrôles à la connexion et à la lecture autorisée.
- Un émulateur Android 15 AOSP opérationnel et un navigateur pour l'autre compte ; accès HTTPS aux instances du Fil. Une équipe équipée peut compléter ensuite sur appareil de recette pour les restrictions constructeur.
- Sources de la recette `docs/fdroid/net.lefil.fdroid.yml`, version `0.2.1` / code `3`. L'équipe utilise le build de test existant ou compile ces sources dans son environnement d'essai. Aucune publication d'APK ou modification du VPS n'est nécessaire.

Ne pas utiliser les comptes personnels d'un utilisateur pour remplacer ces préconditions.

## Contrôles publics déjà acquis

Le [rapport HTTPS d'origine](../validation/fdroid-finalization/public-https-report.json) a été récupéré, sans répéter son exécution. L'[observation anonyme](../validation/fdroid-finalization/public-network.json) confirme les formulaires, sans envoyer de requête de connexion.

| Route | Réponse sans compte | Ce qui reste à établir avec les comptes d'essai |
|---|---|---|
| Agora `/message` | HTTP 200 avec formulaire Friendica ; NodeInfo public accessible | Soumission de la connexion avec session/jeton exigés par le formulaire, persistance des cookies, liste de messages et pagination. |
| Atrium `index.php?r=mail%2Fmail%2Findex` | Redirection vers la connexion, puis HTTP 200 avec champ mot de passe et champ CSRF | Connexion HumHub en conservant le CSRF et les cookies ; accès effectif à Mail/Talk. |
| Talk `/mail/mail/notification-list` et `/mail/mail/get-new-message-count-json` | HTTP 404 sur les routes « propres » | Le lecteur possède déjà un repli vers les routes `index.php?r=...` ; ne pas diagnostiquer à tort un échec de connexion sur ce seul 404. |
| Talk `index.php?r=mail%2Fmail%2Fnotification-list` et `index.php?r=mail%2Fmail%2Fget-new-message-count-json` | Redirection vers la connexion, puis HTTP 200 | Format authentifié réel, identifiants stables des messages, nombre d'éléments par page et éventuel chemin de pagination. |

Le transport natif ne suit pas ces redirections : il doit signaler une session requise. Une page de connexion obtenue avec un navigateur suivant les redirections n'est pas une réponse de messagerie. Les formulaires n'ont pas été soumis et aucun mot de passe ou CSRF n'a été conservé dans les preuves.

Les essais de connexion, lecture, textes identiques, pagination et réception réelle peuvent commencer sur émulateur avec autorisation des comptes. Les restrictions propres au matériel/constructeur et la veille réelle restent à contrôler ensuite sur un téléphone de recette. Ni un test JVM ni cette observation Chromium ne les remplacent.

## Séquence et critères

| Contrôle | Opération de l'équipe de recette | Preuve attendue |
|---|---|---|
| Connexion | Ouvrir Agora puis Atrium dans l'app, se connecter aux comptes d'essai et rouvrir l'app | Services et sessions accessibles ; absence de redirection vers la connexion lors d'une lecture authentifiée. HTTP 200 seul ne suffit pas. |
| Activation | Activer la surveillance et autoriser les notifications Android | Permission accordée, statut de premier plan présent, sans fermeture de l'activité. |
| Premier message | Envoyer un message balisé de recette depuis l'autre compte, sur chaque service | Alerte reçue réellement ; noter heure d'envoi, réception, délai et service. Un simple compteur ou PID ne suffit pas. |
| Conversation déjà non lue | Sans lire, envoyer un deuxième message | Nouvelle alerte correspondant à la nouvelle version de la conversation. |
| Texte identique | Envoyer deux messages identiques à des intervalles permettant une vérification entre eux | Deux nouveaux événements distingués ; toute omission est un échec à analyser, pas une validation déduite des fixtures. |
| Pagination | Créer plus de cinq conversations non lues Talk et dépasser une page Agora, avec un fil épinglé si disponible | Relever les conversations détectées ou manquées. Les lecteurs actuels ne promettent pas une couverture exhaustive. Une limitation constatée reste documentée et bloque toute affirmation contraire. |
| Lien d'alerte | Toucher les alertes de deux conversations, activité ouverte puis fermée | Bonne conversation sur le bon service ; Talk est HumHub et utilise la route `index.php?r=mail%2Fmail%2Findex`. |
| Lecture ailleurs | Lire dans le navigateur, puis envoyer un nouveau message | Nouvelle réception sans répétition indéfinie de l'ancien événement. |
| Session expirée / réseau | Faire expirer uniquement une session d'essai ou couper le réseau de l'émulateur, puis rétablir et reconnecter | Statut cohérent, reprise, historique non effacé par erreur et absence de rafale d'anciens messages. |
| Arrière-plan et veille simulée | Mettre l'activité en arrière-plan, puis utiliser le mode veille de l'émulateur ; envoyer un message de recette et réveiller l'émulateur | Délai mesuré et reprise constatée. Distinguer réception pendant la veille et réception après réveil. |
| Redémarrage | Avec surveillance activée, redémarrer puis déverrouiller l'émulateur et envoyer un nouveau message | Réception observée après reprise autorisée par Android. Le démarrage d'un service seul n'est pas une preuve. |
| Désactivation / refus | Désactiver, puis tester un refus de permission et un canal bloqué dans un profil d'essai | Pas d'alerte prétendument livrée ; réactivation sans crash ni perte d'un nouveau message non lu. |

La simulation Doze peut utiliser `adb -s emulator-5554 shell dumpsys deviceidle force-idle`, puis `adb -s emulator-5554 shell dumpsys deviceidle unforce`, exclusivement sur cet émulateur de recette. Le redémarrage utilise `adb -s emulator-5554 reboot`. Conserver les observations séparées des tests sur téléphone ; un émulateur ne démontre pas les restrictions d'un constructeur.

## Consignation et décision

Utiliser [communication-result.template.json](submission/communication-result.template.json). Renseigner commit, version Android/WebView, réglages batterie, versions serveur connues, heures et résultats. Les captures sont expurgées de données privées ; ne pas joindre de corps de message, identifiant personnel ou cookie. Les versions HumHub/Mail non établies restent « inconnues ».

En cas d'échec, isoler la première requête ou réponse qui rompt le scénario, puis corriger uniquement le code Android ou l'environnement de test. Une réponse authentifiée anonymisée peut devenir une fixture de régression avec l'autorisation de l'opérateur. Ne pas changer les serveurs ni inventer d'endpoint.

Une exécution réussie permet d'étayer la fonction native correspondante auprès des mainteneurs. Les affirmations de la fiche ne sont renforcées qu'après ces observations. L'exception d'ancienneté et l'authentification GitLab restent des sujets distincts.
