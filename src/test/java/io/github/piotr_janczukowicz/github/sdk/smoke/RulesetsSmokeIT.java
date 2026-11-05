package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredLinearHistory;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetPayloadTarget;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.rulesets.Rules;
import io.github.piotr_janczukowicz.github.sdk.rulesets.Ruleset;
import java.io.IOException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class RulesetsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void ruleset_shouldBeCreatedChangedReadAndDeleted_whenARepositoryHasRulesets() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = ENV.name("ruleset");
        final Ruleset created = create(repository, name);
        try {
            final Ruleset renamed = created.edit()
                    .name(name + "-renamed")
                    .enforcement(GitHubRepositoryRuleEnforcement.DISABLED)
                    .addRule(Rules.requirePullRequest(1))
                    .removeRule(GitHubRepositoryRuleRequiredLinearHistory.class)
                    .send();
            final Ruleset reloaded = renamed.refresh();
            final boolean isListed = repository.rulesets().anyMatch(listed -> listed.id() == created.id());
            final long versions = created.history().count();

            assertSoftly(softly -> {
                softly.assertThat(created.enforcement()).isEqualTo(GitHubRepositoryRuleEnforcement.ACTIVE);
                softly.assertThat(created.rules()).hasSize(2);
                softly.assertThat(reloaded.name()).isEqualTo(name + "-renamed");
                softly.assertThat(reloaded.enforcement()).isEqualTo(GitHubRepositoryRuleEnforcement.DISABLED);
                softly.assertThat(reloaded.rules()).hasSize(2);
                softly.assertThat(isListed).isTrue();
                softly.assertThat(versions).isGreaterThanOrEqualTo(2L);
            });
        } finally {
            created.delete();
        }

        assertThat(repository.ruleset(created.id())).isEmpty();
        assertThat(repository.ruleSuites().count()).isNotNegative();
    }

    private static Ruleset create(final Repository repository, final String name) throws IOException {
        try {
            return repository
                    .newRuleset(
                            name, GitHubRepositoryRulesetPayloadTarget.BRANCH, GitHubRepositoryRuleEnforcement.ACTIVE)
                    .includeRefs("refs/heads/" + name)
                    .rule(Rules.requireLinearHistory())
                    .rule(Rules.restrictDeletion())
                    .create();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses rulesets here: no Administration permission, or a private repository on"
                    + " a plan without them");
            throw e;
        }
    }
}
