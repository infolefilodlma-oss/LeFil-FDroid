# LeFil-FDroid
Application Android officielle du réseau Le Fil — Agora, Atrium, Talk et Radio.

Développement des notifications : branche `notifications-reelles`, PR #2 ; les corrections de validation sont réalisées sur une branche dédiée. Talk est la messagerie **HumHub** intégrée à Atrium, pas Nextcloud Talk.

Le menu natif ouvre Agora, Atrium et Talk et permet d'activer/désactiver la surveillance des messages. Se connecter d'abord aux deux services dans l'application. Cette surveillance expérimentale, désactivée par défaut, utilise des sessions WebView et du polling sans services Google ; la veille et les restrictions Android peuvent la retarder. La réception exhaustive reste à vérifier (pagination et empreintes des messages).

- [Compilation, dépendances et signature F-Droid](docs/fdroid/BUILD.md)
- [Recette sur téléphone et comptes réels](docs/PHONE-ACCEPTANCE.md)
- [État de candidature F-Droid](docs/fdroid/SUBMISSION.md)
- [Critères officiels vérifiés et écarts restants](docs/fdroid/ADMISSIBILITY.md)
- [Logos officiels et contrôle de l'identité visuelle](docs/BRANDING.md)
- [Captures Android 15 et rapports de contrôle du logo](docs/validation/branding/README.md)

L'APK de débogage sert uniquement aux essais. Aucun résultat de compilation ou test simulé ne prouve une réception sur téléphone, ni une admission au catalogue F-Droid.

Dernière vérification : [reprise du moteur et candidat 0.2.1](docs/validation/RENDERER-RECOVERY.md) — 70 tests JVM réussis et un test natif réussi dans l'émulateur Android 15. La panne signalée sur téléphone reste à recontrôler ; les essais authentifiés et de notifications sur téléphone restent à effectuer.

Finalisation F-Droid : descriptions corrigées pour qualifier la surveillance d'expérimentale, auteur et `TetheredNet` renseignés, recette épinglée et contribution officielle préparée. [Bilan de préparation](docs/fdroid/CHECKS.json). La candidature n'est pas encore déposée ; admission et publication ne sont pas acquises.

La [déclaration réseau](docs/fdroid/NETWORK-DEPENDENCIES.md) distingue les bibliothèques Android libres des ressources Web tierces observées (Google Fonts et emoji WordPress). Le [relevé de finalisation](docs/validation/fdroid-finalization/README.md) conserve les licences des 68 modules de production et les contrôles publics ; il ne valide pas les messages authentifiés.

Identité visuelle contrôlée : logo officiel bleu sur blanc dans le lanceur et le thème clair, variante transparente existante dans le thème sombre. Les [captures réelles et rapports](docs/validation/branding/README.md) attestent 70 tests JVM et un test natif réussis, Lint sans erreur (14 avertissements), compilation et scans de la nouvelle source F-Droid réussis. Les contrôles portent sur un émulateur Android 15 ; ils ne valident pas les notifications réelles.
