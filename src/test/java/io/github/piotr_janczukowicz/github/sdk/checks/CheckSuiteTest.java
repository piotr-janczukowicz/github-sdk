package io.github.piotr_janczukowicz.github.sdk.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuite;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuiteConclusion;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuiteStatus;
import io.github.piotr_janczukowicz.github.api.checks.GitHubChecksSuitesApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ExtrasFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CheckSuiteTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ExtrasFixtures.SUITE_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCheckSuite response = ExtrasFixtures.checkSuite();
    private final CheckSuite sut = new CheckSuite(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenSuiteIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.headSha()).isEqualTo(ExtrasFixtures.SHA);
            softly.assertThat(sut.headBranch()).hasValue("main");
            softly.assertThat(sut.status()).hasValue(GitHubCheckSuiteStatus.COMPLETED);
            softly.assertThat(sut.conclusion()).hasValue(GitHubCheckSuiteConclusion.SUCCESS);
            softly.assertThat(sut.isSuccessful()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void headBranch_shouldBeEmpty_whenGitHubReportsNone() {
        final CheckSuite running = runningSuite();

        final Optional<String> headBranch = running.headBranch();

        assertThat(headBranch).isEmpty();
    }

    @Test
    void conclusion_shouldBeEmpty_whenTheSuiteHasNotCompleted() {
        final CheckSuite running = runningSuite();

        final Optional<GitHubCheckSuiteConclusion> conclusion = running.conclusion();

        assertThat(conclusion).isEmpty();
    }

    @Test
    void isSuccessful_shouldBeFalse_whenTheSuiteHasNotCompleted() {
        final CheckSuite running = runningSuite();

        final boolean successful = running.isSuccessful();

        assertThat(successful).isFalse();
    }

    @Test
    void status_shouldBeEmpty_whenGitHubReportsNone() {
        final CheckSuite bare = new CheckSuite(repository, ExtrasFixtures.checkSuiteWith("main", null, null));

        assertThat(bare.status()).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedSuite_whenItExists() throws IOException {
        final GitHubCheckSuite reloaded = ExtrasFixtures.checkSuite();
        final GitHubChecksSuitesApi api = github.checks().suites();
        doReturn(Optional.of(reloaded)).when(api).getSuite(OWNER, NAME, ID);

        final CheckSuite result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheSuiteIsGone() throws IOException {
        final GitHubChecksSuitesApi api = github.checks().suites();
        doReturn(Optional.empty()).when(api).getSuite(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    private CheckSuite runningSuite() {
        return new CheckSuite(
                repository, ExtrasFixtures.checkSuiteWith(null, GitHubCheckSuiteStatus.IN_PROGRESS, null));
    }

    @Test
    void rerequest_shouldAskForTheChecksAgain_whenCalled() throws IOException {
        final GitHubChecksSuitesApi api = github.checks().suites();

        sut.rerequest();

        verify(api).rerequestSuite(OWNER, NAME, ID);
    }
}
