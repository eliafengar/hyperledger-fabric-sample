package com.afengar.blockchain.hlf.counter.configuration;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.var;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@Slf4j
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "hyperledger", ignoreUnknownFields = false)
public class App {
    private boolean fabricTLS = false;
    private boolean fabricCATLS = false;
    private AppChannel channel;
    private List<AppOrg> organizations = new ArrayList<>();
    private List<AppCA> fabricCAs = new ArrayList<>();
    private List<AppChaincode> chaincodes = new ArrayList<>();

    @Setter(onMethod = @__({ @Autowired }))
    private FileSystemStore fileSystemStore;

    @PostConstruct
    public void init() {
        this.organizations.forEach(org -> {
            org.init(this.fabricTLS);
            org.setAppChannel(this.channel);
        });
        this.fabricCAs.forEach(ca -> ca.init(this.fabricCATLS));

        for (AppChaincode cc : this.chaincodes) {
            for (String orgName : cc.getOrgs()) {
                cc.getAppOrgs().add(this.getAppOrgByName(orgName));
            }
            cc.setFileSystemStore(fileSystemStore);
        }

        try {
            this.enrollUsersSetup();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private AppOrg getAppOrgByName(String name) {
        return this.organizations.stream().filter(org -> org.getName().equals(name)).findFirst().get();
    }

    /**
     * Will register and enroll users persisting them to samplestore.
     */
    private void enrollUsersSetup() throws Exception {

        log.info("***** Enrolling Users *****");
        for (var appOrg : this.organizations) {

            AppCA orgCA = this.getCAByName(appOrg.getCaName());
            HFCAClient caClient = orgCA.getClient();

            if (this.fabricTLS) {
                // This shows how to get a client TLS certificate from Fabric CA
                // we will use one client TLS certificate for orderer peers etc.
                EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
                enrollmentRequestTLS.addHost("localhost");
                enrollmentRequestTLS.setProfile("tls");
                Enrollment enroll = caClient.enroll("admin", "adminpw", enrollmentRequestTLS);
                String tlsCertPEM = enroll.getCert();
                String tlsKeyPEM = this.getPEMStringFromPrivateKey(enroll.getKey());

                Properties tlsProperties = new Properties();
                tlsProperties.put("clientKeyBytes", tlsKeyPEM.getBytes(StandardCharsets.UTF_8));
                tlsProperties.put("clientCertBytes", tlsCertPEM.getBytes(StandardCharsets.UTF_8));
                Map<String, Properties> clientTLSProperties = new HashMap<>();
                clientTLSProperties.put(appOrg.getName(), tlsProperties);

                // Save in samplestore for follow on tests.
                this.fileSystemStore.storeClientPEMTLCertificate(appOrg, tlsCertPEM);
                this.fileSystemStore.storeClientPEMTLSKey(appOrg, tlsKeyPEM);
            }

            HFCAInfo info = caClient.info(); // just check if we connect at all.
            String infoName = info.getCAName();
            log.info("CA Info Name: " + infoName);

            String orgName = appOrg.getName();
            String orgMspid = appOrg.getMspId();
            String orgDomainName = appOrg.getDomain();

            AppUser admin = this.fileSystemStore.getMember("admin", orgName);
            if (!admin.isEnrolled()) { // Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(caClient.enroll(admin.getName(), "adminpw"));
                admin.setMspId(orgMspid);
            }
            admin.saveState();
            orgCA.setAdminUser(admin);

            for (var peer : appOrg.getPeers()) {

                for (String userName : peer.getUsers()) {
                    AppUser user = this.fileSystemStore.getMember(userName, appOrg.getName());
                    if (!user.isRegistered()) { // users need to be registered AND enrolled
                        RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                        user.setEnrollmentSecret(caClient.register(rr, admin));
                    }
                    if (!user.isEnrolled()) {
                        user.setEnrollment(caClient.enroll(user.getName(), user.getEnrollmentSecret()));
                        user.setMspId(orgMspid);
                    }
                    user.saveState();
                    peer.addUser(user);
                }

                AppUser peerOrgAdmin = this.fileSystemStore.getMember(orgName + "Admin", orgName, orgMspid,
                        this.findFileSk(Paths
                                .get("/Users/eliafengar/dev/hlf-counter/network/organizations/peerOrganizations/",
                                        orgDomainName, String.format("/users/Admin@%s/msp/keystore", orgDomainName))
                                .toFile()),
                        Paths.get("/Users/eliafengar/dev/hlf-counter/network/organizations/peerOrganizations/",
                                orgDomainName, String.format("/users/Admin@%s/msp/signcerts/cert.pem", orgDomainName))
                                .toFile());
                peerOrgAdmin.saveState();
                // A special user that can create channels, join peers and install chaincode
                appOrg.setPeerAdmin(peerOrgAdmin);
            }
        }
    }

    private String getPEMStringFromPrivateKey(PrivateKey privateKey) throws IOException {
        StringWriter pemStrWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(pemStrWriter);
        pemWriter.writeObject(privateKey);
        pemWriter.close();
        return pemStrWriter.toString();
    }

    private File findFileSk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(String.format("Matches returned null does %s directory exist?",
                    directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(String.format("Expected in %s only 1 sk file but found %d",
                    directory.getAbsoluteFile().getName(), matches.length));
        }
        return matches[0];
    }

    private AppCA getCAByName(String caName) {
        AppCA retVal = null;

        Optional<AppCA> optionalCA = this.fabricCAs.stream().filter(ca -> ca.getName().equals(caName)).findFirst();

        if (optionalCA.isPresent())
            retVal = optionalCA.get();

        return retVal;
    }
}