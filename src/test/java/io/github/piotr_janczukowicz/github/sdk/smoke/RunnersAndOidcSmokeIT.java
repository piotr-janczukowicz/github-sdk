package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.actions.GitHubOidcCustomSubRepo;
import io.github.piotr_janczukowicz.github.client.exception.GitHubApiException;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.util.List;
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
            final List<?> runners = repository.selfHostedRunners().all().toList();
            final List<?> applications = repository.selfHostedRunners().applications();

            assertSoftly(softly -> {
                softly.assertThat(runners).doesNotContainNull();
                softly.assertThat(applications).isNotEmpty();
            });
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses self-hosted runners here: the token needs the Administration permission ("
                    + e.getMessage() + ")");
        }
    }

    @Test
    void oidcSubjectClaim_shouldBeRead_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        try {
            final GitHubOidcCustomSubRepo claim = repository.oidcSubjectClaim();

            assertThat(claim).isNotNull();
        } catch (final GitHubApiException e) {
            Assumptions.abort("GitHub refuses the OIDC settings here (" + e.getMessage() + ")");
        }
    }
}
