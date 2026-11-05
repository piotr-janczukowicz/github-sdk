package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Branch;
import io.github.piotr_janczukowicz.github.sdk.content.BranchProtection;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class BranchProtectionSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void protection_shouldBeSetChangedReadAndRemoved_whenABranchIsProtected() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String name = ENV.name("protected");
        SandboxBranches.createBranch(ENV, name, SandboxBranches.branchSha(ENV, base));
        try {
            final Branch branch = repository.branch(name).orElseThrow();
            final BranchProtection first = protect(branch);
            final BranchProtection second = branch.protect().allowDeletions().send();
            final BranchProtection reloaded = second.refresh();
            final boolean isFlagged = branch.refresh().isProtected();

            assertSoftly(softly -> {
                softly.assertThat(first.isEnforcedForAdmins()).isTrue();
                softly.assertThat(first.requiredApprovals()).hasValue(1);
                softly.assertThat(first.isLinearHistoryRequired()).isTrue();
                softly.assertThat(first.allowsDeletions()).isFalse();
                softly.assertThat(second.isEnforcedForAdmins()).isFalse();
                softly.assertThat(second.requiredApprovals()).isEmpty();
                softly.assertThat(second.allowsDeletions()).isTrue();
                softly.assertThat(reloaded.allowsDeletions()).isTrue();
                softly.assertThat(isFlagged).isTrue();
            });

            reloaded.remove();
            assertThat(branch.protection()).isEmpty();
        } finally {
            SandboxBranches.deleteBranch(ENV, name);
        }
    }

    private static BranchProtection protect(final Branch branch) throws IOException {
        try {
            return branch.protect()
                    .enforceAdmins()
                    .requireLinearHistory()
                    .requirePullRequestReviews(1)
                    .requireStatusChecks(false, List.of())
                    .send();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses branch protection here: no Administration permission, or a private"
                    + " repository on a plan without it");
            throw e;
        }
    }
}
