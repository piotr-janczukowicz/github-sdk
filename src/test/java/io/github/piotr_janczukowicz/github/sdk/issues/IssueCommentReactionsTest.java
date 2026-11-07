package io.github.piotr_janczukowicz.github.sdk.issues;

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
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReactionsFixtures;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueCommentReactionsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 2002L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Issue issue = new Issue(GitHubSdk.of(github).repository(OWNER, NAME), IssuesFixtures.issue());
    private final IssueComment sut = new IssueComment(issue, IssuesFixtures.issueComment());

    @Test
    void react_shouldSendTheContentAndReturnTheReaction_whenCalled() throws IOException {
        final GitHubReaction created = ReactionsFixtures.reaction();
        when(github.reactions().createForIssueComment(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateForCommitCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateForCommitCommentPayload.class);

        final Reaction reaction = sut.react(GitHubReactionContent.EYES);

        verify(github.reactions()).createForIssueComment(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().content()).isEqualTo(GitHubReactionContent.EYES);
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
        when(github.reactions().listForIssueComment(OWNER, NAME, ID).stream()).thenReturn(Stream.of(listed));

        final List<Reaction> reactions = sut.reactions().toList();

        assertThat(reactions)
                .singleElement()
                .satisfies(reaction -> assertThat(reaction.response()).isSameAs(listed));
    }

    @Test
    void delete_shouldDeleteFromTheComment_whenCalled() throws IOException {
        when(github.reactions().listForIssueComment(OWNER, NAME, ID).stream())
                .thenReturn(Stream.of(ReactionsFixtures.reaction()));
        final Reaction reaction = sut.reactions().toList().getFirst();

        reaction.delete();

        verify(github.reactions()).deleteForIssueComment(OWNER, NAME, ID, ReactionsFixtures.REACTION_ID);
    }
}
