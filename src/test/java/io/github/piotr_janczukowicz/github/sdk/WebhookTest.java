package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHook;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubWebhookConfig;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class WebhookTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 7007L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubHook record = RepositoryContentFixtures.webhook();
    private final Webhook sut = new Webhook(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenWebhookIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.events()).containsExactly("push");
            softly.assertThat(sut.payloadUrl()).contains("https://example.com/hook");
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void payloadUrl_shouldBeEmpty_whenTheConfigHasNone() {
        final Webhook withoutUrl = new Webhook(
                repository,
                GitHubHookBuilder.builder(record)
                        .config(new GitHubWebhookConfig(null, null, null, null))
                        .build());

        final Optional<String> payloadUrl = withoutUrl.payloadUrl();

        assertThat(payloadUrl).isEmpty();
    }

    @Test
    void refresh_shouldReturnTheReloadedWebhook_whenGitHubHasIt() throws IOException {
        final GitHubHook reloaded =
                GitHubHookBuilder.builder(record).active(false).build();
        when(github.repositories().webhooks().getWebhook(OWNER, NAME, ID)).thenReturn(Optional.of(reloaded));

        final Webhook refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.record()).isSameAs(reloaded);
            softly.assertThat(sut.isActive()).isTrue();
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheWebhookIsGone() throws IOException {
        when(github.repositories().webhooks().getWebhook(OWNER, NAME, ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7007");
    }

    @Test
    void delete_shouldDeleteTheWebhook_whenCalled() throws IOException {
        sut.delete();

        verify(github.repositories().webhooks()).deleteWebhook(OWNER, NAME, ID);
    }
}
