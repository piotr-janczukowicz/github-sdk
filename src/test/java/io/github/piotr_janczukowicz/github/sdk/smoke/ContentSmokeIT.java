package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.sdk.Branch;
import io.github.piotr_janczukowicz.github.sdk.Commit;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ContentSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void branch_shouldBeReadAndListed_whenItIsTheDefaultBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = repository.fetch().orElseThrow().response().defaultBranch();

        final Optional<Branch> branch = repository.branch(name);
        final boolean isListed =
                repository.branches().limit(50).anyMatch(listed -> listed.name().equals(name));

        assertThat(branch).map(Branch::name).contains(name);
        assertThat(isListed).isTrue();
    }

    @Test
    void commit_shouldBeReadAndListed_whenItIsTheHeadOfTheDefaultBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = repository.fetch().orElseThrow().response().defaultBranch();
        final Commit head = repository.branch(name).orElseThrow().commit();

        final Optional<Commit> read = repository.commit(head.sha());
        final boolean isListed =
                repository.commits().limit(10).anyMatch(listed -> listed.sha().equals(head.sha()));

        assertThat(read).map(Commit::sha).contains(head.sha());
        assertThat(read.orElseThrow().message()).isNotBlank();
        assertThat(isListed).isTrue();
    }

    @Test
    void branch_shouldBeEmpty_whenTheNameDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<Branch> branch = repository.branch(ENV.name("missing"));

        assertThat(branch).isEmpty();
    }
}
