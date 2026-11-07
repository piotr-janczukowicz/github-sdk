package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionExpiry;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionGroup;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionLimitResponse;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.interactions.RepositoryInteractions;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class InteractionsAndCodeQualitySmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void interactionLimit_shouldBeSetReadAndRemoved_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final RepositoryInteractions interactions = repository.interactions();
        final GitHubInteractionLimitResponse set = restrict(interactions);
        try {
            final Optional<GitHubInteractionLimitResponse> current = interactions.current();

            assertSoftly(softly -> {
                softly.assertThat(set.limit()).isEqualTo(GitHubInteractionGroup.COLLABORATORS_ONLY);
                softly.assertThat(current).isPresent();
                softly.assertThat(current.map(GitHubInteractionLimitResponse::limit))
                        .contains(GitHubInteractionGroup.COLLABORATORS_ONLY);
            });
        } finally {
            interactions.remove();
        }

        final Optional<GitHubInteractionLimitResponse> afterRemoval = interactions.current();

        assertThat(afterRemoval).isEmpty();
    }

    @Test
    void codeQuality_shouldBeRead_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            final List<?> findings = repository.codeQuality().findings().toList();
            final Optional<?> setup = repository.codeQuality().setup();

            assertSoftly(softly -> {
                softly.assertThat(findings).doesNotContainNull();
                softly.assertThat(setup).isNotNull();
            });
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses code quality here: no permission or the feature is not available");
        }
    }

    private static GitHubInteractionLimitResponse restrict(final RepositoryInteractions interactions)
            throws IOException {
        try {
            return interactions.restrict(GitHubInteractionGroup.COLLABORATORS_ONLY, GitHubInteractionExpiry.ONE_DAY);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses interaction limits here: no Administration permission");
            throw e;
        }
    }
}
