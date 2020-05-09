package com.afengar.blockchain.hlf.counter.configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeCollectionConfiguration;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.LifecycleChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.LifecycleChaincodePackage;
import org.hyperledger.fabric.sdk.LifecycleQueryChaincodeDefinitionsResult;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.var;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
// @Component
@Slf4j
public class AppChaincode {

    private String name;
    private String version;
    private String label;
    private Type type;
    private String sourceLocation;
    private String path;
    private String metadadataSource;
    private boolean initRequired;
    private int deployWaitTime;
    private int invokeWaitTime;
    private int proposalWaitTime;
    private List<String> orgs = new ArrayList<>();
    private List<AppOrg> appOrgs = new ArrayList<>();
    private long sequence = 1L;

    private LifecycleChaincodeEndorsementPolicy endorsementPolicy = null;
    private String collectionConfigurationPath;

    private FileSystemStore fileSystemStore;

    public void installAndVerify() throws Exception {
        this.sequence = Long.parseLong(this.version);

        LifecycleChaincodePackage chaincodePackage = createLifecycleChaincodePackage(this.label, this.type,
                this.sourceLocation, this.path, this.metadadataSource);

        List<AppOrg> orgsToInstall = new ArrayList<>();
        for (var appOrg : this.appOrgs) {
            if (!this.queryInstalledForOrg(appOrg))
                orgsToInstall.add(appOrg);
        }

        HashSet<String> expectedApproved = new HashSet<>();
        HashSet<String> expectedUnApproved = new HashSet<>();
        for (var appOrg : orgsToInstall) {
            expectedUnApproved.add(appOrg.getMspId());
        }

        for (var appOrg : orgsToInstall) {
            log.info(String.format("Org %s installs the chaincode on its peers.", appOrg.getName()));
            String chaincodePackageID = this.installChaincode(appOrg, chaincodePackage);
            if (chaincodePackageID == null)
                throw new Exception("Error Installing Chaincode, PackageID returned null for Org: " + appOrg.getName());
            log.info("Org: " + appOrg.getName() + " Chaincode Label: " + this.label + " PackageId:"
                    + chaincodePackageID);
            this.fileSystemStore.setValue("chaincode." + appOrg.getName(), chaincodePackageID);
            boolean installed = this.queryInstalledByPackageId(appOrg, chaincodePackageID);
            if (!installed)
                throw new Exception(
                        "Error Installing Chaincode, Query Installed returned false for Org: " + appOrg.getName());
            this.approveForOrg(appOrg, chaincodePackageID);
            expectedApproved.add(appOrg.getMspId());
            expectedUnApproved.remove(appOrg.getMspId());
            boolean commited = this.checkCommitReadiness(appOrg, expectedApproved, expectedUnApproved);
            if (!commited)
                throw new Exception("Error Installing Chaincode, Query Commit Readiness returned false for Org: "
                        + appOrg.getName());
        }

        // Only 1 Org Needs to commit definition - using otherFabricPeers - the other
        // Org will approve as well
        if (orgsToInstall.size() > 0)
            this.commitChaincodeDefinition(orgsToInstall.get(0));

        for (var appOrg : orgsToInstall) {
            boolean commited = this.queryChaincodeDefinitionCommited(appOrg);
            if (!commited)
                throw new Exception("Error Installing Chaincode, Query Commit Definition returned false for Org: "
                        + appOrg.getName());
        }

        if (this.initRequired)
            this.execute(orgsToInstall.get(0).getName(), "init", new String[] { "" }, null, true);

        log.info("Finish Deploying Chaincode");
    }

    private LifecycleChaincodePackage createLifecycleChaincodePackage(String chaincodeLabel, Type chaincodeType,
            String chaincodeSourceLocation, String chaincodePath, String metadadataSource) throws Exception {
        log.info(String.format("creating install package %s.", chaincodeLabel));

        Path metadataSourcePath = null;
        if (this.metadadataSource != null && !this.metadadataSource.isEmpty()) {
            metadataSourcePath = Paths.get(metadadataSource);
        }
        LifecycleChaincodePackage lifecycleChaincodePackage = LifecycleChaincodePackage.fromSource(chaincodeLabel,
                Paths.get(chaincodeSourceLocation), chaincodeType, chaincodePath, metadataSourcePath);

        return lifecycleChaincodePackage;
    }

    private String installChaincode(AppOrg appOrg, LifecycleChaincodePackage lifecycleChaincodePackage)
            throws Exception {

        int numInstallProposal = 0;
        HFClient client = appOrg.getClient();
        List<Peer> peers = appOrg.getFabricPeers();

        numInstallProposal = numInstallProposal + peers.size();

        var installProposalRequest = client.newLifecycleInstallChaincodeRequest();
        installProposalRequest.setLifecycleChaincodePackage(lifecycleChaincodePackage);
        installProposalRequest.setProposalWaitTime(this.deployWaitTime);

        var responses = client.sendLifecycleInstallChaincodeRequest(installProposalRequest, peers);
        if (responses == null)
            return null;

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        String packageID = null;
        for (var response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                log.info(String.format("Successful install proposal response Txid: %s from peer %s",
                        response.getTransactionID(), response.getPeer().getName()));
                successful.add(response);
                packageID = response.getPackageId();
            } else {
                failed.add(response);
            }
        }

        log.info(String.format("Received %d install proposal responses. Successful+verified: %d . Failed: %d",
                numInstallProposal, successful.size(), failed.size()));

        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            log.error("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
        }

        return packageID;

    }

    private boolean queryInstalledByPackageId(AppOrg appOrg, String chaincodePackageID) throws Exception {

        int installCount = 0;
        var request = appOrg.getClient().newLifecycleQueryInstalledChaincodeRequest();
        request.setPackageID(chaincodePackageID);

        var responses = appOrg.getClient().sendLifecycleQueryInstalledChaincode(request, appOrg.getFabricPeers());
        for (var response : responses) {
            if (response.getStatus() == Status.SUCCESS) {
                log.info(String.format("Chaincode label %s, packageId %s found on peer %s ", this.label,
                        chaincodePackageID, response.getPeer().getName()));
                ++installCount;
            } else {
                log.error(String.format("Peer %s had chaincode label mismatch, Expected %s, Actual %s",
                        response.getPeer().getName(), this.label, response.getLabel()));
            }
        }
        return installCount == appOrg.getFabricPeers().size();
    }

    private void approveForOrg(AppOrg appOrg, String chaincodePackageID) throws Exception {
        log.info(appOrg.getName() + " approving chaincode definition for my org.");

        var request = appOrg.getClient().newLifecycleApproveChaincodeDefinitionForMyOrgRequest();
        request.setSequence(this.sequence);
        request.setChaincodeName(this.name);
        request.setChaincodeVersion(this.version);
        request.setInitRequired(this.initRequired);

        if (null != this.collectionConfigurationPath && !this.collectionConfigurationPath.isEmpty()) {
            ChaincodeCollectionConfiguration collectionConfiguration = ChaincodeCollectionConfiguration
                    .fromYamlFile(new File(this.collectionConfigurationPath));
            request.setChaincodeCollectionConfiguration(collectionConfiguration);
        }

        if (null != this.endorsementPolicy) {
            request.setChaincodeEndorsementPolicy(this.endorsementPolicy);
        }

        request.setPackageId(chaincodePackageID);

        var responses = appOrg.getChannel().sendLifecycleApproveChaincodeDefinitionForMyOrgProposal(request,
                appOrg.getFabricPeers());

        TransactionEvent transactionEvent = appOrg.getChannel().sendTransaction(responses).get(this.invokeWaitTime,
                TimeUnit.SECONDS);
        if (!transactionEvent.isValid())
            throw new Exception("Error Approving Chaincode for Org: " + appOrg.getName());

    }

    private boolean checkCommitReadiness(AppOrg appOrg, HashSet<String> expectedApproved,
            HashSet<String> expectedUnApproved) throws Exception {

        boolean retVal = false;
        var request = appOrg.getClient().newLifecycleSimulateCommitChaincodeDefinitionRequest();
        request.setSequence(this.sequence);
        request.setChaincodeName(this.name);
        request.setChaincodeVersion(this.version);
        if (null != this.endorsementPolicy) {
            request.setChaincodeEndorsementPolicy(this.endorsementPolicy);
        }
        if (null != this.collectionConfigurationPath && !this.collectionConfigurationPath.isEmpty()) {
            ChaincodeCollectionConfiguration collectionConfiguration = ChaincodeCollectionConfiguration
                    .fromYamlFile(new File(this.collectionConfigurationPath));
            request.setChaincodeCollectionConfiguration(collectionConfiguration);
        }
        request.setInitRequired(initRequired);

        var responses = appOrg.getChannel().sendLifecycleCheckCommitReadinessRequest(request, appOrg.getFabricPeers());
        for (var resp : responses) {
            final Peer peer = resp.getPeer();
            log.info(String.format("Approved orgs failed on %s", peer), expectedApproved, resp.getApprovedOrgs());
            log.info(String.format("UnApproved orgs failed on %s", peer), expectedUnApproved, resp.getUnApprovedOrgs());
            retVal = expectedApproved.size() == resp.getApprovedOrgs().size()
                    && expectedUnApproved.size() == resp.getUnApprovedOrgs().size();

        }
        return retVal;
    }

    private void commitChaincodeDefinition(AppOrg appOrg) throws Exception {
        var request = appOrg.getClient().newLifecycleCommitChaincodeDefinitionRequest();
        request.setSequence(this.sequence);
        request.setChaincodeName(this.name);
        request.setChaincodeVersion(this.version);
        if (null != this.endorsementPolicy) {
            request.setChaincodeEndorsementPolicy(this.endorsementPolicy);
        }
        if (null != this.collectionConfigurationPath && !this.collectionConfigurationPath.isEmpty()) {
            ChaincodeCollectionConfiguration collectionConfiguration = ChaincodeCollectionConfiguration
                    .fromYamlFile(new File(this.collectionConfigurationPath));
            request.setChaincodeCollectionConfiguration(collectionConfiguration);
        }
        request.setInitRequired(initRequired);

        List<Peer> endorsingPeers = new ArrayList<>(appOrg.getFabricPeers());
        endorsingPeers.addAll(appOrg.getFabricOtherPeers());
        var responses = appOrg.getChannel().sendLifecycleCommitChaincodeDefinitionProposal(request, endorsingPeers);

        for (var resp : responses) {

            final Peer peer = resp.getPeer();
            if (resp.getStatus() != ChaincodeResponse.Status.SUCCESS)
                log.info(String.format("%s had unexpected status.", peer.toString()));
            if (!resp.isVerified())
                log.info(String.format("%s not verified.", peer.toString()));
        }

        TransactionEvent transactionEvent = appOrg.getChannel().sendTransaction(responses).get(this.invokeWaitTime,
                TimeUnit.SECONDS);
        if (!transactionEvent.isValid())
            throw new Exception("Error commit Chaincode Definition");
    }

    private boolean queryChaincodeDefinitionCommited(AppOrg appOrg) throws Exception {
        int commited = 0;
        var request = appOrg.getClient().newLifecycleQueryChaincodeDefinitionsRequest();
        var proposalResponses = appOrg.getChannel().lifecycleQueryChaincodeDefinitions(request,
                appOrg.getFabricPeers());
        for (var proposalResponse : proposalResponses) {
            Peer peer = proposalResponse.getPeer();

            if (proposalResponse.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                var chaincodeDefinitions = proposalResponse.getLifecycleQueryChaincodeDefinitionsResult();

                Optional<String> matchingName = chaincodeDefinitions.stream()
                        .map(LifecycleQueryChaincodeDefinitionsResult::getName).filter(Predicate.isEqual(this.name))
                        .findAny();
                if (matchingName.isPresent()) {
                    log.info(String.format("On peer %s return namespace for chaincode %s", peer, this.name));
                    ++commited;
                }
            }
        }
        return commited == proposalResponses.size();
    }

    public void execute(String orgName, String funcName, String[] args, Map<String, byte[]> transientMap,
            boolean doInit) throws Exception {
        AppOrg appOrg = this.getAppOrgByName(orgName);

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        TransactionProposalRequest transactionProposalRequest = appOrg.getClient().newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeName(this.name);
        transactionProposalRequest.setChaincodeLanguage(this.type);
        transactionProposalRequest.setUserContext(appOrg.getPeerAdmin());

        transactionProposalRequest.setFcn(funcName);
        transactionProposalRequest.setProposalWaitTime(this.proposalWaitTime);
        transactionProposalRequest.setArgs(args);
        if (doInit)
            transactionProposalRequest.setInit(doInit);
        if (transientMap != null)
            transactionProposalRequest.setTransientMap(transientMap);

        // Collection<ProposalResponse> transactionPropResp =
        // channel.sendTransactionProposalToEndorsers(transactionProposalRequest);
        Collection<ProposalResponse> transactionPropResp = appOrg.getChannel()
                .sendTransactionProposal(transactionProposalRequest, appOrg.getChannel().getPeers());
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                log.info(String.format("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(),
                        response.getPeer().getName()));
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        log.info(String.format("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                transactionPropResp.size(), successful.size(), failed.size()));
        if (failed.size() > 0) {
            ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
            log.error("Not enough endorsers for executeChaincode:" + failed.size() + " endorser error: "
                    + firstTransactionProposalResponse.getMessage() + ". Was verified: "
                    + firstTransactionProposalResponse.isVerified());
        }
        log.info("Successfully received transaction proposal responses.");

        // System.exit(10);

        ////////////////////////////
        // Send Transaction Transaction to orderer
        log.info("Sending chaincode transaction " + this.transactionDetailsString(funcName, args) + " to orderer.");
        TransactionEvent transactionEvent = appOrg.getChannel().sendTransaction(successful).get(this.invokeWaitTime,
                TimeUnit.SECONDS);
        if (!transactionEvent.isValid())
            throw new Exception("Error execute Chaincode " + this.transactionDetailsString(funcName, args));
    }

    private String transactionDetailsString(String funcName, String[] args) {
        List<String> strList = Arrays.asList(args);
        String joinedString = String.join(", ", strList);
        String retVal = "(Function: " + funcName + ", Args: " + joinedString + ")";
        return retVal;
    }

    public String query(String orgName, String funcName, String[] args, Map<String, byte[]> transientMap)
            throws Exception {
        AppOrg appOrg = this.getAppOrgByName(orgName);

        List<String> strList = Arrays.asList(args);
        String joinedString = String.join(", ", strList);

        log.info("Query chaincode " + this.name + " for the value of " + joinedString);
        QueryByChaincodeRequest queryByChaincodeRequest = appOrg.getClient().newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn(funcName);
        queryByChaincodeRequest.setChaincodeName(this.name);
        if (transientMap != null)
            queryByChaincodeRequest.setTransientMap(transientMap);

        String retVal = "";
        Collection<ProposalResponse> queryProposals = appOrg.getChannel().queryByChaincode(queryByChaincodeRequest,
                appOrg.getChannel().getPeers());
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                log.error("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: "
                        + proposalResponse.getStatus() + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                log.info(String.format("Query payload of %s from peer %s returned %s", funcName,
                        proposalResponse.getPeer().getName(), payload));
                retVal = payload;
            }
        }
        return retVal;
    }

    private AppOrg getAppOrgByName(String orgName) {
        return this.appOrgs.stream().filter(org -> org.getName().equals(orgName)).findFirst().get();
    }

    private boolean queryInstalledForOrg(AppOrg appOrg) throws Exception {
        boolean retVal = false;
        String chaincodePackageID = null;
        if (this.fileSystemStore.hasValue("chaincode." + appOrg.getName()))
            chaincodePackageID = this.fileSystemStore.getValue("chaincode." + appOrg.getName());

        var request = appOrg.getClient().newLifecycleQueryInstalledChaincodesRequest();
        var responses = appOrg.getClient().sendLifecycleQueryInstalledChaincodes(request, appOrg.getFabricPeers());
        for (var response : responses) {
            if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                for (var result : response.getLifecycleQueryInstalledChaincodesResult()) {
                    if (chaincodePackageID != null)
                        retVal = result.getLabel().equals(this.label)
                                && result.getPackageId().equals(chaincodePackageID);
                    else
                        retVal = result.getLabel().equals(this.label);
                }
            }
        }
        return retVal;
    }
}