package io.github.piotr_janczukowicz.github.sdk.projects;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectField;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldDataType;
import java.io.IOException;
import java.util.Optional;

/**
 * A field of a {@link Project}. The REST API can list, read and add fields but not change or delete them, so the object
 * is read-only. {@link #response()} gives the complete response, including the options of a single select field.
 */
public final class ProjectField {

    private final Project project;
    private final GitHubProjectField response;

    ProjectField(final Project project, final GitHubProjectField response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the field belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the field id, which {@link ProjectItem#setField(long, String)} takes.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the node id, which GraphQL uses to address the field.
     *
     * @return the node id, or empty when the response has none
     */
    public Optional<String> nodeId() {
        return Optional.ofNullable(response.nodeId());
    }

    /**
     * Returns the field name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the type of the values of the field.
     *
     * @return the data type
     */
    public GitHubProjectFieldDataType dataType() {
        return response.dataType();
    }

    /**
     * Loads the field again. This object is not changed.
     *
     * @return the field as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the field is gone
     */
    public ProjectField refresh() throws IOException {
        return project.field(id()).orElseThrow(() -> new IllegalStateException("Field " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubProjectField response() {
        return response;
    }
}
