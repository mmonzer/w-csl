#!/bin/bash

CERT_FILE=../csl-docker/config/nginx/certs/csl_hmi.crt
#CERT_FILE=../csl-docker/config/nginx/certs/csl_hmi.crt
CACERTS_PATH=cacerts.jks
PASS=changeit
CERT_ALIAS=CSL_HMI

rm "$CACERT"

echo "yes" | keytool -import -v -trustcacerts \
-alias $CERT_ALIAS -file $CERT_FILE \
-keystore $CACERTS_PATH -keypass $PASS \
-storepass $PASS
