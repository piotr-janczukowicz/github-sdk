package io.github.piotr_janczukowicz.github.sdk.projects;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItem;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemContentType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubUpdateProjectItemFieldPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubUpdateProjectItemPayload;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An item of a {@link Project}: an issue, a pull request or a draft issue. The object is immutable, and
 * {@link #response()} gives the complete response, including the field values.
 */
public final class ProjectItem {

    private final Project project;
    private final GitHubProjectItem response;

    ProjectItem(final Project project, final GitHubProjectItem response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the item belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the item id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the node id, which GraphQL uses to address the item.
     *
     * @return the node id, or empty when the response has none
     */
    public Optional<String> nodeId() {
        return Optional.ofNullable(response.nodeId());
    }

    /**
     * Returns what the item holds: an issue, a pull request or a draft issue.
     *
     * @return the content type
     */
    public GitHubProjectItemContentType contentType() {
        return response.contentType();
    }

    /**
     * Returns the content of the item as GitHub sent it, for example the title and number of an issue.
     *
     * @return the content properties, empty when the response has none
     */
    public Map<String, Object> content() {
        final Map<String, Object> value = response.content();
        return value == null ? Map.of() : value;
    }

    /**
     * Sets the value of a field and returns the item as GitHub holds it afterwards ({@code PATCH
     * /orgs/{org}/projectsV2/{project_number}/items/{item_id}} or the user variant). The value is text: for a number
     * field the number as text, for a date field an ISO date, for a single select field the id of the option. The item
     * this method is called on is not changed.
     *
     * @param fieldId the id of the field
     * @param value the new value
     * @return the item after the update
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IOException if the transport fails
     */
    public ProjectItem setField(final long fieldId, final String value) throws IOException {
        return update(fieldId, OptionalNull.of(requireNonNull(value, "value")));
    }

    /**
     * Clears the value of a field and returns the item as GitHub holds it afterwards. The item this method is called on
     * is not changed.
     *
     * @param fieldId the id of the field
     * @return the item after the update
     * @throws IOException if the transport fails
     */
    public ProjectItem clearField(final long fieldId) throws IOException {
        return update(fieldId, OptionalNull.ofNull());
    }

    /**
     * Loads the item again. This object is not changed.
     *
     * @return the item as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the item is gone
     */
    public ProjectItem refresh() throws IOException {
        return project.item(id()).orElseThrow(() -> new IllegalStateException("Item " + id() + " is gone"));
    }

    /**
     * Removes the item from the project ({@code DELETE /orgs/{org}/projectsV2/{project_number}/items/{item_id}} or the
     * user variant). The issue or pull request itself is not deleted.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        project.gateway().deleteItem(project.number(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubProjectItem response() {
        return response;
    }

    private ProjectItem update(final long fieldId, final OptionalNull<String> value) throws IOException {
        final GitHubUpdateProjectItemPayload payload =
                new GitHubUpdateProjectItemPayload(List.of(new GitHubUpdateProjectItemFieldPayload(fieldId, value)));
        return new ProjectItem(project, project.gateway().updateItem(project.number(), id(), payload));
    }
}
