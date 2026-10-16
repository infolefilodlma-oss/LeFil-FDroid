# Mécanismes Android retenus et références

La couche native ne consiste pas seulement à ouvrir le site : elle consulte les deux services avec les cookies du WebView, mémorise les empreintes déjà signalées, crée les canaux Android et les `PendingIntent` immuables, puis ouvre la conversation dans `MainActivity` à froid ou via `onNewIntent`.

Le service de premier plan `specialUse` et sa justification sont déclarés dans le manifeste. Le démarrage résulte d'une activation explicite depuis l'activité. Le choix est persistant ; `BOOT_COMPLETED` et `MY_PACKAGE_REPLACED` ne démarrent la surveillance que si elle est activée. `START_STICKY` laisse Android décider d'une reprise après destruction du processus. Cela n'autorise ni contournement de l'arrêt forcé, ni exemption automatique des restrictions système/constructeur.

L'ancienne alarme inexacte ponctuelle tentait un `startService` depuis l'arrière-plan et ne se réarmait via `onStartCommand` que si ce démarrage réussissait. Après un refus Android, elle ne fournissait plus de relance ; elle est retirée, sans remplacement par des alarmes exactes ou une promesse de disponibilité continue.

Les notifications refusées ou dont le canal Messages est bloqué ne sont pas mémorisées comme livrées. Un échec HTTP, une réponse inconnue ou une session expirée ne vide pas l'historique. Les réponses partielles ne prouvent pas la lecture des conversations absentes. Les liens de notification nécessitent HTTPS et l'origine exacte Agora ou Atrium. Les marges des barres système sont appliquées à l'interface pour la cible API 35.

La notification persistante fournit un statut de reconnexion ou d'indisponibilité quand le service l'observe. Le menu donne accès aux réglages Android ; aucune exemption batterie n'est imposée au premier lancement.

À partir d'Android 8, `onRenderProcessGone` retire et détruit le WebView devenu inutilisable, puis retourne `true` pour prendre en charge la perte du moteur. Un panneau natif permet de recréer l'affichage sur la dernière URL, sans effacer les sessions ni recharger en boucle. Le menu reste disponible. Android 7 ne fournit pas ce rappel. Le déclencheur `chrome://crash` figure exclusivement dans le test instrumenté, jamais dans le code de l'application.

## Documentation officielle

- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/services/fgs/service-types#special-use
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://developer.android.com/training/monitoring-device-state/doze-standby
- https://developer.android.com/about/versions/15/behavior-changes-15

Ces pages n'étaient pas accessibles dans le réseau initial (CONNECT 403). La documentation API et les règles Android 15 ont pu être consultées dans les sources officielles AOSP :

- `Service.START_STICKY` et `startForeground` : https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/core/java/android/app/Service.java
- `AlarmManager.setAndAllowWhileIdle` : https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/apex/jobscheduler/framework/java/android/app/AlarmManager.java
- `FGS_BOOT_COMPLETED_RESTRICTIONS` : https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/services/core/java/com/android/server/am/ActiveServices.java (le type `specialUse` figure dans les exceptions Android 15 ; cela ne prouve pas la réception sur appareil).
- `WebViewClient.onRenderProcessGone`, nettoyage du WebView et déclencheur de test : https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/core/java/android/webkit/WebViewClient.java ; https://developer.android.com/reference/android/webkit/WebViewClient#onRenderProcessGone(android.webkit.WebView,android.webkit.RenderProcessGoneDetail)

## Contrats des services consultés

Les fichiers amont décrivent le format attendu, mais **pas la version installée sur les serveurs du Fil** :

- https://github.com/friendica/friendica/blob/develop/mod/message.php
- https://github.com/friendica/friendica/blob/develop/view/theme/frio/templates/mail_list.tpl
- https://github.com/friendica/friendica/blob/develop/view/templates/mail_list.tpl
- https://github.com/humhub/mail/blob/master/controllers/MailController.php (`notification-list` : cinq conversations)
- https://github.com/humhub/mail/blob/master/widgets/InboxMessagePreview.php
- https://github.com/humhub/mail/blob/master/widgets/views/inboxMessagePreview.php
- https://github.com/humhub/mail/blob/master/helpers/Url.php (`/mail/mail/index`, pas Nextcloud Talk)

Les tests utilisent des fragments synthétiques de ces structures, sans reproduire ni inclure leur code serveur. La validité des cookies, routes et réponses réelles reste un prérequis aux essais d'acceptation.
