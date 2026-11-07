package io.github.piotr_janczukowicz.github.sdk.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.emojis.GitHubEmojisApi;
import io.github.piotr_janczukowicz.github.api.gitignore.GitHubGitignoreApi;
import io.github.piotr_janczukowicz.github.api.gitignore.GitHubGitignoreTemplate;
import io.github.piotr_janczukowicz.github.api.markdown.GitHubMarkdownApi;
import io.github.piotr_janczukowicz.github.api.markdown.GitHubMarkdownMode;
import io.github.piotr_janczukowicz.github.api.markdown.GitHubRenderMarkdownPayload;
import io.github.piotr_janczukowicz.github.api.meta.GitHubApiOverview;
import io.github.piotr_janczukowicz.github.api.meta.GitHubMetaApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class UtilitiesTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final Utilities sut = sdk.utilities();

    @Test
    void zen_shouldReturnTheQuote_whenCalled() throws IOException {
        final GitHubMetaApi api = github.meta();
        doReturn("Keep it logically awesome.").when(api).zen();

        final String zen = sut.zen();

        assertThat(zen).isEqualTo("Keep it logically awesome.");
    }

    @Test
    void overview_shouldReturnTheOverview_whenCalled() throws IOException {
        final GitHubApiOverview overview = mock(GitHubApiOverview.class);
        final GitHubMetaApi api = github.meta();
        doReturn(overview).when(api).get();

        final GitHubApiOverview result = sut.overview();

        assertThat(result).isSameAs(overview);
    }

    @Test
    void emojis_shouldReturnTheEmojis_whenCalled() throws IOException {
        final GitHubEmojisApi api = github.emojis();
        doReturn(Map.of("+1", "https://example.com/1.png")).when(api).get();

        final Map<String, String> emojis = sut.emojis();

        assertThat(emojis).containsKey("+1");
    }

    @Test
    void renderMarkdown_shouldUsePlainMarkdownMode_whenNoRepositoryIsGiven() throws IOException {
        final GitHubMarkdownApi api = github.markdown();
        doReturn("<p>hi</p>").when(api).render(any(GitHubRenderMarkdownPayload.class));
        final ArgumentCaptor<GitHubRenderMarkdownPayload> payload =
                ArgumentCaptor.forClass(GitHubRenderMarkdownPayload.class);

        final String html = sut.renderMarkdown("hi");

        verify(api).render(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().mode()).isEqualTo(GitHubMarkdownMode.MARKDOWN);
            softly.assertThat(payload.getValue().context()).isNull();
            softly.assertThat(html).isEqualTo("<p>hi</p>");
        });
    }

    @Test
    void renderMarkdown_shouldResolveReferencesAgainstTheRepository_whenOneIsGiven() throws IOException {
        final Repository repository = sdk.repository("octocat", "hello-world");
        final GitHubMarkdownApi api = github.markdown();
        doReturn("<p>#1</p>").when(api).render(any(GitHubRenderMarkdownPayload.class));
        final ArgumentCaptor<GitHubRenderMarkdownPayload> payload =
                ArgumentCaptor.forClass(GitHubRenderMarkdownPayload.class);

        sut.renderMarkdown("#1", repository);

        verify(api).render(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().mode()).isEqualTo(GitHubMarkdownMode.GFM);
            softly.assertThat(payload.getValue().context()).isEqualTo("octocat/hello-world");
        });
    }

    @Test
    void renderMarkdown_shouldThrowNullPointerException_whenTextIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.renderMarkdown(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
    }

    @Test
    void renderMarkdown_shouldThrowNullPointerException_whenContextIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.renderMarkdown("#1", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("context");
    }

    @Test
    void renderMarkdown_shouldThrowNullPointerException_whenTextIsNullAndContextIsGiven() {
        final Repository repository = sdk.repository("octocat", "hello-world");

        final Throwable thrown = catchThrowable(() -> sut.renderMarkdown(null, repository));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
    }

    @Test
    void gitignoreTemplates_shouldReturnTheNames_whenCalled() throws IOException {
        final GitHubGitignoreApi api = github.gitignore();
        doReturn(List.of("Java", "Maven")).when(api).templates();

        final List<String> templates = sut.gitignoreTemplates();

        assertThat(templates).containsExactly("Java", "Maven");
    }

    @Test
    void gitignoreTemplate_shouldReturnTheTemplate_whenGitHubHasIt() throws IOException {
        final GitHubGitignoreTemplate template = new GitHubGitignoreTemplate("Java", "*.class");
        final GitHubGitignoreApi api = github.gitignore();
        doReturn(Optional.of(template)).when(api).get("Java");

        final Optional<GitHubGitignoreTemplate> found = sut.gitignoreTemplate("Java");

        assertThat(found).containsSame(template);
    }

    @Test
    void gitignoreTemplate_shouldReturnEmpty_whenGitHubHasNoTemplate() throws IOException {
        final GitHubGitignoreApi api = github.gitignore();
        doReturn(Optional.empty()).when(api).get("Missing");

        final Optional<GitHubGitignoreTemplate> found = sut.gitignoreTemplate("Missing");

        assertThat(found).isEmpty();
    }

    @Test
    void gitignoreTemplate_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.gitignoreTemplate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void gitignoreTemplate_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.gitignoreTemplate(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> new Utilities(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }
}
