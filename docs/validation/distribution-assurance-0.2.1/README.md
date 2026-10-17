# Contrôle de distribution 0.2.1 — limites externes établies

## Résultats réellement obtenus

- [APK de diffusion 0.2.1](https://github.com/infolefilodlma-oss/LeFil-FDroid/releases/download/v0.2.1/LeFil.apk) téléchargé à nouveau : HTTP 200, SHA-256 `b0141391fad9fe1cfea638fd33a69d4fee1839f14747320820856577f57d4cb2`, certificat de diffusion inchangé. [Preuve](distribution-check.json).
- Copie chiffrée locale restaurée dans un répertoire temporaire privé neuf. La clé privée restaurée signe une donnée aléatoire, puis sa signature est vérifiée avec le certificat public attendu. La copie temporaire est supprimée. Aucune nouvelle clé ni nouvelle version d'APK n'est créée. [Résultat](key-recovery.json), [contrôle reproductible](../../../tools/KeyRecoveryCheck.java).
- Les [70 tests JVM acquis](access-and-notifications.json) sont conservés sans réexécution, avec zéro échec dans les rapports examinés. Les simulations Android 34/35 vérifient notamment les canaux, permissions, intentions de conversation et règles de relance ; elles ne prouvent pas la réception de messages authentifiés.
- Les [contrôles d'installation déjà acquis](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39035916585) ne sont pas recommencés. Ils concernent l'APK portant exactement cette empreinte et ce certificat, sur émulateur Android 15.
- `main`, la PR nº 2, le tag source, les sources Android, les logos et les fichiers de candidature F-Droid restent inchangés. [Préservation](preservation-and-blockers.json).

## Première rupture : conservation indépendante de la clé

**La sauvegarde externe durable n'est pas établie.** La récupération ci-dessus
concerne une copie sur le même volume Codex ; elle ne constitue pas une sauvegarde
indépendante. Aucun coffre externe ni aucune identité de stockage autorisée n'est
configuré dans l'environnement. Les droits de gestion des secrets GitHub ont déjà
été refusés : [preuve d'autorisation](../direct-release/final-signing-authorization.json).
L'accès de publication d'un APK ne fournit pas ces droits.

Le prérequis extérieur est un accès technique autorisé à un coffre indépendant
du projet, permettant dépôt chiffré, relecture et récupération de la même clé.
Sans destination et identité autorisées, aucun emplacement externe ne peut être
inventé ni déclaré sauvegardé. Les secrets ne sont pas demandés dans la conversation.

Les [documents officiels consultés](official-guidance.json) couvrent Android,
la conservation/récupération selon OWASP et les signatures F-Droid. L'Android
actuel rappelle que perdre sa clé hors du mécanisme Play App Signing empêche
les mises à jour. Ce projet n'utilise pas Play App Signing. Les recommandations
OWASP de sauvegarde et de plan de récupération ne sont pas présentées comme une
exigence d'admission F-Droid supplémentaire.

## Notifications : ce qui exige encore des accès réels

Les lecteurs existants réutilisent les sessions WebView Friendica/HumHub.
Aucun compte de recette destiné à ces instances n'est disponible. Aucun compte
personnel n'a été utilisé et aucun message de test n'a été envoyé.

L'inspection confirme que le lecteur Agora ne parcourt pas les pages suivantes,
et que le lecteur Talk utilise la liste d'aperçu et son compteur. Conserver
l'historique d'une liste partielle ne récupère pas les conversations absentes.
Le texte d'aperçu Talk ne constitue pas un identifiant stable de chaque nouveau
message : deux messages identiques restent un cas non résolu. Le format
authentifié déployé et ses identifiants doivent être observés avant une correction.

La [recette existante](../../fdroid/COMMUNICATION-ACCEPTANCE.md) est prête pour
un opérateur technique : deux comptes de recette par service, authentification,
messages successifs/identiques, pagination, notification réellement reçue et
conversation ouverte. Ces essais peuvent commencer sur émulateur une fois les
comptes disponibles. Le matériel et les restrictions constructeur doivent être
contrôlés ensuite sur un téléphone de recette. Aucun résultat réel n'est déduit
des fixtures ou du succès d'installation. [État détaillé](access-and-notifications.json).

## Mises à jour et F-Droid

Les prochaines versions directes doivent conserver `net.lefil.fdroid`, la même
identité de signature et augmenter `versionCode` au-delà de 3. La réinstallation
avec le même certificat a déjà réussi ; un APK signé autrement ne constitue pas
une mise à jour compatible sans mécanisme de migration adapté.

La distribution directe n'interdit pas l'inclusion F-Droid. La documentation
officielle permet la conservation du certificat développeur avec une compilation
reproductible vérifiée, `Binaries`/`Builds.binary` et `AllowedAPKSigningKeys`.
La [proposition séparée](../../release/net.lefil.fdroid.developer-signed.yml)
existe déjà, et son URL de binaire est désormais disponible. Elle n'est pas
activée dans la MR !52965 ; aucune vérification par le serveur officiel n'est
annoncée. La migration automatique entre canaux n'est donc pas garantie à ce stade.

**STOP motivé :** accès au coffre indépendant et comptes de recette manquants.
L'APK existant reste disponible, sans remplacement sous le même numéro. Une
distribution promettant des notifications exhaustives et éprouvées en veille
ne peut pas être déclarée validée.
