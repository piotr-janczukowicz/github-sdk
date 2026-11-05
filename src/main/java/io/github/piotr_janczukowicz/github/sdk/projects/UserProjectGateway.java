package io.github.piotr_janczukowicz.github.sdk.projects;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.projects.GitHubAddProjectFieldPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubAddProjectItemPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubCreateDraftItemPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubCreateProjectViewPayload;
import io.github.piotr_janczukowicz.github.api.projects.GitHubListProjectItemsFilter;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProject;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectField;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItem;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemSimple;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectView;
import io.github.piotr_janczukowicz.github.api.projects.GitHubUpdateProjectItemPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/** The project calls for a user. */
final class UserProjectGateway implements ProjectGateway {

    private final GitHub github;
    private final String login;

    UserProjectGateway(final GitHub github, final String login) {
        this.github = github;
        this.login = login;
    }

    @Override
    public Stream<GitHubProject> projects() throws IOException {
        return github.projects().project().listForUser(login).stream();
    }

    @Override
    public Optional<GitHubProject> project(final long number) throws IOException {
        return github.projects().project().getForUser(login, number);
    }

    @Override
    public Stream<GitHubProjectItem> items(final long project, final GitHubListProjectItemsFilter filter)
            throws IOException {
        return github.projects().items().listForUser(login, project, filter, GitHubPageRequest.byPage()).stream();
    }

    @Override
    public Optional<GitHubProjectItem> item(final long project, final long id) throws IOException {
        return github.projects().items().getForUser(login, project, id);
    }

    @Override
    public GitHubProjectItemSimple addItem(final long project, final GitHubAddProjectItemPayload payload)
            throws IOException {
        return github.projects().items().addForUser(login, project, payload);
    }

    @Override
    public GitHubProjectItem updateItem(final long project, final long id, final GitHubUpdateProjectItemPayload payload)
            throws IOException {
        return github.projects().items().updateForUser(login, project, id, payload);
    }

    @Override
    public void deleteItem(final long project, final long id) throws IOException {
        github.projects().items().deleteForUser(login, project, id);
    }

    @Override
    public Stream<GitHubProjectItem> viewItems(final long project, final long view) throws IOException {
        return github.projects().items().listViewItemsForUser(login, project, view).stream();
    }

    @Override
    public GitHubProjectItemSimple addDraft(final GitHubProject project, final GitHubCreateDraftItemPayload payload)
            throws IOException {
        return github.projects()
                .drafts()
                .createForAuthenticatedUser(String.valueOf(project.owner().id()), project.number(), payload);
    }

    @Override
    public Stream<GitHubProjectField> fields(final long project) throws IOException {
        return github.projects().fields().listForUser(login, project).stream();
    }

    @Override
    public Optional<GitHubProjectField> field(final long project, final long id) throws IOException {
        return github.projects().fields().getForUser(login, project, id);
    }

    @Override
    public GitHubProjectField addBasicField(final long project, final GitHubAddProjectFieldPayload.Basic payload)
            throws IOException {
        return github.projects().fields().addForUser(login, project, payload);
    }

    @Override
    public GitHubProjectField addSingleSelectField(
            final long project, final GitHubAddProjectFieldPayload.SingleSelect payload) throws IOException {
        return github.projects().fields().addForUser(login, project, payload);
    }

    @Override
    public GitHubProjectView createView(final long project, final GitHubCreateProjectViewPayload payload)
            throws IOException {
        return github.projects().views().createForUser(login, project, payload);
    }
}
