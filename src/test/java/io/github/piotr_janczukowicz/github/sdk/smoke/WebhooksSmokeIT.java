package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.webhooks.Webhook;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
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
        final Webhook created = createOrAbort(repository, url);
        boolean isDeleted = false;
        try {
            final boolean isListed = repository.webhooks().anyMatch(listed -> listed.id() == created.id());
            final Webhook updated =
                    created.update().addEvents(List.of("issues")).active(false).send();
            final Webhook refreshed = updated.refresh();
            refreshed.delete();
            isDeleted = true;
            final boolean isGone = repository.webhook(created.id()).isEmpty();

            assertSoftly(softly -> {
                softly.assertThat(created.payloadUrl()).contains(url);
                softly.assertThat(created.events()).containsExactly("push");
                softly.assertThat(isListed).isTrue();
                softly.assertThat(updated.isActive()).isFalse();
                softly.assertThat(refreshed.events()).contains("push", "issues");
                softly.assertThat(isGone).isTrue();
            });
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

    private static Webhook createOrAbort(final Repository repository, final String url) throws IOException {
        try {
            return repository.createWebhook(url, List.of("push"));
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token may not manage webhooks of the sandbox repository");
            throw e;
        }
    }
}
