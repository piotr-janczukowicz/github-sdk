package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesRulesApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleDeletion;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleset;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetTarget;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdateRepoRulesetPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RulesetFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RulesetUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = RulesetFixtures.RULESET_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRepositoriesRulesApi api = github.repositories().rules();
    private final GitHubRepositoryRule held = Rules.requireLinearHistory();
    private final Ruleset ruleset = new Ruleset(repository, RulesetFixtures.rulesetWith(List.of(held)));

    @Test
    void send_shouldSendOnlyTheChosenFields_whenNoRuleIsChanged() throws IOException {
        final GitHubRepositoryRuleset updated = RulesetFixtures.ruleset();
        doReturn(updated).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());
        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        final Ruleset result = ruleset.edit()
                .name("renamed")
                .enforcement(GitHubRepositoryRuleEnforcement.EVALUATE)
                .refs(List.of("~ALL"), List.of("refs/heads/x"))
                .send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(payload.getValue().name()).isEqualTo("renamed");
            softly.assertThat(payload.getValue().enforcement()).isEqualTo(GitHubRepositoryRuleEnforcement.EVALUATE);
            softly.assertThat(payload.getValue().target()).isNull();
            softly.assertThat(payload.getValue().rules()).isNull();
            softly.assertThat(payload.getValue().conditions().refName().include())
                    .containsExactly("~ALL");
            softly.assertThat(payload.getValue().conditions().refName().exclude())
                    .containsExactly("refs/heads/x");
        });
    }

    @Test
    void send_shouldSendNothingElse_whenNothingIsChosen() throws IOException {
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());
        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        ruleset.edit().send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isNull();
            softly.assertThat(payload.getValue().enforcement()).isNull();
            softly.assertThat(payload.getValue().conditions()).isNull();
            softly.assertThat(payload.getValue().rules()).isNull();
        });
    }

    @Test
    void send_shouldKeepTheHeldRulesAndAddTheNewOne_whenARuleIsAdded() throws IOException {
        doReturn(Optional.of(RulesetFixtures.rulesetWith(List.of(held))))
                .when(api)
                .getRepoRuleset(OWNER, NAME, ID);
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());
        final GitHubRepositoryRule added = Rules.restrictDeletion();
        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        ruleset.edit().addRule(added).send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertThat(payload.getValue().rules()).containsExactly(held, added);
    }

    @Test
    void send_shouldReplaceTheRuleOfTheSameType_whenARuleIsAddedAgain() throws IOException {
        final GitHubRepositoryRule oldPullRequest = Rules.requirePullRequest(1);
        doReturn(Optional.of(RulesetFixtures.rulesetWith(List.of(held, oldPullRequest))))
                .when(api)
                .getRepoRuleset(OWNER, NAME, ID);
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());
        final GitHubRepositoryRule newPullRequest = Rules.requirePullRequest(2);
        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        ruleset.edit().addRule(newPullRequest).send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertThat(payload.getValue().rules()).containsExactly(held, newPullRequest);
    }

    @Test
    void send_shouldDropTheRuleOfThatType_whenARuleIsRemoved() throws IOException {
        final GitHubRepositoryRule deletion = Rules.restrictDeletion();
        doReturn(Optional.of(RulesetFixtures.rulesetWith(List.of(held, deletion))))
                .when(api)
                .getRepoRuleset(OWNER, NAME, ID);
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());
        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        ruleset.edit().removeRule(GitHubRepositoryRuleDeletion.class).send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertThat(payload.getValue().rules()).containsExactly(held);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenRulesChangeAndTheRulesetIsGone() throws IOException {
        doReturn(Optional.empty()).when(api).getRepoRuleset(OWNER, NAME, ID);
        final RulesetUpdate sut = ruleset.edit().addRule(Rules.restrictDeletion());

        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final RulesetUpdate sut = ruleset.edit();

        final Throwable thrown = catchThrowable(() -> sut.name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void enforcement_shouldThrowNullPointerException_whenEnforcementIsNull() {
        final RulesetUpdate sut = ruleset.edit();

        final Throwable thrown = catchThrowable(() -> sut.enforcement(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("enforcement");
    }

    @Test
    void refs_shouldThrowNullPointerException_whenIncludeIsNull() {
        final RulesetUpdate sut = ruleset.edit();

        final Throwable thrown = catchThrowable(() -> sut.refs(null, List.of()));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("include");
    }

    @Test
    void refs_shouldThrowNullPointerException_whenExcludeIsNull() {
        final RulesetUpdate sut = ruleset.edit();

        final Throwable thrown = catchThrowable(() -> sut.refs(List.of(), null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("exclude");
    }

    @Test
    void addRule_shouldThrowNullPointerException_whenRuleIsNull() {
        final RulesetUpdate sut = ruleset.edit();

        final Throwable thrown = catchThrowable(() -> sut.addRule(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("rule");
    }

    @Test
    void removeRule_shouldThrowNullPointerException_whenTypeIsNull() {
        final RulesetUpdate sut = ruleset.edit();

        final Throwable thrown = catchThrowable(() -> sut.removeRule(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    @Test
    void send_shouldNotLoadTheRuleset_whenNoRuleIsChanged() throws IOException {
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());

        ruleset.edit().name("renamed").send();

        verify(api, never()).getRepoRuleset(eq(OWNER), eq(NAME), anyLong());
    }

    @Test
    void refs_shouldThrowIllegalArgumentException_whenAPatternIsBlank() {
        final RulesetUpdate update = ruleset.edit();
        final List<String> include = List.of(" ");
        final List<String> exclude = List.of();

        final Throwable thrown = catchThrowable(() -> update.refs(include, exclude));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("include");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenARefRulesetGetsNoIncludedRef() {
        final RulesetUpdate update = ruleset.edit().refs(List.of(), List.of());

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("at least one included ref");
    }

    @Test
    void removeRule_shouldDropARuleAddedEarlierInTheSameUpdate_whenTheTypeMatches() throws IOException {
        doReturn(Optional.of(RulesetFixtures.rulesetWith(List.of(held))))
                .when(api)
                .getRepoRuleset(OWNER, NAME, ID);
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());
        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        ruleset.edit()
                .addRule(Rules.restrictDeletion())
                .removeRule(GitHubRepositoryRuleDeletion.class)
                .send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertThat(payload.getValue().rules()).containsExactly(held);
    }

    @Test
    void send_shouldNotRequireIncludedRefs_whenThePushRulesetHasNoRefTarget() throws IOException {
        final Ruleset pushRuleset = new Ruleset(repository, rulesetWithTarget(GitHubRepositoryRulesetTarget.PUSH));
        doReturn(RulesetFixtures.ruleset()).when(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), any());

        final ArgumentCaptor<GitHubUpdateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateRepoRulesetPayload.class);

        pushRuleset.edit().refs(List.of(), List.of()).send();

        verify(api).updateRepoRuleset(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertThat(payload.getValue().conditions().refName().include()).isEmpty();
    }

    @Test
    void send_shouldThrowIllegalStateException_whenATagRulesetGetsNoIncludedRef() {
        final Ruleset tagRuleset = new Ruleset(repository, rulesetWithTarget(GitHubRepositoryRulesetTarget.TAG));
        final RulesetUpdate update = tagRuleset.edit().refs(List.of(), List.of());

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("TAG");
    }

    private static GitHubRepositoryRuleset rulesetWithTarget(final GitHubRepositoryRulesetTarget target) {
        final GitHubRepositoryRuleset base = mock(GitHubRepositoryRuleset.class);
        lenient().when(base.id()).thenReturn(ID);
        lenient().when(base.target()).thenReturn(target);
        return base;
    }
}
