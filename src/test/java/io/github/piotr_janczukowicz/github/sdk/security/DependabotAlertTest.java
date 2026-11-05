package io.github.piotr_janczukowicz.github.sdk.security;

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
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlert;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertDismissedReason;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertSeverity;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertState;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertUpdateState;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertsApi;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubUpdateAlertPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.SecurityFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class DependabotAlertTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = SecurityFixtures.DEPENDABOT_NUMBER;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubDependabotAlert response = SecurityFixtures.dependabotAlert();
    private final DependabotAlert sut = new DependabotAlert(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAlertIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.state()).isEqualTo(GitHubDependabotAlertState.OPEN);
            softly.assertThat(sut.isOpen()).isTrue();
            softly.assertThat(sut.packageName()).hasValue("lodash");
            softly.assertThat(sut.severity()).isEqualTo(GitHubDependabotAlertSeverity.HIGH);
            softly.assertThat(sut.summary()).isEqualTo("Prototype pollution");
            softly.assertThat(sut.htmlUrl()).endsWith("dependabot/7");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void isOpen_shouldBeFalse_whenTheAlertIsDismissed() {
        final DependabotAlert dismissed = new DependabotAlert(
                repository, SecurityFixtures.dependabotAlertWith(GitHubDependabotAlertState.DISMISSED));

        assertThat(dismissed.isOpen()).isFalse();
    }

    @Test
    void packageName_shouldBeEmpty_whenGitHubReportsNoPackage() {
        final DependabotAlert bare = new DependabotAlert(repository, SecurityFixtures.dependabotAlertWithoutPackage());

        assertThat(bare.packageName()).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedAlert_whenItExists() throws IOException {
        final GitHubDependabotAlert reloaded = SecurityFixtures.dependabotAlertWith(GitHubDependabotAlertState.FIXED);
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(Optional.of(reloaded)).when(api).getAlert(OWNER, NAME, NUMBER);

        final DependabotAlert result = sut.refresh();

        assertThat(result.state()).isEqualTo(GitHubDependabotAlertState.FIXED);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAlertIsGone() throws IOException {
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, NUMBER);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void dismiss_shouldSendTheReasonWithoutComment_whenOnlyTheReasonIsGiven() throws IOException {
        final GitHubDependabotAlert dismissed =
                SecurityFixtures.dependabotAlertWith(GitHubDependabotAlertState.DISMISSED);
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(dismissed).when(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAlertPayload.class);

        final DependabotAlert result = sut.dismiss(DependabotDismissal.NOT_USED);

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubDependabotAlertUpdateState.DISMISSED);
            softly.assertThat(payload.getValue().dismissedReason())
                    .isEqualTo(GitHubDependabotAlertDismissedReason.NOT_USED);
            softly.assertThat(payload.getValue().dismissedComment()).isNull();
            softly.assertThat(result.response()).isSameAs(dismissed);
        });
    }

    @Test
    void dismiss_shouldSendTheComment_whenOneIsGiven() throws IOException {
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(response).when(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAlertPayload.class);

        sut.dismiss(DependabotDismissal.TOLERABLE_RISK, "Accepted");

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertThat(payload.getValue().dismissedComment()).isEqualTo("Accepted");
    }

    @Test
    void reopen_shouldSendTheOpenStateOnly_whenCalled() throws IOException {
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(response).when(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAlertPayload.class);

        sut.reopen();

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubDependabotAlertUpdateState.OPEN);
            softly.assertThat(payload.getValue().dismissedReason()).isNull();
        });
    }

    @Test
    void dismiss_shouldThrowNullPointerException_whenReasonIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.dismiss(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("reason");
    }

    @Test
    void dismiss_shouldThrowNullPointerException_whenCommentIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.dismiss(DependabotDismissal.NOT_USED, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("comment");
    }
}
