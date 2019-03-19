# Tesla API & Logger

Compiled streaming logger can be used standalone with `java -jar TeslaApi-0.1.jar -u <username> -p <password>`. The first vehicle will be used and streamed to the CSV `logs/vehicle_<vin>.csv` (logs directory needs to be already created).

Client implementation targeted for the streaming websocket endpoint in [TeslaClient](src/main/scala/com/zuyezheng/tesla/api/TeslaClient.scala).

Actor with reconnect and reauthentication to stream to CSV in [TeslaLogger](src/main/scala/com/zuyezheng/tesla/TeslaLogger.scala)
