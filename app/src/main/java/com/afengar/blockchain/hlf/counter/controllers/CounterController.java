package com.afengar.blockchain.hlf.counter.controllers;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.afengar.blockchain.hlf.counter.configuration.AppOrg;
import com.afengar.blockchain.hlf.counter.configuration.AppChaincode;
import com.afengar.blockchain.hlf.counter.configuration.App;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.var;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CounterController {

    @Autowired
    private App app;

    private String chaincodeName = "counter_cc_kotlin";
    private AppChaincode currentChaincode;

    @RequestMapping(method = RequestMethod.GET, path = "counts/{orgName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String counts(@PathVariable(name = "orgName") String orgName,
            @RequestParam(name = "private", required = false) boolean isPrivate,
            @RequestParam(name = "collection", required = false) String collectionName) {
        try {
            if (orgName == null || orgName.isEmpty())
                orgName = app.getOrganizations().get(0).getName();

            Map<String, byte[]> transientMap = null;
            if (isPrivate) {
                transientMap = new HashMap<>();
                transientMap.put("collectionName", collectionName.getBytes());
            }

            return currentChaincode.query(orgName, "readCounter", new String[] {}, transientMap);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return "";
    }

    @RequestMapping(method = RequestMethod.GET, path = "increment/{orgName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void increment(@PathVariable(name = "orgName") String orgName, @RequestParam(name = "delta") String delta,
            @RequestParam(name = "private", required = false) boolean isPrivate,
            @RequestParam(name = "collection", required = false) String collectionName) {
        this.executeChaincode(orgName, "increment", delta, isPrivate, collectionName);
    }

    @RequestMapping(method = RequestMethod.GET, path = "decrement/{orgName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void decrement(@PathVariable(name = "orgName") String orgName, @RequestParam(name = "delta") String delta,
            @RequestParam(name = "private", required = false) boolean isPrivate,
            @RequestParam(name = "collection", required = false) String collectionName) {
        this.executeChaincode(orgName, "decrement", delta, isPrivate, collectionName);
    }

    private void executeChaincode(String orgName, String funcName, String delta, boolean isPrivate,
            String collectionName) {
        try {
            if (orgName == null || orgName.isEmpty())
                orgName = app.getOrganizations().get(0).getName();

            if (delta == null || delta.isEmpty())
                delta = "1";

            Map<String, byte[]> transientMap = null;
            if (isPrivate) {
                transientMap = new HashMap<>();
                transientMap.put("collectionName", collectionName.getBytes());
            }
            currentChaincode.execute(orgName, funcName, new String[] { delta }, transientMap, false);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "deployChainCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public void deployCC(@RequestParam(name = "createChannel") boolean createChannel) {
        try {

            ////////////////////////////
            // Construct and run the channels
            List<AppOrg> orgs = app.getOrganizations();

            boolean createFabricChannel = createChannel;
            for (var org : orgs) {
                org.constructChannel(createFabricChannel, createChannel);
                if (createChannel && !org.verifyNoInstalledChaincodes()) {
                    log.info("Found Chaincode already Installed in Org: " + org.getName() + " Channel:"
                            + org.getAppChannel().getName());
                    return;
                }
                createFabricChannel = false;
            }

            for (AppOrg org : orgs) {
                for (AppOrg otherOrg : orgs) {
                    if (!org.getName().equals(otherOrg.getName()))
                        org.addOtherOrgPeers(otherOrg.getPeers());
                }
            }

            // For Logging purposes
            for (AppOrg org : orgs) {
                log.info(String.format("Org: %s, Channel Name: %s, Peers: %s", org.getName(),
                        org.getChannel().getName(), org.getChannel().getPeers().size()));
            }

            AppChaincode cc = this.getChaincodeByName(this.chaincodeName);
            this.currentChaincode = cc;
            cc.installAndVerify();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private AppChaincode getChaincodeByName(String ccName) {
        AppChaincode retVal = null;

        Optional<AppChaincode> optionalCC = app.getChaincodes().stream().filter(cc -> cc.getName().equals(ccName))
                .findFirst();

        if (optionalCC.isPresent())
            retVal = optionalCC.get();

        return retVal;
    }
}