package io.github.piotr_janczukowicz.github.sdk.projects;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectView;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectViewLayout;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * A view of a {@link Project}, such as a table or a board. The REST API can create a view and list its items but not
 * list, change or delete views. {@link #response()} gives the complete response.
 */
public final class ProjectView {

    private final Project project;
    private final GitHubProjectView response;

    ProjectView(final Project project, final GitHubProjectView response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the view belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the view number.
     *
     * @return the number
     */
    public int number() {
        return response.number();
    }

    /**
     * Returns the node id, which GraphQL uses to address the view.
     *
     * @return the node id
     */
    public String nodeId() {
        return response.nodeId();
    }

    /**
     * Returns the view name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the layout of the view.
     *
     * @return the layout
     */
    public GitHubProjectViewLayout layout() {
        return response.layout();
    }

    /**
     * Lists the items the view shows, with its filter applied ({@code GET
     * /orgs/{org}/projectsV2/{project_number}/views/{view_number}/items} or the user variant). The stream is lazy and
     * can be used once.
     *
     * @return the items
     * @throws IOException if the transport fails
     */
    public Stream<ProjectItem> items() throws IOException {
        return project.gateway().viewItems(project.number(), number()).map(listed -> new ProjectItem(project, listed));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubProjectView response() {
        return response;
    }
}
