package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesRuleSuitesApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesRulesApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleset;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRuleSuite;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRuleSuiteListItem;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RulesetFixtures;
import io.github.piotr_janczukowicz.github.sdk.rulesets.Ruleset;
import io.github.piotr_janczukowicz.github.sdk.rulesets.RulesetDraft;
import io.github.piotr_janczukowicz.github.sdk.rulesets.RulesetEnforcement;
import io.github.piotr_janczukowicz.github.sdk.rulesets.RulesetTarget;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryRulesetsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = RulesetFixtures.RULESET_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void rulesets_shouldLoadEachListedRuleset_whenTheyExist() throws IOException {
        final GitHubRepositoryRuleset listed = RulesetFixtures.ruleset();
        final GitHubRepositoryRuleset loaded = RulesetFixtures.ruleset();
        when(github.repositories().rules().getRepoRulesets(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(Optional.of(loaded)).when(api).getRepoRuleset(OWNER, NAME, ID);

        final List<Ruleset> rulesets = sut.rulesets().toList();

        assertThat(rulesets).singleElement().satisfies(ruleset -> {
            assertThat(ruleset.response()).isSameAs(loaded);
            assertThat(ruleset.repository()).isSameAs(sut);
        });
    }

    @Test
    void rulesets_shouldThrowIllegalStateException_whenAListedRulesetIsGone() throws IOException {
        final GitHubRepositoryRuleset listed = RulesetFixtures.ruleset();
        when(github.repositories().rules().getRepoRulesets(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(Optional.empty()).when(api).getRepoRuleset(OWNER, NAME, ID);
        final Stream<Ruleset> stream = sut.rulesets();

        final Throwable thrown = catchThrowable(stream::toList);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void rulesets_shouldThrowUncheckedIoException_whenLoadingAListedRulesetFails() throws IOException {
        final GitHubRepositoryRuleset listed = RulesetFixtures.ruleset();
        when(github.repositories().rules().getRepoRulesets(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        when(api.getRepoRuleset(OWNER, NAME, ID)).thenThrow(new IOException("boom"));
        final Stream<Ruleset> stream = sut.rulesets();

        final Throwable thrown = catchThrowable(stream::toList);

        assertThat(thrown).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void ruleset_shouldReturnTheRuleset_whenItExists() throws IOException {
        final GitHubRepositoryRuleset loaded = RulesetFixtures.ruleset();
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(Optional.of(loaded)).when(api).getRepoRuleset(OWNER, NAME, ID);

        final Optional<Ruleset> found = sut.ruleset(ID);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void ruleset_shouldBeEmpty_whenGitHubHasNone() throws IOException {
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(Optional.empty()).when(api).getRepoRuleset(OWNER, NAME, ID);

        final Optional<Ruleset> found = sut.ruleset(ID);

        assertThat(found).isEmpty();
    }

    @Test
    void newRuleset_shouldStartADraft_whenCalled() {
        final RulesetDraft draft = sut.newRuleset("protect-main", RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        assertThat(draft).isNotNull();
    }

    @Test
    void ruleSuites_shouldStreamTheSuites_whenTheyExist() throws IOException {
        final GitHubRuleSuiteListItem suite = mock(GitHubRuleSuiteListItem.class);
        when(github.repositories().ruleSuites().getRepoRuleSuites(OWNER, NAME).stream())
                .thenReturn(Stream.of(suite));

        final List<GitHubRuleSuiteListItem> suites = sut.ruleSuites().toList();

        assertThat(suites).containsExactly(suite);
    }

    @Test
    void ruleSuite_shouldReturnTheSuite_whenItExists() throws IOException {
        final GitHubRuleSuite suite = mock(GitHubRuleSuite.class);
        final GitHubRepositoriesRuleSuitesApi api = github.repositories().ruleSuites();
        doReturn(Optional.of(suite)).when(api).getRepoRuleSuite(OWNER, NAME, 5L);

        final Optional<GitHubRuleSuite> found = sut.ruleSuite(5L);

        assertSoftly(softly -> softly.assertThat(found).containsSame(suite));
    }
}
