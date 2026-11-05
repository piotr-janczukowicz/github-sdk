package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchesBranchProtectionApi;
import io.github.piotr_janczukowicz.github.api.branches.GitHubUpdateBranchProtectionPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProtectionFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class BranchProtectionUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final Branch branch = new Branch(repository, RepositoryContentFixtures.branch());
    private final GitHubBranchesBranchProtectionApi api = github.branches().branchProtection();

    @Test
    void send_shouldSwitchEverythingOff_whenNothingIsChosen() throws IOException {
        final GitHubBranchProtection held = ProtectionFixtures.bareProtection();
        doReturn(Optional.of(held)).when(api).getBranchProtection(OWNER, NAME, "main");
        final ArgumentCaptor<GitHubUpdateBranchProtectionPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateBranchProtectionPayload.class);

        final BranchProtection result = branch.protect().send();

        verify(api).updateBranchProtection(eq(OWNER), eq(NAME), eq("main"), payload.capture());
        final GitHubUpdateBranchProtectionPayload sent = payload.getValue();
        assertSoftly(softly -> {
            softly.assertThat(sent.requiredStatusChecks().value()).isNull();
            softly.assertThat(sent.requiredPullRequestReviews().value()).isNull();
            softly.assertThat(sent.restrictions().value()).isNull();
            softly.assertThat(sent.enforceAdmins().value()).isFalse();
            softly.assertThat(sent.allowForcePushes().value()).isFalse();
            softly.assertThat(sent.requiredLinearHistory()).isFalse();
            softly.assertThat(sent.allowDeletions()).isFalse();
            softly.assertThat(sent.blockCreations()).isFalse();
            softly.assertThat(sent.requiredConversationResolution()).isFalse();
            softly.assertThat(sent.lockBranch()).isFalse();
            softly.assertThat(sent.allowForkSyncing()).isFalse();
            softly.assertThat(result.response()).isSameAs(held);
        });
    }

    @Test
    void send_shouldSendEveryChosenSetting_whenAllAreChosen() throws IOException {
        doReturn(Optional.of(ProtectionFixtures.protection())).when(api).getBranchProtection(OWNER, NAME, "main");
        final ArgumentCaptor<GitHubUpdateBranchProtectionPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateBranchProtectionPayload.class);

        branch.protect()
                .requireStatusChecks(true, List.of("build"))
                .requirePullRequestReviews(2, true, true, true)
                .enforceAdmins()
                .requireLinearHistory()
                .allowForcePushes()
                .allowDeletions()
                .blockCreations()
                .requireConversationResolution()
                .lockBranch()
                .allowForkSyncing()
                .restrictPushesTo(List.of("octocat"), List.of("core"), List.of("ci"))
                .send();

        verify(api).updateBranchProtection(eq(OWNER), eq(NAME), eq("main"), payload.capture());
        final GitHubUpdateBranchProtectionPayload sent = payload.getValue();
        assertSoftly(softly -> {
            softly.assertThat(sent.requiredStatusChecks().value().strict()).isTrue();
            softly.assertThat(sent.requiredStatusChecks().value().contexts()).containsExactly("build");
            softly.assertThat(sent.requiredPullRequestReviews().value().requiredApprovingReviewCount())
                    .isEqualTo(2);
            softly.assertThat(sent.requiredPullRequestReviews().value().dismissStaleReviews())
                    .isTrue();
            softly.assertThat(sent.requiredPullRequestReviews().value().requireCodeOwnerReviews())
                    .isTrue();
            softly.assertThat(sent.requiredPullRequestReviews().value().requireLastPushApproval())
                    .isTrue();
            softly.assertThat(sent.restrictions().value().users()).containsExactly("octocat");
            softly.assertThat(sent.restrictions().value().teams()).containsExactly("core");
            softly.assertThat(sent.restrictions().value().apps()).containsExactly("ci");
            softly.assertThat(sent.enforceAdmins().value()).isTrue();
            softly.assertThat(sent.allowForcePushes().value()).isTrue();
            softly.assertThat(sent.requiredLinearHistory()).isTrue();
            softly.assertThat(sent.allowDeletions()).isTrue();
            softly.assertThat(sent.blockCreations()).isTrue();
            softly.assertThat(sent.requiredConversationResolution()).isTrue();
            softly.assertThat(sent.lockBranch()).isTrue();
            softly.assertThat(sent.allowForkSyncing()).isTrue();
        });
    }

    @Test
    void send_shouldRequireTheApprovalsOnly_whenNoReviewOptionsAreGiven() throws IOException {
        doReturn(Optional.of(ProtectionFixtures.protection())).when(api).getBranchProtection(OWNER, NAME, "main");
        final ArgumentCaptor<GitHubUpdateBranchProtectionPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateBranchProtectionPayload.class);

        branch.protect().requirePullRequestReviews(1).send();

        verify(api).updateBranchProtection(eq(OWNER), eq(NAME), eq("main"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue()
                            .requiredPullRequestReviews()
                            .value()
                            .requiredApprovingReviewCount())
                    .isEqualTo(1);
            softly.assertThat(payload.getValue()
                            .requiredPullRequestReviews()
                            .value()
                            .dismissStaleReviews())
                    .isFalse();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenGitHubReportsNoRuleAfterwards() throws IOException {
        doReturn(Optional.empty()).when(api).getBranchProtection(OWNER, NAME, "main");
        final BranchProtectionUpdate update = branch.protect();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("has no protection");
        verify(api).updateBranchProtection(eq(OWNER), eq(NAME), eq("main"), any());
    }

    @Test
    void requireStatusChecks_shouldThrowNullPointerException_whenContextsAreNull() {
        final BranchProtectionUpdate update = branch.protect();

        final Throwable thrown = catchThrowable(() -> update.requireStatusChecks(true, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("contexts");
    }

    @Test
    void restrictPushesTo_shouldThrowNullPointerException_whenAListIsNull() {
        final BranchProtectionUpdate update = branch.protect();

        final Throwable thrown = catchThrowable(() -> update.restrictPushesTo(List.of(), null, List.of()));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("teams");
    }

    @Test
    void requirePullRequestReviews_shouldThrowIllegalArgumentException_whenApprovalsAreOutOfRange() {
        final Throwable thrown = catchThrowable(() -> branch.protect().requirePullRequestReviews(7));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("approvals");
    }

    @Test
    void requirePullRequestReviews_shouldThrowIllegalArgumentException_whenApprovalsAreNegative() {
        final Throwable thrown = catchThrowable(() -> branch.protect().requirePullRequestReviews(-1));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("approvals");
    }

    @Test
    void requireStatusChecks_shouldThrowIllegalArgumentException_whenAContextIsBlank() {
        final Throwable thrown = catchThrowable(() -> branch.protect().requireStatusChecks(false, List.of(" ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("contexts");
    }

    @Test
    void restrictPushesTo_shouldThrowIllegalArgumentException_whenALoginIsBlank() {
        final Throwable thrown =
                catchThrowable(() -> branch.protect().restrictPushesTo(List.of(" "), List.of(), List.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("users");
    }
}
