package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubPullRequestReviewEvent;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequestReview;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequestReviewComment;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class PullRequestReviewsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void review_shouldBeSubmittedListedAndLoaded_whenItIsACommentReview() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("review");
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, "smoke/" + ENV.runId() + "-review.txt");
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("review")));

            final PullRequestReview submitted =
                    pullRequest.review(GitHubPullRequestReviewEvent.COMMENT, "smoke review");
            final boolean isListed = pullRequest.reviews().anyMatch(review -> review.id() == submitted.id());
            final PullRequestReview loaded = pullRequest.review(submitted.id()).orElseThrow();
            final boolean isUnknownEmpty = pullRequest.review(Long.MAX_VALUE).isEmpty();

            assertSoftly(softly -> {
                softly.assertThat(submitted.state()).isEqualTo("COMMENTED");
                softly.assertThat(submitted.body()).isEqualTo("smoke review");
                softly.assertThat(isListed).isTrue();
                softly.assertThat(loaded.id()).isEqualTo(submitted.id());
                softly.assertThat(isUnknownEmpty).isTrue();
            });
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void draftReview_shouldBeEditedAndSubmitted_whenItIsPending() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("draft");
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, "smoke/" + ENV.runId() + "-draft.txt");
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("draft")));

            final PullRequestReview draft = pullRequest.draftReview("first thoughts");
            final PullRequestReview edited = draft.edit("better thoughts");
            final PullRequestReview refreshed = edited.refresh();
            final PullRequestReview submitted = refreshed.submit(GitHubPullRequestReviewEvent.COMMENT);

            assertSoftly(softly -> {
                softly.assertThat(draft.isPending()).isTrue();
                softly.assertThat(edited.body()).isEqualTo("better thoughts");
                softly.assertThat(refreshed.isPending()).isTrue();
                softly.assertThat(submitted.state()).isEqualTo("COMMENTED");
                softly.assertThat(submitted.body()).isEqualTo("better thoughts");
            });
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void draftReview_shouldBeGone_whenItIsDeletedWhilePending() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("discard");
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, "smoke/" + ENV.runId() + "-discard.txt");
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("discard")));
            final PullRequestReview draft = pullRequest.draftReview("to be discarded");

            draft.deletePending();
            final Optional<PullRequestReview> afterDelete = pullRequest.review(draft.id());

            assertThat(afterDelete).isEmpty();
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void reviewComment_shouldBeAddedRepliedEditedListedAndDeleted_whenItIsOnALine() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("inline");
        final String path = "smoke/" + ENV.runId() + "-inline.txt";
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, path);
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("inline")));
            final String commitId = pullRequest.response().head().sha();

            final PullRequestReviewComment comment = pullRequest.commentOnLine(commitId, path, 1, "inline remark");
            final PullRequestReviewComment reply = comment.reply("inline answer");
            final PullRequestReviewComment edited = comment.edit("edited remark");
            final boolean isListed = pullRequest.reviewComments().anyMatch(listed -> listed.id() == reply.id());
            final PullRequestReviewComment refreshed = edited.refresh();
            comment.delete();
            reply.delete();

            assertSoftly(softly -> {
                softly.assertThat(comment.path()).isEqualTo(path);
                softly.assertThat(comment.line()).hasValue(1);
                softly.assertThat(reply.body()).isEqualTo("inline answer");
                softly.assertThat(edited.body()).isEqualTo("edited remark");
                softly.assertThat(isListed).isTrue();
                softly.assertThat(refreshed.body()).isEqualTo("edited remark");
            });
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }

    @Test
    void comments_shouldListTheInlineComments_whenAReviewCarriesThem() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String head = ENV.name("carried");
        SandboxBranches.createBranch(ENV, head, SandboxBranches.branchSha(ENV, base));
        final String path = "smoke/" + ENV.runId() + "-carried.txt";
        PullRequest pullRequest = null;
        try {
            SandboxBranches.commitFile(ENV, head, path);
            pullRequest = repository.createPullRequest(SandboxBranches.newPullRequest(head, base, ENV.name("carried")));
            final PullRequestReviewComment comment =
                    pullRequest.commentOnLine(pullRequest.response().head().sha(), path, 1, "carried remark");
            final long reviewId = comment.response().pullRequestReviewId();

            final PullRequestReview review = pullRequest.review(reviewId).orElseThrow();
            final List<Long> ids =
                    review.comments().map(PullRequestReviewComment::id).toList();
            comment.delete();

            assertThat(ids).contains(comment.id());
        } finally {
            if (pullRequest != null) {
                pullRequest.close();
            }
            SandboxBranches.deleteBranch(ENV, head);
        }
    }
}
