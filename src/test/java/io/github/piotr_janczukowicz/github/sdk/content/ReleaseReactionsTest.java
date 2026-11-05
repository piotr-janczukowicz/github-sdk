package io.github.piotr_janczukowicz.github.sdk.content;

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
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForReleasePayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReleaseReactionContent;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ReactionsFixtures;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

@UnitTest
class ReleaseReactionsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = 9009L;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final Release sut = new Release(repository, RepositoryContentFixtures.release());

    @Test
    void react_shouldSendTheContentAndReturnTheReaction_whenCalled() throws IOException {
        final GitHubReaction created = ReactionsFixtures.reaction();
        when(github.reactions().createForRelease(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(created);
        final ArgumentCaptor<GitHubCreateForReleasePayload> payload =
                ArgumentCaptor.forClass(GitHubCreateForReleasePayload.class);

        final Reaction reaction = sut.react(GitHubReactionContent.EYES);

        verify(github.reactions()).createForRelease(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().content()).isEqualTo(GitHubReleaseReactionContent.EYES);
            softly.assertThat(reaction.response()).isSameAs(created);
        });
    }

    @Test
    void react_shouldThrowNullPointerException_whenContentIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.react(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
    }

    @ParameterizedTest
    @EnumSource(
            value = GitHubReactionContent.class,
            names = {"MINUS_ONE", "CONFUSED"})
    void react_shouldThrowIllegalArgumentException_whenTheReleaseDoesNotAcceptTheEmoji(
            final GitHubReactionContent content) {
        final Throwable thrown = catchThrowable(() -> sut.react(content));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(content.name());
    }

    @Test
    void reactions_shouldStreamTheReactions_whenTheyExist() throws IOException {
        final GitHubReaction listed = ReactionsFixtures.reaction();
        when(github.reactions().listForRelease(OWNER, NAME, ID).stream()).thenReturn(Stream.of(listed));

        final List<Reaction> reactions = sut.reactions().toList();

        assertThat(reactions)
                .singleElement()
                .satisfies(reaction -> assertThat(reaction.response()).isSameAs(listed));
    }

    @Test
    void reactions_shouldDeleteFromTheParent_whenAReactionIsDeleted() throws IOException {
        when(github.reactions().listForRelease(OWNER, NAME, ID).stream())
                .thenReturn(Stream.of(ReactionsFixtures.reaction()));
        final Reaction reaction = sut.reactions().toList().getFirst();

        reaction.delete();

        verify(github.reactions()).deleteForRelease(OWNER, NAME, ID, ReactionsFixtures.REACTION_ID);
    }
}
