package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComparisonStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubDiffEntry;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Branch;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.git.Comparison;
import io.github.piotr_janczukowicz.github.sdk.git.Ref;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class GitDataSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void commit_shouldWriteAndDeleteFilesInOneCommit_whenChangesAreCollected() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String baseSha = repository.branch(base).orElseThrow().commit().sha();
        final String name = ENV.name("git");
        final String keep = "smoke/" + ENV.runId() + "/keep.txt";
        final String drop = "smoke/" + ENV.runId() + "/drop.txt";
        final Branch branch = repository.createBranch(name, baseSha);
        try {
            final Commit first = repository
                    .commitTo(name)
                    .write(keep, "kept")
                    .write(drop, new byte[] {1, 2, 3})
                    .send("Add two files");
            final Commit second = repository.commitTo(name).delete(drop).send("Drop one file");
            final Comparison comparison = repository.compare(base, name).orElseThrow();
            final List<GitHubDiffEntry> files = comparison.files();
            final Ref ref = repository.ref("heads/" + name).orElseThrow();
            final Ref moved = ref.update(baseSha, true);

            assertSoftly(softly -> {
                softly.assertThat(branch.name()).isEqualTo(name);
                softly.assertThat(first.sha()).isNotEqualTo(baseSha);
                softly.assertThat(second.sha()).isEqualTo(ref.sha());
                softly.assertThat(comparison.status()).isEqualTo(GitHubCommitComparisonStatus.AHEAD);
                softly.assertThat(comparison.aheadBy()).isEqualTo(2);
                softly.assertThat(comparison.commits()).extracting(Commit::sha).contains(first.sha(), second.sha());
                softly.assertThat(files).extracting(GitHubDiffEntry::filename).containsExactly(keep);
                softly.assertThat(moved.sha()).isEqualTo(baseSha);
            });
            assertThat(repository.compare(base, name).orElseThrow().status())
                    .isEqualTo(GitHubCommitComparisonStatus.IDENTICAL);
        } finally {
            SandboxBranches.deleteBranch(ENV, name);
        }
        assertThat(repository.ref("heads/" + name)).isEmpty();
    }

    @Test
    void refsAndTrees_shouldBeListedAndLoaded_whenTheRepositoryHasContent() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();

        final List<Ref> heads = repository.refs("heads/").toList();
        final Ref head = repository.ref("heads/" + base).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(heads).extracting(Ref::name).contains("refs/heads/" + base);
            softly.assertThat(head.type()).isEqualTo("commit");
        });
        assertThat(repository.tree(head.sha(), true)).isPresent();
        assertThat(repository.ref("heads/" + ENV.name("missing"))).isEmpty();
    }
}
