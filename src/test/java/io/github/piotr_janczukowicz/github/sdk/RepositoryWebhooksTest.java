package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateWebhookPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHook;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.github.sdk.webhooks.Webhook;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryWebhooksTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubHook hook = RepositoryContentFixtures.webhook();

    @Test
    void webhook_shouldReturnTheWebhook_whenGitHubHasIt() throws IOException {
        when(github.repositories().webhooks().getWebhook(OWNER, NAME, 7007L)).thenReturn(Optional.of(hook));

        final Optional<Webhook> found = sut.webhook(7007L);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(hook);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void webhook_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.repositories().webhooks().getWebhook(OWNER, NAME, 1L)).thenReturn(Optional.empty());

        final Optional<Webhook> found = sut.webhook(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void webhooks_shouldWrapEachWebhook_whenTheRepositoryHasWebhooks() throws IOException {
        when(github.repositories().webhooks().listWebhooks(OWNER, NAME).stream())
                .thenReturn(Stream.of(hook));

        final List<Webhook> webhooks = sut.webhooks().toList();

        assertThat(webhooks).extracting(Webhook::response).containsExactly(hook);
    }

    @Test
    void createWebhook_shouldSendTheUrlAndEvents_whenCalledWithBoth() throws IOException {
        when(github.repositories().webhooks().createWebhook(eq(OWNER), eq(NAME), any(GitHubCreateWebhookPayload.class)))
                .thenReturn(hook);
        final ArgumentCaptor<GitHubCreateWebhookPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateWebhookPayload.class);

        final Webhook created = sut.createWebhook("https://example.com/hook", List.of("push"));

        verify(github.repositories().webhooks()).createWebhook(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("web");
            softly.assertThat(payload.getValue().config().url()).isEqualTo("https://example.com/hook");
            softly.assertThat(payload.getValue().config().contentType()).isEqualTo("json");
            softly.assertThat(payload.getValue().events()).containsExactly("push");
            softly.assertThat(created.response()).isSameAs(hook);
        });
    }

    @Test
    void createWebhook_shouldSendThePayload_whenCalledWithAPayload() throws IOException {
        final GitHubCreateWebhookPayload payload = new GitHubCreateWebhookPayload("web", null, List.of("push"), true);
        when(github.repositories().webhooks().createWebhook(OWNER, NAME, payload))
                .thenReturn(hook);

        final Webhook created = sut.createWebhook(payload);

        assertThat(created.response()).isSameAs(hook);
    }

    @Test
    void createWebhook_shouldThrowIllegalArgumentException_whenUrlIsBlank() {
        final List<String> events = List.of("push");

        final Throwable thrown = catchThrowable(() -> sut.createWebhook(" ", events));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("payloadUrl");
    }

    @Test
    void createWebhook_shouldThrowIllegalArgumentException_whenAnEventIsBlank() {
        final List<String> events = List.of("push", " ");

        final Throwable thrown = catchThrowable(() -> sut.createWebhook("https://example.com/hook", events));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("events");
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenAnEventIsNull() {
        final List<String> events = Collections.singletonList(null);

        final Throwable thrown = catchThrowable(() -> sut.createWebhook("https://example.com/hook", events));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("events");
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenEventsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.createWebhook("https://example.com/hook", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("events");
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createWebhook(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }
}
