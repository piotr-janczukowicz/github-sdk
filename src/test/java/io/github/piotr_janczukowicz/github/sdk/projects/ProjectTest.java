package io.github.piotr_janczukowicz.github.sdk.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldDataType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldSingleSelectOption;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItem;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemContentType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemSimple;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectState;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectView;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectViewLayout;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProjectFixtures;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectTest {

    private static final long NUMBER = ProjectFixtures.NUMBER;
    private static final long ITEM_ID = ProjectFixtures.ITEM_ID;

    private final ProjectGateway gateway = mock(ProjectGateway.class);
    private final GitHubProject response = ProjectFixtures.project();
    private final GitHubProjectItemSimple added = ProjectFixtures.itemSimple();
    private final Project sut = new Project(gateway, response);
    private final Repository repository =
            GitHubSdk.of(mock(GitHub.class, RETURNS_DEEP_STUBS)).repository("octocat", "hello-world");

    @Test
    void accessors_shouldExposeTheResponse_whenProjectIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.number()).isEqualTo(NUMBER);
            softly.assertThat(sut.nodeId()).contains("PVT_1");
            softly.assertThat(sut.ownerLogin()).isEqualTo("octocat");
            softly.assertThat(sut.title()).isEqualTo("Roadmap");
            softly.assertThat(sut.state()).contains(GitHubProjectState.OPEN);
            softly.assertThat(sut.isPublic()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenGatewayIsNull() {
        final Throwable thrown = catchThrowable(() -> new Project(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("gateway");
    }

    @Test
    void refresh_shouldReturnTheReloadedProject_whenItExists() throws IOException {
        final GitHubProject reloaded = ProjectFixtures.project();
        when(gateway.project(NUMBER)).thenReturn(Optional.of(reloaded));

        final Project result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheProjectIsGone() throws IOException {
        when(gateway.project(NUMBER)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void items_shouldStreamTheItemsWithoutAFilter_whenCalled() throws IOException {
        final GitHubProjectItem listed = ProjectFixtures.item();
        when(gateway.items(eq(NUMBER), any())).thenReturn(Stream.of(listed));
        final ArgumentCaptor<GitHubListProjectItemsFilter> filter =
                ArgumentCaptor.forClass(GitHubListProjectItemsFilter.class);

        final List<ProjectItem> items = sut.items().toList();

        verify(gateway).items(eq(NUMBER), filter.capture());
        assertSoftly(softly -> {
            softly.assertThat(items).singleElement().satisfies(item -> {
                assertThat(item.response()).isSameAs(listed);
                assertThat(item.project()).isSameAs(sut);
            });
            softly.assertThat(filter.getValue().q()).isNull();
        });
    }

    @Test
    void items_shouldSendTheQuery_whenOneIsGiven() throws IOException {
        when(gateway.items(eq(NUMBER), any())).thenReturn(Stream.empty());
        final ArgumentCaptor<GitHubListProjectItemsFilter> filter =
                ArgumentCaptor.forClass(GitHubListProjectItemsFilter.class);

        final List<ProjectItem> found = sut.items("is:issue").toList();
        assertThat(found).isEmpty();

        verify(gateway).items(eq(NUMBER), filter.capture());
        assertThat(filter.getValue().q()).isEqualTo("is:issue");
    }

    @Test
    void items_shouldThrowIllegalArgumentException_whenQueryIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.items(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
    }

    @Test
    void item_shouldReturnTheItem_whenItExists() throws IOException {
        final GitHubProjectItem loaded = ProjectFixtures.item();
        when(gateway.item(NUMBER, ITEM_ID)).thenReturn(Optional.of(loaded));

        final Optional<ProjectItem> found = sut.item(ITEM_ID);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void addIssue_shouldAddByRepositoryAndReturnTheItem_whenCalled() throws IOException {
        final Issue issue = mock(Issue.class);
        when(issue.repository()).thenReturn(repository);
        when(issue.number()).thenReturn(12);
        final GitHubProjectItem loaded = ProjectFixtures.item();
        when(gateway.addItem(eq(NUMBER), any())).thenReturn(added);
        when(gateway.item(NUMBER, ITEM_ID)).thenReturn(Optional.of(loaded));
        final ArgumentCaptor<GitHubAddProjectItemPayload> payload =
                ArgumentCaptor.forClass(GitHubAddProjectItemPayload.class);

        final ProjectItem result = sut.addIssue(issue);

        verify(gateway).addItem(eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(loaded);
            softly.assertThat(payload.getValue().type()).isEqualTo(GitHubProjectItemContentType.ISSUE);
            softly.assertThat(payload.getValue().owner()).isEqualTo("octocat");
            softly.assertThat(payload.getValue().repo()).isEqualTo("hello-world");
            softly.assertThat(payload.getValue().number()).isEqualTo(12);
        });
    }

    @Test
    void addPullRequest_shouldAddByRepositoryAndReturnTheItem_whenCalled() throws IOException {
        final PullRequest pullRequest = mock(PullRequest.class);
        when(pullRequest.repository()).thenReturn(repository);
        when(pullRequest.number()).thenReturn(34);
        final GitHubProjectItem loaded = ProjectFixtures.item();
        when(gateway.addItem(eq(NUMBER), any())).thenReturn(added);
        when(gateway.item(NUMBER, ITEM_ID)).thenReturn(Optional.of(loaded));
        final ArgumentCaptor<GitHubAddProjectItemPayload> payload =
                ArgumentCaptor.forClass(GitHubAddProjectItemPayload.class);

        final ProjectItem result = sut.addPullRequest(pullRequest);

        verify(gateway).addItem(eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(loaded);
            softly.assertThat(payload.getValue().type()).isEqualTo(GitHubProjectItemContentType.PULL_REQUEST);
            softly.assertThat(payload.getValue().number()).isEqualTo(34);
        });
    }

    @Test
    void addIssue_shouldThrowIllegalStateException_whenTheAddedItemIsNotFound() throws IOException {
        final Issue issue = mock(Issue.class);
        when(issue.repository()).thenReturn(repository);
        when(issue.number()).thenReturn(12);
        when(gateway.addItem(eq(NUMBER), any())).thenReturn(added);
        when(gateway.item(NUMBER, ITEM_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.addIssue(issue));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("not found");
    }

    @Test
    void addDraft_shouldSendTitleAndBodyAndReturnTheItem_whenCalled() throws IOException {
        final GitHubProjectItem loaded = ProjectFixtures.item();
        when(gateway.addDraft(eq(response), any())).thenReturn(added);
        when(gateway.item(NUMBER, ITEM_ID)).thenReturn(Optional.of(loaded));
        final ArgumentCaptor<GitHubCreateDraftItemPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDraftItemPayload.class);

        final ProjectItem result = sut.addDraft("Idea", "Details");

        verify(gateway).addDraft(eq(response), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(loaded);
            softly.assertThat(payload.getValue().title()).isEqualTo("Idea");
            softly.assertThat(payload.getValue().body()).isEqualTo("Details");
        });
    }

    @Test
    void addDraft_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addDraft(" ", null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void addIssue_shouldThrowNullPointerException_whenIssueIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addIssue(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("issue");
    }

    @Test
    void addPullRequest_shouldThrowNullPointerException_whenPullRequestIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addPullRequest(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("pullRequest");
    }

    @Test
    void fields_shouldStreamTheFields_whenTheyExist() throws IOException {
        final GitHubProjectField listed = ProjectFixtures.field();
        when(gateway.fields(NUMBER)).thenReturn(Stream.of(listed));

        final List<ProjectField> fields = sut.fields().toList();

        assertThat(fields).singleElement().satisfies(field -> {
            assertThat(field.response()).isSameAs(listed);
            assertThat(field.project()).isSameAs(sut);
        });
    }

    @Test
    void field_shouldReturnTheField_whenItExists() throws IOException {
        final GitHubProjectField loaded = ProjectFixtures.field();
        when(gateway.field(NUMBER, ProjectFixtures.FIELD_ID)).thenReturn(Optional.of(loaded));

        final Optional<ProjectField> found = sut.field(ProjectFixtures.FIELD_ID);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void addFields_shouldSendTheMatchingDataType_whenTextNumberAndDateFieldsAreAdded() throws IOException {
        final GitHubProjectField created = ProjectFixtures.field();
        when(gateway.addBasicField(eq(NUMBER), any())).thenReturn(created);
        final ArgumentCaptor<GitHubAddProjectFieldPayload.Basic> payload =
                ArgumentCaptor.forClass(GitHubAddProjectFieldPayload.Basic.class);

        final ProjectField text = sut.addTextField("Notes");
        sut.addNumberField("Points");
        sut.addDateField("Due");

        verify(gateway, times(3)).addBasicField(eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(text.response()).isSameAs(created);
            softly.assertThat(payload.getAllValues())
                    .extracting(GitHubAddProjectFieldPayload.Basic::dataType)
                    .containsExactly(
                            GitHubProjectFieldDataType.TEXT,
                            GitHubProjectFieldDataType.NUMBER,
                            GitHubProjectFieldDataType.DATE);
        });
    }

    @Test
    void addTextField_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addTextField(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void addSingleSelectField_shouldSendTheOptions_whenCalled() throws IOException {
        final GitHubProjectField created = ProjectFixtures.field();
        when(gateway.addSingleSelectField(eq(NUMBER), any())).thenReturn(created);
        final ArgumentCaptor<GitHubAddProjectFieldPayload.SingleSelect> payload =
                ArgumentCaptor.forClass(GitHubAddProjectFieldPayload.SingleSelect.class);

        final ProjectField result = sut.addSingleSelectField("Status", List.of("Todo", "Done"));

        verify(gateway).addSingleSelectField(eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(payload.getValue().name()).isEqualTo("Status");
            softly.assertThat(payload.getValue().singleSelectOptions())
                    .extracting(GitHubProjectFieldSingleSelectOption::name)
                    .containsExactly("Todo", "Done");
        });
    }

    @Test
    void addSingleSelectField_shouldThrowNullPointerException_whenOptionsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.addSingleSelectField("Status", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("options");
    }

    @Test
    void createView_shouldSendNameAndLayout_whenCalled() throws IOException {
        final GitHubProjectView created = ProjectFixtures.view();
        when(gateway.createView(eq(NUMBER), any())).thenReturn(created);
        final ArgumentCaptor<GitHubCreateProjectViewPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateProjectViewPayload.class);

        final ProjectView result = sut.createView("Board", GitHubProjectViewLayout.BOARD);

        verify(gateway).createView(eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(payload.getValue().name()).isEqualTo("Board");
            softly.assertThat(payload.getValue().layout()).isEqualTo(GitHubProjectViewLayout.BOARD);
            softly.assertThat(payload.getValue().filter()).isNull();
        });
    }

    @Test
    void createView_shouldThrowNullPointerException_whenLayoutIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createView("Board", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("layout");
    }

    @Test
    void addSingleSelectField_shouldThrowIllegalArgumentException_whenOptionsAreEmptyOrBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addSingleSelectField("Status", List.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("options");
    }

    @Test
    void addSingleSelectField_shouldThrowIllegalArgumentException_whenAnOptionIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addSingleSelectField("Status", List.of("Todo", " ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("options");
    }
}
