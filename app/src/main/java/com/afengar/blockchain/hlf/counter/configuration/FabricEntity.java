package com.afengar.blockchain.hlf.counter.configuration;

import java.util.Properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class FabricEntity {
    protected String name = "";
    protected String url = "";
    protected Properties props = new Properties();

    protected void init(boolean isTLS) {
        if (isTLS)
            this.tlsify();
    }

    protected void tlsify() {
        this.url = this.url.trim();
        if (this.url.startsWith("grpc"))
            this.url = this.url.replaceFirst("^grpc://", "grpcs://");
        if (this.url.startsWith("http"))
            this.url = this.url.replaceFirst("^http://", "https://");
    }
}