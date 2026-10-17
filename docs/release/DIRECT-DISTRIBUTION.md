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

## Clé de diffusion préparée

Une clé RSA 4096 bits a été créée dans un conteneur PKCS12 chiffré, avec un mot de
passe aléatoire et une validité de 18 263 jours. L’alias est `lefil-release`.
Les fichiers privés sont hors du dépôt, sous permissions 0600 dans un répertoire
0700. Ils ne sont ni des artefacts Actions, ni des fichiers Git.

Seul le [certificat public](LeFil-signing-certificate.pem) est conservé ici.
Empreinte SHA-256 :

`d0755310dbd84319bbd0cb658475168cb9234cd1b60f9893a092afa19c31b902`

La conservation durable sous le contrôle du propriétaire n’est pas encore
établie. L’enregistrement sécurisé des valeurs déjà préparées nécessite une
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
champs avec l’empreinte réelle du certificat et l’URL de la future Release.
`fdroidserver 2.4.5 readmeta` et `lint -f` ont réussi sur cette proposition après
formatage canonique ; les autres champs restent identiques à la candidature.
Le [rapport](../validation/direct-release/developer-signature-metadata.json)
précise que le binaire public n’existe pas et que cette proposition n’est ni
activée dans la MR !52965 ni vérifiée par le serveur F-Droid.

Après la conservation vérifiée de la clé, contrôler l’APK signé avec ce même
certificat et préparer cette configuration F-Droid avant toute publication.
Le tag stable devra désigner le commit réellement construit. Les mises à jour
ultérieures devront augmenter `versionCode` et garder la même identité de
signature. Aucun APK non signé ou signé avec une clé d’essai ne doit devenir un
asset de Release.

GitHub documente un lien permanent vers le dernier asset portant un nom constant :
`releases/latest/download/LeFil.apk`. Ce suffixe décrit le lien futur ; il ne
désigne pas un téléchargement existant pour Le Fil à ce stade.

## Sources officielles consultées

- [Signature Android](https://developer.android.com/studio/publish/app-signing).
- [Distribution Android hors boutique](https://developer.android.com/distribute/marketing-tools/alternative-distribution).
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).
- [Liens vers les assets de Release](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases).
- [F-Droid : conservation de la signature du développeur](https://f-droid.org/docs/Reproducible_Builds/#exclusively-publishing-upstream-developer-signed-apks).

Les empreintes et extraits des documents réellement reçus figurent dans le
rapport de contrôle. Une compilation ou un succès de ce contrôle ne constitue
ni une admission F-Droid ni une validation des notifications.
