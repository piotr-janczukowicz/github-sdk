package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueEvent;
import io.github.piotr_janczukowicz.github.api.issues.GitHubTimelineEvent;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.issues.IssueComment;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequestReviewComment;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ReactionsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void reaction_shouldBeAddedListedAndDeleted_whenItIsOnAnIssue() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue issue = repository.createIssue(ENV.name("reactions"));
        try {
            final Reaction heart = issue.react(ReactionContent.HEART);
            final Reaction rocket = issue.react(ReactionContent.ROCKET);
            final List<ReactionContent> before =
                    issue.reactions().map(Reaction::content).toList();
            heart.delete();
            final List<ReactionContent> after =
                    issue.reactions().map(Reaction::content).toList();

            assertSoftly(softly -> {
                softly.assertThat(heart.content()).isEqualTo(ReactionContent.HEART);
                softly.assertThat(rocket.content()).isEqualTo(ReactionContent.ROCKET);
                softly.assertThat(before).contains(ReactionContent.HEART, ReactionContent.ROCKET);
                softly.assertThat(after).contains(ReactionContent.ROCKET).doesNotContain(ReactionContent.HEART);
            });
        } finally {
            issue.close();
        }
    }

    @Test
    void reaction_shouldBeAddedAndListed_whenItIsOnAnIssueComment() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue issue = repository.createIssue(ENV.name("comment-reactions"));
        try {
            final IssueComment comment = issue.comment("smoke comment");

            final Reaction eyes = comment.react(ReactionContent.EYES);
            final List<Long> ids = comment.reactions().map(Reaction::id).toList();
            eyes.delete();

            assertThat(ids).contains(eyes.id());
            assertThat(comment.reactions().map(Reaction::id)).doesNotContain(eyes.id());
        } finally {
            issue.close();
        }
    }

    @Test
    void reaction_shouldBeAddedAndListed_whenItIsOnAReviewComment() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("review-reactions");
        final String path = "smoke/" + ENV.runId() + "-review-reactions.txt";
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, path);
            pullRequest = repository.createPullRequest(
                    SandboxBranches.newPullRequest(head, base, ENV.name("review-reactions")));
            final PullRequestReviewComment comment =
                    pullRequest.commentOnLine(pullRequest.response().head().sha(), path, 1, "react to me");

            final Reaction hooray = comment.react(ReactionContent.HOORAY);
            final List<Long> ids = comment.reactions().map(Reaction::id).toList();
            hooray.delete();
            comment.delete();

            assertThat(ids).contains(hooray.id());
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void eventsAndTimeline_shouldRecordWhatHappened_whenAnIssueIsCommentedAndClosed() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Issue issue = repository.createIssue(ENV.name("events"));

        issue.comment("smoke comment");
        final Issue closed = issue.close();
        final List<String> events = closed.events().map(GitHubIssueEvent::event).toList();
        final List<String> timeline =
                closed.timeline().map(GitHubTimelineEvent::event).toList();

        assertSoftly(softly -> {
            softly.assertThat(events).contains("closed");
            softly.assertThat(timeline).contains("commented", "closed");
        });
    }
}
