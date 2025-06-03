# Getting started

## 1 CSL-installer
In `CSL-installer` project:
```bash
$ .setup.sh
....
Do you want to login using you username/token [Y/n]  # Y (first time, n to keep previous login)
...
Should CSL create a self-signed certificate ? [y/N]  # y to create a new self-signed certificate
...
Download CPE/CVE database? [Y/n] # not necessary
...
Creating the api key ...
API KEY -5iP5HtAE.Z4s3SgsYUJPazCM7XbRyBAq3bXCzXHVG- has been created   # API key to save : 5iP5HtAE.Z4s3SgsYUJPazCM7XbRyBAq3bXCzXHVG
The environment variables file for csl client is created in csl_client.env
...
Tap`s name [tap01]: # Enter to have the default name
```

## 2 CSL-W (client and server)
In the `w-csl` project, we need to store the certificate. We need first to add the right path to the certificate file,
in my case : `CERT_FILE=../csl-installer/config/nginx/certs/csl_hmi.crt`
```bash
nano ./create_certificate_store.sh
```
Now we can just execute the script ( if problem with key tools we need to install openjdk)
```bash
./create_certificate_store.sh   # may need change if computer in other language that english
```

## 3 Check connexion
Now we can verify that everything runs as excepted ( the API key is ok):
```bash
docker logs -f csl_client
```
This logs may contain errors due to missing modules or db.
We should just check that we are connected to the server:
```bash
...
main.CSLIDSMainClient: Connected to server
...
```

## 4 Configure IntelliJ

This is an old Eclipse project, so we need to download the JDK v17 ~~Temurin v11~~ at:

`Project Structure > Project Settings > Project > SDK > Download JDK`

Now lets configure the run/debug configuration:
- Create a new Gradle~~Application~~ configuration (plus in the top right)
- Name it
- Select command : `clean build run -b buildClient.gradle -x test` or `clean build run -b buildServer.gradle -x test`
- Gradle project : `w-csl`
- Add the following environment variables:
```
CSL.ALERT_VIEWER.IP=localhost
CSL.DISCOVERY.MANAGER_IP=localhost
CSL.DISCOVERY.MANAGER_PROTOCOL=http
CSL.DISCOVERY.MANAGER_TIMEZONE=UTC
CSL.GLOBAL.API_KEY=5iP5HtAE.Z4s3SgsYUJPazCM7XbRyBAq3bXCzXHVG  # the API key saved above
CSL.GLOBAL.FORCE_HOST_NAME_RESOLUTION=true
CSL.GLOBAL.IP_SERVER_REMOTE=localhost
CSL.GLOBAL.LAUNCH_WEB_API_SERVER=true
CSL.GLOBAL.PORT_SERVER_REMOTE=0
CSL.GLOBAL.SERVER_REMOTE_URL_PREFIX=/jws
CSL.GLOBAL.USE_SSL=true
CSL.IDS_CONF.HISTORY_LENGTH=60
```
or copy and paste
```
CSL.ALERT_VIEWER.IP=localhost;CSL.DISCOVERY.MANAGER_IP=localhost;CSL.DISCOVERY.MANAGER_PROTOCOL=http;CSL.DISCOVERY.MANAGER_TIMEZONE=UTC;CSL.GLOBAL.API_KEY=5iP5HtAE.Z4s3SgsYUJPazCM7XbRyBAq3bXCzXHVG;CSL.GLOBAL.FORCE_HOST_NAME_RESOLUTION=true;CSL.GLOBAL.IP_SERVER_REMOTE=localhost;CSL.GLOBAL.LAUNCH_WEB_API_SERVER=true;CSL.GLOBAL.PORT_SERVER_REMOTE=0;CSL.GLOBAL.SERVER_REMOTE_URL_PREFIX=/jws;CSL.GLOBAL.USE_SSL=true;CSL.IDS_CONF.HISTORY_LENGTH=60
```
Before continue, change the API key with the new one created above.

- Add some options to the VM execution:

`Modify options > Add VM options`

and copy-paste:
```
-Dlogback.configurationFile=resources/logback.xml
-Djavax.net.ssl.trustStore=./cacerts.jks
-Djavax.net.ssl.trustStorePassword=changeit
```

NOTE: if running there is an error about the version of java. We may need to define the right version for the compiler:

`Settings > Build, Execution, Deployment > Compiler > Java compiler`

## 5 Run the client
For instance, we will run the client out of the container:
```
docker stop csl_client
```
It may take several seconds to stop. Then, we can launch the client from IntelliJ with the configuration from above.
Again some errors may happen because of missing modules, but we will observe that some services are initialized and some API endpoints registered.

The API should now work, and we can test it with Postman sending a POST to `localhost:9900/status` with the following body:
```json
{
    "cmd":"get_status"
}
```
An answer similar to the following is expected:
```json
{
    "discovery" : {
        "is_http_api_reachable" : true,
        "is_websocket_connected" : true
    },
    "taps" : {
        "active_taps" : []
    }
}
```
