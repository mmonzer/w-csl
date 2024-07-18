#!/bin/bash

jarfile=$1
LOGBACK_CONFIG_FILE=resources/logback.xml

if [ "$USE_SSL" == "true" ]; then
    CERT_FILE=/etc/certs/nginx.cert
    CACERT=cacerts.jks
    PASS=changeit
    CERT_ALIAS=CSL_HMI

    echo "yes" | keytool -import -v -trustcacerts \
    -alias $CERT_ALIAS -file $CERT_FILE \
    -keystore $CACERT -keypass $PASS \
    -storepass $PASS

    java -Djavax.net.ssl.trustStore=$CACERT -Djavax.net.ssl.trustStorePassword=$PASS -Dlogback.configurationFile=$LOGBACK_CONFIG_FILE -jar $jarfile

else
    java -Dlogback.configurationFile=$LOGBACK_CONFIG_FILE -jar $jarfile
fi