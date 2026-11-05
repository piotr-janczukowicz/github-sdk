package io.github.piotr_janczukowicz.github.sdk.webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHook;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdateWebhookPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class WebhookUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 7007L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubHook response = RepositoryContentFixtures.webhook();
    private final Webhook webhook = new Webhook(GitHubSdk.of(github).repository(OWNER, NAME), response);

    @Test
    void send_shouldSendOnlyTheSetFieldsAndReturnTheResponse_whenEventsAndActiveAreSet() throws IOException {
        final GitHubHook updated =
                GitHubHookBuilder.builder(response).active(false).build();
        when(github.repositories()
                        .webhooks()
                        .updateWebhook(eq(OWNER), eq(NAME), eq(ID), any(GitHubUpdateWebhookPayload.class)))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateWebhookPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateWebhookPayload.class);

        final Webhook result =
                webhook.update().events(List.of("push", "issues")).active(false).send();

        verify(github.repositories().webhooks()).updateWebhook(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().events()).containsExactly("push", "issues");
            softly.assertThat(payload.getValue().active()).isFalse();
            softly.assertThat(payload.getValue().addEvents()).isNullOrEmpty();
            softly.assertThat(payload.getValue().config()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(webhook.isActive()).isTrue();
        });
    }

    @Test
    void send_shouldSendTheAddedAndRemovedEvents_whenTheyAreSet() throws IOException {
        when(github.repositories()
                        .webhooks()
                        .updateWebhook(eq(OWNER), eq(NAME), eq(ID), any(GitHubUpdateWebhookPayload.class)))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdateWebhookPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateWebhookPayload.class);

        webhook.update()
                .addEvents(List.of("issues"))
                .removeEvents(List.of("push"))
                .send();

        verify(github.repositories().webhooks()).updateWebhook(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().addEvents()).containsExactly("issues");
            softly.assertThat(payload.getValue().removeEvents()).containsExactly("push");
        });
    }

    @Test
    void send_shouldThrowIllegalStateExceptionWithoutARequest_whenNothingIsSet() {
        final WebhookUpdate update = webhook.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing");
        verifyNoInteractions(github.repositories().webhooks());
    }

    @Test
    void events_shouldThrowNullPointerException_whenEventsAreNull() {
        final WebhookUpdate update = webhook.update();

        final Throwable thrown = catchThrowable(() -> update.events(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("events");
    }

    @Test
    void addEvents_shouldThrowNullPointerException_whenEventsAreNull() {
        final WebhookUpdate update = webhook.update();

        final Throwable thrown = catchThrowable(() -> update.addEvents(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("events");
    }

    @Test
    void removeEvents_shouldThrowNullPointerException_whenEventsAreNull() {
        final WebhookUpdate update = webhook.update();

        final Throwable thrown = catchThrowable(() -> update.removeEvents(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("events");
    }

    @Test
    void events_shouldThrowIllegalArgumentException_whenAnEventNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> webhook.update().events(List.of("push", " ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("events");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenWebhookIsNull() {
        final Throwable thrown = catchThrowable(() -> new WebhookUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("webhook");
    }
}
