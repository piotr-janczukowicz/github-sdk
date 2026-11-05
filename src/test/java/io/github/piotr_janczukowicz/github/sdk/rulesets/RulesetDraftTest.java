package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateRepoRulesetPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesRulesApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleset;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassActorType;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassMode;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetPayloadTarget;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RulesetFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RulesetDraftTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String RULESET = "protect-main";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void create_shouldSendEverythingChosenAndReturnTheRuleset_whenCalled() throws IOException {
        final GitHubRepositoryRuleset created = RulesetFixtures.ruleset();
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(created).when(api).createRepoRuleset(eq(OWNER), eq(NAME), any(GitHubCreateRepoRulesetPayload.class));
        final GitHubRepositoryRule rule = Rules.restrictDeletion();
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE)
                .includeRefs("~DEFAULT_BRANCH")
                .excludeRefs("refs/heads/legacy")
                .rule(rule)
                .bypass(BypassActorType.TEAM, 7L, BypassMode.ALWAYS)
                .bypassDeployKeys(BypassMode.PULL_REQUEST);
        final ArgumentCaptor<GitHubCreateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateRepoRulesetPayload.class);

        final Ruleset result = sut.create();

        verify(api).createRepoRuleset(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(payload.getValue().name()).isEqualTo(RULESET);
            softly.assertThat(payload.getValue().target()).isEqualTo(GitHubRepositoryRulesetPayloadTarget.BRANCH);
            softly.assertThat(payload.getValue().enforcement()).isEqualTo(GitHubRepositoryRuleEnforcement.ACTIVE);
            softly.assertThat(payload.getValue().rules()).containsExactly(rule);
            softly.assertThat(payload.getValue().conditions().refName().include())
                    .containsExactly("~DEFAULT_BRANCH");
            softly.assertThat(payload.getValue().conditions().refName().exclude())
                    .containsExactly("refs/heads/legacy");
            softly.assertThat(payload.getValue().bypassActors()).hasSize(2);
            softly.assertThat(payload.getValue().bypassActors().getFirst().actorId())
                    .isEqualTo(7L);
            softly.assertThat(payload.getValue().bypassActors().getFirst().actorType())
                    .isEqualTo(GitHubRepositoryRulesetBypassActorType.TEAM);
            softly.assertThat(payload.getValue().bypassActors().getFirst().bypassMode())
                    .isEqualTo(GitHubRepositoryRulesetBypassMode.ALWAYS);
            softly.assertThat(payload.getValue().bypassActors().getLast().actorId())
                    .isNull();
            softly.assertThat(payload.getValue().bypassActors().getLast().actorType())
                    .isEqualTo(GitHubRepositoryRulesetBypassActorType.DEPLOY_KEY);
            softly.assertThat(payload.getValue().bypassActors().getLast().bypassMode())
                    .isEqualTo(GitHubRepositoryRulesetBypassMode.PULL_REQUEST);
        });
    }

    @Test
    void create_shouldOmitConditionsAndBypassActors_whenAPushRulesetHasNone() throws IOException {
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(RulesetFixtures.ruleset())
                .when(api)
                .createRepoRuleset(eq(OWNER), eq(NAME), any(GitHubCreateRepoRulesetPayload.class));
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.PUSH, RulesetEnforcement.EVALUATE);
        final ArgumentCaptor<GitHubCreateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateRepoRulesetPayload.class);

        sut.create();

        verify(api).createRepoRuleset(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().target()).isEqualTo(GitHubRepositoryRulesetPayloadTarget.PUSH);
            softly.assertThat(payload.getValue().enforcement()).isEqualTo(GitHubRepositoryRuleEnforcement.EVALUATE);
            softly.assertThat(payload.getValue().conditions()).isNull();
            softly.assertThat(payload.getValue().bypassActors()).isNull();
            softly.assertThat(payload.getValue().rules()).isEmpty();
        });
    }

    @Test
    void create_shouldSendTheExcludedRefs_whenOnlyExclusionsAreGiven() throws IOException {
        final GitHubRepositoriesRulesApi api = github.repositories().rules();
        doReturn(RulesetFixtures.ruleset())
                .when(api)
                .createRepoRuleset(eq(OWNER), eq(NAME), any(GitHubCreateRepoRulesetPayload.class));
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.PUSH, RulesetEnforcement.DISABLED)
                .excludeRefs("refs/heads/x");
        final ArgumentCaptor<GitHubCreateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateRepoRulesetPayload.class);

        sut.create();

        verify(api).createRepoRuleset(eq(OWNER), eq(NAME), payload.capture());
        assertThat(payload.getValue().conditions().refName().exclude()).containsExactly("refs/heads/x");
    }

    @Test
    void create_shouldThrowIllegalStateException_whenATagRulesetHasNoIncludedRef() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.TAG, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(sut::create);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("TAG");
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(
                () -> new RulesetDraft(repository, " ", RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenTargetIsNull() {
        final Throwable thrown =
                catchThrowable(() -> new RulesetDraft(repository, RULESET, null, RulesetEnforcement.ACTIVE));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("target");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenEnforcementIsNull() {
        final Throwable thrown =
                catchThrowable(() -> new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("enforcement");
    }

    @Test
    void includeRefs_shouldThrowNullPointerException_whenPatternsAreNull() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(() -> sut.includeRefs((String[]) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("patterns");
    }

    @Test
    void excludeRefs_shouldThrowNullPointerException_whenPatternsAreNull() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(() -> sut.excludeRefs((String[]) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("patterns");
    }

    @Test
    void rule_shouldThrowNullPointerException_whenRuleIsNull() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(() -> sut.rule(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("rule");
    }

    @Test
    void bypass_shouldThrowNullPointerException_whenTypeIsNull() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(() -> sut.bypass(null, 1L, BypassMode.ALWAYS));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    @Test
    void bypass_shouldThrowNullPointerException_whenModeIsNull() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(() -> sut.bypass(BypassActorType.TEAM, 1L, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("mode");
    }

    @Test
    void bypassDeployKeys_shouldThrowNullPointerException_whenModeIsNull() {
        final RulesetDraft sut = new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(() -> sut.bypassDeployKeys(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("mode");
    }

    @Test
    void enums_shouldMapEveryValueToTheClient_whenUsed() {
        final List<Object> mapped = List.of(
                BypassActorType.INTEGRATION.toClient(),
                BypassActorType.REPOSITORY_ROLE.toClient(),
                BypassActorType.ORGANIZATION_ADMIN.toClient());

        assertThat(mapped)
                .containsExactly(
                        GitHubRepositoryRulesetBypassActorType.INTEGRATION,
                        GitHubRepositoryRulesetBypassActorType.REPOSITORY_ROLE,
                        GitHubRepositoryRulesetBypassActorType.ORGANIZATION_ADMIN);
    }

    @Test
    void includeRefs_shouldThrowIllegalArgumentException_whenAPatternIsBlank() {
        final Throwable thrown = catchThrowable(
                () -> new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE)
                        .includeRefs(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("patterns");
    }

    @Test
    void excludeRefs_shouldThrowIllegalArgumentException_whenAPatternIsBlank() {
        final Throwable thrown = catchThrowable(
                () -> new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE)
                        .excludeRefs(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("patterns");
    }

    @Test
    void bypass_shouldThrowIllegalArgumentException_whenTheTypeIsADeployKey() {
        final Throwable thrown = catchThrowable(
                () -> new RulesetDraft(repository, RULESET, RulesetTarget.BRANCH, RulesetEnforcement.ACTIVE)
                        .bypass(BypassActorType.DEPLOY_KEY, 5L, BypassMode.ALWAYS));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("deploy key");
    }
}
