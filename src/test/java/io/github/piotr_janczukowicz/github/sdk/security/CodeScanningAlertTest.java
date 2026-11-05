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
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlert;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertDismissedReason;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertInstanceList;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertSetState;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertState;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningCodeScanningApi;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubUpdateAlertPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.SecurityFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class CodeScanningAlertTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = SecurityFixtures.CODE_NUMBER;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCodeScanningAlert response = SecurityFixtures.codeAlert();
    private final CodeScanningAlert sut = new CodeScanningAlert(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAlertIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.state()).contains(GitHubCodeScanningAlertState.OPEN);
            softly.assertThat(sut.isOpen()).isTrue();
            softly.assertThat(sut.ruleId()).isEqualTo("java/sql-injection");
            softly.assertThat(sut.ruleName()).isEqualTo("SQL injection");
            softly.assertThat(sut.htmlUrl()).endsWith("code-scanning/3");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void isOpen_shouldBeFalse_whenTheAlertIsDismissed() {
        final CodeScanningAlert dismissed = new CodeScanningAlert(
                repository, SecurityFixtures.codeAlertWith(GitHubCodeScanningAlertState.DISMISSED));

        assertThat(dismissed.isOpen()).isFalse();
    }

    @Test
    void refresh_shouldReturnTheReloadedAlert_whenItExists() throws IOException {
        final GitHubCodeScanningAlert reloaded = SecurityFixtures.codeAlertWith(GitHubCodeScanningAlertState.FIXED);
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(Optional.of(reloaded)).when(api).getAlert(OWNER, NAME, NUMBER);

        final CodeScanningAlert result = sut.refresh();

        assertThat(result.state()).contains(GitHubCodeScanningAlertState.FIXED);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAlertIsGone() throws IOException {
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, NUMBER);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void instances_shouldStreamTheInstances_whenTheyExist() throws IOException {
        final GitHubCodeScanningAlertInstanceList instance = mock(GitHubCodeScanningAlertInstanceList.class);
        when(github.codeScanning().codeScanning().listAlertInstances(eq(OWNER), eq(NAME), eq(NUMBER)).stream())
                .thenReturn(Stream.of(instance));

        final List<GitHubCodeScanningAlertInstanceList> instances =
                sut.instances().toList();

        assertThat(instances).containsExactly(instance);
    }

    @Test
    void dismiss_shouldSendTheReasonWithoutComment_whenOnlyTheReasonIsGiven() throws IOException {
        final GitHubCodeScanningAlert dismissed =
                SecurityFixtures.codeAlertWith(GitHubCodeScanningAlertState.DISMISSED);
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(dismissed).when(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAlertPayload.class);

        final CodeScanningAlert result = sut.dismiss(CodeScanningDismissal.FALSE_POSITIVE);

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubCodeScanningAlertSetState.DISMISSED);
            softly.assertThat(payload.getValue().dismissedReason())
                    .isEqualTo(GitHubCodeScanningAlertDismissedReason.FALSE_POSITIVE);
            softly.assertThat(payload.getValue().dismissedComment()).isNull();
            softly.assertThat(result.response()).isSameAs(dismissed);
        });
    }

    @Test
    void dismiss_shouldSendTheComment_whenOneIsGiven() throws IOException {
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(response).when(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAlertPayload.class);

        sut.dismiss(CodeScanningDismissal.MITIGATED, "Handled upstream");

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertThat(payload.getValue().dismissedComment()).isEqualTo("Handled upstream");
    }

    @Test
    void reopen_shouldSendTheOpenStateOnly_whenCalled() throws IOException {
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(response).when(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAlertPayload.class);

        sut.reopen();

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubCodeScanningAlertSetState.OPEN);
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
        final Throwable thrown = catchThrowable(() -> sut.dismiss(CodeScanningDismissal.MITIGATED, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("comment");
    }
}
