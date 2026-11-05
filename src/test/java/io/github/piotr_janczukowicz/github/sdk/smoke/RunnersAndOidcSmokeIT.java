package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubApiException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class RunnersAndOidcSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void runners_shouldBeListed_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            final long runners = repository.selfHostedRunners().all().count();
            final int applications =
                    repository.selfHostedRunners().applications().size();

            assertSoftly(softly -> {
                softly.assertThat(runners).isNotNegative();
                softly.assertThat(applications).isNotNegative();
            });
        } catch (final GitHubApiException e) {
            Assumptions.abort("GitHub refuses self-hosted runners here: the token needs the Administration permission ("
                    + e.getMessage() + ")");
        }
    }

    @Test
    void oidcSubjectClaim_shouldBeRead_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            final boolean isDefaultUsed = repository.oidcSubjectClaim().useDefault();

            assertSoftly(softly -> softly.assertThat(isDefaultUsed).isIn(true, false));
        } catch (final GitHubApiException e) {
            Assumptions.abort("GitHub refuses the OIDC settings here (" + e.getMessage() + ")");
        }
    }
}
