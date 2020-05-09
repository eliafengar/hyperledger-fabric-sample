package com.afengar.blockchain.hlf.counter.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
// import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Channel.PeerOptions;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.var;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@Slf4j
public class AppOrg {

    private HFClient client;

    private AppChannel appChannel;
    private Channel channel;

    private String name = "org";
    private String mspId = "orgMSP";
    private String domain = "org.example.com";
    private String caName = "ca_org";

    private AppUser peerAdmin;

    private List<AppPeer> peers = new ArrayList<>();
    private List<Peer> fabricPeers = new ArrayList<>();
    private List<Peer> fabricOtherPeers = new ArrayList<>();

    private List<AppOrderer> orderers = new ArrayList<>();
    private List<Orderer> fabricOrderers = new ArrayList<>();

    public void init(boolean isTLS) {
        try {
            this.peers.forEach(peer -> peer.init(isTLS));
            this.orderers.forEach(orderer -> orderer.init(isTLS));
            this.client = HFClient.createNewInstance();
            this.client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (CryptoException | InvalidArgumentException | IllegalAccessException | InstantiationException
                | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void constructChannel(boolean createFabricChannel, boolean joinPeers) throws Exception {

        log.info(String.format("Constructing channel %s", name));
        // Picking the first peerAdmin in order to create the Orderers.
        this.client.setUserContext(this.peerAdmin);

        this.fabricOrderers.clear();
        for (var appOrderer : this.orderers) {
            // example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping
            // rates.
            appOrderer.getProps().put("grpc.NettyChannelBuilderOption.keepAliveTime",
                    new Object[] { 5L, TimeUnit.MINUTES });
            appOrderer.getProps().put("grpc.NettyChannelBuilderOption.keepAliveTimeout",
                    new Object[] { 8L, TimeUnit.SECONDS });
            appOrderer.getProps().put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] { true });

            Orderer orderer = this.client.newOrderer(appOrderer.getName(), appOrderer.getUrl(), appOrderer.getProps());
            this.fabricOrderers.add(orderer);
        }

        // Just pick the first orderer in the list to create the channel.
        Orderer anOrderer = this.fabricOrderers.get(0);

        // URL location =
        // AppOrg.class.getProtectionDomain().getCodeSource().getLocation();
        // String path = location.getPath() + "/" + channelName + ".tx";
        File txFile = new File(this.appChannel.getTxFile());
        if (!txFile.exists()) {
            if (!this.generateChannelTx(txFile.getAbsolutePath())) {
                throw new Exception("Error Creating Channel Tx File");
            }
        }
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(txFile);

        try {
            this.channel = createFabricChannel
                    ? this.client.newChannel(this.appChannel.getName(), anOrderer, channelConfiguration,
                            this.client.getChannelConfigurationSignature(channelConfiguration, peerAdmin))
                    : this.client.newChannel(this.appChannel.getName()).addOrderer(anOrderer);
        } catch (Exception ex) {
            if (createFabricChannel) {
                this.channel = this.client.newChannel(this.appChannel.getName()).addOrderer(anOrderer);
            }
        }

        this.channel.initialize();
        log.info(String.format("Created channel %s", this.appChannel.getName()));

        this.fabricPeers.clear();
        for (var appPeer : this.peers) {
            String peerName = appPeer.getName();
            String peerLocation = appPeer.getUrl();

            // Example of setting specific options on grpc's NettyChannelBuilder
            appPeer.getProps().put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, appPeer.getProps());
            this.fabricPeers.add(peer);

            // Peer foundPeer = this.getJoinedPeer(peer.getName(), this.mspId);
            // if (foundPeer == null) {
            if (joinPeers) {
                this.channel.joinPeer(peer,
                        PeerOptions.createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER,
                                PeerRole.LEDGER_QUERY, PeerRole.CHAINCODE_QUERY, PeerRole.EVENT_SOURCE)));
                log.info(String.format("Peer %s joined channel %s", peerName, this.appChannel.getName()));
            } else {
                this.channel.addPeer(peer,
                        PeerOptions.createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER,
                                PeerRole.LEDGER_QUERY, PeerRole.CHAINCODE_QUERY, PeerRole.EVENT_SOURCE)));
            }
        }

        // add remaining orderers if any.
        for (int i = 1; i < this.fabricOrderers.size(); i++) {
            Orderer orderer = this.fabricOrderers.get(i);
            // Orderer foundOrderer = this.getJoinedOrderer(orderer.getName(), this.mspId);
            // if (foundOrderer == null)
            this.channel.addOrderer(orderer);
        }
    }

    private boolean generateChannelTx(String outFilePath) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("configtxgen", "-profile", this.appChannel.getProfile(),
                "-outputCreateChannelTx", outFilePath, "-channelID", this.appChannel.getName(), "-configPath",
                this.appChannel.getConfigPath());
        Process process = builder.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append("\n");
            }
            log.info(out.toString());
        }
        return process.exitValue() == 0;
    }

    public void addOtherOrgPeers(List<AppPeer> otherOrgAppPeers) throws InvalidArgumentException {

        this.fabricOtherPeers.clear();
        for (AppPeer otherAppPeer : otherOrgAppPeers) {

            otherAppPeer.getProps().put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = this.client.newPeer(otherAppPeer.getName(), otherAppPeer.getUrl(), otherAppPeer.getProps());
            this.fabricOtherPeers.add(peer);

            this.channel.addPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER,
                    PeerRole.LEDGER_QUERY, PeerRole.CHAINCODE_QUERY, PeerRole.EVENT_SOURCE)));
        }
    }

    public boolean verifyNoInstalledChaincodes() throws ProposalException, InvalidArgumentException {

        var results = this.client.sendLifecycleQueryInstalledChaincodes(
                this.client.newLifecycleQueryInstalledChaincodesRequest(), this.fabricPeers);

        for (var result : results) {
            if (result.getStatus() == Status.SUCCESS) {
                var lifecycleQueryInstalledChaincodesResult = result.getLifecycleQueryInstalledChaincodesResult();
                if (lifecycleQueryInstalledChaincodesResult.isEmpty())
                    return true;
            } else {
                return false;
            }
        }
        return false;
    }

    // private Peer getJoinedPeer(String name, String mspId) throws
    // InvalidArgumentException {
    // Collection<Peer> channelPeers = this.channel.getPeers();
    // if (channelPeers != null && channelPeers.size() > 0) {
    // Optional<Peer> optional = channelPeers.stream().filter(peer ->
    // peer.getName().equals(name)).findFirst();
    // if (optional.isPresent())
    // return optional.get();
    // }
    // return null;
    // }

    // private Orderer getJoinedOrderer(String name, String mspId) throws
    // InvalidArgumentException {
    // Collection<Orderer> channOrderers = this.channel.getOrderers();
    // if (channOrderers != null && channOrderers.size() > 0) {
    // Optional<Orderer> optional = channOrderers.stream().filter(orderer ->
    // orderer.getName().equals(name))
    // .findFirst();
    // if (optional.isPresent())
    // return optional.get();
    // }
    // return null;
    // }
}