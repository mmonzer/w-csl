# Gradle : Mettre à jour les dépendances

Liens: https://plugins.gradle.org/ et https://central.sonatype.com/

### Préambule

Les scripts de Gradle peuvent être écrits en deux langages : Groovy et Kotlin. Dans CSL_Client, le langage Groovy a été choisi pour sa meilleure lisibilité.

La déclaration des dépendances se fait dans la section `dependencies` du fichier `build.gradle`, comme suit :

```groovy
implementation 'group:name:version'
```

### Décomposition :

- **`implementation`** : Définit dans quelle phase du cycle de vie du build la dépendance doit être incluse (autrement appelées configurations).
    - **`implementation`** : Pour la phase de compilation et l'exécution du projet, sans inclure les tests.
    - **`compileOnly`** : Pour la phase de compilation du projet uniquement.
    - **`runtimeOnly`** : Pour la phase d'exécution uniquement.
    - Pour les tests, ajoutez `test` avant chaque préfixe listé ci-dessus. Exemple : `testImplementation`.
    - **`annotationProcessor` :** Pour que la librairie soit reconnus par le processeur
- **`group:name:version`** : Spécifie la librairie utilisée avec sa version, etc. Vous pouvez retrouver toutes ces informations sur le site de Maven Central : [https://central.sonatype.com/](https://central.sonatype.com/), le référentiel central utilisé par Gradle (spécifié dans la section `repositories` du fichier `build.gradle`).

### La mise à jour des librairies

La mise à jour, c'est-à-dire le passage à la dernière version des librairies, peut être effectuée manuellement. Il suffit de consulter la dernière version disponible sur le site de Maven Central et de remplacer la partie version de la déclaration de la dépendance (vue ci-dessus).

Cependant, mettre à jour toutes les librairies manuellement peut rapidement devenir fastidieux. Divers plugins peuvent être installés pour faciliter cette tâche.

Les plugins reconnus par Gradle sont visibles sur le site suivant : [https://plugins.gradle.org/](https://plugins.gradle.org/)

**Attention** : Il est fréquent que les plugins ne soient pas reconnus immédiatement par Gradle. Un problème courant est l'incompatibilité des versions entre celles du plugin et celle de Gradle utilisée par le projet (voir la version avec `./gradlew --version`). Référez-vous aux dépôts GitHub du plugin pour obtenir des informations supplémentaires sur son implémentation (voir l'exemple dans [README.md](http://readme.md/)).

### Plugins pour mettre à jour les librairies

### Plugin 1 : Repérer les librairies à mettre à jour

Ajoutez cette importation dans la section `plugins` d'un des fichiers `buildClient.gradle` ou `buildServer.gradle` :

```groovy
id "com.github.ben-manes.versions" version "0.51.0"
```

Une fois que le projet compile correctement, le plugin sera importé et vous pourrez exécuter la tâche suivante :

```bash
./gradlew dependencyUpdates
```

Cette commande affichera la liste des librairies à mettre à jour, avec leurs versions actuelles et les versions disponibles.

### Plugin 2 : Remplacer les versions des librairies utilisées par les dernières

**ATTENTION** : Ce plugin ne prend pas en compte l'incompatibilité entre les différentes versions des librairies. Après l'exécution de la tâche, vous devrez probablement adapter les versions des librairies en résolvant les conflits (voir le guide : `resolutionConflitsGradle.md`).

Ajoutez ces importations dans la section `plugins` d'un des fichiers `buildClient.gradle` ou `buildServer.gradle` :

```groovy
id 'se.patrikerdes.use-latest-versions' version '0.2.18'
id 'com.github.ben-manes.versions' version '0.41.0'
```

Une fois que le projet compile correctement, les plugins seront importés et vous pourrez exécuter la tâche suivante :

```bash
./gradlew useLatestVersions
```

Cette commande remplacera automatiquement les versions des dépendances dans le `build.gradle` par leurs dernières versions disponibles.

Si des conflits entre les librairies émergent (ce qui est probable), référez-vous au guide : `resolutionConflitsGradle.md`.