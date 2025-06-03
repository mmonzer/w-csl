# Threads :
The CSL-Client has multiple threads running in parallel for the dealing for the different asynchronous tasks. 

Here there is a non-exhaustive list of the threads subjects:

- autoDelete files [client side] (uncorrelated) com.csl.util.FileStorageService.FileStorageService : autodeletes (temporary?) files for discovery service
- sync all discovery [client side] (uncorrelated) main.services.DiscoveryService.init : sync all in discovery service
- sync all autocrypt [client side] (uncorrelated) main.services.AutoCryptService.launchAutoSync : sync all in autocrypt service
- start import bson [client side] (uncorrelated??) com.csl.intercom.cslscan.services.ImportExportBsonService.init : start a new import task from the queue.
- handle import/export bson [client side] (uncorrelated) com.csl.intercom.cslscan.services.ImportExportBsonService.init : calls the handlers of the different export/import tasks. This handlers will overwrite the XCorrelationId. TODO: this is dirty coded.
- timeout msg ws client-server [server side] (correlated) com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.startTimeOutDetector : Stop ws requests to CSL-Client if timeout.
- reconnect ws client-server [client side] (uncorrelated) main.CSLIDSMainClient.openWsConnectionWithCSLServer : reconnects the CSL WS if disconnected
- keep alive ws client-server [client side] (uncorrelated) main.CSLIDSMainClient.openWsConnectionWithCSLServer : keeps alive the CSL WS if connected
- status notifier [client side] (uncorrelated) com.csl.intercom.status.StatusNotifier.StatusNotifier :  Sends notifications of the current status of the different communication interfaces.
- FileLog [client side] (?) com.csl.defaultclasses.FileLog.startLog : saves UDP logs to file. TODO : coded like in Stone Age
- activity Monitor [client side] (uncorrelated) com.csl.monitor.ActivityMonitor.startTicTask : sends monitor of activity to the IHM.
- UDPClient + UDP processor (3) [client side] (?) com.csl.udp.CSLFlowManager.startListener : Up to 3 threads. The first one receives the UDP messages and put it to queue and its treated by the second one.
- keep alive ihm ws [server side] (uncorrelated) com.csl.web.CSLHttpServerJetty.startRefreshWebSocketTask keeps alive the IHM WS
- authentication [not used?] (?) com.csl.web.auth.AuthentificationManager.AuthentificationManager
- Json Thread factory [everywhere] (?) com.ucsl.json.Json.* : json format
- WS message handler [client side] (correlated with message) main.CSLIDSMainClient.handleServerMessage : handles incoming messages from CSL WS.
- Scan sanitizer [client side] (uncorrelated) com.csl.intercom.services.CpeScanService.init : Check the scans in scanEntities, to get their status from CSL-Scan directly, and handle them accordingly.
- Scan handling [client side] (correlated) com.csl.intercom.services.CpeScanService.scheduleScanHandlingIfNecessary : Check if the scans handling task is currently scheduled, and if not start it.
- sync ext connection info [client side] (uncorrelated) com.csl.intercom.services.ExternalConnectionInfoSynchronizationService.ExternalConnectionInfoSynchronizationService : Synchronize external connection infos
- sync ext connection info templates [client side] (uncorrelated) com.csl.intercom.services.ExternalConnectionInfoTemplatesSynchronizationService.ExternalConnectionInfoTemplatesSynchronizationService : Synchronize external discovered devices
- reconnect ws scan [client side] (uncorrelated) com.csl.intercom.cslscan.ScanWebSocketHandler.ScanWebSocketHandler : Try to reconnect to CSL-Scan WS.
- reconnect mqtt [client side] (uncorrelated) com.csl.intercom.broker.CSLMqttBrokerHandler.CSLMqttBrokerHandler : Try to reconnect to mqtt server.
- timeout mqtt [client side?] (uncorrelated) com.csl.intercom.broker.ApiMessageSender.connectClientToSend : If pending message has timed out, mark as error.
- Process Utils (3) [not used?] (?) com.csl.util.ProcessUtil.* : mystery
- Nmap (2) [?] (?) lib.unpacked.org.nmap4j_csl.core.scans.BaseScan.executeAsynchronousScan : nmap scan ?
