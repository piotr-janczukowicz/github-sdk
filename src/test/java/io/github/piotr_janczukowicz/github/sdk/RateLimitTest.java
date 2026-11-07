package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.ratelimit.GitHubRateLimit;
import io.github.piotr_janczukowicz.github.api.ratelimit.GitHubRateLimitOverview;
import io.github.piotr_janczukowicz.github.api.ratelimit.GitHubRateLimitResources;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class RateLimitTest {

    private final GitHubRateLimitOverview response = mock(GitHubRateLimitOverview.class);
    private final RateLimit sut = new RateLimit(response);

    @Test
    void limit_shouldReturnTheCoreLimit_whenTheOverviewIsLoaded() {
        arrangeCore();

        final int limit = sut.limit();

        assertThat(limit).isEqualTo(5000);
    }

    @Test
    void remaining_shouldReturnTheCoreRemaining_whenTheOverviewIsLoaded() {
        arrangeCore();

        final int remaining = sut.remaining();

        assertThat(remaining).isEqualTo(4990);
    }

    @Test
    void resetAt_shouldReturnTheCoreResetInstant_whenTheOverviewIsLoaded() {
        arrangeCore();

        final Instant resetAt = sut.resetAt();

        assertThat(resetAt).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
    }

    @Test
    void response_shouldReturnTheOverview_whenCalled() {
        final GitHubRateLimitOverview result = sut.response();

        assertThat(result).isSameAs(response);
    }

    private void arrangeCore() {
        final GitHubRateLimit core = new GitHubRateLimit(5000, 4990, 1_700_000_000L, 10);
        final GitHubRateLimitResources resources = mock(GitHubRateLimitResources.class);
        when(resources.core()).thenReturn(core);
        when(response.resources()).thenReturn(resources);
    }
}
