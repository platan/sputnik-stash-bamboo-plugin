package com.github.platan.bamboo.sputnik;

import java.net.URI;

public final class StashRepositoryDetails {
    
    private final URI url;
    private final String projectKey;
    private final String repositorySlug;
    private final String branchName;
    private final String checkoutLocation;

    public StashRepositoryDetails(URI url, String projectKey, String repositorySlug, String branchName, String checkoutLocation) {
        this.url = url;
        this.projectKey = projectKey;
        this.repositorySlug = repositorySlug;
        this.branchName = branchName;
        this.checkoutLocation = checkoutLocation;
    }

    public URI getUrl() {
        return url;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getRepositorySlug() {
        return repositorySlug;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getCheckoutLocation() {
        return checkoutLocation;
    }
}
