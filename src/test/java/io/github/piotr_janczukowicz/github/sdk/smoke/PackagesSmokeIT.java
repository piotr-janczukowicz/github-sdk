package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackage;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class PackagesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void packages_shouldBeListed_whenTheTokenMayReadThem() throws IOException {
        final List<RegistryPackage> packages = list();
        Assumptions.assumeFalse(packages.isEmpty(), "the sandbox owner has no container packages");

        assertSoftly(softly -> softly.assertThat(packages).allSatisfy(found -> {
            softly.assertThat(found.name()).isNotBlank();
            softly.assertThat(found.versionCount()).isNotNegative();
        }));
    }

    private static List<RegistryPackage> list() throws IOException {
        try {
            return ENV.sdk()
                    .user(ENV.owner())
                    .orElseThrow()
                    .registryPackages(GitHubPackageType.CONTAINER)
                    .toList();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses packages here: the REST API for packages needs a classic token with"
                    + " read:packages (" + e.getMessage() + ")");
            throw e;
        }
    }
}
