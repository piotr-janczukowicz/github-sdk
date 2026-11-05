package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetConditions;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetConditionsRefName;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetTarget;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdateRepoRulesetPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes of a {@link Ruleset} and sends them in one request. Only what was chosen here is sent, so everything
 * else stays as it is. GitHub replaces the whole list of rules when one is sent, so {@link #addRule} and
 * {@link #removeRule} read the rules the ruleset holds at {@link #send()} time and send the changed list. A rule added
 * or removed by someone else between that read and the update is lost.
 */
public final class RulesetUpdate {

    private final Ruleset ruleset;
    private @Nullable String name;
    private @Nullable GitHubRepositoryRuleEnforcement enforcement;
    private @Nullable List<String> includedRefs;
    private @Nullable List<String> excludedRefs;
    private final List<GitHubRepositoryRule> addedRules = new ArrayList<>();
    private final List<Class<? extends GitHubRepositoryRule>> removedRules = new ArrayList<>();

    RulesetUpdate(final Ruleset ruleset) {
        this.ruleset = ruleset;
    }

    /**
     * Renames the ruleset.
     *
     * @param newName the new name
     * @return this update
     * @throws NullPointerException if {@code newName} is {@code null}
     * @throws IllegalArgumentException if {@code newName} is blank
     */
    public RulesetUpdate name(final String newName) {
        this.name = requireNotBlank(newName, "name");
        return this;
    }

    /**
     * Changes whether the ruleset is enforced, only evaluated, or disabled.
     *
     * @param newEnforcement the new enforcement
     * @return this update
     * @throws NullPointerException if {@code newEnforcement} is {@code null}
     */
    public RulesetUpdate enforcement(final GitHubRepositoryRuleEnforcement newEnforcement) {
        this.enforcement = requireNonNull(newEnforcement, "enforcement");
        return this;
    }

    /**
     * Replaces the ref patterns of the ruleset, both lists at once.
     *
     * @param include the patterns to include, see {@link RulesetDraft#includeRefs(String...)}
     * @param exclude the patterns to exclude
     * @return this update
     * @throws NullPointerException if {@code include}, {@code exclude} or a pattern is {@code null}
     * @throws IllegalArgumentException if a pattern is blank
     */
    public RulesetUpdate refs(final List<String> include, final List<String> exclude) {
        this.includedRefs = Arguments.requireNotBlankElements(include, "include");
        this.excludedRefs = Arguments.requireNotBlankElements(exclude, "exclude");
        return this;
    }

    /**
     * Adds a rule, and replaces the rule of the same type when the ruleset already has one, since GitHub allows each
     * type once.
     *
     * @param rule the rule, built with {@link Rules} or from any client rule record
     * @return this update
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    public RulesetUpdate addRule(final GitHubRepositoryRule rule) {
        addedRules.add(requireNonNull(rule, "rule"));
        return this;
    }

    /**
     * Removes the rule of a type, whether the ruleset already has it or it was added earlier in this update. It always
     * removes the existing rule of that type too; to cancel only a pending addition, do not call {@code addRule}.
     *
     * @param type the client record class of the rule, for example {@code GitHubRepositoryRuleDeletion.class}
     * @return this update
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public RulesetUpdate removeRule(final Class<? extends GitHubRepositoryRule> type) {
        removedRules.add(requireNonNull(type, "type"));
        addedRules.removeIf(rule -> rule.getClass() == type);
        return this;
    }

    /**
     * Sends the changes ({@code PUT /repos/{owner}/{repo}/rulesets/{ruleset_id}}). When rules were added or removed it
     * first loads the ruleset again to get the current rules. The ruleset this update was made from is not changed.
     *
     * @return the ruleset as GitHub holds it afterwards
     * @throws IOException if the transport fails
     * @throws IllegalStateException if rules were changed and the ruleset is gone, or the refs were replaced with an
     *     empty include list on a branch or tag ruleset
     */
    public Ruleset send() throws IOException {
        requireIncludedRefs();
        final Repository repository = ruleset.repository();
        return new Ruleset(
                repository,
                github().repositories()
                        .rules()
                        .updateRepoRuleset(repository.owner(), repository.name(), ruleset.id(), payload()));
    }

    private GitHubUpdateRepoRulesetPayload payload() throws IOException {
        return new GitHubUpdateRepoRulesetPayload(
                name, null, enforcement, null, conditions(), areRulesChanged() ? changedRules() : null);
    }

    private @Nullable GitHubRepositoryRulesetConditions conditions() {
        if (includedRefs == null || excludedRefs == null) {
            return null;
        }
        return new GitHubRepositoryRulesetConditions(
                new GitHubRepositoryRulesetConditionsRefName(includedRefs, excludedRefs));
    }

    private boolean areRulesChanged() {
        return !addedRules.isEmpty() || !removedRules.isEmpty();
    }

    private List<GitHubRepositoryRule> changedRules() throws IOException {
        final List<GitHubRepositoryRule> result =
                new ArrayList<>(ruleset.refresh().rules());
        result.removeIf(rule -> isOfRemovedType(rule) || isReplaced(rule));
        result.addAll(addedRules);
        return List.copyOf(result);
    }

    private boolean isOfRemovedType(final GitHubRepositoryRule rule) {
        return removedRules.contains(rule.getClass());
    }

    private boolean isReplaced(final GitHubRepositoryRule rule) {
        return addedRules.stream().anyMatch(added -> added.getClass() == rule.getClass());
    }

    private void requireIncludedRefs() {
        final GitHubRepositoryRulesetTarget target = ruleset.target();
        final boolean isRefTarget =
                target == GitHubRepositoryRulesetTarget.BRANCH || target == GitHubRepositoryRulesetTarget.TAG;
        if (isRefTarget && includedRefs != null && includedRefs.isEmpty()) {
            throw new IllegalStateException("A " + target + " ruleset needs at least one included ref pattern");
        }
    }

    private GitHub github() {
        return Internals.github(ruleset.repository());
    }
}
