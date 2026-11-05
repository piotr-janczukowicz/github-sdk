package io.github.piotr_janczukowicz.github.sdk.content;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubAutolink;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;

/**
 * An autolink reference of a {@link Repository}, which turns text such as {@code TICKET-123} into a link to another
 * system. The object is immutable, and {@link #response()} gives the complete response.
 */
public final class Autolink {

    private final Repository repository;
    private final GitHubAutolink response;

    public Autolink(final Repository repository, final GitHubAutolink response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the autolink belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the autolink id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the text that starts a reference, for example {@code TICKET-}.
     *
     * @return the key prefix
     */
    public String keyPrefix() {
        return response.keyPrefix();
    }

    /**
     * Returns the link that the reference turns into, with {@code <num>} where the reference number goes.
     *
     * @return the URL template
     */
    public String urlTemplate() {
        return response.urlTemplate();
    }

    /**
     * Tells whether the reference may contain letters as well as digits.
     *
     * @return {@code true} when the reference is alphanumeric
     */
    public boolean isAlphanumeric() {
        return response.isAlphanumeric();
    }

    /**
     * Loads the autolink again ({@code GET /repos/{owner}/{repo}/autolinks/{autolink_id}}). This object is not changed.
     *
     * @return the autolink as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the autolink is gone
     */
    public Autolink refresh() throws IOException {
        return repository.autolink(id()).orElseThrow(() -> new IllegalStateException("Autolink " + id() + " is gone"));
    }

    /**
     * Deletes the autolink ({@code DELETE /repos/{owner}/{repo}/autolinks/{autolink_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final GitHub github = Internals.github(repository);
        github.repositories().autolinks().deleteAutolink(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubAutolink response() {
        return response;
    }
}
