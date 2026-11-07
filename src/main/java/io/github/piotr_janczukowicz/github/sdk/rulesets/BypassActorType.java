package io.github.piotr_janczukowicz.github.sdk.rulesets;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassActorType;

/** The kind of actor that may bypass a ruleset. */
public enum BypassActorType {
    /** A GitHub App; the actor id is the app id. */
    INTEGRATION(GitHubRepositoryRulesetBypassActorType.INTEGRATION),
    /** A repository role; the actor id is the role id, for example 5 for admin. */
    REPOSITORY_ROLE(GitHubRepositoryRulesetBypassActorType.REPOSITORY_ROLE),
    /** A team of the organization; the actor id is the team id. */
    TEAM(GitHubRepositoryRulesetBypassActorType.TEAM),
    /** A deploy key; it has no actor id. */
    DEPLOY_KEY(GitHubRepositoryRulesetBypassActorType.DEPLOY_KEY),
    /** The administrators of the organization; the actor id is 1. */
    ORGANIZATION_ADMIN(GitHubRepositoryRulesetBypassActorType.ORGANIZATION_ADMIN);

    private final GitHubRepositoryRulesetBypassActorType client;

    BypassActorType(final GitHubRepositoryRulesetBypassActorType client) {
        this.client = client;
    }

    GitHubRepositoryRulesetBypassActorType toClient() {
        return client;
    }
}
