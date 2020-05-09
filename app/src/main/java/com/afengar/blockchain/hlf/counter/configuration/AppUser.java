package com.afengar.blockchain.hlf.counter.configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

import org.bouncycastle.util.encoders.Hex;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.util.internal.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class AppUser implements User, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -3793764158544178827L;

    @Autowired
    private transient FileSystemStore fileSystemStore;

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    private Enrollment enrollment;
    private String mspId;

    private String enrollmentSecret;
    private String organization;
    private String keyValStoreName;

    public AppUser(String name, String org, FileSystemStore store) {
        this.name = name;
        this.organization = org;
        this.fileSystemStore = store;
        this.keyValStoreName = toKeyValStoreName(this.name, org);
        String memberStr = this.fileSystemStore.getValue(keyValStoreName);
        if (null == memberStr) {
            saveState();
        } else {
            restoreState();
        }

    }

    public boolean isRegistered() {
        return !StringUtil.isNullOrEmpty(enrollmentSecret);
    }

    public boolean isEnrolled() {
        return this.enrollment != null;
    }

    public void saveState() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            this.fileSystemStore.setValue(keyValStoreName, Hex.toHexString(bos.toByteArray()));
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AppUser restoreState() {
        String memberStr = this.fileSystemStore.getValue(keyValStoreName);
        if (null != memberStr) {
            // The user was found in the key value store, so restore the
            // state.
            byte[] serialized = Hex.decode(memberStr);
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            try {
                ObjectInputStream ois = new ObjectInputStream(bis);
                AppUser state = (AppUser) ois.readObject();
                if (state != null) {
                    this.name = state.name;
                    this.roles = state.roles;
                    this.account = state.account;
                    this.affiliation = state.affiliation;
                    this.organization = state.organization;
                    this.enrollmentSecret = state.enrollmentSecret;
                    this.enrollment = state.enrollment;
                    this.mspId = state.mspId;
                    return this;
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Could not restore state of member %s", this.name), e);
            }
        }
        return null;
    }

    public static String toKeyValStoreName(String name, String org) {
        return "user." + name + org;
    }

    public static boolean isStored(String name, String org, FileSystemStore fs) {
        return fs.hasValue(toKeyValStoreName(name, org));
    }
}