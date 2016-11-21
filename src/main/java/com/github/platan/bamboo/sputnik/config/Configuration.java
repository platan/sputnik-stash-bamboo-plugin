package com.github.platan.bamboo.sputnik.config;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class Configuration {

    private final Credentials credentials;
    private final String sputnikPath;
    private final Map<String, String> properties;

    public Configuration(Credentials credentials, String sputnikPath, Map<String, String> properties) {
        this.credentials = credentials;
        this.sputnikPath = sputnikPath;
        this.properties = ImmutableMap.copyOf(properties);
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public String getSputnikPath() {
        return sputnikPath;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
