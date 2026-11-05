package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicenseContent;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCommunityProfile;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubContributorActivity;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubParticipationStats;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubViewTraffic;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Autolink;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.content.CommitComment;
import io.github.piotr_janczukowicz.github.sdk.utilities.Utilities;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

        final boolean isGone = isGoneOrForbidden(repository, created.id());

        assertThat(isGone).isTrue();
    }

    private static boolean isGoneOrForbidden(final Repository repository, final long id) throws IOException {
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
        final String prefix = "SMOKE" + ENV.runId().toUpperCase(Locale.ROOT) + "-";
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

        final Optional<Autolink> afterDelete = repository.autolink(added.id());

        assertThat(afterDelete).isEmpty();
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

        final GitHubCommunityProfile community = repository.communityProfile();
        final GitHubParticipationStats participation = repository.statistics().participation();
        final List<GitHubContributorActivity> contributors =
                repository.statistics().contributors();
        final GitHubViewTraffic traffic = readTraffic(repository);
        final Optional<GitHubLicenseContent> license = repository.license();

        assertSoftly(softly -> {
            softly.assertThat(community).isNotNull();
            softly.assertThat(participation).isNotNull();
            softly.assertThat(contributors).isNotNull();
            softly.assertThat(traffic).isNotNull();
            softly.assertThat(license).isNotNull();
        });
    }

    private static GitHubViewTraffic readTraffic(final Repository repository) throws IOException {
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

        final String zen = utilities.zen();
        final String html = utilities.renderMarkdown("**bold**");
        final Optional<?> template = utilities.gitignoreTemplate("Java");
        final Optional<?> mit = ENV.sdk().license("mit");
        final List<?> licenses = ENV.sdk().licenses().limit(3).toList();

        assertSoftly(softly -> {
            softly.assertThat(emojis).containsKey("+1");
            softly.assertThat(templates).contains("Java");
            softly.assertThat(zen).isNotBlank();
            softly.assertThat(html).contains("<strong>bold</strong>");
            softly.assertThat(template).isPresent();
            softly.assertThat(mit).isPresent();
            softly.assertThat(licenses).isNotEmpty();
        });
    }
}
