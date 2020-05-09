

function createOrg1 {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/peerOrganizations/org1.delobz.com/

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/peerOrganizations/org1.delobz.com/
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:7054 --caname ca-org1 --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-org1.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-org1.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-org1.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-7054-ca-org1.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/peerOrganizations/org1.delobz.com/msp/config.yaml

  echo
	echo "Register peer0"
  echo
  set -x
	fabric-ca-client register --caname ca-org1 --id.name peer0 --id.secret peer0pw --id.type peer --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

  echo
  echo "Register user"
  echo
  set -x
  fabric-ca-client register --caname ca-org1 --id.name user1 --id.secret user1pw --id.type client --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

  echo
  echo "Register the org admin"
  echo
  set -x
  fabric-ca-client register --caname ca-org1 --id.name org1admin --id.secret org1adminpw --id.type admin --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

	mkdir -p organizations/peerOrganizations/org1.delobz.com/peers
  mkdir -p organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com

  echo
  echo "## Generate the peer0 msp"
  echo
  set -x
	fabric-ca-client enroll -u https://peer0:peer0pw@localhost:7054 --caname ca-org1 -M ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/msp --csr.hosts peer0.org1.delobz.com --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/msp/config.yaml

  echo
  echo "## Generate the peer0-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://peer0:peer0pw@localhost:7054 --caname ca-org1 -M ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls --enrollment.profile tls --csr.hosts peer0.org1.delobz.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x


  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/ca.crt
  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/signcerts/* ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/server.crt
  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/keystore/* ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/server.key

  mkdir ${PWD}/organizations/peerOrganizations/org1.delobz.com/msp/tlscacerts
  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org1.delobz.com/msp/tlscacerts/ca.crt

  mkdir ${PWD}/organizations/peerOrganizations/org1.delobz.com/tlsca
  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org1.delobz.com/tlsca/tlsca.org1.delobz.com-cert.pem

  mkdir ${PWD}/organizations/peerOrganizations/org1.delobz.com/ca
  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/peers/peer0.org1.delobz.com/msp/cacerts/* ${PWD}/organizations/peerOrganizations/org1.delobz.com/ca/ca.org1.delobz.com-cert.pem

  mkdir -p organizations/peerOrganizations/org1.delobz.com/users
  mkdir -p organizations/peerOrganizations/org1.delobz.com/users/User1@org1.delobz.com

  echo
  echo "## Generate the user msp"
  echo
  set -x
	fabric-ca-client enroll -u https://user1:user1pw@localhost:7054 --caname ca-org1 -M ${PWD}/organizations/peerOrganizations/org1.delobz.com/users/User1@org1.delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

  mkdir -p organizations/peerOrganizations/org1.delobz.com/users/Admin@org1.delobz.com

  echo
  echo "## Generate the org admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://org1admin:org1adminpw@localhost:7054 --caname ca-org1 -M ${PWD}/organizations/peerOrganizations/org1.delobz.com/users/Admin@org1.delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/org1/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/org1.delobz.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/org1.delobz.com/users/Admin@org1.delobz.com/msp/config.yaml

}


function createOrg2 {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/peerOrganizations/org2.delobz.com/

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/peerOrganizations/org2.delobz.com/
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:8054 --caname ca-org2 --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-org2.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-org2.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-org2.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-8054-ca-org2.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/peerOrganizations/org2.delobz.com/msp/config.yaml

  echo
	echo "Register peer0"
  echo
  set -x
	fabric-ca-client register --caname ca-org2 --id.name peer0 --id.secret peer0pw --id.type peer --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

  echo
  echo "Register user"
  echo
  set -x
  fabric-ca-client register --caname ca-org2 --id.name user1 --id.secret user1pw --id.type client --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

  echo
  echo "Register the org admin"
  echo
  set -x
  fabric-ca-client register --caname ca-org2 --id.name org2admin --id.secret org2adminpw --id.type admin --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

	mkdir -p organizations/peerOrganizations/org2.delobz.com/peers
  mkdir -p organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com

  echo
  echo "## Generate the peer0 msp"
  echo
  set -x
	fabric-ca-client enroll -u https://peer0:peer0pw@localhost:8054 --caname ca-org2 -M ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/msp --csr.hosts peer0.org2.delobz.com --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/msp/config.yaml

  echo
  echo "## Generate the peer0-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://peer0:peer0pw@localhost:8054 --caname ca-org2 -M ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls --enrollment.profile tls --csr.hosts peer0.org2.delobz.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x


  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/ca.crt
  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/signcerts/* ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/server.crt
  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/keystore/* ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/server.key

  mkdir ${PWD}/organizations/peerOrganizations/org2.delobz.com/msp/tlscacerts
  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org2.delobz.com/msp/tlscacerts/ca.crt

  mkdir ${PWD}/organizations/peerOrganizations/org2.delobz.com/tlsca
  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org2.delobz.com/tlsca/tlsca.org2.delobz.com-cert.pem

  mkdir ${PWD}/organizations/peerOrganizations/org2.delobz.com/ca
  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/peers/peer0.org2.delobz.com/msp/cacerts/* ${PWD}/organizations/peerOrganizations/org2.delobz.com/ca/ca.org2.delobz.com-cert.pem

  mkdir -p organizations/peerOrganizations/org2.delobz.com/users
  mkdir -p organizations/peerOrganizations/org2.delobz.com/users/User1@org2.delobz.com

  echo
  echo "## Generate the user msp"
  echo
  set -x
	fabric-ca-client enroll -u https://user1:user1pw@localhost:8054 --caname ca-org2 -M ${PWD}/organizations/peerOrganizations/org2.delobz.com/users/User1@org2.delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

  mkdir -p organizations/peerOrganizations/org2.delobz.com/users/Admin@org2.delobz.com

  echo
  echo "## Generate the org admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://org2admin:org2adminpw@localhost:8054 --caname ca-org2 -M ${PWD}/organizations/peerOrganizations/org2.delobz.com/users/Admin@org2.delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/org2/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/org2.delobz.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/org2.delobz.com/users/Admin@org2.delobz.com/msp/config.yaml

}

function createOrg3 {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/peerOrganizations/org3.delobz.com/

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/peerOrganizations/org3.delobz.com/
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:9054 --caname ca-org3 --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-org3.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-org3.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-org3.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-9054-ca-org3.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/peerOrganizations/org3.delobz.com/msp/config.yaml

  echo
	echo "Register peer0"
  echo
  set -x
	fabric-ca-client register --caname ca-org3 --id.name peer0 --id.secret peer0pw --id.type peer --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

  echo
  echo "Register user"
  echo
  set -x
  fabric-ca-client register --caname ca-org3 --id.name user1 --id.secret user1pw --id.type client --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

  echo
  echo "Register the org admin"
  echo
  set -x
  fabric-ca-client register --caname ca-org3 --id.name org3admin --id.secret org3adminpw --id.type admin --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

	mkdir -p organizations/peerOrganizations/org3.delobz.com/peers
  mkdir -p organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com

  echo
  echo "## Generate the peer0 msp"
  echo
  set -x
	fabric-ca-client enroll -u https://peer0:peer0pw@localhost:9054 --caname ca-org3 -M ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/msp --csr.hosts peer0.org3.delobz.com --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/msp/config.yaml

  echo
  echo "## Generate the peer0-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://peer0:peer0pw@localhost:9054 --caname ca-org3 -M ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls --enrollment.profile tls --csr.hosts peer0.org3.delobz.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x


  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/ca.crt
  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/signcerts/* ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/server.crt
  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/keystore/* ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/server.key

  mkdir ${PWD}/organizations/peerOrganizations/org3.delobz.com/msp/tlscacerts
  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org3.delobz.com/msp/tlscacerts/ca.crt

  mkdir ${PWD}/organizations/peerOrganizations/org3.delobz.com/tlsca
  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/tls/tlscacerts/* ${PWD}/organizations/peerOrganizations/org3.delobz.com/tlsca/tlsca.org3.delobz.com-cert.pem

  mkdir ${PWD}/organizations/peerOrganizations/org3.delobz.com/ca
  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/peers/peer0.org3.delobz.com/msp/cacerts/* ${PWD}/organizations/peerOrganizations/org3.delobz.com/ca/ca.org3.delobz.com-cert.pem

  mkdir -p organizations/peerOrganizations/org3.delobz.com/users
  mkdir -p organizations/peerOrganizations/org3.delobz.com/users/User1@org3.delobz.com

  echo
  echo "## Generate the user msp"
  echo
  set -x
	fabric-ca-client enroll -u https://user1:user1pw@localhost:9054 --caname ca-org3 -M ${PWD}/organizations/peerOrganizations/org3.delobz.com/users/User1@org3.delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

  mkdir -p organizations/peerOrganizations/org3.delobz.com/users/Admin@org3.delobz.com

  echo
  echo "## Generate the org admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://org3admin:org3adminpw@localhost:9054 --caname ca-org3 -M ${PWD}/organizations/peerOrganizations/org3.delobz.com/users/Admin@org3.delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/org3/tls-cert.pem
  set +x

  cp ${PWD}/organizations/peerOrganizations/org3.delobz.com/msp/config.yaml ${PWD}/organizations/peerOrganizations/org3.delobz.com/users/Admin@org3.delobz.com/msp/config.yaml

}

function createOrderer {

  echo
	echo "Enroll the CA admin"
  echo
	mkdir -p organizations/ordererOrganizations/delobz.com

	export FABRIC_CA_CLIENT_HOME=${PWD}/organizations/ordererOrganizations/delobz.com
#  rm -rf $FABRIC_CA_CLIENT_HOME/fabric-ca-client-config.yaml
#  rm -rf $FABRIC_CA_CLIENT_HOME/msp

  set -x
  fabric-ca-client enroll -u https://admin:adminpw@localhost:10054 --caname ca-orderer --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  echo 'NodeOUs:
  Enable: true
  ClientOUIdentifier:
    Certificate: cacerts/localhost-10054-ca-orderer.pem
    OrganizationalUnitIdentifier: client
  PeerOUIdentifier:
    Certificate: cacerts/localhost-10054-ca-orderer.pem
    OrganizationalUnitIdentifier: peer
  AdminOUIdentifier:
    Certificate: cacerts/localhost-10054-ca-orderer.pem
    OrganizationalUnitIdentifier: admin
  OrdererOUIdentifier:
    Certificate: cacerts/localhost-10054-ca-orderer.pem
    OrganizationalUnitIdentifier: orderer' > ${PWD}/organizations/ordererOrganizations/delobz.com/msp/config.yaml


  echo
	echo "Register orderer"
  echo
  set -x
	fabric-ca-client register --caname ca-orderer --id.name orderer --id.secret ordererpw --id.type orderer --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
    set +x

  echo
  echo "Register the orderer admin"
  echo
  set -x
  fabric-ca-client register --caname ca-orderer --id.name ordererAdmin --id.secret ordererAdminpw --id.type admin --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

	mkdir -p organizations/ordererOrganizations/delobz.com/orderers
  mkdir -p organizations/ordererOrganizations/delobz.com/orderers/delobz.com

  mkdir -p organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com

  echo
  echo "## Generate the orderer msp"
  echo
  set -x
	fabric-ca-client enroll -u https://orderer:ordererpw@localhost:10054 --caname ca-orderer -M ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/msp --csr.hosts orderer.delobz.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  cp ${PWD}/organizations/ordererOrganizations/delobz.com/msp/config.yaml ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/msp/config.yaml

  echo
  echo "## Generate the orderer-tls certificates"
  echo
  set -x
  fabric-ca-client enroll -u https://orderer:ordererpw@localhost:10054 --caname ca-orderer -M ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls --enrollment.profile tls --csr.hosts orderer.delobz.com --csr.hosts localhost --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  cp ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/tlscacerts/* ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/ca.crt
  cp ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/signcerts/* ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/server.crt
  cp ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/keystore/* ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/server.key

  mkdir ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/msp/tlscacerts
  cp ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/tlscacerts/* ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/msp/tlscacerts/tlsca.delobz.com-cert.pem

  mkdir ${PWD}/organizations/ordererOrganizations/delobz.com/msp/tlscacerts
  cp ${PWD}/organizations/ordererOrganizations/delobz.com/orderers/orderer.delobz.com/tls/tlscacerts/* ${PWD}/organizations/ordererOrganizations/delobz.com/msp/tlscacerts/tlsca.delobz.com-cert.pem

  mkdir -p organizations/ordererOrganizations/delobz.com/users
  mkdir -p organizations/ordererOrganizations/delobz.com/users/Admin@delobz.com

  echo
  echo "## Generate the admin msp"
  echo
  set -x
	fabric-ca-client enroll -u https://ordererAdmin:ordererAdminpw@localhost:10054 --caname ca-orderer -M ${PWD}/organizations/ordererOrganizations/delobz.com/users/Admin@delobz.com/msp --tls.certfiles ${PWD}/organizations/fabric-ca/ordererOrg/tls-cert.pem
  set +x

  cp ${PWD}/organizations/ordererOrganizations/delobz.com/msp/config.yaml ${PWD}/organizations/ordererOrganizations/delobz.com/users/Admin@delobz.com/msp/config.yaml


}
