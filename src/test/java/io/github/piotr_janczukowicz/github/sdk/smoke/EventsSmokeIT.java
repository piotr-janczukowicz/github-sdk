package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class EventsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final int LIMIT = 5;

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void events_shouldBeListed_whenTheFeedsAreRead() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final List<GitHubEvent> repositoryEvents =
                repository.events().limit(LIMIT).toList();
        final List<GitHubEvent> networkEvents =
                repository.networkEvents().limit(LIMIT).toList();
        final List<GitHubEvent> publicEvents =
                ENV.sdk().publicEvents().limit(LIMIT).toList();
        final List<GitHubEvent> userEvents =
                ENV.sdk().user(ENV.owner()).orElseThrow().events().limit(LIMIT).toList();

        assertSoftly(softly -> {
            softly.assertThat(repositoryEvents).hasSizeLessThanOrEqualTo(LIMIT);
            softly.assertThat(networkEvents).hasSizeLessThanOrEqualTo(LIMIT);
            softly.assertThat(publicEvents).isNotEmpty();
            softly.assertThat(userEvents).hasSizeLessThanOrEqualTo(LIMIT);
        });
        assertThat(publicEvents).allSatisfy(event -> assertThat(event.id()).isNotBlank());
    }
}
