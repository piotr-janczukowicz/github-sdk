package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuite;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuitePreference;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCreateCheckRunPayload;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCreateSuitePayload;
import io.github.piotr_janczukowicz.github.api.checks.GitHubSetSuitesPreferencesPayload;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckRun;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckSuite;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ExtrasFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryChecksTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ChecksFixtures.CHECK_RUN_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void checkRun_shouldReturnTheCheckRun_whenGitHubHasIt() throws IOException {
        final GitHubCheckRun loaded = ChecksFixtures.checkRun();
        when(github.checks().runs().getCheckRun(OWNER, NAME, ID)).thenReturn(Optional.of(loaded));

        final Optional<CheckRun> found = sut.checkRun(ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void checkRun_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        when(github.checks().runs().getCheckRun(OWNER, NAME, ID)).thenReturn(Optional.empty());

        final Optional<CheckRun> found = sut.checkRun(ID);

        assertThat(found).isEmpty();
    }

    @Test
    void createCheckRun_shouldSendNameAndSha_whenCalledWithBoth() throws IOException {
        final GitHubCheckRun created = ChecksFixtures.checkRun();
        when(github.checks().runs().createCheckRun(eq(OWNER), eq(NAME), any())).thenReturn(created);
        final ArgumentCaptor<GitHubCreateCheckRunPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateCheckRunPayload.class);

        final CheckRun run = sut.createCheckRun("build", "abc123");

        verify(github.checks().runs()).createCheckRun(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(run.response()).isSameAs(created);
            softly.assertThat(payload.getValue().name()).isEqualTo("build");
            softly.assertThat(payload.getValue().headSha()).isEqualTo("abc123");
        });
    }

    @Test
    void createCheckRun_shouldSendThePayload_whenCalledWithOne() throws IOException {
        final GitHubCreateCheckRunPayload payload =
                new GitHubCreateCheckRunPayload("build", "abc123", null, null, null, null, null, null, null, null);
        final GitHubCheckRun created = ChecksFixtures.checkRun();
        when(github.checks().runs().createCheckRun(OWNER, NAME, payload)).thenReturn(created);

        final CheckRun run = sut.createCheckRun(payload);

        assertThat(run.response()).isSameAs(created);
    }

    @Test
    void createCheckRun_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createCheckRun(" ", "abc123"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void createCheckRun_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createCheckRun((GitHubCreateCheckRunPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void createCheckSuite_shouldSendTheSha_whenCalled() throws IOException {
        final GitHubCheckSuite created = ExtrasFixtures.checkSuite();
        when(github.checks().suites().createSuite(eq(OWNER), eq(NAME), any())).thenReturn(created);
        final ArgumentCaptor<GitHubCreateSuitePayload> payload =
                ArgumentCaptor.forClass(GitHubCreateSuitePayload.class);

        final CheckSuite suite = sut.createCheckSuite("abc123");

        verify(github.checks().suites()).createSuite(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(suite.response()).isSameAs(created);
            softly.assertThat(payload.getValue().headSha()).isEqualTo("abc123");
        });
    }

    @Test
    void createCheckSuite_shouldThrowIllegalArgumentException_whenShaIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createCheckSuite(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeCheckSuitePreferences_shouldSendThePayload_whenCalled() throws IOException {
        final GitHubSetSuitesPreferencesPayload payload = new GitHubSetSuitesPreferencesPayload(null);
        final GitHubCheckSuitePreference preference = mock(GitHubCheckSuitePreference.class);
        when(github.checks().suites().updateSuitesPreferences(OWNER, NAME, payload))
                .thenReturn(preference);

        final GitHubCheckSuitePreference result = sut.changeCheckSuitePreferences(payload);

        assertThat(result).isSameAs(preference);
    }

    @Test
    void changeCheckSuitePreferences_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.changeCheckSuitePreferences(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }
}
