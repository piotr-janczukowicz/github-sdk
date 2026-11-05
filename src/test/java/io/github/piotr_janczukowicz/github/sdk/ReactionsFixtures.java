package io.github.piotr_janczukowicz.github.sdk;

import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionBuilder;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReactionContent;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;

final class ReactionsFixtures {

    static final long REACTION_ID = 7007L;

    private ReactionsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitHubReaction reaction() {
        return GitHubReactionBuilder.builder()
                .id(REACTION_ID)
                .nodeId("REA_kwDOA")
                .user(mock(GitHubSimpleUser.class))
                .content(GitHubReactionContent.HEART)
                .createdAt(Instant.parse("2026-02-03T04:05:06Z"))
                .build();
    }
}
