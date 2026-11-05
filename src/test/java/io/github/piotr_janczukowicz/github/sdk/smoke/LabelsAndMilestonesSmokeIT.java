package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneState;
import io.github.piotr_janczukowicz.github.sdk.Issue;
import io.github.piotr_janczukowicz.github.sdk.Label;
import io.github.piotr_janczukowicz.github.sdk.Milestone;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class LabelsAndMilestonesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void label_shouldRoundTrip_whenCreatedReadListedAndDeleted() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = ENV.name("label");
        final Label created = repository.createLabel(name, "ededed");
        boolean isDeleted = false;
        try {
            final boolean isRead = repository.label(name).isPresent();
            final boolean isListed =
                    repository.labels().anyMatch(listed -> listed.name().equals(name));
            created.delete();
            isDeleted = true;

            assertThat(created.color()).isEqualTo("ededed");
            assertThat(isRead).isTrue();
            assertThat(isListed).isTrue();
            assertThat(repository.label(name)).isEmpty();
        } finally {
            if (!isDeleted) {
                created.delete();
            }
        }
    }

    @Test
    void milestone_shouldBeCreatedAssignedToAnIssueAndDeleted_whenItIsOpen() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Milestone created = repository.createMilestone(ENV.name("milestone"));
        Issue issue = null;
        try {
            issue = repository.createIssue(ENV.name("planned"));
            final Issue planned = issue.update().milestone(created.number()).send();
            issue = planned;

            assertThat(repository.milestone(created.number()))
                    .map(Milestone::title)
                    .contains(ENV.name("milestone"));
            assertThat(repository.milestones().anyMatch(listed -> listed.number() == created.number()))
                    .isTrue();
            assertThat(planned.milestone()).map(Milestone::number).contains(created.number());
        } finally {
            if (issue != null) {
                issue.close();
            }
            created.delete();
        }
    }

    @Test
    void label_shouldBeUpdatedAndRefreshed_whenItsColorChanges() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Label created = repository.createLabel(ENV.name("recolor"), "ededed");
        try {
            final Label updated = created.update().color("ffffff").send();
            final Label refreshed = updated.refresh();

            assertThat(updated.color()).isEqualTo("ffffff");
            assertThat(refreshed.color()).isEqualTo("ffffff");
            assertThat(created.color()).isEqualTo("ededed");
        } finally {
            created.delete();
        }
    }

    @Test
    void milestone_shouldBeUpdatedClosedReopenedAndRefreshed_whenItIsOpen() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Milestone created = repository.createMilestone(ENV.name("phase"));
        try {
            final Milestone renamed =
                    created.update().title(ENV.name("phase-two")).send();
            final Milestone closed = renamed.close();
            final Milestone reopened = closed.reopen();
            final Milestone refreshed = reopened.refresh();

            assertThat(renamed.title()).isEqualTo(ENV.name("phase-two"));
            assertThat(closed.record().state()).isEqualTo(GitHubMilestoneState.CLOSED);
            assertThat(reopened.record().state()).isEqualTo(GitHubMilestoneState.OPEN);
            assertThat(refreshed.title()).isEqualTo(ENV.name("phase-two"));
        } finally {
            created.delete();
        }
    }

    @Test
    void label_shouldBeEmpty_whenTheNameDoesNotExist() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final boolean isPresent = repository.label(ENV.name("missing")).isPresent();

        assertThat(isPresent).isFalse();
    }
}
