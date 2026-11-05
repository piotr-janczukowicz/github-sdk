package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckRun;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckRun;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ChecksFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryChecksTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ChecksFixtures.CHECK_RUN_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void checkRun_shouldReturnTheCheckRun_whenGitHubHasIt() throws IOException {
        final GitHubCheckRun loaded = ChecksFixtures.checkRun();
        when(github.checks().runs().getCheckRun(OWNER, NAME, ID)).thenReturn(Optional.of(loaded));

        final Optional<CheckRun> found = sut.checkRun(ID);

        assertThat(found).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void checkRun_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        when(github.checks().runs().getCheckRun(OWNER, NAME, ID)).thenReturn(Optional.empty());

        final Optional<CheckRun> found = sut.checkRun(ID);

        assertThat(found).isEmpty();
    }
}
