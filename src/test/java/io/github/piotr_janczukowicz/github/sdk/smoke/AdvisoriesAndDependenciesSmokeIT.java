package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubGlobalAdvisory;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.advisories.Advisory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class AdvisoriesAndDependenciesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final int LIMIT = 3;

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void globalAdvisories_shouldBeListedAndLoaded_whenTheDatabaseIsRead() throws IOException {
        final List<GitHubGlobalAdvisory> advisories =
                ENV.sdk().globalAdvisories().limit(LIMIT).toList();
        final String ghsaId = advisories.getFirst().ghsaId();

        final GitHubGlobalAdvisory loaded = ENV.sdk().globalAdvisory(ghsaId).orElseThrow();

        final boolean isUnknownPresent =
                ENV.sdk().globalAdvisory("GHSA-0000-0000-0000").isPresent();

        assertSoftly(softly -> {
            softly.assertThat(advisories).hasSize(LIMIT);
            softly.assertThat(loaded.ghsaId()).isEqualTo(ghsaId);
            softly.assertThat(isUnknownPresent).isFalse();
        });
    }

    @Test
    void sbom_shouldBeRequested_whenTheDependencyGraphIsAvailable() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            final String sbomUrl = repository.dependencies().requestSbomReport().sbomUrl();

            assertThat(sbomUrl).isNotBlank();
        } catch (final GitHubForbiddenException | UnsupportedOperationException e) {
            Assumptions.abort("GitHub refuses the SBOM report here (" + e.getMessage() + ")");
        }
    }

    @Test
    void advisories_shouldBeListed_whenTheTokenMayReadThem() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            final List<Advisory> advisories =
                    repository.advisories().all().limit(LIMIT).toList();

            assertThat(advisories)
                    .allSatisfy(advisory -> assertThat(advisory.ghsaId()).isNotBlank());
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses repository advisories here: the token needs the Repository security"
                    + " advisories permission (" + e.getMessage() + ")");
        }
    }
}
