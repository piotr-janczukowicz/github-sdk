package io.github.piotr_janczukowicz.github.sdk.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProjectFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class UserProjectGatewayTest {

    private static final String LOGIN = "octocat";
    private static final long NUMBER = ProjectFixtures.NUMBER;
    private static final long ID = ProjectFixtures.ITEM_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final UserProjectGateway sut = new UserProjectGateway(github, LOGIN);

    @Test
    void projects_shouldStreamTheProjects_whenTheyExist() throws IOException {
        final GitHubProject project = ProjectFixtures.project();
        when(github.projects().project().listForUser(LOGIN).stream()).thenReturn(Stream.of(project));

        final List<GitHubProject> projects = sut.projects().toList();

        assertThat(projects).containsExactly(project);
    }

    @Test
    void project_shouldReturnTheProject_whenItExists() throws IOException {
        final GitHubProject project = ProjectFixtures.project();
        when(github.projects().project().getForUser(LOGIN, NUMBER)).thenReturn(Optional.of(project));

        final Optional<GitHubProject> found = sut.project(NUMBER);

        assertThat(found).containsSame(project);
    }

    @Test
    void items_shouldStreamTheItems_whenTheyExist() throws IOException {
        final GitHubProjectItem item = ProjectFixtures.item();
        final GitHubListProjectItemsFilter filter = GitHubListProjectItemsFilter.empty();
        when(github
                        .projects()
                        .items()
                        .listForUser(eq(LOGIN), eq(NUMBER), eq(filter), eq(GitHubPageRequest.byCursor()))
                        .stream())
                .thenReturn(Stream.of(item));

        final List<GitHubProjectItem> items = sut.items(NUMBER, filter).toList();

        assertThat(items).containsExactly(item);
    }

    @Test
    void item_shouldReturnTheItem_whenItExists() throws IOException {
        final GitHubProjectItem item = ProjectFixtures.item();
        when(github.projects().items().getForUser(LOGIN, NUMBER, ID)).thenReturn(Optional.of(item));

        final Optional<GitHubProjectItem> found = sut.item(NUMBER, ID);

        assertThat(found).containsSame(item);
    }

    @Test
    void addItem_shouldReturnTheAddedItem() throws IOException {
        final GitHubAddProjectItemPayload payload = mock(GitHubAddProjectItemPayload.class);
        final GitHubProjectItemSimple added = ProjectFixtures.itemSimple();
        when(github.projects().items().addForUser(LOGIN, NUMBER, payload)).thenReturn(added);

        final GitHubProjectItemSimple result = sut.addItem(NUMBER, payload);

        assertThat(result).isSameAs(added);
    }

    @Test
    void updateItem_shouldReturnTheUpdatedItem() throws IOException {
        final GitHubUpdateProjectItemPayload payload = mock(GitHubUpdateProjectItemPayload.class);
        final GitHubProjectItem updated = ProjectFixtures.item();
        when(github.projects().items().updateForUser(LOGIN, NUMBER, ID, payload))
                .thenReturn(updated);

        final GitHubProjectItem result = sut.updateItem(NUMBER, ID, payload);

        assertThat(result).isSameAs(updated);
    }

    @Test
    void deleteItem_shouldDeleteTheItem() throws IOException {
        sut.deleteItem(NUMBER, ID);

        verify(github.projects().items()).deleteForUser(LOGIN, NUMBER, ID);
    }

    @Test
    void viewItems_shouldStreamTheItems_whenTheyExist() throws IOException {
        final GitHubProjectItem item = ProjectFixtures.item();
        when(github.projects().items().listViewItemsForUser(LOGIN, NUMBER, 3L).stream())
                .thenReturn(Stream.of(item));

        final List<GitHubProjectItem> items = sut.viewItems(NUMBER, 3L).toList();

        assertThat(items).containsExactly(item);
    }

    @Test
    void addDraft_shouldReturnTheDraftItem() throws IOException {
        final GitHubCreateDraftItemPayload payload = new GitHubCreateDraftItemPayload("Idea", null);
        final GitHubProjectItemSimple added = ProjectFixtures.itemSimple();
        final GitHubProject project = ProjectFixtures.project();
        when(github.projects()
                        .drafts()
                        .createForAuthenticatedUser(String.valueOf(ProjectFixtures.OWNER_ID), NUMBER, payload))
                .thenReturn(added);

        final GitHubProjectItemSimple result = sut.addDraft(project, payload);

        assertThat(result).isSameAs(added);
    }

    @Test
    void fields_shouldStreamTheFields_whenTheyExist() throws IOException {
        final GitHubProjectField field = ProjectFixtures.field();
        when(github.projects().fields().listForUser(LOGIN, NUMBER).stream()).thenReturn(Stream.of(field));

        final List<GitHubProjectField> fields = sut.fields(NUMBER).toList();

        assertThat(fields).containsExactly(field);
    }

    @Test
    void field_shouldReturnTheField_whenItExists() throws IOException {
        final GitHubProjectField field = ProjectFixtures.field();
        when(github.projects().fields().getForUser(LOGIN, NUMBER, ProjectFixtures.FIELD_ID))
                .thenReturn(Optional.of(field));

        final Optional<GitHubProjectField> found = sut.field(NUMBER, ProjectFixtures.FIELD_ID);

        assertThat(found).containsSame(field);
    }

    @Test
    void addBasicField_shouldReturnTheField() throws IOException {
        final GitHubAddProjectFieldPayload.Basic payload = mock(GitHubAddProjectFieldPayload.Basic.class);
        final GitHubProjectField field = ProjectFixtures.field();
        when(github.projects().fields().addForUser(LOGIN, NUMBER, payload)).thenReturn(field);

        final GitHubProjectField result = sut.addBasicField(NUMBER, payload);

        assertThat(result).isSameAs(field);
    }

    @Test
    void addSingleSelectField_shouldReturnTheField() throws IOException {
        final GitHubAddProjectFieldPayload.SingleSelect payload = mock(GitHubAddProjectFieldPayload.SingleSelect.class);
        final GitHubProjectField field = ProjectFixtures.field();
        when(github.projects().fields().addForUser(LOGIN, NUMBER, payload)).thenReturn(field);

        final GitHubProjectField result = sut.addSingleSelectField(NUMBER, payload);

        assertThat(result).isSameAs(field);
    }

    @Test
    void createView_shouldReturnTheView() throws IOException {
        final GitHubCreateProjectViewPayload payload = mock(GitHubCreateProjectViewPayload.class);
        final GitHubProjectView view = ProjectFixtures.view();
        when(github.projects().views().createForUser(LOGIN, NUMBER, payload)).thenReturn(view);

        final GitHubProjectView result = sut.createView(NUMBER, payload);

        assertThat(result).isSameAs(view);
    }
}
