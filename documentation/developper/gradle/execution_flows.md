# Execution flow
I will try to explain in a sequential way the execution flow.

## At initialization (client)
1. Initialize context
2. Register/Init services 
    - scan : init different clients and scan synchronization services.
    - autocrypt : reinit API handlers, synchronization of everything related, register to status.
    - tap : read taps configurations and create commands.
    - status : get notifier and set callbacks.
    - monitor : read taps configurations and create commands.
3. Re-init/re-register services
4. Send cmd to DBApi
5. Start WCSL websocket
6. Launch servers if required
7. Launch API server

## Scan synchronizations (client)
Runs in other thread than main, every N minutes.
Order:
1. Devices
2. Connections
3. CPE items
4. MicrosoftKB
5. CPE items deletions
6. MicrosoftKB deletions

## Autocrypt synchronizations (client)
Runs in other thread than main, every N minutes.
Order:
1. Issuers
2. Roles
3. Certificates
