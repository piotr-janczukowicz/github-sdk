package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleset;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassActor;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetTarget;
import io.github.piotr_janczukowicz.github.sdk.rulesets.Rules;
import java.util.List;

public final class RulesetFixtures {

    public static final long RULESET_ID = 41L;

    private RulesetFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubRepositoryRuleset ruleset() {
        return rulesetWith(List.of(Rules.restrictCreation()));
    }

    public static GitHubRepositoryRuleset rulesetWith(final List<GitHubRepositoryRule> rules) {
        final GitHubRepositoryRuleset ruleset = mock(GitHubRepositoryRuleset.class);
        lenient().when(ruleset.id()).thenReturn(RULESET_ID);
        lenient().when(ruleset.name()).thenReturn("protect-main");
        lenient().when(ruleset.target()).thenReturn(GitHubRepositoryRulesetTarget.BRANCH);
        lenient().when(ruleset.enforcement()).thenReturn(GitHubRepositoryRuleEnforcement.ACTIVE);
        lenient().when(ruleset.rules()).thenReturn(rules);
        lenient().when(ruleset.bypassActors()).thenReturn(List.of(mock(GitHubRepositoryRulesetBypassActor.class)));
        return ruleset;
    }

    public static GitHubRepositoryRuleset rulesetWithoutRulesAndActors() {
        final GitHubRepositoryRuleset ruleset = mock(GitHubRepositoryRuleset.class);
        lenient().when(ruleset.id()).thenReturn(RULESET_ID);
        lenient().when(ruleset.rules()).thenReturn(null);
        lenient().when(ruleset.bypassActors()).thenReturn(null);
        return ruleset;
    }
}
