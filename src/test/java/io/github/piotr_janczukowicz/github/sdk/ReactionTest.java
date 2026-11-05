package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

@UnitTest
class ReactionTest {

    private final AtomicLong deletedId = new AtomicLong();
    private final GitHubReaction response = ReactionsFixtures.reaction();
    private final Reaction sut = new Reaction(response, deletedId::set);

    @Test
    void accessors_shouldExposeTheResponse_whenReactionIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ReactionsFixtures.REACTION_ID);
            softly.assertThat(sut.content()).isEqualTo(ReactionContent.HEART);
            softly.assertThat(sut.user()).containsSame(response.user());
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void user_shouldBeEmpty_whenTheResponseHasNoUser() {
        final GitHubReaction withoutUser =
                GitHubReactionBuilder.builder(response).user(null).build();
        final Reaction reaction = new Reaction(withoutUser, deletedId::set);

        assertThat(reaction.user()).isEmpty();
    }

    @Test
    void delete_shouldRemoveTheReactionById_whenCalled() throws IOException {
        sut.delete();

        assertThat(deletedId).hasValue(ReactionsFixtures.REACTION_ID);
    }

    @Test
    void delete_shouldPassTheFailureOn_whenTheDeletionFails() {
        final IOException failure = new IOException("transport failed");
        final Reaction failing = new Reaction(response, reactionId -> {
            throw failure;
        });

        final Throwable thrown = catchThrowable(failing::delete);

        assertThat(thrown).isSameAs(failure);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenDeletionIsNull() {
        final Throwable thrown = catchThrowable(() -> new Reaction(response, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("deletion");
    }
}
