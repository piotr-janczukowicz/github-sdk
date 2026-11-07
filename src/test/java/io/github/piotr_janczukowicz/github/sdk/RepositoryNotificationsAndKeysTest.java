package io.github.piotr_janczukowicz.github.sdk;

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
import io.github.piotr_janczukowicz.github.api.activity.GitHubActivityNotificationsApi;
import io.github.piotr_janczukowicz.github.api.activity.GitHubListRepoNotificationsForAuthenticatedUserFilter;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThread;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubCreateDeployKeyPayload;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKey;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKeysApi;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.deploykeys.DeployKey;
import io.github.piotr_janczukowicz.github.sdk.fixtures.NotificationFixtures;
import io.github.piotr_janczukowicz.github.sdk.notifications.Notification;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryNotificationsAndKeysTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void notifications_shouldStreamTheNotifications_whenTheyExist() throws IOException {
        final GitHubThread listed = NotificationFixtures.thread();
        when(github
                        .activity()
                        .notifications()
                        .listRepoNotificationsForAuthenticatedUser(
                                eq(OWNER),
                                eq(NAME),
                                any(GitHubListRepoNotificationsForAuthenticatedUserFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<Notification> notifications = sut.notifications().toList();

        assertThat(notifications)
                .singleElement()
                .satisfies(notification -> assertSoftly(softly -> {
                    softly.assertThat(notification.response()).isSameAs(listed);
                    softly.assertThat(notification.id()).isEqualTo(NotificationFixtures.THREAD_ID);
                }));
    }

    @Test
    void notifications_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.notifications(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void markNotificationsRead_shouldMarkTheRepositoryNotificationsAsRead() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();

        sut.markNotificationsRead();

        verify(api).markRepoNotificationsAsRead(OWNER, NAME);
    }

    @Test
    void deployKeys_shouldStreamTheKeys_whenTheyExist() throws IOException {
        final GitHubDeployKey listed = NotificationFixtures.deployKey();
        when(github.deployKeys().listDeployKeys(eq(OWNER), eq(NAME), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));

        final List<DeployKey> keys = sut.deployKeys().toList();

        assertThat(keys)
                .singleElement()
                .satisfies(key -> assertSoftly(softly -> {
                    softly.assertThat(key.response()).isSameAs(listed);
                    softly.assertThat(key.repository()).isSameAs(sut);
                }));
    }

    @Test
    void deployKey_shouldReturnTheKey_whenGitHubHasIt() throws IOException {
        final GitHubDeployKeysApi api = github.deployKeys();
        final GitHubDeployKey loaded = NotificationFixtures.deployKey();
        doReturn(Optional.of(loaded)).when(api).getDeployKey(OWNER, NAME, NotificationFixtures.KEY_ID);

        final Optional<DeployKey> found = sut.deployKey(NotificationFixtures.KEY_ID);

        assertThat(found)
                .hasValueSatisfying(key -> assertSoftly(softly -> {
                    softly.assertThat(key.response()).isSameAs(loaded);
                    softly.assertThat(key.repository()).isSameAs(sut);
                }));
    }

    @Test
    void deployKey_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubDeployKeysApi api = github.deployKeys();
        doReturn(Optional.empty()).when(api).getDeployKey(OWNER, NAME, 1L);

        final Optional<DeployKey> found = sut.deployKey(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void addDeployKey_shouldAddAReadOnlyKey_whenNoFlagIsGiven() throws IOException {
        final GitHubDeployKey created = NotificationFixtures.deployKey();
        final GitHubDeployKeysApi api = github.deployKeys();
        doReturn(created).when(api).createDeployKey(eq(OWNER), eq(NAME), any(GitHubCreateDeployKeyPayload.class));
        final ArgumentCaptor<GitHubCreateDeployKeyPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDeployKeyPayload.class);

        final DeployKey key = sut.addDeployKey("ci", "ssh-ed25519 AAAA");

        verify(api).createDeployKey(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("ci");
            softly.assertThat(payload.getValue().key()).isEqualTo("ssh-ed25519 AAAA");
            softly.assertThat(payload.getValue().readOnly()).isTrue();
            softly.assertThat(key.response()).isSameAs(created);
        });
    }

    @Test
    void addDeployKey_shouldAllowWriting_whenReadOnlyIsFalse() throws IOException {
        final GitHubDeployKeysApi api = github.deployKeys();
        doReturn(NotificationFixtures.deployKey())
                .when(api)
                .createDeployKey(eq(OWNER), eq(NAME), any(GitHubCreateDeployKeyPayload.class));
        final ArgumentCaptor<GitHubCreateDeployKeyPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDeployKeyPayload.class);

        sut.addDeployKey("ci", "ssh-ed25519 AAAA", false);

        verify(api).createDeployKey(eq(OWNER), eq(NAME), payload.capture());
        assertThat(payload.getValue().readOnly()).isFalse();
    }

    @Test
    void addDeployKey_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addDeployKey(" ", "ssh-ed25519 AAAA"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void addDeployKey_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addDeployKey("ci", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
    }
}
