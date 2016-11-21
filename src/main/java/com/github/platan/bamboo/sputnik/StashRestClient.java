package com.github.platan.bamboo.sputnik;

import com.google.common.base.Optional;

public interface StashRestClient {
    Optional<Long> getPullRequestId(String projectKey, String repositorySlug, String branchName);
}
