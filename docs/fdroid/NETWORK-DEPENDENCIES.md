# Dépendances réseau — Le Fil 0.2.1

Déclaration établie à partir du code candidat et d'observations publiques du 17 octobre 2026. Aucun compte n'a été utilisé, aucun formulaire soumis et aucun serveur modifié. Les descriptions Fastlane signalent désormais les ressources tierces effectivement observées, sans annoncer de notifications validées.

## Application et services

| Usage | Destination / comportement | Dépendance et preuve |
|---|---|---|
| Accès d'accueil | `https://le-fil.net/` | Site WordPress ; licence amont GPL-2.0-or-later. |
| Agora | `https://social.le-fil.net/message` | Friendica ; licence amont AGPL v3. Le formulaire de connexion répond, sans lecture authentifiée. |
| Atrium / Talk | `https://team.le-fil.net/`, `index.php?r=mail%2Fmail%2Findex` | HumHub et module Mail/Messenger, jamais Nextcloud Talk. HumHub propose une licence libre AGPL-3.0-only ; la fiche officielle Mail déclare AGPL v3. Versions déployées HumHub/Mail inconnues. |
| Surveillance native facultative | HTTPS uniquement vers `social.le-fil.net` et `team.le-fil.net` | Requêtes avec les cookies de session du WebView. Redirections HTTP non suivies ; connexion requise signalée sur redirection, refus ou formulaire de connexion. Aucun courtier push Google. Fonction désactivée par défaut et non validée avec des comptes réels. |
| Navigation Web | Domaine `le-fil.net` et ses sous-domaines dans le WebView ; liens de navigation externes transmis au navigateur système | Ce filtre de navigation **ne filtre pas les sous-ressources** des pages. JavaScript, stockage DOM et cookies tiers sont activés. Les pages distantes et le navigateur externe peuvent contacter d'autres hôtes. |

Les instances ne sont pas configurables : `TetheredNet` est déclaré dans la recette. Les licences ci-dessus sont celles des logiciels amont ; elles ne constituent pas un audit des versions, thèmes ou extensions réellement déployés. Les URL et empreintes des textes consultés figurent dans [server-licenses.json](../validation/fdroid-finalization/server-licenses.json).

## Ressources tierces observées sans connexion

| Hôte HTTPS | Pages observées | Rôle visible |
|---|---|---|
| `fonts.googleapis.com` | Accueil Le Fil, connexion Atrium/Talk | Feuilles CSS Google Fonts, chargées automatiquement. |
| `fonts.gstatic.com` | Accueil Le Fil, connexion Atrium/Talk | Fichiers de police Poppins `.woff2`. |
| `s.w.org` | Accueil Le Fil | Image emoji WordPress. |
| `social.le-fil.net` | Agora `/message` | Aucune autre origine observée dans cette fenêtre publique. |

Les requêtes de ressources transmettent au minimum au serveur destinataire les informations réseau et HTTP nécessaires à leur chargement. Le relevé n'enregistre ni cookies, ni en-têtes, ni jetons, ni corps de messages. Les valeurs de paramètres sont supprimées, sauf la route publique HumHub. Il ne prouve pas l'absence de suivi : le marqueur public « Site Kit by Google » est présent dans l'accueil, sans requête d'analytics identifiée dans la fenêtre observée. Ce marqueur seul ne prouve ni un suivi actif ni son absence après connexion ou interaction.

Dans un contrôle distinct bloquant les hôtes Google, les liens publics Agora/Atrium/Talk et le champ de mot de passe Atrium restent visibles. Cela démontre un repli de ces contrôles publics, pas le fonctionnement authentifié ni l'indépendance complète des sites vis-à-vis de Google. La définition officielle de `NonFreeNet` vise les applications promouvant ou dépendant entièrement d'un service propriétaire ; une police distante ne suffit pas ici à démontrer cette dépendance totale. `NonFreeNet` et `Tracking` ne sont pas affirmés sur cette seule base ; les requêtes et le marqueur sont explicitement exposés à la revue F-Droid dans les descriptions et notes de maintenance. Toute observation ultérieure de suivi ou de dépendance propriétaire devra actualiser cette classification.

Les éléments embarqués Android n'utilisent pas de GMS/Firebase : les **68 modules sélectionnés de `releaseRuntimeClasspath`** ont une déclaration Apache 2.0 dans leur POM officiel. [Relevé complet](../validation/fdroid-finalization/runtime-dependencies.json), avec versions et empreintes des artefacts/POM. Les modules de test, les outils de construction et le WebView fourni par Android sont des périmètres distincts. L'absence de SDK propriétaire embarqué ne signifie pas absence de requêtes vers Google depuis les pages.

## Portée et limites

Le navigateur d'observation est Chromium, avec un profil anonyme neuf par service, sans soumission de formulaire, durant une fenêtre bornée après chargement. Ce n'est pas un test du WebView Android. Les requêtes non en lecture et les médias sont bloqués. La liste décrit ce qui a été vu ; elle n'est pas une liste exhaustive des requêtes possibles après connexion, défilement, lecture de contenus fédérés, lecture multimédia ou modification des sites. Les ressources des conversations et contenus utilisateurs peuvent dépendre d'autres domaines.

Les contrôles HTTPS existants ont été récupérés de leur artefact d'origine, sans les relancer. Les URL Talk « propres » renvoient 404 ; les routes `index.php?r=...` conduisent au formulaire de connexion. Ce comportement est déjà pris en charge par le repli du lecteur Android. HTTP 200 sur ce formulaire ne signifie pas réception de messages.

Preuves : [relevés et provenance](../validation/fdroid-finalization/README.md). Les tests restants avec des comptes d'essai autorisés et, séparément, sur téléphone sont détaillés dans [COMMUNICATION-ACCEPTANCE.md](COMMUNICATION-ACCEPTANCE.md).
