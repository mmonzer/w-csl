#!/bin/bash

CERT_FILE=../../csl-installer/config/nginx/certs/csl_hmi.crt
CACERT=cacerts.jks
PASS=changeit
CERT_ALIAS=CSL_HMI

rm $CACERT

echo "yes" | keytool -import -v -trustcacerts \
-alias $CERT_ALIAS -file $CERT_FILE \
-keystore $CACERT -keypass $PASS \
-storepass $PASS
