package io.github.piotr_janczukowicz.github.sdk.projects;

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
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The calls for the projects of one owner. GitHub has one set of endpoints for organizations and another for users, and
 * this interface hides which one is used, so the project objects do not care about the owner kind.
 */
interface ProjectGateway {

    Stream<GitHubProject> projects() throws IOException;

    Optional<GitHubProject> project(long number) throws IOException;

    Stream<GitHubProjectItem> items(long project, GitHubListProjectItemsFilter filter) throws IOException;

    Optional<GitHubProjectItem> item(long project, long id) throws IOException;

    GitHubProjectItemSimple addItem(long project, GitHubAddProjectItemPayload payload) throws IOException;

    GitHubProjectItem updateItem(long project, long id, GitHubUpdateProjectItemPayload payload) throws IOException;

    void deleteItem(long project, long id) throws IOException;

    Stream<GitHubProjectItem> viewItems(long project, long view) throws IOException;

    GitHubProjectItemSimple addDraft(GitHubProject project, GitHubCreateDraftItemPayload payload) throws IOException;

    Stream<GitHubProjectField> fields(long project) throws IOException;

    Optional<GitHubProjectField> field(long project, long id) throws IOException;

    GitHubProjectField addBasicField(long project, GitHubAddProjectFieldPayload.Basic payload) throws IOException;

    GitHubProjectField addSingleSelectField(long project, GitHubAddProjectFieldPayload.SingleSelect payload)
            throws IOException;

    GitHubProjectView createView(long project, GitHubCreateProjectViewPayload payload) throws IOException;
}
