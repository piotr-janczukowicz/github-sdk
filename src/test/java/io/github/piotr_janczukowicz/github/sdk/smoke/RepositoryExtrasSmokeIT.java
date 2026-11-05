package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Autolink;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.content.CommitComment;
import io.github.piotr_janczukowicz.github.sdk.utilities.Utilities;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class RepositoryExtrasSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void commitComment_shouldBeCreatedListedEditedAndDeleted_whenItIsManaged() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final Commit commit = repository.branch(base).orElseThrow().commit();
        final CommitComment created = comment(commit, "smoke " + ENV.runId());
        try {
            final List<CommitComment> listed = commit.comments().toList();
            final CommitComment edited = created.edit("edited " + ENV.runId());
            final CommitComment reloaded = created.refresh();
            final boolean isInRepositoryList =
                    repository.commitComments().limit(50).anyMatch(found -> found.id() == created.id());

            assertSoftly(softly -> {
                softly.assertThat(created.commitId()).isEqualTo(commit.sha());
                softly.assertThat(listed).extracting(CommitComment::id).contains(created.id());
                softly.assertThat(edited.body()).isEqualTo("edited " + ENV.runId());
                softly.assertThat(reloaded.body()).isEqualTo("edited " + ENV.runId());
                softly.assertThat(isInRepositoryList).isTrue();
            });
        } finally {
            created.delete();
        }
        assertThat(isGone(repository, created.id())).isTrue();
    }

    /** GitHub answers HTTP 404 for a deleted comment, but with a fine-grained token it can answer 403 instead. */
    private static boolean isGone(final Repository repository, final long id) throws IOException {
        try {
            return repository.commitComment(id).isEmpty();
        } catch (final GitHubForbiddenException e) {
            return true;
        }
    }

    private static CommitComment comment(final Commit commit, final String body) throws IOException {
        try {
            return commit.comment(body);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Contents write permission, so GitHub refuses commit comments");
            throw e;
        }
    }

    @Test
    void autolink_shouldBeAddedListedReloadedAndDeleted_whenItIsManaged() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String prefix = "SMOKE" + ENV.runId().toUpperCase(java.util.Locale.ROOT) + "-";
        final Autolink added = addAutolink(repository, prefix);
        try {
            final List<Autolink> listed = repository.autolinks().toList();
            final Autolink reloaded = added.refresh();

            assertSoftly(softly -> {
                softly.assertThat(added.keyPrefix()).isEqualTo(prefix);
                softly.assertThat(listed).extracting(Autolink::id).contains(added.id());
                softly.assertThat(reloaded.id()).isEqualTo(added.id());
            });
        } finally {
            added.delete();
        }
        assertThat(repository.autolink(added.id())).isEmpty();
    }

    private static Autolink addAutolink(final Repository repository, final String prefix) throws IOException {
        try {
            return repository.addAutolink(prefix, "https://tickets.example.com/<num>");
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Administration permission, so GitHub refuses autolinks");
            throw e;
        }
    }

    @Test
    void metrics_shouldBeReadable_whenTheTokenAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        assertThat(repository.communityProfile()).isNotNull();
        assertThat(repository.statistics().participation()).isNotNull();
        assertThat(repository.statistics().contributors()).isNotNull();
        assertThat(readTraffic(repository)).isNotNull();
        assertThat(repository.license()).isNotNull();
    }

    private static Object readTraffic(final Repository repository) throws IOException {
        try {
            return repository.traffic().views();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("Traffic needs push access and the Administration permission");
            throw e;
        }
    }

    @Test
    void utilities_shouldAnswer_whenCalled() throws IOException {
        final Utilities utilities = ENV.sdk().utilities();

        final Map<String, String> emojis = utilities.emojis();
        final List<String> templates = utilities.gitignoreTemplates();

        assertSoftly(softly -> {
            softly.assertThat(emojis).containsKey("+1");
            softly.assertThat(templates).contains("Java");
        });
        assertThat(utilities.zen()).isNotBlank();
        assertThat(utilities.renderMarkdown("**bold**")).contains("<strong>bold</strong>");
        assertThat(utilities.gitignoreTemplate("Java")).isPresent();
        assertThat(ENV.sdk().license("mit")).isPresent();
        assertThat(ENV.sdk().licenses().limit(3).toList()).isNotEmpty();
    }
}
