package io.github.piotr_janczukowicz.github.sdk.notifications;

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
import io.github.piotr_janczukowicz.github.api.activity.GitHubActivityNotificationsApi;
import io.github.piotr_janczukowicz.github.api.activity.GitHubSetThreadSubscriptionPayload;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThread;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadSubscription;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.NotificationFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class NotificationTest {

    private static final long ID = 5005L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubThread response = NotificationFixtures.thread();
    private final Notification sut = new Notification(sdk, response);

    @Test
    void accessors_shouldExposeTheResponse_whenNotificationIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(NotificationFixtures.THREAD_ID);
            softly.assertThat(sut.title()).isEqualTo("Found a bug");
            softly.assertThat(sut.type()).isEqualTo("Issue");
            softly.assertThat(sut.reason()).isEqualTo("mention");
            softly.assertThat(sut.isUnread()).isTrue();
            softly.assertThat(sut.repository().owner()).isEqualTo("octocat");
            softly.assertThat(sut.repository().name()).isEqualTo("hello-world");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedNotification_whenItExists() throws IOException {
        final GitHubThread reloaded = NotificationFixtures.readThread();
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(Optional.of(reloaded)).when(api).getThread(ID);

        final Notification result = sut.refresh();

        assertThat(result.isUnread()).isFalse();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheNotificationIsGone() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(Optional.empty()).when(api).getThread(ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void markRead_shouldMarkTheThreadAsRead_whenCalled() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();

        sut.markRead();

        verify(api).markThreadAsRead(ID);
    }

    @Test
    void markDone_shouldMarkTheThreadAsDone_whenCalled() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();

        sut.markDone();

        verify(api).markThreadAsDone(ID);
    }

    @Test
    void subscription_shouldReturnTheSubscription_whenTheUserHasOne() throws IOException {
        final GitHubThreadSubscription held = NotificationFixtures.subscription();
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(Optional.of(held)).when(api).getThreadSubscriptionForAuthenticatedUser(ID);

        final Optional<GitHubThreadSubscription> subscription = sut.subscription();

        assertThat(subscription).containsSame(held);
    }

    @Test
    void subscription_shouldBeEmpty_whenTheUserHasNone() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(Optional.empty()).when(api).getThreadSubscriptionForAuthenticatedUser(ID);

        final Optional<GitHubThreadSubscription> subscription = sut.subscription();

        assertThat(subscription).isEmpty();
    }

    @Test
    void subscribe_shouldSetAnUnignoredSubscription_whenCalled() throws IOException {
        final GitHubThreadSubscription held = NotificationFixtures.subscription();
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(held).when(api).setThreadSubscription(eq(ID), any(GitHubSetThreadSubscriptionPayload.class));
        final ArgumentCaptor<GitHubSetThreadSubscriptionPayload> payload =
                ArgumentCaptor.forClass(GitHubSetThreadSubscriptionPayload.class);

        final GitHubThreadSubscription result = sut.subscribe();

        verify(api).setThreadSubscription(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().ignored()).isFalse();
            softly.assertThat(result).isSameAs(held);
        });
    }

    @Test
    void ignore_shouldSetAnIgnoredSubscription_whenCalled() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(NotificationFixtures.subscription())
                .when(api)
                .setThreadSubscription(eq(ID), any(GitHubSetThreadSubscriptionPayload.class));
        final ArgumentCaptor<GitHubSetThreadSubscriptionPayload> payload =
                ArgumentCaptor.forClass(GitHubSetThreadSubscriptionPayload.class);

        sut.ignore();

        verify(api).setThreadSubscription(eq(ID), payload.capture());
        assertThat(payload.getValue().ignored()).isTrue();
    }

    @Test
    void unsubscribe_shouldDeleteTheSubscription_whenCalled() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();

        sut.unsubscribe();

        verify(api).deleteThreadSubscription(ID);
    }
}
