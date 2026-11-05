package io.github.piotr_janczukowicz.github.sdk.pulls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PullRequestsFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReactionsFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReviewsFixtures;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullRequestReviewCommentReactionsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ReviewsFixtures.COMMENT_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final PullRequest pullRequest = new PullRequest(repository, PullRequestsFixtures.pullRequest());
    private final PullRequestReviewComment sut = new PullRequestReviewComment(pullRequest, ReviewsFixtures.comment());

    @Test
    void react_shouldSendTheContentAndReturnTheReaction_whenCalled() throws IOException {
        final GitHubReaction created = ReactionsFixtures.reaction();
        when(github.reactions().createForPullRequestReviewComment(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateForCommitCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateForCommitCommentPayload.class);

        final Reaction reaction = sut.react(ReactionContent.PLUS_ONE);

        verify(github.reactions()).createForPullRequestReviewComment(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().content()).isEqualTo(GitHubReactionContent.PLUS_ONE);
            softly.assertThat(reaction.response()).isSameAs(created);
        });
    }

    @Test
    void react_shouldThrowNullPointerException_whenContentIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.react(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
    }

    @Test
    void reactions_shouldStreamTheReactions_whenTheyExist() throws IOException {
        final GitHubReaction listed = ReactionsFixtures.reaction();
        when(github.reactions().listForPullRequestReviewComment(OWNER, NAME, ID).stream())
                .thenReturn(Stream.of(listed));

        final List<Reaction> reactions = sut.reactions().toList();

        assertThat(reactions)
                .singleElement()
                .satisfies(reaction -> assertThat(reaction.response()).isSameAs(listed));
    }

    @Test
    void reactionDelete_shouldDeleteFromTheComment_whenCalled() throws IOException {
        when(github.reactions().listForPullRequestReviewComment(OWNER, NAME, ID).stream())
                .thenReturn(Stream.of(ReactionsFixtures.reaction()));
        final Reaction reaction = sut.reactions().toList().getFirst();

        reaction.delete();

        verify(github.reactions()).deleteForPullRequestComment(OWNER, NAME, ID, ReactionsFixtures.REACTION_ID);
    }
}
