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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        final RulesetDraft sut = new RulesetDraft(
                        repository,
                        RULESET,
                        GitHubRepositoryRulesetPayloadTarget.BRANCH,
                        GitHubRepositoryRuleEnforcement.ACTIVE)
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
        final RulesetDraft sut = new RulesetDraft(
                repository,
                RULESET,
                GitHubRepositoryRulesetPayloadTarget.PUSH,
                GitHubRepositoryRuleEnforcement.EVALUATE);
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
        final RulesetDraft sut = new RulesetDraft(
                        repository,
                        RULESET,
                        GitHubRepositoryRulesetPayloadTarget.PUSH,
                        GitHubRepositoryRuleEnforcement.DISABLED)
                .excludeRefs("refs/heads/x");
        final ArgumentCaptor<GitHubCreateRepoRulesetPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateRepoRulesetPayload.class);

        sut.create();

        verify(api).createRepoRuleset(eq(OWNER), eq(NAME), payload.capture());
        assertThat(payload.getValue().conditions().refName().exclude()).containsExactly("refs/heads/x");
    }

    @Test
    void create_shouldThrowIllegalStateException_whenATagRulesetHasNoIncludedRef() {
        final RulesetDraft sut = new RulesetDraft(
                repository, RULESET, GitHubRepositoryRulesetPayloadTarget.TAG, GitHubRepositoryRuleEnforcement.ACTIVE);

        final Throwable thrown = catchThrowable(sut::create);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("TAG");
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> new RulesetDraft(
                repository, " ", GitHubRepositoryRulesetPayloadTarget.BRANCH, GitHubRepositoryRuleEnforcement.ACTIVE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenTargetIsNull() {
        final Throwable thrown = catchThrowable(
                () -> new RulesetDraft(repository, RULESET, null, GitHubRepositoryRuleEnforcement.ACTIVE));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("target");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenEnforcementIsNull() {
        final Throwable thrown = catchThrowable(
                () -> new RulesetDraft(repository, RULESET, GitHubRepositoryRulesetPayloadTarget.BRANCH, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("enforcement");
    }

    @Test
    void includeRefs_shouldThrowNullPointerException_whenPatternsAreNull() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.includeRefs((String[]) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("patterns");
    }

    @Test
    void excludeRefs_shouldThrowNullPointerException_whenPatternsAreNull() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.excludeRefs((String[]) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("patterns");
    }

    @Test
    void rule_shouldThrowNullPointerException_whenRuleIsNull() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.rule(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("rule");
    }

    @Test
    void bypass_shouldThrowNullPointerException_whenTypeIsNull() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.bypass(null, 1L, BypassMode.ALWAYS));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    @Test
    void bypass_shouldThrowNullPointerException_whenModeIsNull() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.bypass(BypassActorType.TEAM, 1L, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("mode");
    }

    @Test
    void bypassDeployKeys_shouldThrowNullPointerException_whenModeIsNull() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.bypassDeployKeys(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("mode");
    }

    @ParameterizedTest
    @MethodSource("actorTypes")
    void toClient_shouldMapTheValueToTheClient_whenUsed(
            final BypassActorType type, final GitHubRepositoryRulesetBypassActorType expected) {
        final GitHubRepositoryRulesetBypassActorType client = type.toClient();

        assertThat(client).isEqualTo(expected);
    }

    private static Stream<Arguments> actorTypes() {
        return Stream.of(
                Arguments.of(BypassActorType.INTEGRATION, GitHubRepositoryRulesetBypassActorType.INTEGRATION),
                Arguments.of(BypassActorType.REPOSITORY_ROLE, GitHubRepositoryRulesetBypassActorType.REPOSITORY_ROLE),
                Arguments.of(BypassActorType.TEAM, GitHubRepositoryRulesetBypassActorType.TEAM),
                Arguments.of(BypassActorType.DEPLOY_KEY, GitHubRepositoryRulesetBypassActorType.DEPLOY_KEY),
                Arguments.of(
                        BypassActorType.ORGANIZATION_ADMIN, GitHubRepositoryRulesetBypassActorType.ORGANIZATION_ADMIN));
    }

    @Test
    void includeRefs_shouldThrowIllegalArgumentException_whenAPatternIsBlank() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.includeRefs(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("patterns");
    }

    @Test
    void excludeRefs_shouldThrowIllegalArgumentException_whenAPatternIsBlank() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.excludeRefs(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("patterns");
    }

    @Test
    void bypass_shouldThrowIllegalArgumentException_whenTheTypeIsADeployKey() {
        final RulesetDraft sut = branchDraft();

        final Throwable thrown = catchThrowable(() -> sut.bypass(BypassActorType.DEPLOY_KEY, 5L, BypassMode.ALWAYS));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("deploy key");
    }

    private RulesetDraft branchDraft() {
        return new RulesetDraft(
                repository,
                RULESET,
                GitHubRepositoryRulesetPayloadTarget.BRANCH,
                GitHubRepositoryRuleEnforcement.ACTIVE);
    }
}
