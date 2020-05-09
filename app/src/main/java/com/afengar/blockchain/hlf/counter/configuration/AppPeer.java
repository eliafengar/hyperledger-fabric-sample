package com.afengar.blockchain.hlf.counter.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import org.hyperledger.fabric.sdk.User;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Component
public class AppPeer extends FabricEntity {
    private String adminUserName = "peerAdmin";

    private List<String> users = new ArrayList<>();
    private Map<String, User> usersMap = new HashMap<>();

    public void addUser(AppUser user) {
        this.usersMap.put(user.getName(), user);
    }
}