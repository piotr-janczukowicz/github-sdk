package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositorySubscription;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.User;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ActivitySmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void star_shouldBeSetCheckedListedAndRemoved_whenTheUserStarsTheSandbox() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final boolean wasStarred = readStarred(repository);
        try {
            repository.star();
            final boolean isStarred = repository.isStarred();
            final boolean isListedAsStargazer =
                    repository.stargazers().anyMatch(user -> user.login().equalsIgnoreCase(ENV.owner()));
            final User user = ENV.sdk().user(ENV.owner()).orElseThrow();
            final boolean isListedAsStarred = user.starredRepositories()
                    .anyMatch(starred -> starred.name().equals(ENV.repo()));
            repository.unstar();
            final boolean isStillStarred = repository.isStarred();

            assertSoftly(softly -> {
                softly.assertThat(isStarred).isTrue();
                softly.assertThat(isListedAsStargazer).isTrue();
                softly.assertThat(isListedAsStarred).isTrue();
                softly.assertThat(isStillStarred).isFalse();
            });
        } finally {
            if (wasStarred) {
                repository.star();
            } else {
                repository.unstar();
            }
        }
    }

    @Test
    void subscription_shouldBeWatchedIgnoredAndRemoved_whenTheUserChangesIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Optional<GitHubRepositorySubscription> before = readSubscription(repository);
        try {
            final GitHubRepositorySubscription watched = repository.watch();
            final GitHubRepositorySubscription ignored = repository.ignore();
            repository.unwatch();
            final Optional<GitHubRepositorySubscription> after = repository.subscription();

            assertSoftly(softly -> {
                softly.assertThat(watched.subscribed()).isTrue();
                softly.assertThat(watched.ignored()).isFalse();
                softly.assertThat(ignored.ignored()).isTrue();
                softly.assertThat(after).isEmpty();
            });
        } finally {
            restore(repository, before);
        }
    }

    @Test
    void watchers_shouldBeListed_whenTheRepositoryHasWatchers() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final long count = repository.watchers().limit(5).count();

        assertThat(count).isNotNegative();
    }

    @Test
    void forks_shouldBeListed_whenTheRepositoryHasForksOrNone() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<Repository> forks = repository.forks().limit(5).toList();

        assertThat(forks).allSatisfy(fork -> assertThat(fork.name()).isNotBlank());
    }

    private static boolean readStarred(final Repository repository) throws IOException {
        try {
            return repository.isStarred();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token cannot read stars; GitHub answered 403");
            throw e;
        }
    }

    private static Optional<GitHubRepositorySubscription> readSubscription(final Repository repository)
            throws IOException {
        try {
            return repository.subscription();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token cannot read the repository subscription; GitHub answered 403");
            throw e;
        }
    }

    private static void restore(final Repository repository, final Optional<GitHubRepositorySubscription> before)
            throws IOException {
        if (before.isEmpty()) {
            repository.unwatch();
        } else if (before.get().ignored()) {
            repository.ignore();
        } else {
            repository.watch();
        }
    }
}
