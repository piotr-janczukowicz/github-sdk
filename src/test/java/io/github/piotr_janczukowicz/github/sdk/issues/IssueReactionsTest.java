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
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueEvent;
import io.github.piotr_janczukowicz.github.api.issues.GitHubTimelineEvent;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.IssuesFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReactionsFixtures;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueReactionsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 42;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final Issue sut = new Issue(repository, IssuesFixtures.issue());

    @Test
    void react_shouldSendTheContentAndReturnTheReaction_whenCalled() throws IOException {
        final GitHubReaction created = ReactionsFixtures.reaction();
        when(github.reactions().createForIssue(eq(OWNER), eq(NAME), eq(NUMBER), any()))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateForCommitCommentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateForCommitCommentPayload.class);

        final Reaction reaction = sut.react(ReactionContent.ROCKET);

        verify(github.reactions()).createForIssue(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().content()).isEqualTo(GitHubReactionContent.ROCKET);
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
        when(github.reactions().listForIssue(OWNER, NAME, NUMBER).stream()).thenReturn(Stream.of(listed));

        final List<Reaction> reactions = sut.reactions().toList();

        assertThat(reactions).singleElement().satisfies(reaction -> {
            assertThat(reaction.response()).isSameAs(listed);
        });
    }

    @Test
    void reactionDelete_shouldDeleteFromTheIssue_whenCalled() throws IOException {
        when(github.reactions().listForIssue(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(ReactionsFixtures.reaction()));
        final Reaction reaction = sut.reactions().toList().getFirst();

        reaction.delete();

        verify(github.reactions()).deleteForIssue(OWNER, NAME, NUMBER, ReactionsFixtures.REACTION_ID);
    }

    @Test
    void events_shouldStreamTheEvents_whenTheyExist() throws IOException {
        final GitHubIssueEvent event = mock(GitHubIssueEvent.class);
        when(github.issues().events().listEvents(OWNER, NAME, NUMBER).stream()).thenReturn(Stream.of(event));

        final List<GitHubIssueEvent> events = sut.events().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void timeline_shouldStreamTheEntries_whenTheyExist() throws IOException {
        final GitHubTimelineEvent entry = mock(GitHubTimelineEvent.class);
        when(github.issues().timeline().listEventsForTimeline(OWNER, NAME, NUMBER).stream())
                .thenReturn(Stream.of(entry));

        final List<GitHubTimelineEvent> timeline = sut.timeline().toList();

        assertThat(timeline).containsExactly(entry);
    }
}
