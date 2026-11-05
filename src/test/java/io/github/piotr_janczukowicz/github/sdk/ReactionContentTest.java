package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class ReactionContentTest {

    @Test
    void toClient_shouldMapEveryContentToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(ReactionContent.values())
                .allMatch(content -> content.toClient().name().equals(content.name()));

        assertThat(isEveryNameKept).isTrue();
    }

    @Test
    void fromClient_shouldMapEveryClientValueBack_whenCalled() {
        final boolean isEveryValueRestored = Arrays.stream(GitHubReactionContent.values())
                .allMatch(content -> ReactionContent.fromClient(content).toClient() == content);

        assertThat(isEveryValueRestored).isTrue();
    }

    @Test
    void values_shouldMatchTheClientValues_whenCompared() {
        assertThat(ReactionContent.values()).hasSameSizeAs(GitHubReactionContent.values());
    }
}
