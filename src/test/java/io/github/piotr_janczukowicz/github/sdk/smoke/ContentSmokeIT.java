package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Branch;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
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
    void branch_shouldBeFound_whenItIsTheDefaultBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = repository.fetch().orElseThrow().response().defaultBranch();

        final Optional<Branch> branch = repository.branch(name);

        assertThat(branch).map(Branch::name).contains(name);
    }

    @Test
    void branches_shouldListTheDefaultBranch_whenTheRepositoryHasOne() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = repository.fetch().orElseThrow().response().defaultBranch();

        final boolean isListed =
                repository.branches().anyMatch(listed -> listed.name().equals(name));

        assertThat(isListed).isTrue();
    }

    @Test
    void commit_shouldBeFound_whenItIsTheHeadOfTheDefaultBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = repository.fetch().orElseThrow().response().defaultBranch();
        final Commit head = repository.branch(name).orElseThrow().commit();

        final Optional<Commit> read = repository.commit(head.sha());

        assertSoftly(softly -> {
            softly.assertThat(read).map(Commit::sha).contains(head.sha());
            softly.assertThat(read)
                    .map(Commit::message)
                    .hasValueSatisfying(message -> assertThat(message).isNotBlank());
        });
    }

    @Test
    void commits_shouldListTheHead_whenItIsTheHeadOfTheDefaultBranch() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = repository.fetch().orElseThrow().response().defaultBranch();
        final Commit head = repository.branch(name).orElseThrow().commit();

        final boolean isListed =
                repository.commits().limit(10).anyMatch(listed -> listed.sha().equals(head.sha()));

        assertThat(isListed).isTrue();
    }

    @Test
    void branch_shouldBeFound_whenItsNameContainsASlash() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = createSlashBranch(repository);
        try {
            final Optional<Branch> branch = repository.branch(name);

            assertThat(branch).map(Branch::name).contains(name);
        } finally {
            deleteSlashBranch(name);
        }
    }

    @Test
    void branches_shouldListTheBranch_whenItsNameContainsASlash() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = createSlashBranch(repository);
        try {
            final boolean isListed =
                    repository.branches().anyMatch(listed -> listed.name().equals(name));

            assertThat(isListed).isTrue();
        } finally {
            deleteSlashBranch(name);
        }
    }

    private static String createSlashBranch(final Repository repository) throws IOException {
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String sha = repository.branch(base).orElseThrow().commit().sha();
        final String name = ENV.name("slash") + "/nested";
        ENV.github()
                .gitDatabase()
                .refs()
                .createRef(ENV.owner(), ENV.repo(), new GitHubCreateRefPayload("refs/heads/" + name, sha));
        return name;
    }

    private static void deleteSlashBranch(final String name) throws IOException {
        ENV.github().gitDatabase().refs().deleteRef(ENV.owner(), ENV.repo(), "heads/" + name);
    }

    @Test
    void branch_shouldBeEmpty_whenTheNameDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<Branch> branch = repository.branch(ENV.name("missing"));

        assertThat(branch).isEmpty();
    }
}
