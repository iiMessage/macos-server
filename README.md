# iiMessage
#### Improved iMessage. Improved how? This one's for everyone.

## macOS Server
The macOS Server runs on your mac. It constantly checks for changes in the messages database, and when one is found, it is encrypted and forwarded to the connected clients (see [android client](https://github.com/iiMessage/android-client) and [web client](https://github.com/iiMessage/web-client)). It also listens for incoming messages, and relays them through the Messages program on your mac. The server program (and the mac) must be running in order to send or receive messages with the client.

## Requirements
* A macOS (or formerly, OS X) device with a public IP address
* iCloud account with iMessage
* Java 8 or above
* A client to send/receive the messages

## Setup
1. Download [the latest version of macOS Server](https://github.com/iiMessage/macos-server/releases)
2. Open terminal (search in spotlight)
3. Change to the downloads directory  
    `cd ~/Downloads`
4. Run the file  
    `java -jar macos-server.jar`  
    or, if you want to change the port: `java -jar macos-server.jar #`
5. Type in any password (you won't see anything changing, but it's working) and press enter
6. Open up one of the clients, and connect with the given IP and the password you just entered

## Compiling from source
1. Clone the iiMessage macOS Server onto your mac  
    `git clone https://github.com/iiMessage/macos-server.git`
2. Change to the server directory  
    `cd macos-server`
3. Compile the jar file  
    `gradlew shadowJar`
4. Run it!  
    `java -jar build/libs/macos-server-all.jar`