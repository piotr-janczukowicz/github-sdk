package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.webhooks.Webhook;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class WebhooksSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void webhook_shouldRoundTrip_whenCreatedListedUpdatedRefreshedAndDeleted() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String url = "https://example.com/" + ENV.name("hook");
        final Webhook created = repository.createWebhook(url, List.of("push"));
        boolean isDeleted = false;
        try {
            final boolean isListed = repository.webhooks().anyMatch(listed -> listed.id() == created.id());
            final Webhook updated =
                    created.update().addEvents(List.of("issues")).active(false).send();
            final Webhook refreshed = updated.refresh();
            refreshed.delete();
            isDeleted = true;

            assertThat(created.payloadUrl()).contains(url);
            assertThat(created.events()).containsExactly("push");
            assertThat(isListed).isTrue();
            assertThat(updated.isActive()).isFalse();
            assertThat(refreshed.events()).contains("push", "issues");
            assertThat(repository.webhook(created.id())).isEmpty();
        } finally {
            if (!isDeleted) {
                created.delete();
            }
        }
    }

    @Test
    void webhook_shouldBeEmpty_whenTheIdDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final boolean isPresent = repository.webhook(1L).isPresent();

        assertThat(isPresent).isFalse();
    }
}
