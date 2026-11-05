package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubApiException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.actions.RepositoryActionsCache;
import io.github.piotr_janczukowicz.github.sdk.security.RepositoryCodeScanning;
import java.io.IOException;
import java.util.OptionalInt;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ActionsCacheAndCodeScanningSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void actionsCache_shouldBeReadAndItsLimitRewritten_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final RepositoryActionsCache caches = repository.actionsCache();

        try {
            final long count = caches.caches().count();
            final int activeCount = caches.usage().activeCachesCount();
            final OptionalInt retention = caches.retentionDays();
            if (retention.isPresent()) {
                caches.setRetentionDays(retention.getAsInt());
            }

            assertSoftly(softly -> {
                softly.assertThat(count).isNotNegative();
                softly.assertThat(activeCount).isNotNegative();
            });
        } catch (final GitHubApiException e) {
            Assumptions.abort("GitHub refuses the Actions cache here (" + e.getMessage() + ")");
        }
    }

    @Test
    void codeScanning_shouldBeRead_whenTheRepositoryAllowsIt() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final RepositoryCodeScanning scanning = repository.codeScanning();

        try {
            final long analyses = scanning.analyses().count();
            final int databases = scanning.databases().size();
            final boolean hasSetup = scanning.defaultSetup().state() != null;

            assertSoftly(softly -> {
                softly.assertThat(analyses).isNotNegative();
                softly.assertThat(databases).isNotNegative();
                softly.assertThat(hasSetup).isTrue();
            });
        } catch (final GitHubApiException e) {
            Assumptions.abort("GitHub refuses code scanning here: no Code scanning alerts permission or the feature"
                    + " is not available (" + e.getMessage() + ")");
        }
    }
}
