package io.github.piotr_janczukowicz.github.sdk.deployments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubEnvironment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubEnvironmentProtectionRule;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;

/**
 * A deployment environment of a {@link Repository}, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Environment {

    private final Repository repository;
    private final GitHubEnvironment response;

    public Environment(final Repository repository, final GitHubEnvironment response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the environment belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the name of the environment.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the link to the environment on GitHub.
     *
     * @return the URL
     */
    public String htmlUrl() {
        return response.htmlUrl();
    }

    /**
     * Returns the protection rules of the environment as GitHub returns them.
     *
     * @return the rules, empty when GitHub reports none
     */
    public List<GitHubEnvironmentProtectionRule> protectionRules() {
        final List<GitHubEnvironmentProtectionRule> rules = response.protectionRules();
        return rules == null ? List.of() : rules;
    }

    /**
     * Loads the environment again ({@code GET /repos/{owner}/{repo}/environments/{environment_name}}). This object is
     * not changed.
     *
     * @return the environment as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the environment is gone
     */
    public Environment refresh() throws IOException {
        return repository
                .environment(name())
                .orElseThrow(() -> new IllegalStateException("Environment " + name() + " is gone"));
    }

    /**
     * Deletes the environment ({@code DELETE /repos/{owner}/{repo}/environments/{environment_name}}), together with its
     * secrets and variables.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.deployments().environments().deleteAnEnvironment(repository.owner(), repository.name(), name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubEnvironment response() {
        return response;
    }
}
