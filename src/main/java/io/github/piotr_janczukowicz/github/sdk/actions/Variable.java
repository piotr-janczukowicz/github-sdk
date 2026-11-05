package io.github.piotr_janczukowicz.github.sdk.actions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubUpdateRepoVariablePayload;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsVariable;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * An Actions variable of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()}
 * gives the complete response.
 */
public final class Variable {

    private final Repository repository;
    private final GitHubActionsVariable response;

    public Variable(final Repository repository, final GitHubActionsVariable response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the variable belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the name of the variable.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the value of the variable.
     *
     * @return the value
     */
    public String value() {
        return response.value();
    }

    /**
     * Loads the variable again ({@code GET /repos/{owner}/{repo}/actions/variables/{name}}). This object is not
     * changed.
     *
     * @return the variable as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the variable is gone
     */
    public Variable refresh() throws IOException {
        final GitHub github = Internals.github(repository);
        return github.actions()
                .variables()
                .getRepoVariable(repository.owner(), repository.name(), name())
                .map(reloaded -> new Variable(repository, reloaded))
                .orElseThrow(() -> new IllegalStateException("Variable " + name() + " is gone"));
    }

    /**
     * Changes the value ({@code PATCH /repos/{owner}/{repo}/actions/variables/{name}}). This object is not changed.
     *
     * @param value the new value
     * @return the variable as GitHub holds it afterwards
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the variable is gone
     */
    public Variable update(final String value) throws IOException {
        final GitHub github = Internals.github(repository);
        github.actions()
                .variables()
                .updateRepoVariable(
                        repository.owner(),
                        repository.name(),
                        name(),
                        new GitHubUpdateRepoVariablePayload(name(), requireNonNull(value, "value")));
        return refresh();
    }

    /**
     * Deletes the variable ({@code DELETE /repos/{owner}/{repo}/actions/variables/{name}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.actions().variables().deleteRepoVariable(repository.owner(), repository.name(), name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubActionsVariable response() {
        return response;
    }
}
