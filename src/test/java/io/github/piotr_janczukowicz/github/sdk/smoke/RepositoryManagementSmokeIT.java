package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class RepositoryManagementSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void update_shouldChangeAndRestoreTheDescriptionAndHomepage_whenTheyAreSet() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Repository original = repository.fetch().orElseThrow();
        final String description =
                Objects.requireNonNullElse(original.response().description(), "");
        final String homepage = Objects.requireNonNullElse(original.response().homepage(), "");
        final String changed = "smoke " + ENV.runId();
        final Repository updated = update(repository, changed, "https://example.com/" + ENV.runId());
        try {
            assertSoftly(softly -> {
                softly.assertThat(updated.name()).isEqualTo(ENV.repo());
                softly.assertThat(updated.response().description()).isEqualTo(changed);
                softly.assertThat(updated.response().homepage()).isEqualTo("https://example.com/" + ENV.runId());
            });
        } finally {
            repository.update().description(description).homepage(homepage).send();
        }
    }

    private static Repository update(final Repository repository, final String description, final String homepage)
            throws IOException {
        try {
            return repository
                    .update()
                    .description(description)
                    .homepage(homepage)
                    .send();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Administration permission, so GitHub refuses changing the repository");
            throw e;
        }
    }

    @Test
    void topics_shouldBeReplacedAndRestored_whenTheyAreSet() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final List<String> original = repository.topics();
        final List<String> replaced = replaceTopics(repository, List.of("smoke-test", "github-sdk"));
        try {
            final List<String> reloaded = repository.topics();

            assertSoftly(softly -> {
                softly.assertThat(replaced).containsExactlyInAnyOrder("smoke-test", "github-sdk");
                softly.assertThat(reloaded).containsExactlyInAnyOrderElementsOf(replaced);
            });
        } finally {
            repository.replaceTopics(original);
        }
        assertThat(repository.topics()).containsExactlyInAnyOrderElementsOf(original);
    }

    private static List<String> replaceTopics(final Repository repository, final List<String> topics)
            throws IOException {
        try {
            return repository.replaceTopics(topics);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Administration permission, so GitHub refuses changing the topics");
            throw e;
        }
    }

    @Test
    void languagesAndContributors_shouldBeListed_whenTheRepositoryHasContent() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        assertSoftly(softly -> {
            softly.assertThatCode(repository::languages).doesNotThrowAnyException();
            softly.assertThatCode(() -> repository.contributors().limit(5).toList())
                    .doesNotThrowAnyException();
        });
    }
}
