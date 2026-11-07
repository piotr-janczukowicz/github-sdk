package io.github.piotr_janczukowicz.github.sdk.rulesets;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassMode;

/** How an actor may bypass a ruleset. */
public enum BypassMode {
    /** The actor may bypass the rules on any push or merge. */
    ALWAYS(GitHubRepositoryRulesetBypassMode.ALWAYS),
    /** The actor may bypass the rules only through a pull request. */
    PULL_REQUEST(GitHubRepositoryRulesetBypassMode.PULL_REQUEST);

    private final GitHubRepositoryRulesetBypassMode client;

    BypassMode(final GitHubRepositoryRulesetBypassMode client) {
        this.client = client;
    }

    GitHubRepositoryRulesetBypassMode toClient() {
        return client;
    }
}
