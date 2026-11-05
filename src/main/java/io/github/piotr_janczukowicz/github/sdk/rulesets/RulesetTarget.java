package io.github.piotr_janczukowicz.github.sdk.rulesets;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetPayloadTarget;

/** What a ruleset applies to. */
public enum RulesetTarget {
    /** Branches whose names match the ref patterns of the ruleset. */
    BRANCH(GitHubRepositoryRulesetPayloadTarget.BRANCH),
    /** Tags whose names match the ref patterns of the ruleset. */
    TAG(GitHubRepositoryRulesetPayloadTarget.TAG),
    /** Pushes, which are checked as a whole and not by ref name. */
    PUSH(GitHubRepositoryRulesetPayloadTarget.PUSH);

    private final GitHubRepositoryRulesetPayloadTarget client;

    RulesetTarget(final GitHubRepositoryRulesetPayloadTarget client) {
        this.client = client;
    }

    GitHubRepositoryRulesetPayloadTarget toClient() {
        return client;
    }
}
