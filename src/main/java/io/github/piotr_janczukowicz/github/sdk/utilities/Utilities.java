package io.github.piotr_janczukowicz.github.sdk.utilities;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitignore.GitHubGitignoreTemplate;
import io.github.piotr_janczukowicz.github.api.markdown.GitHubMarkdownMode;
import io.github.piotr_janczukowicz.github.api.markdown.GitHubRenderMarkdownPayload;
import io.github.piotr_janczukowicz.github.api.meta.GitHubApiOverview;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The small endpoints of GitHub that need no repository: the zen quote, the API overview, emojis, Markdown rendering
 * and the {@code .gitignore} templates. The object holds no state; every method asks GitHub, and the results are the
 * client's records or plain text.
 */
public final class Utilities {

    private final GitHubSdk sdk;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @throws NullPointerException if an argument is {@code null}
     */
    public Utilities(final GitHubSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Returns a random line of GitHub's design philosophy ({@code GET /zen}).
     *
     * @return the quote
     * @throws IOException if the transport fails
     */
    public String zen() throws IOException {
        return github().meta().zen();
    }

    /**
     * Returns the overview of the API, such as the IP ranges GitHub uses ({@code GET /meta}), as GitHub returns it.
     *
     * @return the overview
     * @throws IOException if the transport fails
     */
    public GitHubApiOverview overview() throws IOException {
        return github().meta().get();
    }

    /**
     * Returns every emoji GitHub supports ({@code GET /emojis}).
     *
     * @return the image URL by emoji name
     * @throws IOException if the transport fails
     */
    public Map<String, String> emojis() throws IOException {
        return github().emojis().get();
    }

    /**
     * Renders Markdown to HTML in plain Markdown mode ({@code POST /markdown}).
     *
     * @param text the Markdown source
     * @return the HTML
     * @throws NullPointerException if {@code text} is {@code null}
     * @throws IOException if the transport fails
     */
    public String renderMarkdown(final String text) throws IOException {
        return github().markdown()
                .render(new GitHubRenderMarkdownPayload(
                        requireNonNull(text, "text"), GitHubMarkdownMode.MARKDOWN, null));
    }

    /**
     * Renders Markdown to HTML in GitHub Flavored Markdown mode, so that references such as {@code #123} and
     * {@code @octocat} become links relative to a repository ({@code POST /markdown}).
     *
     * @param text the Markdown source
     * @param context the repository that references are resolved against
     * @return the HTML
     * @throws NullPointerException if {@code text} or {@code context} is {@code null}
     * @throws IOException if the transport fails
     */
    public String renderMarkdown(final String text, final Repository context) throws IOException {
        final Repository repository = requireNonNull(context, "context");
        return github().markdown()
                .render(new GitHubRenderMarkdownPayload(
                        requireNonNull(text, "text"),
                        GitHubMarkdownMode.GFM,
                        repository.owner() + "/" + repository.name()));
    }

    /**
     * Lists the names of the {@code .gitignore} templates ({@code GET /gitignore/templates}).
     *
     * @return the template names
     * @throws IOException if the transport fails
     */
    public List<String> gitignoreTemplates() throws IOException {
        return github().gitignore().templates();
    }

    /**
     * Loads one {@code .gitignore} template ({@code GET /gitignore/templates/{name}}).
     *
     * @param name the template name, for example {@code Java}
     * @return the template, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GitHubGitignoreTemplate> gitignoreTemplate(final String name) throws IOException {
        return github().gitignore().get(requireNotBlank(name, "name"));
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
