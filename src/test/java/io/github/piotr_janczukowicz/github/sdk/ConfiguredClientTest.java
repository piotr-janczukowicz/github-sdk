package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.GitHubClient;
import io.github.piotr_janczukowicz.github.client.GitHubClientConfiguration;
import io.github.piotr_janczukowicz.github.client.GitHubClients;
import io.github.piotr_janczukowicz.http.client.authentication.AuthenticationProviders;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import io.github.piotr_janczukowicz.pagination.PaginationConfiguration;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Builds the SDK on a client with retry, conditional cache, rate limit listener and paging configured. The README
 * section "Configure the client" shows this code, so a change in the client's configuration API breaks this test before
 * it leaves the documentation wrong.
 */
@UnitTest
class ConfiguredClientTest {

    @Test
    void of_shouldWorkOnAConfiguredClient_whenRetryCacheAndPagingAreSet() {
        final AtomicBoolean exhausted = new AtomicBoolean();
        final GitHubClientConfiguration configuration = GitHubClientConfiguration.builder()
                .retry(RetryConfiguration.builder()
                        .maxRateLimitAttempts(5)
                        .maxBackoff(Duration.ofSeconds(30))
                        .totalDeadline(Duration.ofMinutes(5))
                        .build())
                .conditionalCacheMaxEntries(500)
                .rateLimitListener(snapshot -> exhausted.set(snapshot.isExhausted()))
                .pagination(PaginationConfiguration.builder()
                        .perPage(100)
                        .maxPages(5)
                        .build())
                .build();

        try (GitHubClient client =
                GitHubClients.create(AuthenticationProviders.bearer("token-for-a-test"), "my-app/1.0", configuration)) {
            final GitHubSdk sdk = GitHubSdk.of(client);
            final Repository repository = sdk.repository("octocat", "Hello-World");

            assertSoftly(softly -> {
                softly.assertThat(repository.owner()).isEqualTo("octocat");
                softly.assertThat(configuration.retry().attempts().maxRateLimitAttempts())
                        .isEqualTo(5);
                softly.assertThat(configuration.retry().totalDeadline()).isEqualTo(Duration.ofMinutes(5));
                softly.assertThat(configuration.conditionalCacheMaxEntries()).isEqualTo(500);
                softly.assertThat(client.pagination().perPage()).isEqualTo(100);
                softly.assertThat(client.pagination().maxPages()).isEqualTo(5);
            });
        }
    }

    @Test
    void defaults_shouldKeepTheClientsOwnPaging_whenNothingIsConfigured() {
        try (GitHubClient client =
                GitHubClients.create(AuthenticationProviders.bearer("token-for-a-test"), "my-app/1.0")) {
            final GitHubSdk sdk = GitHubSdk.of(client);

            assertThat(sdk.repository("octocat", "Hello-World").name()).isEqualTo("Hello-World");
            assertThat(client.pagination().perPage()).isPositive();
        }
    }
}
