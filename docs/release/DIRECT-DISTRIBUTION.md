# Diffusion directe de Le Fil 0.2.1

Paquet existant : `net.lefil.fdroid`, version `0.2.1`, code `3`. Sources épinglées :
`25189d316d8345a601b4dae2fa9987d286831d1e`. Le code, les tests et les logos ne changent
pas. La branche `codex/direct-release-0.2.1` est indépendante de `main`, de la PR nº 2
et de la branche de candidature F-Droid.

## Accès effectivement utilisé

Le [contrôle GitHub Actions](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39028454881)
a obtenu des réponses HTTP 200 de l’API GitHub et des documentations officielles
Android, GitHub et F-Droid. Une nouvelle activation réseau Codex n’est pas un
prérequis pour ces opérations exécutées dans GitHub Actions.

Le [résultat conservé](../validation/direct-release/preflight.json) distingue
l’accès au dépôt de l’autorisation de gérer ses secrets. Les appels de lecture
des secrets et de leur clé publique de chiffrement ont reçu
`Resource not accessible by integration (HTTP 403)` avec le jeton du workflow.
Cela ne signifie pas qu’il faut créer un nouveau compte ou un jeton personnel.

## Contrôles du build release

Le [workflow de contrôle](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39028853518)
reconstruit le même commit dans deux répertoires propres, compare les APK non
signés octet par octet et vérifie l’identifiant, la version, l’absence du mode
débogage et l’alignement ZIP. Une clé temporaire valable deux jours est utilisée
uniquement pour les contrôles d’installation et de démarrage dans l’émulateur
Android 15. Les [résultats récupérés](../validation/direct-release/release-checks.json)
confirment deux APK non signés strictement identiques, une installation, une
réinstallation avec la même signature temporaire et deux démarrages réussis de
l’application dans cet émulateur. Le tampon de crash enregistré est vide.

Empreinte des deux APK non signés :
`40b13feceb592df21e4b2e9374e4b0e3adac734fed5bbe7c207f4de13ed59d63`.
Elle correspond aussi au build F-Droid local déjà conservé dans le run
`39014443718`, sans valider le serveur officiel F-Droid.

Le premier run a terminé en échec uniquement lors de l’enregistrement Git des
rapports : les fichiers `.log` sont ignorés par le dépôt. L’ajout explicite de
ces rapports a été corrigé et leur artefact original récupéré sans reconstruire
ou réinstaller l’application. Le [contrôle de récupération réussi](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39029541592)
et sa [provenance](../validation/direct-release/evidence-provenance.json)
conservent les conclusions réelles de chaque étape et les empreintes des fichiers.

Ni cet APK d’essai, ni sa clé temporaire ne sont publiés. Cette clé est supprimée
du runner. Seuls les rapports et les captures réelles sont conservés. Ces
contrôles ne valident pas la signature de production, un téléphone physique,
les services authentifiés ou les notifications. Les 70 tests JVM existants ne
sont pas réexécutés pour des sources inchangées.

La comparaison de deux builds sur un runner commun ne constitue pas la
vérification par le serveur officiel F-Droid.

## APK signé avec la clé de diffusion

L'APK a ensuite été produit localement avec Gradle 8.7 et les outils Android
officiels, depuis le même commit. Son empreinte avant signature est strictement
identique à celle des builds précédents. La clé de diffusion existante est
utilisée localement ; aucun enregistrement de secrets GitHub n'est nécessaire
pour cette signature et aucun fichier privé n'entre dans GitHub Actions.

APK signé SHA-256 :
`b0141391fad9fe1cfea638fd33a69d4fee1839f14747320820856577f57d4cb2`.

Le [contrôle avec le certificat de diffusion](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39035916585)
a réussi : signature Android API 24 à 35, version, absence du mode débogage,
alignement ZIP, installation et réinstallation du fichier exact, deux démarrages
avec interface native et page publique affichées. L'APK relu depuis l'émulateur
Android 15 a la même empreinte. Le tampon de crash est vide. Le
[rapport](../validation/signed-distribution/result.json) et les captures sont
conservés. Les 70 tests JVM ne sont pas relancés.

Le premier essai s'est arrêté lors de l'export de l'interface après un code 137
de `uiautomator`. Son [résultat initial](../validation/signed-distribution/attempt-1-result.json)
est conservé. La collecte exige désormais un fichier XML neuf et préserve le
code de l'outil. Lors du contrôle réussi, les deux exports ont terminé avec le
code zéro ; aucune tolérance d'erreur n'a été utilisée pour ce résultat.

Le [téléchargement depuis GitHub](../validation/signed-distribution/direct-download.json)
a aussi été effectué dans l'environnement Codex : HTTP 200, empreinte et
certificat identiques. Ces contrôles ne valident pas un téléphone physique ni
les notifications Agora/Talk authentifiées.

## Publication et téléchargement vérifiés

La [Release 0.2.1](https://github.com/infolefilodlma-oss/LeFil-FDroid/releases/tag/v0.2.1)
est publiée. Le tag annoté `v0.2.1` désigne exactement le commit construit
`25189d316d8345a601b4dae2fa9987d286831d1e`.

- [Lien permanent de téléchargement Android](https://github.com/infolefilodlma-oss/LeFil-FDroid/releases/latest/download/LeFil.apk).
- [Fichier de la version 0.2.1](https://github.com/infolefilodlma-oss/LeFil-FDroid/releases/download/v0.2.1/LeFil.apk).

Le [rapport de publication](../validation/signed-distribution/publication.json)
atteste les deux téléchargements anonymes depuis GitHub Actions. Le
[contrôle distinct depuis Codex](../validation/signed-distribution/codex-release-downloads.json)
atteste HTTP 200, 4 919 502 octets, même SHA-256 et signature revérifiée après
téléchargement pour les deux URL. Aucune clé privée n'est disponible dans le
runner ou dans les assets de Release.

La création du tag a aussi déclenché automatiquement la
[CI Android standard existante](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39036317287),
qui a réussi. Les indicateurs de non-réexécution des tests JVM dans les rapports
des workflows de diffusion décrivent ces workflows ; ils ne décrivent pas cette
CI distincte, déclenchée par GitHub sur le tag. Les tests existants ne sont pas
modifiés. Le [contrôle final de préservation](../validation/signed-distribution/final-preservation.json)
conserve les références protégées et les arbres de sources inchangés.

## Clé de diffusion préparée

Une clé RSA 4096 bits a été créée dans un conteneur PKCS12 chiffré, avec un mot de
passe aléatoire et une validité de 18 263 jours. L’alias est `lefil-release`.
Les fichiers privés sont hors du dépôt, sous permissions 0600 dans un répertoire
0700. Ils ne sont ni des artefacts Actions, ni des fichiers Git.

Seul le [certificat public](LeFil-signing-certificate.pem) est conservé ici.
Empreinte SHA-256 :

`d0755310dbd84319bbd0cb658475168cb9234cd1b60f9893a092afa19c31b902`

La conservation durable sous le contrôle du propriétaire n’est pas encore
établie hors de l'environnement actuel. Une copie locale en lecture seule des
fichiers privés a été vérifiée et synchronisée sur disque, dans un répertoire
privé distinct. Elle ne constitue pas une sauvegarde indépendante externe.
L’enregistrement sécurisé des valeurs déjà préparées dans GitHub nécessiterait une
identité autorisée à gérer les secrets du dépôt :

- `LEFIL_SIGNING_KEYSTORE_BASE64` : conteneur chiffré encodé en base64.
- `LEFIL_SIGNING_STORE_PASSWORD` : mot de passe du conteneur PKCS12.

Avec ce format, le mot de passe de la clé est le même que celui du conteneur ;
un troisième secret n’est pas nécessaire. Ne pas recréer la clé entre versions.
Aucune valeur privée ni lien public vers ces valeurs ne figure dans ce document.

## Continuité avec F-Droid et publication

Android exige une identité de signature compatible pour remplacer une application
installée. Un APK release signé avec cette nouvelle clé ne peut pas remplacer
silencieusement un ancien APK de débogage signé avec une autre clé. Ne pas effacer
les données utilisateur pour forcer ce remplacement.

La recette de la MR !52965 n’utilise pas encore la signature du développeur.
Pour que F-Droid conserve cette signature, sa procédure officielle exige une
vérification reproductible, une URL `Binaries` ou `Builds.binary` et l’empreinte
`AllowedAPKSigningKeys`. Ces champs ne sont pas ajoutés à la candidature par ce
contrôle. Aucune compatibilité de mise à jour intercanaux n’est annoncée comme
validée.

Une [proposition séparée](net.lefil.fdroid.developer-signed.yml) prépare ces deux
champs avec l’empreinte réelle du certificat et l’URL de la Release.
`fdroidserver 2.4.5 readmeta` et `lint -f` ont réussi sur cette proposition après
formatage canonique ; les autres champs restent identiques à la candidature.
Le [rapport](../validation/direct-release/developer-signature-metadata.json)
décrit le contrôle antérieur à l'existence du binaire public. Celui-ci est
désormais téléchargeable, mais cette proposition n’est ni activée dans la MR
!52965 ni vérifiée par le serveur F-Droid.

La signature locale et les contrôles avec ce même certificat ont désormais
réussi. La conservation externe reste non vérifiée ; la procédure de publication
ne doit pas prétendre que les secrets GitHub ou un coffre externe sont configurés.
La proposition F-Droid reste séparée et la candidature n'est pas modifiée.
Le tag stable désigne le commit réellement construit. Les mises à jour
ultérieures devront augmenter `versionCode` et garder la même identité de
signature. Aucun APK non signé ou signé avec une clé d’essai ne doit devenir un
asset de Release.

Le lien `releases/latest/download/LeFil.apk` est désormais actif et vérifié.
Les prochaines versions directes devront conserver ce nom d'asset et la même
identité de signature pour permettre les mises à jour Android.

## Sources officielles consultées

- [Signature Android](https://developer.android.com/studio/publish/app-signing).
- [Distribution Android hors boutique](https://developer.android.com/distribute/marketing-tools/alternative-distribution).
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).
- [Liens vers les assets de Release](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases).
- [F-Droid : conservation de la signature du développeur](https://f-droid.org/docs/Reproducible_Builds/#exclusively-publishing-upstream-developer-signed-apks).

Les empreintes et extraits des documents réellement reçus figurent dans le
rapport de contrôle. Une compilation ou un succès de ce contrôle ne constitue
ni une admission F-Droid ni une validation des notifications.
