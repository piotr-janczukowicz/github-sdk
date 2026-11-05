package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.search.GitHubCommitSearchResultItem;
import io.github.piotr_janczukowicz.github.sdk.Issue;
import io.github.piotr_janczukowicz.github.sdk.RateLimit;
import io.github.piotr_janczukowicz.github.sdk.User;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class SearchSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void issues_shouldBeLoadedFromTheSandbox_whenTheQueryNamesTheRepository() throws IOException {
        final List<Issue> issues = ENV.sdk()
                .search()
                .issues("repo:" + ENV.owner() + "/" + ENV.repo() + " is:issue")
                .limit(3)
                .toList();

        assertThat(issues).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(issues)
                .allSatisfy(issue -> assertThat(issue.repository().name()).isEqualToIgnoringCase(ENV.repo()));
    }

    @Test
    void repositories_shouldFindTheSandbox_whenTheQueryNamesOwnerAndRepository() throws IOException {
        final boolean isFound = ENV.sdk()
                .search()
                .repositories(ENV.repo() + " in:name user:" + ENV.owner())
                .limit(10)
                .anyMatch(repository -> repository.name().equalsIgnoreCase(ENV.repo()));

        assertThat(isFound).isTrue();
    }

    @Test
    void users_shouldLoadOrganizationHits_whenTheQueryAsksForOrganizations() throws IOException {
        final List<User> users =
                ENV.sdk().search().users("github in:login type:org").limit(3).toList();

        assertThat(users).isNotEmpty();
        assertThat(users).allSatisfy(user -> assertThat(user.login()).isNotBlank());
    }

    @Test
    void users_shouldFindTheSandboxOwner_whenTheQueryNamesTheLogin() throws IOException {
        final boolean isFound = ENV.sdk()
                .search()
                .users(ENV.owner() + " in:login")
                .limit(10)
                .anyMatch(user -> user.login().equalsIgnoreCase(ENV.owner()));

        assertThat(isFound).isTrue();
    }

    @Test
    void commits_shouldRespectTheLimit_whenTheQueryNamesTheRepository() throws IOException {
        final List<GitHubCommitSearchResultItem> commits = ENV.sdk()
                .search()
                .commits("repo:" + ENV.owner() + "/" + ENV.repo() + " author-date:>2000-01-01")
                .limit(3)
                .toList();

        assertThat(commits).isNotEmpty().hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void rateLimit_shouldReportTheCoreResource_whenGitHubReportsIt() throws IOException {
        final Optional<RateLimit> rateLimit = ENV.sdk().rateLimit();

        assertThat(rateLimit).isPresent();
        assertThat(rateLimit.orElseThrow().limit()).isPositive();
        assertThat(rateLimit.orElseThrow().remaining())
                .isBetween(0, rateLimit.orElseThrow().limit());
        assertThat(rateLimit.orElseThrow().resetAt()).isAfter(Instant.now().minusSeconds(3600));
    }
}
