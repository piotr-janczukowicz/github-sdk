package io.github.piotr_janczukowicz.github.sdk.rulesets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleEnforcement;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRuleset;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetBypassActor;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryRulesetTarget;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRulesetVersion;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A ruleset of a {@link Repository}: a named set of rules that applies to branches, tags or pushes. Rulesets are the
 * newer replacement of branch protection, and both can apply to the same branch. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover, such as the
 * conditions.
 */
public final class Ruleset {

    private final Repository repository;
    private final GitHubRepositoryRuleset response;

    public Ruleset(final Repository repository, final GitHubRepositoryRuleset response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the ruleset belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the ruleset id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the ruleset name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns what the ruleset applies to.
     *
     * @return the target
     */
    public GitHubRepositoryRulesetTarget target() {
        return response.target();
    }

    /**
     * Returns whether the ruleset is enforced, only evaluated, or disabled.
     *
     * @return the enforcement
     */
    public GitHubRepositoryRuleEnforcement enforcement() {
        return response.enforcement();
    }

    /**
     * Returns the rules of the ruleset.
     *
     * @return the rules, empty when the ruleset has none
     */
    public List<GitHubRepositoryRule> rules() {
        return Objects.requireNonNullElse(response.rules(), List.of());
    }

    /**
     * Returns the actors that may bypass the ruleset.
     *
     * @return the bypass actors, empty when there are none
     */
    public List<GitHubRepositoryRulesetBypassActor> bypassActors() {
        return Objects.requireNonNullElse(response.bypassActors(), List.of());
    }

    /**
     * Loads the ruleset again ({@code GET /repos/{owner}/{repo}/rulesets/{ruleset_id}}). This object is not changed.
     *
     * @return the ruleset as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the ruleset is gone
     */
    public Ruleset refresh() throws IOException {
        return repository.ruleset(id()).orElseThrow(() -> new IllegalStateException("Ruleset " + id() + " is gone"));
    }

    /**
     * Starts a change of the ruleset; nothing is sent until {@link RulesetUpdate#send()}.
     *
     * @return a new update for this ruleset
     */
    public RulesetUpdate edit() {
        return new RulesetUpdate(this);
    }

    /**
     * Lists the earlier versions of the ruleset, newest first ({@code GET
     * /repos/{owner}/{repo}/rulesets/{ruleset_id}/history}). The stream is lazy and can be used once.
     *
     * @return the versions with who changed the ruleset and when
     * @throws IOException if the transport fails
     */
    public Stream<GitHubRulesetVersion> history() throws IOException {
        final GitHub github = Internals.github(repository);
        return github
                .repositories()
                .rules()
                .getRepoRulesetHistory(repository.owner(), repository.name(), id())
                .stream();
    }

    /**
     * Deletes the ruleset ({@code DELETE /repos/{owner}/{repo}/rulesets/{ruleset_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.repositories().rules().deleteRepoRuleset(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubRepositoryRuleset response() {
        return response;
    }
}
