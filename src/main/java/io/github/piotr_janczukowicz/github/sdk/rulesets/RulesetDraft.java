package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateRepoRulesetPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassActor;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetConditions;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetConditionsRefName;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Collects a new ruleset for a {@link Repository} and creates it in one request. Nothing is sent until
 * {@link #create()}. A branch or tag ruleset needs at least one {@link #includeRefs(String...)} pattern, otherwise
 * {@link #create()} fails before sending anything.
 */
public final class RulesetDraft {

    private final Repository repository;
    private final String name;
    private final RulesetTarget target;
    private final RulesetEnforcement enforcement;
    private final List<String> includedRefs = new ArrayList<>();
    private final List<String> excludedRefs = new ArrayList<>();
    private final List<GitHubRepositoryRule> rules = new ArrayList<>();
    private final List<GitHubRepositoryRulesetBypassActor> bypassActors = new ArrayList<>();

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param name the name
     * @param target the target
     * @param enforcement the enforcement
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a text argument is blank
     */
    public RulesetDraft(
            final Repository repository,
            final String name,
            final RulesetTarget target,
            final RulesetEnforcement enforcement) {
        this.repository = requireNonNull(repository, "repository");
        this.name = requireNotBlank(name, "name");
        this.target = requireNonNull(target, "target");
        this.enforcement = requireNonNull(enforcement, "enforcement");
    }

    /**
     * Applies the ruleset to the refs that match these patterns. A pattern is a {@code fnmatch} pattern such as
     * {@code refs/heads/release/*}, or {@code ~DEFAULT_BRANCH} for the default branch, or {@code ~ALL} for every ref.
     *
     * @param patterns the patterns to include
     * @return this draft
     * @throws NullPointerException if {@code patterns} is {@code null}
     */
    public RulesetDraft includeRefs(final String... patterns) {
        includedRefs.addAll(List.of(requireNonNull(patterns, "patterns")));
        return this;
    }

    /**
     * Leaves out the refs that match these patterns, which takes precedence over {@link #includeRefs(String...)}.
     *
     * @param patterns the patterns to exclude
     * @return this draft
     * @throws NullPointerException if {@code patterns} is {@code null}
     */
    public RulesetDraft excludeRefs(final String... patterns) {
        excludedRefs.addAll(List.of(requireNonNull(patterns, "patterns")));
        return this;
    }

    /**
     * Adds a rule. Build the common ones with {@link Rules}, or pass any client rule record.
     *
     * @param rule the rule
     * @return this draft
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    public RulesetDraft rule(final GitHubRepositoryRule rule) {
        rules.add(requireNonNull(rule, "rule"));
        return this;
    }

    /**
     * Lets an actor bypass the ruleset.
     *
     * @param type the kind of actor
     * @param actorId the id of the actor, see {@link BypassActorType} for what each kind uses
     * @param mode how the actor may bypass
     * @return this draft
     * @throws NullPointerException if {@code type} or {@code mode} is {@code null}
     */
    public RulesetDraft bypass(final BypassActorType type, final long actorId, final BypassMode mode) {
        bypassActors.add(new GitHubRepositoryRulesetBypassActor(
                actorId,
                requireNonNull(type, "type").toClient(),
                requireNonNull(mode, "mode").toClient()));
        return this;
    }

    /**
     * Lets deploy keys bypass the ruleset; a deploy key has no actor id.
     *
     * @param mode how deploy keys may bypass
     * @return this draft
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public RulesetDraft bypassDeployKeys(final BypassMode mode) {
        bypassActors.add(new GitHubRepositoryRulesetBypassActor(
                null,
                BypassActorType.DEPLOY_KEY.toClient(),
                requireNonNull(mode, "mode").toClient()));
        return this;
    }

    /**
     * Creates the ruleset ({@code POST /repos/{owner}/{repo}/rulesets}). This draft is not changed.
     *
     * @return the ruleset as GitHub holds it
     * @throws IllegalStateException if a branch or tag ruleset has no included ref pattern
     * @throws IOException if the transport fails
     */
    public Ruleset create() throws IOException {
        if (target != RulesetTarget.PUSH && includedRefs.isEmpty()) {
            throw new IllegalStateException("A " + target + " ruleset needs at least one included ref pattern");
        }
        final GitHub github = Internals.github(repository);
        return new Ruleset(
                repository,
                github.repositories().rules().createRepoRuleset(repository.owner(), repository.name(), payload()));
    }

    private GitHubCreateRepoRulesetPayload payload() {
        return new GitHubCreateRepoRulesetPayload(
                name,
                target.toClient(),
                enforcement.toClient(),
                bypassActors.isEmpty() ? null : List.copyOf(bypassActors),
                conditions(),
                List.copyOf(rules));
    }

    private @Nullable GitHubRepositoryRulesetConditions conditions() {
        if (includedRefs.isEmpty() && excludedRefs.isEmpty()) {
            return null;
        }
        return new GitHubRepositoryRulesetConditions(
                new GitHubRepositoryRulesetConditionsRefName(List.copyOf(includedRefs), List.copyOf(excludedRefs)));
    }
}
