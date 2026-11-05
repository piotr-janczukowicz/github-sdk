package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.User;
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
    void events_shouldBeListed_whenTheRepositoryFeedIsRead() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<GitHubEvent> events = repository.events().limit(LIMIT).toList();

        assertThat(events).allSatisfy(event -> assertThat(event.id()).isNotBlank());
    }

    @Test
    void networkEvents_shouldBeListed_whenTheNetworkFeedIsRead() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final List<GitHubEvent> events = repository.networkEvents().limit(LIMIT).toList();

        assertThat(events).allSatisfy(event -> assertThat(event.id()).isNotBlank());
    }

    @Test
    void publicEvents_shouldBeListed_whenThePublicFeedIsRead() throws IOException {
        final List<GitHubEvent> events = ENV.sdk().publicEvents().limit(LIMIT).toList();

        assertThat(events)
                .isNotEmpty()
                .allSatisfy(event -> assertThat(event.id()).isNotBlank());
    }

    @Test
    void events_shouldBeListed_whenTheUserFeedIsRead() throws IOException {
        final User owner = ENV.sdk().user(ENV.owner()).orElseThrow();

        final List<GitHubEvent> events = owner.events().limit(LIMIT).toList();

        assertThat(events).allSatisfy(event -> assertThat(event.id()).isNotBlank());
    }
}
