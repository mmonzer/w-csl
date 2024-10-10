# Threads :
The CSL-Client has multiple threads running in parallel for the dealing for the different asynchronous tasks. 

Here there is a non-exhaustive list of the threads subjects:
- autoDelete files
- sync all discovery
- sync all autocrypt
- export bson
- import bson
- reconnect ws  client-server
- keep alive ws client-server
- status notifier
- FileLog ???
- activity Monitor
- UDPClient
- UDP processor
- refresh ihm ws
- authentication, unsued?
- Json Thread factory
- WS message handler
- Ssh utils
- Scan sanitizer
- File sanitizer
- sync ext connection info
- sync ext connection info templates
- reconnect ws scan
- reconnect mqtt
- timeout mqtt