package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.activity.GitHubThread;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadBuilder;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadSubject;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadSubjectBuilder;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadSubscription;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadSubscriptionBuilder;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKey;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubDeployKeyBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;

public final class NotificationFixtures {

    public static final String THREAD_ID = "5005";
    public static final long KEY_ID = 6006L;

    private static final String API = "https://api.github.com/notifications/threads/5005";

    private NotificationFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubThread thread() {
        return GitHubThreadBuilder.builder()
                .id(THREAD_ID)
                .repository(repository())
                .subject(subject())
                .reason("mention")
                .unread(true)
                .updatedAt(Instant.parse("2026-11-05T10:00:00Z"))
                .url(API)
                .subscriptionUrl(API + "/subscription")
                .build();
    }

    public static GitHubThread readThread() {
        return GitHubThreadBuilder.builder(thread()).unread(false).build();
    }

    public static GitHubThreadSubscription subscription() {
        return GitHubThreadSubscriptionBuilder.builder()
                .subscribed(true)
                .ignored(false)
                .createdAt(Instant.parse("2026-11-05T10:00:00Z"))
                .url(API + "/subscription")
                .threadUrl(API)
                .build();
    }

    public static GitHubDeployKey deployKey() {
        return GitHubDeployKeyBuilder.builder()
                .id(KEY_ID)
                .key("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIExample")
                .url("https://api.github.com/repos/octocat/hello-world/keys/6006")
                .title("ci")
                .verified(true)
                .createdAt(Instant.parse("2026-11-05T10:00:00Z"))
                .readOnly(true)
                .build();
    }

    private static GitHubThreadSubject subject() {
        return GitHubThreadSubjectBuilder.builder()
                .title("Found a bug")
                .url("https://api.github.com/repos/octocat/hello-world/issues/1")
                .latestCommentUrl("https://api.github.com/repos/octocat/hello-world/issues/comments/1")
                .type("Issue")
                .build();
    }

    private static GitHubMinimalRepository repository() {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        lenient().when(owner.login()).thenReturn("octocat");
        final GitHubMinimalRepository repository = mock(GitHubMinimalRepository.class);
        lenient().when(repository.owner()).thenReturn(owner);
        lenient().when(repository.name()).thenReturn("hello-world");
        return repository;
    }
}
