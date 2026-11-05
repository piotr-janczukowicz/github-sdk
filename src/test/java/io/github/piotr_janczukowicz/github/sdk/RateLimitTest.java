package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
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

    @Test
    void accessors_shouldExposeTheCoreResource_whenTheOverviewIsLoaded() {
        final GitHubRateLimit core = new GitHubRateLimit(5000, 4990, 1_700_000_000L, 10);
        final GitHubRateLimitResources resources = mock(GitHubRateLimitResources.class);
        when(resources.core()).thenReturn(core);
        final GitHubRateLimitOverview response = mock(GitHubRateLimitOverview.class);
        when(response.resources()).thenReturn(resources);
        final RateLimit sut = new RateLimit(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.limit()).isEqualTo(5000);
            softly.assertThat(sut.remaining()).isEqualTo(4990);
            softly.assertThat(sut.resetAt()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }
}
