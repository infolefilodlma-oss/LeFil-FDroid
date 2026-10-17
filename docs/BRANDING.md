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

Les nouveaux rendus ont été capturés et examinés dans un émulateur Android 15 AOSP : [accueil](validation/branding/launcher-home.png), [liste des applications](validation/branding/launcher-drawer.png), [application claire](validation/branding/app-light.png) et [sombre](validation/branding/app-dark.png). Le logo est lisible et ses parties colorées ne sont pas découpées sur ce lanceur. La [revue visuelle](validation/branding/visual-review.json) consigne les empreintes et la portée exacte. Les autres lanceurs, formes, modes d'icônes thématiques et le téléphone physique ne sont pas validés.

L'icône Fastlane de 512 × 512 est identique à l'original officiel. Deux captures réelles, non retouchées, sont fournies dans `en-US` et `fr-FR` ; leur [provenance](validation/branding/fastlane-screenshots.json) distingue le premier jeu de captures du second jeu de contrôle. La fiche du catalogue officiel ne pourra être examinée qu'après publication.

Les captures d'émulateur ne valident ni les comptes réels, ni les notifications, ni leur comportement en veille ou après redémarrage. Aucune soumission ou acceptation F-Droid n'est annoncée.

## Première rupture et correction

Le premier essai a terminé le test natif et les deux captures de l'application, puis échoué à trouver « Le Fil » dans le lanceur après un glissement simulé. Le [diagnostic conservé](validation/branding/capture-failure.txt) établit cette première rupture de l'automatisation. La correction remplace ce geste par `Intent.ACTION_ALL_APPS`, attend la liste, puis place l'icône par glissement long. La [nouvelle exécution](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39011609522) réussit, y compris le test natif existant. Aucune correction de communication ou de serveur n'est déduite de cet incident.

Les rapports [JUnit/Lint](validation/branding/android-checks.json) attestent 70 tests JVM sans échec, erreur ni test ignoré, et zéro erreur Lint / 14 avertissements. Deux avertissements `MonochromeLauncherIcon` résultent du choix explicite de conserver le logo couleur ; `IconDuplicates` signale les copies exactes utilisées par les ressources du lanceur et de la barre native. Aucun contrôle n'est désactivé. Le masque de notifications existant reste intact.

Références Android : [JavaDoc officielle de ACTION_ALL_APPS](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/core/java/android/content/Intent.java) et [AdaptiveIconDrawable, zone sûre 66/72](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/graphics/java/android/graphics/drawable/AdaptiveIconDrawable.java). Les sources Android 15 ont été consultées ; le calcul des marges est consigné dans le relevé des ressources.

## Retour arrière

Les originaux sont conservés et tous les changements sont isolés sur la branche de développement. Le dernier état avant intégration du logo est `efb8657b4597af1f9ee78ce28c41887e7b0356bc`, qui conserve déjà la correction du moteur 0.2.1 et les 70 tests. Si une régression de ressources est reproduite, un nouveau commit peut restaurer `app/src/main/res` depuis cet état, puis repasser les contrôles et réépingler la recette. Aucun retour arrière ni changement de `main` n'a été effectué.
