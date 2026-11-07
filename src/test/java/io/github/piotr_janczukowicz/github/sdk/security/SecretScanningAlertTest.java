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
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertResolution;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertState;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertWithMetadata;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertsApi;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningLocation;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubUpdateSecretScanningAlertPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
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
class SecretScanningAlertTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = SecurityFixtures.SECRET_NUMBER;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubSecretScanningAlertWithMetadata response = SecurityFixtures.secretAlert();
    private final SecretScanningAlert sut = new SecretScanningAlert(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAlertIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.state()).isEqualTo(GitHubSecretScanningAlertState.OPEN);
            softly.assertThat(sut.isOpen()).isTrue();
            softly.assertThat(sut.resolution()).isEmpty();
            softly.assertThat(sut.secretType()).contains("github_personal_access_token");
            softly.assertThat(sut.isPubliclyLeaked()).isTrue();
            softly.assertThat(sut.htmlUrl())
                    .hasValueSatisfying(url -> assertThat(url).endsWith("secret-scanning/5"));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldReportTheResolution_whenTheAlertIsResolved() {
        final SecretScanningAlert resolved = new SecretScanningAlert(
                repository,
                SecurityFixtures.secretAlertWith(
                        GitHubSecretScanningAlertState.RESOLVED, GitHubSecretScanningAlertResolution.REVOKED, null));

        assertSoftly(softly -> {
            softly.assertThat(resolved.isOpen()).isFalse();
            softly.assertThat(resolved.resolution()).hasValue(GitHubSecretScanningAlertResolution.REVOKED);
            softly.assertThat(resolved.isPubliclyLeaked()).isFalse();
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedAlert_whenItExists() throws IOException {
        final GitHubSecretScanningAlertWithMetadata reloaded = SecurityFixtures.secretAlertWith(
                GitHubSecretScanningAlertState.RESOLVED, GitHubSecretScanningAlertResolution.WONT_FIX, true);
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(Optional.of(reloaded)).when(api).getAlert(OWNER, NAME, NUMBER);

        final SecretScanningAlert result = sut.refresh();

        assertThat(result.state()).isEqualTo(GitHubSecretScanningAlertState.RESOLVED);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAlertIsGone() throws IOException {
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, NUMBER);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void locations_shouldStreamTheLocations_whenTheyExist() throws IOException {
        final GitHubSecretScanningLocation location = mock(GitHubSecretScanningLocation.class);
        when(github
                        .secretScanning()
                        .alerts()
                        .listAlertLocations(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(location));

        final List<GitHubSecretScanningLocation> locations = sut.locations().toList();

        assertThat(locations).containsExactly(location);
    }

    @Test
    void resolve_shouldSendTheResolutionWithoutComment_whenOnlyTheResolutionIsGiven() throws IOException {
        final GitHubSecretScanningAlertWithMetadata resolved = SecurityFixtures.secretAlertWith(
                GitHubSecretScanningAlertState.RESOLVED, GitHubSecretScanningAlertResolution.REVOKED, true);
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(resolved)
                .when(api)
                .updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateSecretScanningAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateSecretScanningAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateSecretScanningAlertPayload.class);

        final SecretScanningAlert result = sut.resolve(GitHubSecretScanningAlertResolution.REVOKED);

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubSecretScanningAlertState.RESOLVED);
            softly.assertThat(payload.getValue().resolution()).isEqualTo(GitHubSecretScanningAlertResolution.REVOKED);
            softly.assertThat(payload.getValue().resolutionComment()).isNull();
            softly.assertThat(result.response()).isSameAs(resolved);
        });
    }

    @Test
    void resolve_shouldSendTheComment_whenOneIsGiven() throws IOException {
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(response)
                .when(api)
                .updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateSecretScanningAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateSecretScanningAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateSecretScanningAlertPayload.class);

        sut.resolve(GitHubSecretScanningAlertResolution.USED_IN_TESTS, "Test fixture");

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertThat(payload.getValue().resolutionComment().value()).isEqualTo("Test fixture");
    }

    @Test
    void reopen_shouldSendTheOpenStateOnly_whenCalled() throws IOException {
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(response)
                .when(api)
                .updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateSecretScanningAlertPayload.class));
        final ArgumentCaptor<GitHubUpdateSecretScanningAlertPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateSecretScanningAlertPayload.class);

        sut.reopen();

        verify(api).updateAlert(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubSecretScanningAlertState.OPEN);
            softly.assertThat(payload.getValue().resolution()).isNull();
        });
    }

    @Test
    void resolve_shouldThrowNullPointerException_whenResolutionIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.resolve(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("resolution");
    }

    @Test
    void resolve_shouldThrowNullPointerException_whenCommentIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.resolve(GitHubSecretScanningAlertResolution.REVOKED, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("comment");
    }
}
