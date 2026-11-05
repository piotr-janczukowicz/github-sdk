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
import io.github.piotr_janczukowicz.github.api.activity.GitHubSetRepoSubscriptionPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateForkPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubListForksFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositorySubscription;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryActivityTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void star_shouldStarTheRepository_whenCalled() throws IOException {
        sut.star();

        verify(github.activity().starring()).starRepoForAuthenticatedUser(OWNER, NAME);
    }

    @Test
    void unstar_shouldRemoveTheStar_whenCalled() throws IOException {
        sut.unstar();

        verify(github.activity().starring()).unstarRepoForAuthenticatedUser(OWNER, NAME);
    }

    @Test
    void isStarred_shouldReturnWhatGitHubSays_whenCalled() throws IOException {
        when(github.activity().starring().checkRepoIsStarredByAuthenticatedUser(OWNER, NAME))
                .thenReturn(true);

        final boolean isStarred = sut.isStarred();

        assertThat(isStarred).isTrue();
    }

    @Test
    void stargazers_shouldStreamTheUsers_whenTheyExist() throws IOException {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        when(github.activity().starring().listStargazersForRepo(OWNER, NAME).stream())
                .thenReturn(Stream.of(user));

        final List<GitHubSimpleUser> stargazers = sut.stargazers().toList();

        assertThat(stargazers).containsExactly(user);
    }

    @Test
    void watchers_shouldStreamTheUsers_whenTheyExist() throws IOException {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        when(github.activity().watching().listWatchersForRepo(OWNER, NAME).stream())
                .thenReturn(Stream.of(user));

        final List<GitHubSimpleUser> watchers = sut.watchers().toList();

        assertThat(watchers).containsExactly(user);
    }

    @Test
    void subscription_shouldReturnTheSubscription_whenTheUserHasOne() throws IOException {
        final GitHubRepositorySubscription subscription = mock(GitHubRepositorySubscription.class);
        when(github.activity().watching().getRepoSubscription(OWNER, NAME)).thenReturn(Optional.of(subscription));

        final Optional<GitHubRepositorySubscription> found = sut.subscription();

        assertThat(found).containsSame(subscription);
    }

    @Test
    void subscription_shouldBeEmpty_whenTheUserHasNone() throws IOException {
        when(github.activity().watching().getRepoSubscription(OWNER, NAME)).thenReturn(Optional.empty());

        final Optional<GitHubRepositorySubscription> found = sut.subscription();

        assertThat(found).isEmpty();
    }

    @Test
    void watch_shouldSubscribeAndNotIgnore_whenCalled() throws IOException {
        final GitHubRepositorySubscription subscription = mock(GitHubRepositorySubscription.class);
        when(github.activity().watching().setRepoSubscription(eq(OWNER), eq(NAME), any()))
                .thenReturn(subscription);
        final ArgumentCaptor<GitHubSetRepoSubscriptionPayload> payload =
                ArgumentCaptor.forClass(GitHubSetRepoSubscriptionPayload.class);

        final GitHubRepositorySubscription result = sut.watch();

        verify(github.activity().watching()).setRepoSubscription(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().subscribed()).isTrue();
            softly.assertThat(payload.getValue().ignored()).isFalse();
            softly.assertThat(result).isSameAs(subscription);
        });
    }

    @Test
    void ignore_shouldIgnoreAndNotSubscribe_whenCalled() throws IOException {
        final GitHubRepositorySubscription subscription = mock(GitHubRepositorySubscription.class);
        when(github.activity().watching().setRepoSubscription(eq(OWNER), eq(NAME), any()))
                .thenReturn(subscription);
        final ArgumentCaptor<GitHubSetRepoSubscriptionPayload> payload =
                ArgumentCaptor.forClass(GitHubSetRepoSubscriptionPayload.class);

        final GitHubRepositorySubscription result = sut.ignore();

        verify(github.activity().watching()).setRepoSubscription(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().subscribed()).isFalse();
            softly.assertThat(payload.getValue().ignored()).isTrue();
            softly.assertThat(result).isSameAs(subscription);
        });
    }

    @Test
    void unwatch_shouldDeleteTheSubscription_whenCalled() throws IOException {
        sut.unwatch();

        verify(github.activity().watching()).deleteRepoSubscription(OWNER, NAME);
    }

    @Test
    void forks_shouldReturnHandlesOfTheListedForks_whenTheyExist() throws IOException {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("hubot");
        final GitHubMinimalRepository listed = mock(GitHubMinimalRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("hello-world");
        when(github.repositories().forks().listForks(eq(OWNER), eq(NAME), any(), any()).stream())
                .thenReturn(Stream.of(listed));

        final List<Repository> forks = sut.forks().toList();

        assertThat(forks).singleElement().satisfies(fork -> {
            assertThat(fork.owner()).isEqualTo("hubot");
            assertThat(fork.name()).isEqualTo("hello-world");
        });
    }

    @Test
    void forks_shouldPassTheFilterOn_whenOneIsGiven() throws IOException {
        final GitHubListForksFilter filter = mock(GitHubListForksFilter.class);
        when(github.repositories().forks().listForks(eq(OWNER), eq(NAME), eq(filter), any()).stream())
                .thenReturn(Stream.empty());

        final List<Repository> forks = sut.forks(filter).toList();

        assertThat(forks).isEmpty();
    }

    @Test
    void forks_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.forks(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void fork_shouldReturnTheLoadedFork_whenCalled() throws IOException {
        final GitHubFullRepository created = forked();
        when(github.repositories().forks().createFork(OWNER, NAME, null)).thenReturn(created);

        final Repository fork = sut.fork();

        assertSoftly(softly -> {
            softly.assertThat(fork.owner()).isEqualTo("hubot");
            softly.assertThat(fork.name()).isEqualTo("hello-world");
            softly.assertThat(fork.response()).isSameAs(created);
        });
    }

    @Test
    void fork_shouldSendThePayload_whenOneIsGiven() throws IOException {
        final GitHubCreateForkPayload payload = new GitHubCreateForkPayload("my-org", "renamed", true);
        final GitHubFullRepository created = forked();
        when(github.repositories().forks().createFork(OWNER, NAME, payload)).thenReturn(created);

        final Repository fork = sut.fork(payload);

        assertThat(fork.owner()).isEqualTo("hubot");
    }

    @Test
    void fork_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.fork(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    private static GitHubFullRepository forked() {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("hubot");
        final GitHubFullRepository created = mock(GitHubFullRepository.class);
        when(created.owner()).thenReturn(owner);
        when(created.name()).thenReturn("hello-world");
        return created;
    }
}
