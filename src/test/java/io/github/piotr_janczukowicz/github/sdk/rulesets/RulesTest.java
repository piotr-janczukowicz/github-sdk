package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleCreation;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleDeletion;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleNonFastForward;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleParamsStatusCheckConfiguration;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulePullRequest;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredLinearHistory;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredSignatures;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleRequiredStatusChecks;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleUpdate;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class RulesTest {

    @Test
    void restrictCreation_shouldBuildTheMatchingRecord_whenCalled() {
        final GitHubRepositoryRule rule = Rules.restrictCreation();

        assertThat(rule).isInstanceOf(GitHubRepositoryRuleCreation.class);
    }

    @Test
    void restrictDeletion_shouldBuildTheMatchingRecord_whenCalled() {
        final GitHubRepositoryRule rule = Rules.restrictDeletion();

        assertThat(rule).isInstanceOf(GitHubRepositoryRuleDeletion.class);
    }

    @Test
    void restrictUpdates_shouldBuildTheMatchingRecord_whenCalled() {
        final GitHubRepositoryRule rule = Rules.restrictUpdates();

        assertThat(rule).isInstanceOf(GitHubRepositoryRuleUpdate.class);
    }

    @Test
    void blockForcePushes_shouldBuildTheMatchingRecord_whenCalled() {
        final GitHubRepositoryRule rule = Rules.blockForcePushes();

        assertThat(rule).isInstanceOf(GitHubRepositoryRuleNonFastForward.class);
    }

    @Test
    void requireLinearHistory_shouldBuildTheMatchingRecord_whenCalled() {
        final GitHubRepositoryRule rule = Rules.requireLinearHistory();

        assertThat(rule).isInstanceOf(GitHubRepositoryRuleRequiredLinearHistory.class);
    }

    @Test
    void requireSignedCommits_shouldBuildTheMatchingRecord_whenCalled() {
        final GitHubRepositoryRule rule = Rules.requireSignedCommits();

        assertThat(rule).isInstanceOf(GitHubRepositoryRuleRequiredSignatures.class);
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

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void requirePullRequest_shouldThrowIllegalArgumentException_whenApprovalsAreOutOfRange(final int approvals) {
        final Throwable thrown = catchThrowable(() -> Rules.requirePullRequest(approvals));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("approvals");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void requirePullRequest_shouldKeepTheApprovals_whenTheyAreOnTheBoundary(final int approvals) {
        final GitHubRepositoryRulePullRequest rule =
                (GitHubRepositoryRulePullRequest) Rules.requirePullRequest(approvals);

        assertThat(rule.parameters().requiredApprovingReviewCount()).isEqualTo(approvals);
    }

    @Test
    void requireStatusChecks_shouldListTheContexts_whenCalled() {
        final GitHubRepositoryRuleRequiredStatusChecks rule =
                (GitHubRepositoryRuleRequiredStatusChecks) Rules.requireStatusChecks(true, List.of("build", "lint"));

        assertSoftly(softly -> {
            softly.assertThat(rule.parameters().strictRequiredStatusChecksPolicy())
                    .isTrue();
            softly.assertThat(rule.parameters().requiredStatusChecks())
                    .extracting(GitHubRepositoryRuleParamsStatusCheckConfiguration::context)
                    .containsExactly("build", "lint");
        });
    }

    @Test
    void requireStatusChecks_shouldAcceptAnEmptyList_whenNoContextIsGiven() {
        final GitHubRepositoryRuleRequiredStatusChecks rule =
                (GitHubRepositoryRuleRequiredStatusChecks) Rules.requireStatusChecks(false, List.of());

        assertThat(rule.parameters().requiredStatusChecks()).isEmpty();
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
