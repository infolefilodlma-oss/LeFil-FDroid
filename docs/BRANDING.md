# Identité visuelle officielle Le Fil

Consigne du projet : réutiliser les fichiers officiels sans recréer, recolorer ni déformer le logo.

## Originaux reçus

Les deux URL fournies par le responsable du projet ont été récupérées par GitHub Actions avec HTTPS vérifié : [exécution d'import](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39010458906). Les fichiers sont conservés à l'identique dans [branding/official](../branding/official/README.md) ; [sources.json](../branding/official/sources.json) consigne les réponses HTTP 200 et les empreintes SHA-256.

| Usage | Source officielle |
|---|---|
| Android et présentation F-Droid | `Logo-512-1.png`, 512 × 512, lettres bleues sur disque blanc |
| Barre native sur fond clair | Le même PNG, sans teinte ni réencodage |
| Barre native sur fond foncé | Ancienne variante transparente à lettres claires, conservée sans modification |
| Autre original fourni, conservé | `logo-fond-clair_lwsoptimized.webp`, 664 × 545, lettres bleues sur fond transparent |

## Intégration

Le PNG officiel est copié octet pour octet dans `mipmap-nodpi/ic_launcher.png`, `drawable-nodpi/le_fil_logo_light.png` et les deux icônes Fastlane `en-US/images/icon.png` et `fr-FR/images/icon.png`. Aucune image n'est générée ou réencodée. Android effectue lui-même la mise à l'échelle proportionnelle du carré original.

Les anciennes icônes à lettres claires sur blanc, de contraste insuffisant, sont remplacées. L'icône adaptative garde un arrière-plan blanc et une marge de 1/6 autour du carré original, pour le placer dans les 72 unités centrales du calque de 108 unités. Le dessin coloré conserve aussi les marges présentes dans l'original. Les masques des lanceurs restent sous le contrôle d'Android.

La référence facultative `<monochrome>` du lanceur est retirée pour privilégier le logo couleur demandé. Le masque existant `drawable/ic_launcher_monochrome.png`, utilisé pour les petites icônes de notifications Android, reste strictement inchangé. Ce retrait ne valide ni ne modifie la réception des notifications.

La barre native ajoute une image de 40 × 40 dp sans changer la navigation. `drawable-night/le_fil_toolbar_logo.xml` sélectionne l'ancienne variante transparente pour le thème sombre ; la variante bleue sur blanc est utilisée en thème clair. Les images des pages web restent fournies par les sites, sans recoloration injectée ni modification du VPS.

## Contrôles

Les empreintes et dimensions figurent dans [BRANDING-ASSETS.json](validation/BRANDING-ASSETS.json). Le contrôle des copies exactes et de l'absence de teinte est effectué avant compilation.

Les nouveaux rendus du lanceur Android 15 et de l'application en thèmes clair/sombre sont **en attente des captures et de leur examen**. Les autres lanceurs et le téléphone physique restent non validés. L'icône Fastlane sera contrôlée à partir du PNG réellement fourni ; la fiche du catalogue officiel ne pourra être examinée qu'après publication.

Les captures d'émulateur, si elles sont obtenues, ne valident ni les comptes réels, ni les notifications, ni leur comportement en veille ou après redémarrage. Aucune soumission ou acceptation F-Droid n'est annoncée.
