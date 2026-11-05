package io.github.piotr_janczukowicz.github.sdk.projects;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.projects.GitHubAddProjectFieldPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubAddProjectItemPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubCreateDraftItemPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubCreateProjectViewPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubListProjectItemsFilter;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProject;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldDataType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldSingleSelectColor;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldSingleSelectOption;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemContentType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectState;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectViewLayout;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A project (Projects v2) of an organization or user. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover. The REST API cannot create, change or delete a
 * project, so there are no such methods; items, fields and views can be managed.
 */
public final class Project {

    private final ProjectGateway gateway;
    private final GitHubProject response;

    Project(final ProjectGateway gateway, final GitHubProject response) {
        this.gateway = requireNonNull(gateway, "gateway");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project number, which is the key in the URLs and in every call.
     *
     * @return the number
     */
    public long number() {
        return response.number();
    }

    /**
     * Returns the node id, which GraphQL uses to address the project.
     *
     * @return the node id
     */
    public String nodeId() {
        return response.nodeId();
    }

    /**
     * Returns the login of the organization or user that owns the project.
     *
     * @return the owner login
     */
    public String ownerLogin() {
        return response.owner().login();
    }

    /**
     * Returns the project title.
     *
     * @return the title
     */
    public String title() {
        return response.title();
    }

    /**
     * Returns whether the project is open or closed.
     *
     * @return the state
     */
    public GitHubProjectState state() {
        return response.state();
    }

    /**
     * Tells whether everyone can see the project.
     *
     * @return {@code true} when public
     */
    public boolean isPublic() {
        return response.isPublic();
    }

    /**
     * Loads the project again ({@code GET /orgs/{org}/projectsV2/{project_number}} or the user variant). This object is
     * not changed.
     *
     * @return the project as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the project is gone
     */
    public Project refresh() throws IOException {
        return gateway.project(number())
                .map(loaded -> new Project(gateway, loaded))
                .orElseThrow(() -> new IllegalStateException("Project " + number() + " is gone"));
    }

    /**
     * Lists the items of the project ({@code GET /orgs/{org}/projectsV2/{project_number}/items} or the user variant).
     * The stream is lazy and can be used once.
     *
     * @return the items
     * @throws IOException if the transport fails
     */
    public Stream<ProjectItem> items() throws IOException {
        return itemsMatching(GitHubListProjectItemsFilter.empty());
    }

    /**
     * Lists the items that match a search query, in the syntax of the project filter bar, for example {@code is:issue
     * label:bug}.
     *
     * @param query the query
     * @return the matching items
     * @throws NullPointerException if {@code query} is {@code null}
     * @throws IllegalArgumentException if {@code query} is blank
     * @throws IOException if the transport fails
     */
    public Stream<ProjectItem> items(final String query) throws IOException {
        return itemsMatching(new GitHubListProjectItemsFilter(requireNotBlank(query, "query"), null));
    }

    /**
     * Loads one item ({@code GET /orgs/{org}/projectsV2/{project_number}/items/{item_id}} or the user variant).
     *
     * @param id the item id
     * @return the item, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ProjectItem> item(final long id) throws IOException {
        return gateway.item(number(), id).map(loaded -> new ProjectItem(this, loaded));
    }

    /**
     * Adds an issue to the project ({@code POST /orgs/{org}/projectsV2/{project_number}/items} or the user variant).
     *
     * @param issue the issue
     * @return the new item as GitHub holds it
     * @throws NullPointerException if {@code issue} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not report the item it just added
     */
    public ProjectItem addIssue(final Issue issue) throws IOException {
        requireNonNull(issue, "issue");
        return add(GitHubAddProjectItemPayload.byRepository(
                GitHubProjectItemContentType.ISSUE,
                issue.repository().owner(),
                issue.repository().name(),
                issue.number()));
    }

    /**
     * Adds a pull request to the project.
     *
     * @param pullRequest the pull request
     * @return the new item as GitHub holds it
     * @throws NullPointerException if {@code pullRequest} is {@code null}
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not report the item it just added
     */
    public ProjectItem addPullRequest(final PullRequest pullRequest) throws IOException {
        requireNonNull(pullRequest, "pullRequest");
        return add(GitHubAddProjectItemPayload.byRepository(
                GitHubProjectItemContentType.PULL_REQUEST,
                pullRequest.repository().owner(),
                pullRequest.repository().name(),
                pullRequest.number()));
    }

    /**
     * Adds a draft issue to the project ({@code POST /orgs/{org}/projectsV2/{project_number}/drafts}). For a user
     * project, GitHub accepts it only for the authenticated user.
     *
     * @param title the title of the draft
     * @param body the body of the draft, or {@code null} for none
     * @return the new item as GitHub holds it
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not report the item it just added
     */
    public ProjectItem addDraft(final String title, final @Nullable String body) throws IOException {
        final long id = gateway.addDraft(
                        response, new GitHubCreateDraftItemPayload(requireNotBlank(title, "title"), body))
                .id();
        return loaded(id);
    }

    /**
     * Lists the fields of the project ({@code GET /orgs/{org}/projectsV2/{project_number}/fields} or the user variant).
     * The stream is lazy and can be used once.
     *
     * @return the fields
     * @throws IOException if the transport fails
     */
    public Stream<ProjectField> fields() throws IOException {
        return gateway.fields(number()).map(listed -> new ProjectField(this, listed));
    }

    /**
     * Loads one field.
     *
     * @param id the field id
     * @return the field, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ProjectField> field(final long id) throws IOException {
        return gateway.field(number(), id).map(loaded -> new ProjectField(this, loaded));
    }

    /**
     * Adds a text field.
     *
     * @param name the field name
     * @return the new field
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public ProjectField addTextField(final String name) throws IOException {
        return addBasicField(name, GitHubProjectFieldDataType.TEXT);
    }

    /**
     * Adds a number field.
     *
     * @param name the field name
     * @return the new field
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public ProjectField addNumberField(final String name) throws IOException {
        return addBasicField(name, GitHubProjectFieldDataType.NUMBER);
    }

    /**
     * Adds a date field.
     *
     * @param name the field name
     * @return the new field
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public ProjectField addDateField(final String name) throws IOException {
        return addBasicField(name, GitHubProjectFieldDataType.DATE);
    }

    /**
     * Adds a single select field whose options are gray and have no description; GitHub's UI can change that later.
     *
     * @param name the field name
     * @param options the option names
     * @return the new field
     * @throws NullPointerException if {@code name} or {@code options} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public ProjectField addSingleSelectField(final String name, final List<String> options) throws IOException {
        final List<GitHubProjectFieldSingleSelectOption> choices = requireNonNull(options, "options").stream()
                .map(option ->
                        new GitHubProjectFieldSingleSelectOption(option, GitHubProjectFieldSingleSelectColor.GRAY, ""))
                .toList();
        return new ProjectField(
                this,
                gateway.addSingleSelectField(
                        number(), GitHubAddProjectFieldPayload.singleSelect(requireNotBlank(name, "name"), choices)));
    }

    /**
     * Creates a view ({@code POST /orgs/{org}/projectsV2/{project_number}/views} or the user variant) with the default
     * filter, sorting and grouping.
     *
     * @param name the view name
     * @param layout the layout
     * @return the new view
     * @throws NullPointerException if {@code name} or {@code layout} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public ProjectView createView(final String name, final GitHubProjectViewLayout layout) throws IOException {
        return new ProjectView(
                this,
                gateway.createView(
                        number(),
                        new GitHubCreateProjectViewPayload(
                                requireNotBlank(name, "name"),
                                requireNonNull(layout, "layout"),
                                null,
                                null,
                                null,
                                null,
                                null)));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubProject response() {
        return response;
    }

    ProjectGateway gateway() {
        return gateway;
    }

    private Stream<ProjectItem> itemsMatching(final GitHubListProjectItemsFilter filter) throws IOException {
        return gateway.items(number(), filter).map(listed -> new ProjectItem(this, listed));
    }

    private ProjectItem add(final GitHubAddProjectItemPayload payload) throws IOException {
        return loaded(gateway.addItem(number(), payload).id());
    }

    private ProjectItem loaded(final long id) throws IOException {
        return item(id).orElseThrow(() -> new IllegalStateException("Item " + id + " was added but is not found"));
    }

    private ProjectField addBasicField(final String name, final GitHubProjectFieldDataType type) throws IOException {
        return new ProjectField(
                this,
                gateway.addBasicField(
                        number(), GitHubAddProjectFieldPayload.basic(requireNotBlank(name, "name"), type)));
    }
}
