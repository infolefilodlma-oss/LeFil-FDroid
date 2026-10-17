# Identité visuelle officielle Le Fil

Consigne du projet : réutiliser les fichiers officiels déjà validés, sans recréer, recolorer ni déformer le logo.

| Usage | Fichier à utiliser |
|---|---|
| Fond foncé | Variante officielle transparente prévue pour ce fond |
| Fond clair | Logo officiel bleu avec son fond blanc |
| Icône Android, classique et adaptative | Privilégier le logo officiel bleu avec son fond blanc |
| Icône et présentation F-Droid | Privilégier le même fichier officiel bleu sur fond blanc |

Conserver les proportions et les marges du dessin. Aucun filtre de couleur, suppression de fond ou dessin de remplacement ne doit servir à fabriquer un fichier officiel manquant. Les masques appliqués par les lanceurs Android doivent conserver le logo lisible.

## État des fichiers examinés

Les ressources introduites par `e27d5547210dd4b9e0bc18dc1641538cde1392d1` contiennent un logo à lettres claires, motif doré et feuille verte. La ressource couleur transparente est `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png` (432 × 432). Les images classiques `ic_launcher.png` et `ic_launcher_round.png` placent ce dessin sur blanc, en cinq densités. L'examen visuel montre une perte de contraste des lettres sur ce fond.

**Le fichier officiel bleu sur fond blanc est absent du dépôt examiné.** Il doit être fourni avant de remplacer les icônes Android ou d'ajouter l'image de publication F-Droid. Aucun des fichiers présents n'est recoloré pour le fabriquer.

`drawable/ic_launcher_monochrome.png` est un masque monochrome existant, également utilisé pour les petites icônes de notifications Android. Il ne remplace ni le logo couleur transparent ni le logo bleu sur blanc. Les références `<monochrome>` des icônes adaptatives exposent ce masque aux icônes thématiques Android : cet usage devra être revu avec le fichier officiel, afin de conserver l'identité couleur demandée. Aucun changement des notifications n'est effectué pour contourner les règles d'affichage du système.

Les dimensions, transparences et empreintes de toutes les images présentes figurent dans [BRANDING-ASSETS.json](validation/BRANDING-ASSETS.json). Les fichiers graphiques sont conservés à l'identique.

## Emplacements à contrôler après réception du fichier

1. **Écran d'accueil Android :** icône classique et icône adaptative, formes ronde et carrée, fonds clairs et foncés, icônes thématiques activées et désactivées. Vérifier les marges, le contraste, la lisibilité du nom et l'absence de découpe. Un examen des PNG ne valide pas un rendu réel du lanceur.
2. **Dans l'application :** démarrage et thème clair/foncé. La barre native actuelle affiche le titre « Le Fil » sans image ; les logos des pages proviennent du site. Vérifier le choix des variantes dans le contenu réellement affiché, sans modifier le VPS ni injecter une recoloration dans le WebView.
3. **Présentation F-Droid :** réutiliser le fichier bleu sur blanc dans les images Fastlane de publication, vérifier ses proportions et les limites des aperçus. Les captures doivent représenter l'application réelle avec les bons fichiers. La fiche publiée ne pourra être vérifiée qu'après publication effective.

État : inspection des ressources effectuée ; intégration du logo bleu et contrôles de rendu Android/application/F-Droid **en attente du fichier officiel manquant**. Aucun résultat sur téléphone, émulateur ou catalogue publié n'est annoncé.
