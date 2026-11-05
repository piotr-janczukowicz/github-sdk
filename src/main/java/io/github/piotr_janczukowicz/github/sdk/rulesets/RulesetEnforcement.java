package io.github.piotr_janczukowicz.github.sdk.rulesets;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;

/** Whether GitHub enforces a ruleset. */
public enum RulesetEnforcement {
    /** The rules are enforced. */
    ACTIVE(GitHubRepositoryRuleEnforcement.ACTIVE),
    /**
     * The rules are only evaluated and the result is recorded in rule suites, so nothing is blocked: a dry run. GitHub
     * offers it only on the Enterprise plan and refuses it with HTTP 422 elsewhere.
     */
    EVALUATE(GitHubRepositoryRuleEnforcement.EVALUATE),
    /** The ruleset exists but does nothing. */
    DISABLED(GitHubRepositoryRuleEnforcement.DISABLED);

    private final GitHubRepositoryRuleEnforcement client;

    RulesetEnforcement(final GitHubRepositoryRuleEnforcement client) {
        this.client = client;
    }

    GitHubRepositoryRuleEnforcement toClient() {
        return client;
    }
}
