package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.RepositoryFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class FilesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void file_shouldRoundTrip_whenWrittenReadUpdatedAndDeleted() throws IOException {
        final GitHub github = ENV.github();
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String branch = ENV.name("files");
        final String path = "smoke/" + ENV.runId() + "-sdk-file.bin";
        final byte[] binary = {0, 1, 2, (byte) 0xFF, (byte) 0xFE, 10, 13, 127};
        github.gitDatabase()
                .refs()
                .createRef(
                        ENV.owner(),
                        ENV.repo(),
                        new GitHubCreateRefPayload("refs/heads/" + branch, headSha(github, base)));
        try {
            final RepositoryFile created = repository.writeFile(path, "smoke create", binary, branch);
            final RepositoryFile updated =
                    repository.writeFile(path, "smoke update", "héllo".getBytes(StandardCharsets.UTF_8), branch);
            final RepositoryFile read = repository.file(path, branch).orElseThrow();
            final boolean isDirectoryAFile = repository.file("smoke", branch).isPresent();
            read.delete("smoke delete", branch);

            assertThat(created.bytes()).isEqualTo(binary);
            assertThat(updated.sha()).isNotEqualTo(created.sha());
            assertThat(read.text()).isEqualTo("héllo");
            assertThat(isDirectoryAFile).isFalse();
            assertThat(repository.file(path, branch)).isEmpty();
        } finally {
            github.gitDatabase().refs().deleteRef(ENV.owner(), ENV.repo(), "heads/" + branch);
        }
    }

    @Test
    void file_shouldBeEmpty_whenThePathDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final boolean isPresent =
                repository.file("smoke/" + ENV.name("missing")).isPresent();

        assertThat(isPresent).isFalse();
    }

    private static String headSha(final GitHub github, final String branch) throws IOException {
        return github.gitDatabase()
                .refs()
                .getRef(ENV.owner(), ENV.repo(), "heads/" + branch)
                .orElseThrow()
                .object()
                .sha();
    }
}
