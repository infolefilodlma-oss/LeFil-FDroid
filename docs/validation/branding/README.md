# Rendus et contrôles du logo officiel

Le PNG officiel bleu sur blanc est utilisé sans modification de ses octets. La variante transparente existante à lettres claires est conservée pour le thème sombre. Les [originaux et leurs empreintes](../../../branding/official/README.md) et les [consignes d'identité visuelle](../../BRANDING.md) permettent de vérifier leur provenance.

## Captures réelles examinées

Les captures suivantes proviennent d'un émulateur AOSP Android 15, profil Pixel 2, sans compte utilisateur. Elles sont conservées sans recadrage ni retouche.

| Emplacement | Image | Résultat de l'examen |
|---|---|---|
| Accueil Android | [Accueil](launcher-home.png) | Logo bleu sur blanc lisible, dessin coloré non découpé |
| Liste des applications | [Liste](launcher-drawer.png) | Même logo, nom « Le Fil » visible |
| Application, thème clair | [Clair](app-light.png) | Logo bleu sur blanc dans la barre native |
| Application, thème sombre | [Sombre](app-dark.png) | Variante transparente à lettres claires sur fond sombre |

[Exécution des captures et du test natif](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39011609522), [résultat brut](result.json), [examen visuel et empreintes](visual-review.json).

Les deux captures Fastlane en/fr proviennent du premier jeu de captures de la même application : [provenance exacte](fastlane-screenshots.json). Les arrière-plans du site peuvent varier entre captures. Aucune capture de notification ou de conversation privée n'est fabriquée. La fiche publiée F-Droid, les autres lanceurs, les icônes thématiques et le téléphone physique ne sont pas validés.

## Contrôles exécutés

| Contrôle | Résultat | Preuve |
|---|---|---|
| Tests JVM existants | 70 réussis, aucun échec, erreur ni test ignoré | [Relevé](android-checks.json), [XML JUnit](junit) |
| Android Lint | Zéro erreur, 14 avertissements | [Rapport XML](lint-results-debug.xml) |
| Reprise native après deux terminaisons réelles du moteur | Un test réussi sur Android 15, 3,28 s | [Journal natif](native-test.txt) |
| Recette F-Droid épinglée, compilation et scans source/binaire | Réussis | [Résultat original du constructeur](fdroid/result.json), [exécution](https://github.com/infolefilodlma-oss/LeFil-FDroid/actions/runs/39011672052) |

La recette F-Droid vise `99789aa0f9c82d90cef020b33444698a05aafc47`. Son arborescence `app` est identique à celle des contrôles Android et des captures. Les sources Java/Kotlin, les tests et les déclarations Gradle antérieurs sont conservés.

Le [premier essai de capture du lanceur](capture-failure.txt) a échoué après la réussite du test natif et des captures clair/sombre. Le geste simulé a été remplacé par l'action documentée `ACTION_ALL_APPS` et une attente de la liste. La nouvelle exécution réussit ; les assertions et les tests existants ne sont pas désactivés.

Ces résultats ne valident pas les sessions authentifiées Agora/Talk, les notifications réelles, la veille, Doze ou le redémarrage. La candidature officielle F-Droid reste non déposée, non acceptée et non publiée ; consulter le [dossier et ses points ouverts](../../fdroid/SUBMISSION.md).
