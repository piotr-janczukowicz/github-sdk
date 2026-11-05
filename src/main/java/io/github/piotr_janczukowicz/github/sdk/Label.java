package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.issues.GitHubLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateLabelPayload;
import java.io.IOException;
import java.util.Optional;

/**
 * A label of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #record()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Label {

    private final Repository repository;
    private final GitHubLabel record;

    Label(final Repository repository, final GitHubLabel record) {
        this.repository = requireNonNull(repository, "repository");
        this.record = requireNonNull(record, "record");
    }

    /**
     * Returns the repository the label belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the label name.
     *
     * @return the name
     */
    public String name() {
        return record.name();
    }

    /**
     * Returns the label color as a hexadecimal value without the leading {@code #}.
     *
     * @return the color
     */
    public String color() {
        return record.color();
    }

    /**
     * Returns the label description.
     *
     * @return the description, or empty when the label has none
     */
    public Optional<String> description() {
        return Optional.ofNullable(record.description());
    }

    /**
     * Tells whether the label is one of GitHub's default labels.
     *
     * @return {@code true} for a default label
     */
    public boolean isDefault() {
        return record.isDefault();
    }

    /**
     * Deletes the label ({@code DELETE /repos/{owner}/{repo}/labels/{name}}). GitHub removes it from every issue.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        repository.sdk().github().issues().labels().deleteLabel(repository.owner(), repository.name(), name());
    }

    /**
     * Starts a change of several fields at once; nothing is sent until {@link LabelUpdate#send()}.
     *
     * @return a new update for this label
     */
    public LabelUpdate update() {
        return new LabelUpdate(this);
    }

    /**
     * Loads the label again ({@code GET /repos/{owner}/{repo}/labels/{name}}). This object is not changed.
     *
     * @return the label as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the label is gone
     */
    public Label refresh() throws IOException {
        return repository.label(name()).orElseThrow(() -> new IllegalStateException("Label " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubLabel record() {
        return record;
    }

    Label apply(final GitHubUpdateLabelPayload payload) throws IOException {
        return new Label(
                repository,
                repository
                        .sdk()
                        .github()
                        .issues()
                        .labels()
                        .updateLabel(repository.owner(), repository.name(), name(), payload));
    }
}
