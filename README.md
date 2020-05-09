# hyperledger-fabric-java-sample

In This Project you will find a Sample application written in Java and Kotlin Languages that interact with Hyperledger Fabric Network. <br/>
## The Following capabilities are available:
1. REST API that encapsulates the Blockchain network operations (using the Hyperledger Fabric Java SDK) in order to be available for many developers that doesn't have Blockchain knowledge yet.
2. Using Hyperledger Fabric CA (Certificate Authorities) to Register and Enroll Users.
3. Create or Use(ReCreate) a Channel Programmatically.
4. Deploy Chaincode Programmatically (Including all Approvals and Validations)
5. Use of Private Collections to demonstrate privacy between Organizations within the same Channel.
6. Hyperledger Fabric Version 2.1

### Prerequisites:
* Java 8 and above
* Maven
* Gradle
* Docker and Docker Compose
* Basic knowledge of Blockchain and Hyperledger Fabric.
  * https://hyperledger-fabric.readthedocs.io/en/release-2.0/index.html
* Hyperledger Fabric tools - Please refer to https://hyperledger-fabric.readthedocs.io/en/release-2.0/install.html for further information

### Deploy a Network:
The network.sh file will be the main script to work with in order to operate the network
  * Start a Network - `./network.sh up`
  * Start a Network with CA - `./network.sh up -ca`
  * Start a Network with CA and CouchDB instead of LevelDB - `./network.sh up -ca -s couchdb`
  * Stop a Network - `./network.sh down`
> The Network implementation relies on the Hyperledger Fabric Test Network with modification.<br/>
You can choose to work with any Hyperledger Fabric network as long as you have the Admin CA materials for Peers and Orderer <br/>
> Make sure Hyperledger Fabric tools (aka tools in the bin directory) available in the $PATH

### Run the Application:
`cd chaincode` <br/>
`gradle installDist` <br/>
`cd app` <br/>
`mvn clean package` <br/>
`mvn spring-boot:run` <br/>
Base Url will be: http://localhost:8080
* Deploy Chaincode with Channel Creation: http://localhost:8080/deployChainCode?createChannel=true
* Deploy Chaincode and Use Existing Channel: http://localhost:8080/deployChainCode?createChannel=false
* The app uses a Counter Chaincode for demonstration:
  * Increment (aka "Public" for all Orgs) - http://localhost:8080/increment/{orgName}?delta={number}
  * Increment (aka "Public" for all Orgs) - http://localhost:8080/increment/{orgName}?delta={number}&private=true&collection={collectionName}
    * Delta is Optional, Defaults to 1
    * Private collection names configured in the Network e.g. http://localhost:8080/increment/org1?private=true&collection=Org1AndOrg2
  * Decrement (aka "Public" for all Orgs) - http://localhost:8080/decrement/{orgName}?delta={number}
  * Decrement (aka "Public" for all Orgs) - http://localhost:8080/decrement/{orgName}?private=true&collection={collectionName}
    * Delta is Optional, Defaults to 1
    * Private collection names configured in the Network e.g. http://localhost:8080/increment/org3?private=true&collection=Org3Only
  * Counts - http://localhost:8080/counts/{orgName}

### Configuration:
```
hyperledger:
    fabricTLS: true
    fabricCATLS: true
    channel:
        name: counterchannelkotlin
        profile: ThreeOrgsChannel
        configPath: ../../network/configtx
        txFile: ../../artifacts/counterchannel.tx
    organizations:
        -   name: org1
            mspId: Org1MSP
            domain: org1.delobz.com
            caName: ca-org1
            peers:
                -   name: peer0
                    url: grpcs://localhost:7051
                    adminUserName: admin
                    users:
                        -   appUser
                    props:
                        pemFile: ../../network/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/server.crt
                        # clientKeyFile: 
                        # clientCertFile: 
                        hostnameOverride: peer0.org1.delobz.com
                        sslProvider: openSSL
                        negotiationType: TLS
            orderers:
                -   name: orderer
                    url: grpcs://localhost:7050
                    props:
                        pemFile: ../../network/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/server.crt
                        # clientKeyFile: 
                        # clientCertFile: 
                        hostnameOverride: orderer.delobz.com
                        sslProvider: openSSL
                        negotiationType: TLS
        -   name: org2
            mspId: Org2MSP
            domain: org2.delobz.com
            caName: ca-org2
            peers:
                -   name: peer0
                    url: grpcs://localhost:8051
                    adminUserName: admin
                    users:
                        -   appUser
                    props:
                        pemFile: ../../network/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/server.crt
                        # clientKeyFile: 
                        # clientCertFile: 
                        hostnameOverride: peer0.org2.delobz.com
                        sslProvider: openSSL
                        negotiationType: TLS
            orderers:
                -   name: orderer
                    url: grpcs://localhost:7050
                    props:
                        pemFile: ../../network/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/server.crt
                        # clientKeyFile: 
                        # clientCertFile: 
                        hostnameOverride: orderer.delobz.com
                        sslProvider: openSSL
                        negotiationType: TLS
        -   name: org3
            mspId: Org3MSP
            domain: org3.delobz.com
            caName: ca-org3
            peers:
                -   name: peer0
                    url: grpcs://localhost:9051
                    adminUserName: admin
                    users:
                        -   appUser
                    props:
                        pemFile: ../../network/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/server.crt
                        # clientKeyFile: 
                        # clientCertFile: 
                        hostnameOverride: peer0.org3.delobz.com
                        sslProvider: openSSL
                        negotiationType: TLS
            orderers:
                -   name: orderer
                    url: grpcs://localhost:7050
                    props:
                        pemFile: ../../network/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/server.crt
                        # clientKeyFile: 
                        # clientCertFile: 
                        hostnameOverride: orderer.delobz.com
                        sslProvider: openSSL
                        negotiationType: TLS
    fabricCAs:
        -   name: ca-org1
            url: https://localhost:7054
            adminUserName: admin
            props:
                pemFile: ../../network/organizations/peerOrganizations/org1.delobz.com/ca/ca.org1.delobz.com-cert.pem
                allowAllHostNames: "true"
        -   name: ca-org2
            url: https://localhost:8054
            adminUserName: admin
            props:
                pemFile: ../../network/organizations/peerOrganizations/org2.delobz.com/ca/ca.org2.delobz.com-cert.pem
                allowAllHostNames: "true"
        -   name: ca-org3
            url: https://localhost:9054
            adminUserName: admin
            props:
                pemFile: ../../network/organizations/peerOrganizations/org3.delobz.com/ca/ca.org3.delobz.com-cert.pem
                allowAllHostNames: "true"
        -   name: ca-orderer
            url: https://localhost:10054
            adminUserName: admin
    chaincodes:
        -   name: counter_cc_kotlin
            version: 1
            label: CounterLabel
            type: java
            sourceLocation: ../../chaincode/build/install/counter-chaincode
            path:
            metadadataSource:
            initRequired: false
            deployWaitTime: 300000
            invokeWaitTime: 32000
            proposalWaitTime: 120000
            collectionConfigurationPath: ../../network/privatecollections/private-collections.yaml
            orgs:
                -   org1
                -   org2
                -   org3
        -   name: counter_cc_java
            version: 1
            label: CounterLabel
            type: java
            sourceLocation: ../../chaincode-java
            path:
            metadadataSource:
            initRequired: true
            deployWaitTime: 300000
            invokeWaitTime: 32000
            proposalWaitTime: 120000
            collectionConfigurationPath:
            orgs:
                -   org1
                -   org2
                -   org3

```

### Further Work to be Done:
1. Support adding new Org to an Existing Channel
2. Support Endorsment Policies.

