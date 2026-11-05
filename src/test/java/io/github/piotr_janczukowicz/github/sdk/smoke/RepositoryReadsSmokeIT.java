package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTag;
import io.github.piotr_janczukowicz.github.client.exception.GitHubApiException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.RepositoryFile;
import io.github.piotr_janczukowicz.github.sdk.git.Ref;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class RepositoryReadsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void readme_shouldBeLoaded_whenTheSandboxRepositoryHasOne() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<RepositoryFile> readme = repository.readme();

        assertThat(readme).hasValueSatisfying(file -> assertThat(file.text()).isNotBlank());
    }

    @Test
    void activitiesAndCodeowners_shouldBeRead_whenTheyAreRequested() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final long activities = repository.activities().limit(5).count();
        final boolean hasCodeownersAnswer = repository.codeownersErrors().isPresent()
                || repository.codeownersErrors().isEmpty();

        assertSoftly(softly -> {
            softly.assertThat(activities).isBetween(0L, 5L);
            softly.assertThat(hasCodeownersAnswer).isTrue();
        });
    }

    @Test
    void teams_shouldBeListed_whenTheRepositoryBelongsToAnOrganization() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            assertThat(repository.teams().limit(5).count()).isBetween(0L, 5L);
        } catch (final GitHubApiException e) {
            Assumptions.abort("GitHub lists teams only for an organization repository (" + e.getMessage() + ")");
        }
    }

    @Test
    void me_shouldListTheSandboxRepository_whenItIsAccessible() throws IOException {
        final boolean isListed = ENV.sdk()
                .me()
                .repositories()
                .limit(200)
                .anyMatch(repository -> repository.name().equals(ENV.repo()));

        assertThat(isListed).isTrue();
    }

    @Test
    void annotatedTag_shouldBeCreatedLoadedAndDeleted_whenItPointsAtTheDefaultBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String sha = repository.branch(base).orElseThrow().commit().sha();
        final String tagName = ENV.name("tag");
        final Ref ref = repository.createAnnotatedTag(tagName, "smoke annotated tag", sha);
        try {
            final Optional<GitHubGitTag> tag =
                    repository.tagObject(ref.response().object().sha());

            assertSoftly(softly -> {
                softly.assertThat(ref.response().ref()).isEqualTo("refs/tags/" + tagName);
                softly.assertThat(tag).hasValueSatisfying(found -> {
                    assertThat(found.tag()).isEqualTo(tagName);
                    assertThat(found.object().sha()).isEqualTo(sha);
                });
            });
        } finally {
            ref.delete();
        }
    }
}
