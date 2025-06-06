# Organisation of project

## Repository

### Folder : `documentation`
Contains documentation about the project. For the moment there is only a sketch of the developer documentation.
### Folder : `gradle`
Contains the configuration for the gradle executable.
### Folder : `src`
Contains the code of the project. This is detailed in section Code organisation
### Folder : `build` (git ignored)
Contains all the building information: classes, resources, jars, ...
### Folder : `downloads` (git ignored)
Contains all the downloads (deprecated?)
### Folder : `logs` (git ignored)
Contains all the logs created by the application by default.

### File : `.dockerignore`
Ignored files for docker.
###  File : `.gitignore`
Ignored files for git.
###  File : `.gitattributes`
Some git configuration.
###  File : `build.gradle`
Gradle configuration common to server and client
###  File : `buildClient.gradle`
Gradle configuration for client. It uses the `build.gradle` file.
###  File : `buildServer.gradle`
Gradle configuration for server. It uses the `build.gradle` file.
### File : `cacerts.jks`
Certification for secured connexions.
### File : `create_certificate_store.sh`
Script that stores the certification file for the secured connexions.
### File : `CurrentIDSParams.json`
Some flags for the IDS (deprecated?).
### File : `Dockerfile`
Dockerfile for the docker configuration for the CSL-Client.
### File : `Dockerfile.srv`
Dockerfile for the docker configuration for the CSL-Server.
### File : `entrypoint.sh`
Docker entrypoint script for CSL-Client adn CSL-Server.
### File : `gradle.properties`
Some properties of the gradle executable.
### File : `gradlew`
Gradle executable for Linux.
### File : `gradlew.bat`
Gradle executable for Windows.
### File : `settings.gradle`
Some more settings for the gradle executable.

---

# Code organisation
The code follows a more conventional architecture thanks to the work of Lola Tosetto.
In the main code folder(`src`), we have a `main` folder, for the app code, and a `test` folder, for the app tests.
Both follow the same architecture, `java` folder for code/test and a `resources` folder for static resources.

I will briefly detail the `src/main/java` folder and then the `src/main/resources`. The day we have tests, we may complete
with the `src/test/java` folder and then the `src/test/resources`.

## Java source code : `src/main/java`
Contains all packages for the project CSL.
### Subfolder `com/csl`
Contains many classes of the W-CSL. There are many folders, which are relatively organized:
- `alert` : Contains all alert managing files.
- `core` : Contains all the global Context files.
- `defaultclasses` : Generic classes.
- `expections` : Exception classes.
- `ids` : Contains a class for the IDS.
- `intercom` : Contains different packages for managing the communication with the
  different connexions: APIHandlers, MQttHandlers, ... It contains also some json formating tools. 
  It also contains many scan specific classes. It also contains classes about the status service.
- `interfaces.models` : Contains some serializable interfaces. 
- `logger` : Contains some tools for logging.
- `monitor` : Contains classes for the activity history and monitoring.
- `udp` : Contains classes for UDP connexions and data flow.
- `util` : Contains helper tools.
- `web` : Contains tools/handlers/classes for connexion. It's meant to contain proxy, low level web sockets, or servers.
### Subfolder `com/ucsl`
There is a folder `interfaces` for bulk interfaces, but not only. 
There is a `json` folder with the Json class and the corresponding JsonUtils. This is a keystone of the project, as
the Json class is used **everywhere**.


### Subfolder `com/main`
Main classes, services and some tests for W-CSL. Some services contain also some business logic.
- `CSLIDSMainClient` : main method for CSL-Client. This file has much more than only the main method.
- `CSLIDSMainServer` : main method for CSL-Server. This file has much more than only the main method.
- `test` : dummy tests for API runner and UDP connexion. Left in place in order to one day convert them in **real** tests.
- `services` : definition of each service.
