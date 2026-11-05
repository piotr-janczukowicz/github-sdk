package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesRulesApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleset;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetTarget;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRulesetVersion;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RulesetFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RulesetTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = RulesetFixtures.RULESET_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRepositoryRuleset response = RulesetFixtures.ruleset();
    private final Ruleset sut = new Ruleset(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenRulesetIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("protect-main");
            softly.assertThat(sut.target()).isEqualTo(GitHubRepositoryRulesetTarget.BRANCH);
            softly.assertThat(sut.enforcement()).isEqualTo(GitHubRepositoryRuleEnforcement.ACTIVE);
            softly.assertThat(sut.rules()).isEqualTo(response.rules());
            softly.assertThat(sut.bypassActors()).isEqualTo(response.bypassActors());
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenTheResponseHasNoRulesOrBypassActors() {
        final Ruleset bare = new Ruleset(repository, RulesetFixtures.rulesetWithoutRulesAndActors());

        assertSoftly(softly -> {
            softly.assertThat(bare.rules()).isEmpty();
            softly.assertThat(bare.bypassActors()).isEmpty();
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedRuleset_whenItExists() throws IOException {
        final GitHubRepositoryRuleset reloaded = RulesetFixtures.ruleset();
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(Optional.of(reloaded)).when(api).getRepoRuleset(OWNER, NAME, ID);

        final Ruleset result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheRulesetIsGone() throws IOException {
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(Optional.empty()).when(api).getRepoRuleset(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void edit_shouldStartAnUpdate_whenCalled() {
        final RulesetUpdate update = sut.edit();

        assertThat(update).isNotNull();
    }

    @Test
    void history_shouldStreamTheVersions_whenTheyExist() throws IOException {
        final GitHubRulesetVersion version = mock(GitHubRulesetVersion.class);
        when(github.repositories().rules().getRepoRulesetHistory(OWNER, NAME, ID).stream())
                .thenReturn(Stream.of(version));

        final List<GitHubRulesetVersion> versions = sut.history().toList();

        assertThat(versions).containsExactly(version);
    }

    @Test
    void delete_shouldDeleteTheRuleset_whenCalled() throws IOException {
        final GitHubRepositoriesRulesApi api = github.repositories().rules();

        sut.delete();

        verify(api).deleteRepoRuleset(OWNER, NAME, ID);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Ruleset(repository, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }
}
