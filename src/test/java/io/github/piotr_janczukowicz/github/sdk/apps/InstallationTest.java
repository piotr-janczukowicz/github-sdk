package io.github.piotr_janczukowicz.github.sdk.apps;

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
import io.github.piotr_janczukowicz.github.api.apps.GitHubAppApi;
import io.github.piotr_janczukowicz.github.api.apps.GitHubCreateInstallationAccessTokenPayload;
import io.github.piotr_janczukowicz.github.api.apps.GitHubInstallationToken;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubInstallation;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AppFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class InstallationTest {

    private static final long ID = AppFixtures.INSTALLATION_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubAppApi api = github.apps().app();
    private final GitHubInstallation response = AppFixtures.installation();
    private final Installation sut = new Installation(sdk, response);

    @Test
    void accessors_shouldExposeTheResponse_whenInstallationIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.appSlug()).isEqualTo(AppFixtures.SLUG);
            softly.assertThat(sut.targetId()).isEqualTo(5L);
            softly.assertThat(sut.targetType()).isEqualTo("Organization");
            softly.assertThat(sut.isSuspended()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void isSuspended_shouldBeTrue_whenTheResponseHasASuspensionTime() {
        final Installation suspended =
                new Installation(sdk, AppFixtures.installationSuspendedAt(Instant.parse("2026-01-01T00:00:00Z")));

        final boolean isSuspended = suspended.isSuspended();

        assertThat(isSuspended).isTrue();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Installation(sdk, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void refresh_shouldReturnTheReloadedInstallation_whenItExists() throws IOException {
        final GitHubInstallation reloaded = AppFixtures.installation();
        when(api.getInstallation(ID)).thenReturn(Optional.of(reloaded));

        final Installation result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheInstallationIsGone() throws IOException {
        when(api.getInstallation(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void suspend_shouldSuspendAndReturnTheReloadedInstallation_whenCalled() throws IOException {
        final GitHubInstallation reloaded = AppFixtures.installationSuspendedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(api.getInstallation(ID)).thenReturn(Optional.of(reloaded));

        final Installation result = sut.suspend();

        verify(api).suspendInstallation(ID);
        assertThat(result.isSuspended()).isTrue();
    }

    @Test
    void unsuspend_shouldLiftTheSuspensionAndReturnTheReloadedInstallation_whenCalled() throws IOException {
        final GitHubInstallation reloaded = AppFixtures.installation();
        when(api.getInstallation(ID)).thenReturn(Optional.of(reloaded));

        final Installation result = sut.unsuspend();

        verify(api).unsuspendInstallation(ID);
        assertThat(result.isSuspended()).isFalse();
    }

    @Test
    void delete_shouldUninstallTheApp_whenCalled() throws IOException {
        sut.delete();

        verify(api).deleteInstallation(ID);
    }

    @Test
    void accessToken_shouldReturnTheToken_whenCalledWithoutLimits() throws IOException {
        final GitHubInstallationToken token = mock(GitHubInstallationToken.class);
        when(api.createInstallationAccessToken(ID)).thenReturn(token);

        final GitHubInstallationToken result = sut.accessToken();

        assertThat(result).isSameAs(token);
    }

    @Test
    void accessToken_shouldSendTheRepositoryNames_whenTheyAreGiven() throws IOException {
        final GitHubInstallationToken token = mock(GitHubInstallationToken.class);
        when(api.createInstallationAccessToken(eq(ID), any(GitHubCreateInstallationAccessTokenPayload.class)))
                .thenReturn(token);
        final ArgumentCaptor<GitHubCreateInstallationAccessTokenPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateInstallationAccessTokenPayload.class);

        final GitHubInstallationToken result = sut.accessToken(List.of("hello-world"));

        verify(api).createInstallationAccessToken(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(token);
            softly.assertThat(payload.getValue().repositories()).containsExactly("hello-world");
            softly.assertThat(payload.getValue().permissions()).isNull();
        });
    }

    @Test
    void accessToken_shouldThrowNullPointerException_whenRepositoryNamesAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.accessToken(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repositoryNames");
    }
}
