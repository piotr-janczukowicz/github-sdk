package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleCreation;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleDeletion;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleNonFastForward;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulePullRequest;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredLinearHistory;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredSignatures;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredStatusChecks;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleUpdate;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RulesTest {

    @Test
    void simpleRules_shouldBuildTheMatchingRecords_whenCalled() {
        final GitHubRepositoryRule creation = Rules.restrictCreation();
        final GitHubRepositoryRule deletion = Rules.restrictDeletion();
        final GitHubRepositoryRule update = Rules.restrictUpdates();
        final GitHubRepositoryRule forcePush = Rules.blockForcePushes();
        final GitHubRepositoryRule linear = Rules.requireLinearHistory();
        final GitHubRepositoryRule signed = Rules.requireSignedCommits();

        assertSoftly(softly -> {
            softly.assertThat(creation).isInstanceOf(GitHubRepositoryRuleCreation.class);
            softly.assertThat(deletion).isInstanceOf(GitHubRepositoryRuleDeletion.class);
            softly.assertThat(update).isInstanceOf(GitHubRepositoryRuleUpdate.class);
            softly.assertThat(forcePush).isInstanceOf(GitHubRepositoryRuleNonFastForward.class);
            softly.assertThat(linear).isInstanceOf(GitHubRepositoryRuleRequiredLinearHistory.class);
            softly.assertThat(signed).isInstanceOf(GitHubRepositoryRuleRequiredSignatures.class);
        });
    }

    @Test
    void requirePullRequest_shouldSetOnlyTheApprovals_whenNoOptionIsGiven() {
        final GitHubRepositoryRulePullRequest rule = (GitHubRepositoryRulePullRequest) Rules.requirePullRequest(2);

        assertSoftly(softly -> {
            softly.assertThat(rule.parameters().requiredApprovingReviewCount()).isEqualTo(2);
            softly.assertThat(rule.parameters().dismissStaleReviewsOnPush()).isFalse();
            softly.assertThat(rule.parameters().requireCodeOwnerReview()).isFalse();
            softly.assertThat(rule.parameters().requireLastPushApproval()).isFalse();
            softly.assertThat(rule.parameters().requiredReviewThreadResolution())
                    .isFalse();
        });
    }

    @Test
    void requirePullRequest_shouldSetEveryOption_whenTheyAreGiven() {
        final GitHubRepositoryRulePullRequest rule =
                (GitHubRepositoryRulePullRequest) Rules.requirePullRequest(1, true, true, true, true);

        assertSoftly(softly -> {
            softly.assertThat(rule.parameters().dismissStaleReviewsOnPush()).isTrue();
            softly.assertThat(rule.parameters().requireCodeOwnerReview()).isTrue();
            softly.assertThat(rule.parameters().requireLastPushApproval()).isTrue();
            softly.assertThat(rule.parameters().requiredReviewThreadResolution())
                    .isTrue();
        });
    }

    @Test
    void requirePullRequest_shouldThrowIllegalArgumentException_whenApprovalsAreAboveTen() {
        final Throwable thrown = catchThrowable(() -> Rules.requirePullRequest(11));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("approvals");
    }

    @Test
    void requirePullRequest_shouldThrowIllegalArgumentException_whenApprovalsAreNegative() {
        final Throwable thrown = catchThrowable(() -> Rules.requirePullRequest(-1));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("approvals");
    }

    @Test
    void requireStatusChecks_shouldListTheContexts_whenCalled() {
        final GitHubRepositoryRuleRequiredStatusChecks rule =
                (GitHubRepositoryRuleRequiredStatusChecks) Rules.requireStatusChecks(true, List.of("build", "lint"));

        assertSoftly(softly -> {
            softly.assertThat(rule.parameters().strictRequiredStatusChecksPolicy())
                    .isTrue();
            softly.assertThat(rule.parameters().requiredStatusChecks())
                    .extracting(check -> check.context())
                    .containsExactly("build", "lint");
        });
    }

    @Test
    void requireStatusChecks_shouldThrowNullPointerException_whenContextsAreNull() {
        final Throwable thrown = catchThrowable(() -> Rules.requireStatusChecks(false, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("contexts");
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalledByReflection() throws NoSuchMethodException {
        final Constructor<Rules> constructor = Rules.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requireStatusChecks_shouldThrowIllegalArgumentException_whenAContextIsBlank() {
        final Throwable thrown = catchThrowable(() -> Rules.requireStatusChecks(false, List.of(" ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("contexts");
    }
}
