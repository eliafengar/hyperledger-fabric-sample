package com.afengar.blockchain.hlf.counter.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppChannel {
    private String name;
    private String profile;
    private String configPath;
    private String txFile;
}