# Modules
## CSL-Scan
Module that verifies if devices are connected with SNMP, and then it recovers the CPE of the devices. The scans are
handled in parallel and after every scanned device it sends a notification to CSL-Client which calls the DB-api for
sync the data.

The sync happens every 5 minutes (@DiscoveryService.init) and when the scan is started
(@ScanWebSocketHandler.subscribeToNotifications.handleFrame). It stops the sync if scan finished.

The CSL-Client receives the information of Scan through the WS (@ endpoint /discovery/ready) and stored. Then,
at scheduled time (1 sec), the Client queries the db for last date of modification and updates it if newer
(@com.csl.intercom.services.PaginatedSynchronisation.sendData).

## CSL-Probe
Module that listens to the network flow and send alerts when the custom rules are matched. These rules and other
configurations can be customized through the API connexion. The alerts thrown by the IDS and forward through
the UDP socket.

In more detail, this module is formed by Suricata IDS and the manager. The manager create
the interface between the IDS and the CSL-Client.

In deployment, this module runs into a docker container with the IDS and the manager.

## CSL-AutoCrypt
Module that manages the certificates. It is connected through a REST API and the modifier endpoints also update
the db through the DBAPI. It is based on Vault, we can manage certificates, roles, certification authorities, ...
