package com.afengar.blockchain.hlf.counter.configuration;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Component
@Slf4j
public class AppCA extends FabricEntity {

    private String adminUserName;
    private AppUser adminUser;
    private HFCAClient client;

    public void init(boolean isTLS) {
        try {
            super.init(isTLS);
            // this.client = HFCAClient.createNewInstance(this.name, this.url, this.props);
            this.client = HFCAClient.createNewInstance(this.url, this.props);
            this.client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (MalformedURLException | IllegalAccessException | InstantiationException | ClassNotFoundException
                | CryptoException | org.hyperledger.fabric.sdk.exception.InvalidArgumentException
                | NoSuchMethodException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
            this.client = null;
        }
    }
}