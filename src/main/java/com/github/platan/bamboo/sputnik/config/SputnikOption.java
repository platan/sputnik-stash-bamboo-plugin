package com.github.platan.bamboo.sputnik.config;

public enum SputnikOption {

    CONNECTOR_TYPE("connector.type"),
    HOST("connector.host"),
    PORT("connector.port"),
    PATH("connector.path"),
    USE_HTTPS("connector.useHttps"),
    USERNAME("connector.username"),
    PASSWORD("connector.password"),
    PROJECT("connector.project"),
    REPOSITORY("connector.repository"),
    VERIFY_SSL("connector.verifySsl");

    private final String key;

    SputnikOption(String key) {
        this.key = key;
    }

    public String getPrefixedKey() {
        return String.format("sputnik.%s", key);
    }

    public String getKey() {
        return key;
    }

}
