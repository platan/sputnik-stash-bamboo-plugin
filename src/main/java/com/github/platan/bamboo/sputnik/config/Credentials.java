package com.github.platan.bamboo.sputnik.config;

public final class Credentials {
    
    private final String stashUsername;
    private final String stashPassword;

    public Credentials(String stashUsername, String stashPassword) {
        this.stashUsername = stashUsername;
        this.stashPassword = stashPassword;
    }

    public String getStashUsername() {
        return stashUsername;
    }

    public String getStashPassword() {
        return stashPassword;
    }
}
