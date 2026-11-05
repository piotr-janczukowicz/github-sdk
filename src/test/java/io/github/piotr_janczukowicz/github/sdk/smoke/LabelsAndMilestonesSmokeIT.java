package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneState;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.issues.Label;
import io.github.piotr_janczukowicz.github.sdk.issues.Milestone;
import java.io.IOException;
import java.util.Optional;
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
            final Optional<Label> afterDelete = repository.label(name);

            assertSoftly(softly -> {
                softly.assertThat(created.color()).isEqualTo("ededed");
                softly.assertThat(isRead).isTrue();
                softly.assertThat(isListed).isTrue();
                softly.assertThat(afterDelete).isEmpty();
            });
        } finally {
            if (!isDeleted) {
                created.delete();
            }
        }
    }

    @Test
    void milestone_shouldBeCreatedAndAssignedToAnIssue_whenItIsOpen() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Milestone created = repository.createMilestone(ENV.name("milestone"));
        Issue issue = null;
        try {
            issue = repository.createIssue(ENV.name("planned"));
            final Issue planned = issue.update().milestone(created.number()).send();
            issue = planned;

            final Optional<Milestone> loaded = repository.milestone(created.number());
            final boolean isListed = repository.milestones().anyMatch(listed -> listed.number() == created.number());

            assertSoftly(softly -> {
                softly.assertThat(loaded).map(Milestone::title).contains(ENV.name("milestone"));
                softly.assertThat(isListed).isTrue();
                softly.assertThat(planned.milestone()).map(Milestone::number).contains(created.number());
            });
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

            assertSoftly(softly -> {
                softly.assertThat(updated.color()).isEqualTo("ffffff");
                softly.assertThat(refreshed.color()).isEqualTo("ffffff");
                softly.assertThat(created.color()).isEqualTo("ededed");
            });
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

            assertSoftly(softly -> {
                softly.assertThat(renamed.title()).isEqualTo(ENV.name("phase-two"));
                softly.assertThat(closed.response().state()).isEqualTo(GitHubMilestoneState.CLOSED);
                softly.assertThat(reopened.response().state()).isEqualTo(GitHubMilestoneState.OPEN);
                softly.assertThat(refreshed.title()).isEqualTo(ENV.name("phase-two"));
            });
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
