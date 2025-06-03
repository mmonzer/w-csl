# Gradle : Comment résoudre les problèmes de dépendances ?

Liens : https://central.sonatype.com/

Dans un projet contenant de nombreuses librairies, tel que CSL_Client, il est tout à fait normal et habituel d’utiliser différentes versions, directement ou transitivement, d’une même librairie. Cependant, il existe de rares cas où les différentes versions utilisées sont incompatibles et bloquent la compilation ou le lancement du projet.

## 1. Quelle est la cause ?

Lorsque plusieurs versions d’une même librairie sont utilisées, Gradle utilise une méthode de résolution de conflits par défaut qui consiste à prendre la version la plus récente. Si le projet utilise des méthodes ou des classes d’une version antérieure dépréciées (voire supprimées) dans la version choisie par Gradle, des exceptions telles que `ClassNotFoundException` ou `NoClassDefFoundError` (ou `NoSuchMethodError`) seront levées.

## 2. Comment résoudre ces problèmes ?

Il existe plusieurs outils que l’on peut ajouter aux projets tels que le scan de Gradle Enterprise ou Mendio. Mais en utilisant la version de Gradle basique (non Enterprise), les outils pour gérer ces problèmes sont assez limités.

Avant de trouver une solution plus avancée, voici une façon primaire pour repérer quelle dépendance pose problème et adapter ensuite le `build.gradle` pour utiliser la version la plus adaptée.

### Étape 1 : Repérer quelle est la librairie qui provoque l’exception

1. Copier-coller le nom du package levant l’exception et demander à une IA (ChatGPT, Co-pilot, etc.) quelles librairies Gradle doivent être installées pour utiliser ce package.
2. Les imports de librairies par Gradle se font sous la forme suivante (dans le langage Groovy) :

    ```groovy
    implementation 'repo:librairie:version'
    ```


### Étape 2 : Repérer quelles versions de la librairie sont utilisées

1. Si le projet inclut peu de dépendances (je conseille un maximum d’une dizaine), on peut afficher l’arbre entier des dépendances et leurs versions utilisées par Gradle avec la commande :

    ```bash
    ./gradlew dependencies
    ```

2. La solution la plus simple est d’utiliser directement la tâche pré-installée dans Gradle `dependencyInsight`, qui va afficher toutes les versions d’une librairie spécifique et détailler par quelles autres librairies elles sont utilisées :

    ```bash
    ./gradlew dependencyInsight -b buildA.gradle --dependency librairie --configuration phaseDuCycle
    ```

   **Décomposition :**

    - `./gradlew dependencyInsight` : la tâche effectuée
    - `-b build`A : le build ciblé, si votre projet ne contient qu’un `build.gradle`, cette option peut être omise
    - `-dependency librairie` : la dépendance que l’on souhaite inspecter
    - `-configuration phaseDuCycle` : quelle phase du cycle de vie du build on veut viser. Par exemple, si l’exception est levée au moment de la compilation, on utilisera `compileClassPath`; si c’est au moment du lancement, on utilisera `runTimeClassPath`. On peut aussi configurer la commande pour l’adapter aux phases de tests : `testCompileClasspath` **,** `testRuntimeClasspath`

### Étape 3 : Aligner la version de la librairie dans le `build.gradle`

C’est-à-dire choisir la version qui est la plus utilisée par les autres librairies ou qui demande le moins de changements dans le reste du projet.