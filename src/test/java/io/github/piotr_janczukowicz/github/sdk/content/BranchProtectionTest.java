package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchesBranchProtectionApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProtectionFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

@UnitTest
class BranchProtectionTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final Branch branch = new Branch(repository, RepositoryContentFixtures.branch());
    private final GitHubBranchProtection response = ProtectionFixtures.protection();
    private final BranchProtection sut = new BranchProtection(branch, response);

    @Test
    void accessors_shouldExposeTheResponse_whenTheRuleIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.branch()).isSameAs(branch);
            softly.assertThat(sut.isEnforcedForAdmins()).isTrue();
            softly.assertThat(sut.requiredApprovals()).hasValue(ProtectionFixtures.APPROVALS);
            softly.assertThat(sut.isLinearHistoryRequired()).isTrue();
            softly.assertThat(sut.allowsForcePushes()).isFalse();
            softly.assertThat(sut.allowsDeletions()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void isEnforcedForAdmins_shouldBeFalse_whenTheRuleHasNoOptions() {
        final BranchProtection bare = bareRule();

        final boolean enforced = bare.isEnforcedForAdmins();

        assertThat(enforced).isFalse();
    }

    @Test
    void requiredApprovals_shouldBeEmpty_whenTheRuleHasNoOptions() {
        final BranchProtection bare = bareRule();

        final OptionalInt approvals = bare.requiredApprovals();

        assertThat(approvals).isEmpty();
    }

    @Test
    void isLinearHistoryRequired_shouldBeFalse_whenTheRuleHasNoOptions() {
        final BranchProtection bare = bareRule();

        final boolean required = bare.isLinearHistoryRequired();

        assertThat(required).isFalse();
    }

    @Test
    void allowsForcePushes_shouldBeFalse_whenTheRuleHasNoOptions() {
        final BranchProtection bare = bareRule();

        final boolean allowed = bare.allowsForcePushes();

        assertThat(allowed).isFalse();
    }

    @Test
    void allowsDeletions_shouldBeFalse_whenTheRuleHasNoOptions() {
        final BranchProtection bare = bareRule();

        final boolean allowed = bare.allowsDeletions();

        assertThat(allowed).isFalse();
    }

    private BranchProtection bareRule() {
        return new BranchProtection(branch, ProtectionFixtures.bareProtection());
    }

    @Test
    void requiredApprovals_shouldBeEmpty_whenTheReviewsHaveNoApprovalCount() {
        final BranchProtection uncounted =
                new BranchProtection(branch, ProtectionFixtures.protectionWithoutApprovalCount());

        assertThat(uncounted.requiredApprovals()).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedRule_whenItExists() throws IOException {
        final GitHubBranchProtection reloaded = ProtectionFixtures.bareProtection();
        final GitHubBranchesBranchProtectionApi api = github.branches().branchProtection();
        doReturn(Optional.of(reloaded)).when(api).getBranchProtection(OWNER, NAME, "main");

        final BranchProtection result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.branch().name()).isEqualTo("main");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheRuleIsGone() throws IOException {
        final GitHubBranchesBranchProtectionApi api = github.branches().branchProtection();
        doReturn(Optional.empty()).when(api).getBranchProtection(OWNER, NAME, "main");

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("has no protection");
    }

    @Test
    void remove_shouldDeleteTheRule_whenCalled() throws IOException {
        final GitHubBranchesBranchProtectionApi api = github.branches().branchProtection();

        sut.remove();

        verify(api).deleteBranchProtection(OWNER, NAME, "main");
    }
}
