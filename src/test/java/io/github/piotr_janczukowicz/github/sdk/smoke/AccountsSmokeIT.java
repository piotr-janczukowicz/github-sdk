package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.User;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class AccountsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void currentUser_shouldHaveALoginAndBeFoundByIt_whenTheTokenIsValid() throws IOException {
        final User current = ENV.sdk().currentUser();

        final Optional<User> found = ENV.sdk().user(current.login());

        assertSoftly(softly -> {
            softly.assertThat(current.login()).isNotBlank();
            softly.assertThat(found).map(User::login).contains(current.login());
        });
    }

    @Test
    void user_shouldResolveTheSandboxOwner_whenItExists() throws IOException {
        final Optional<User> owner = ENV.sdk().user(ENV.owner());

        assertThat(owner)
                .map(User::login)
                .hasValueSatisfying(login -> assertThat(login).isEqualToIgnoringCase(ENV.owner()));
    }

    @Test
    void user_shouldBeEmpty_whenTheLoginDoesNotExist() throws IOException {
        final Optional<User> user = ENV.sdk().user("smoke-" + ENV.runId() + "-no-such-user");

        assertThat(user).isEmpty();
    }

    @Test
    void repositories_shouldBeOwnedByTheUser_whenTheyAreListed() throws IOException {
        final User current = ENV.sdk().currentUser();

        final List<Repository> repositories = current.repositories().limit(3).toList();

        Assumptions.assumeFalse(repositories.isEmpty(), current.login() + " has no public repositories");
        assertThat(repositories)
                .allSatisfy(repository -> assertThat(repository.owner()).isEqualToIgnoringCase(current.login()));
    }
}
