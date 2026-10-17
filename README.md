# LeFil-FDroid
Application Android officielle du réseau Le Fil — Agora, Atrium, Talk et Radio.

Développement des notifications : branche `notifications-reelles`, PR #2 ; les corrections de validation sont réalisées sur une branche dédiée. Talk est la messagerie **HumHub** intégrée à Atrium, pas Nextcloud Talk.

Le menu natif ouvre Agora, Atrium et Talk et permet d'activer/désactiver la surveillance des messages. Se connecter d'abord aux deux services dans l'application. Cette surveillance utilise des sessions WebView et du polling sans services Google ; la veille et les restrictions Android peuvent la retarder. La réception exhaustive reste à vérifier (pagination et empreintes des messages).

- [Compilation, dépendances et signature F-Droid](docs/fdroid/BUILD.md)
- [Recette sur téléphone et comptes réels](docs/PHONE-ACCEPTANCE.md)
- [État de candidature F-Droid](docs/fdroid/SUBMISSION.md)
- [Critères officiels vérifiés et écarts restants](docs/fdroid/ADMISSIBILITY.md)

L'APK de débogage sert uniquement aux essais. Aucun résultat de compilation ou test simulé ne prouve une réception sur téléphone, ni une admission au catalogue F-Droid.

Dernière vérification : [reprise du moteur et candidat 0.2.1](docs/validation/RENDERER-RECOVERY.md) — 70 tests JVM réussis et un test natif réussi dans l'émulateur Android 15. La panne signalée sur téléphone reste à recontrôler ; les essais authentifiés et de notifications sur téléphone restent à effectuer.

Finalisation F-Droid : descriptions corrigées pour qualifier la surveillance d'expérimentale, auteur et `TetheredNet` renseignés, recette épinglée et contribution officielle préparée. [Bilan de préparation](docs/fdroid/CHECKS.json). La candidature n'est pas encore déposée ; admission et publication ne sont pas acquises.
