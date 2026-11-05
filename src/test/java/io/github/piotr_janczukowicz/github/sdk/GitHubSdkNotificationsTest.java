package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.activity.GitHubActivityNotificationsApi;
import io.github.piotr_janczukowicz.github.api.activity.GitHubListNotificationsForAuthenticatedUserFilter;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThread;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.fixtures.NotificationFixtures;
import io.github.piotr_janczukowicz.github.sdk.notifications.Notification;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GitHubSdkNotificationsTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sut = GitHubSdk.of(github);

    @Test
    void notifications_shouldStreamTheNotifications_whenTheyExist() throws IOException {
        final GitHubThread listed = NotificationFixtures.thread();
        when(github
                        .activity()
                        .notifications()
                        .listNotificationsForAuthenticatedUser(
                                any(GitHubListNotificationsForAuthenticatedUserFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<Notification> notifications = sut.notifications().toList();

        assertThat(notifications)
                .singleElement()
                .satisfies(notification -> assertThat(notification.response()).isSameAs(listed));
    }

    @Test
    void notifications_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.notifications(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void notification_shouldReturnTheNotification_whenGitHubHasIt() throws IOException {
        final GitHubThread loaded = NotificationFixtures.thread();
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(Optional.of(loaded)).when(api).getThread(5005L);

        final Optional<Notification> found = sut.notification("5005");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
        });
    }

    @Test
    void notification_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();
        doReturn(Optional.empty()).when(api).getThread(1L);

        final Optional<Notification> found = sut.notification("1");

        assertThat(found).isEmpty();
    }

    @Test
    void notification_shouldThrowIllegalArgumentException_whenIdIsNotANumber() {
        final Throwable thrown = catchThrowable(() -> sut.notification("abc"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be a number");
    }

    @Test
    void notification_shouldThrowIllegalArgumentException_whenIdIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.notification(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("id");
    }

    @Test
    void markNotificationsRead_shouldMarkEverythingAsRead_whenCalled() throws IOException {
        final GitHubActivityNotificationsApi api = github.activity().notifications();

        sut.markNotificationsRead();

        verify(api).markNotificationsAsRead();
    }
}
