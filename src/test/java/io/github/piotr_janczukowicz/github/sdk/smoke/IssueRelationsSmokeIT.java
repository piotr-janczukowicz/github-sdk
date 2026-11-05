package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubApiException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.issues.IssueComment;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class IssueRelationsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void subIssues_shouldBeAddedListedMovedAndRemoved_whenAParentHasTwoChildren() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue parent = repository.createIssue(ENV.name("parent"));
        final Issue first = repository.createIssue(ENV.name("child-a"));
        final Issue second = repository.createIssue(ENV.name("child-b"));
        try {
            addSubIssue(parent, first);
            parent.addSubIssue(second);
            final List<Integer> before = parent.subIssues().map(Issue::number).toList();
            final int parentNumber = first.parent().orElseThrow().number();
            parent.moveSubIssueBefore(second, first);
            final List<Integer> after = parent.subIssues().map(Issue::number).toList();
            parent.removeSubIssue(first);
            final List<Integer> remaining =
                    parent.subIssues().map(Issue::number).toList();

            assertSoftly(softly -> {
                softly.assertThat(before).containsExactlyInAnyOrder(first.number(), second.number());
                softly.assertThat(parentNumber).isEqualTo(parent.number());
                softly.assertThat(after).containsExactly(second.number(), first.number());
                softly.assertThat(remaining).containsExactly(second.number());
            });
        } finally {
            parent.close();
            first.close();
            second.close();
        }
    }

    @Test
    void dependencies_shouldBeAddedListedAndRemoved_whenOneIssueBlocksAnother() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue blocked = repository.createIssue(ENV.name("blocked"));
        final Issue blocker = repository.createIssue(ENV.name("blocker"));
        try {
            addBlockedBy(blocked, blocker);
            final List<Integer> blockedBy =
                    blocked.blockedBy().map(Issue::number).toList();
            final List<Integer> blocking = blocker.blocking().map(Issue::number).toList();
            blocked.removeBlockedBy(blocker);
            final List<Integer> afterRemoval =
                    blocked.blockedBy().map(Issue::number).toList();

            assertSoftly(softly -> {
                softly.assertThat(blockedBy).containsExactly(blocker.number());
                softly.assertThat(blocking).containsExactly(blocked.number());
                softly.assertThat(afterRemoval).isEmpty();
            });
        } finally {
            blocked.close();
            blocker.close();
        }
    }

    @Test
    void comment_shouldBePinnedAndUnpinned_whenItIsOnAnIssue() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue issue = repository.createIssue(ENV.name("pin"));
        try {
            final IssueComment comment = issue.comment("smoke pin");
            final IssueComment pinned = pin(comment);
            pinned.unpin();

            assertThat(pinned.id()).isEqualTo(comment.id());
        } finally {
            issue.close();
        }
    }

    @Test
    void assignees_shouldIncludeTheOwner_whenTheRepositoryIsPersonal() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final boolean canBeAssigned = repository.canBeAssigned(ENV.owner());
        final long assignees = repository.assignees().limit(5).count();

        assertSoftly(softly -> {
            softly.assertThat(canBeAssigned).isTrue();
            softly.assertThat(assignees).isPositive();
        });
    }

    private static void addSubIssue(final Issue parent, final Issue child) throws IOException {
        try {
            parent.addSubIssue(child);
        } catch (final GitHubApiException e) {
            Assumptions.abort("Sub-issues are not available here (" + e.getMessage() + ")");
        }
    }

    private static void addBlockedBy(final Issue blocked, final Issue blocker) throws IOException {
        try {
            blocked.addBlockedBy(blocker);
        } catch (final GitHubApiException e) {
            Assumptions.abort("Issue dependencies are not available here (" + e.getMessage() + ")");
        }
    }

    private static IssueComment pin(final IssueComment comment) throws IOException {
        try {
            return comment.pin();
        } catch (final GitHubApiException e) {
            Assumptions.abort("Pinning comments is not available here (" + e.getMessage() + ")");
            throw e;
        }
    }
}
