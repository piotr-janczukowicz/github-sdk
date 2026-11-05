package io.github.piotr_janczukowicz.github.sdk.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectField;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldDataType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItem;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectView;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectViewLayout;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProjectFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectFieldAndViewTest {

    private static final long NUMBER = ProjectFixtures.NUMBER;

    private final ProjectGateway gateway = mock(ProjectGateway.class);
    private final Project project = new Project(gateway, ProjectFixtures.project());

    @Test
    void constructor_shouldExposeTheFieldResponse_whenFieldIsLoaded() {
        final GitHubProjectField response = ProjectFixtures.field();

        final ProjectField sut = new ProjectField(project, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.id()).isEqualTo(ProjectFixtures.FIELD_ID);
            softly.assertThat(sut.nodeId()).contains("PVTF_1");
            softly.assertThat(sut.name()).isEqualTo("Status");
            softly.assertThat(sut.dataType()).isEqualTo(GitHubProjectFieldDataType.SINGLE_SELECT);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedField_whenItExists() throws IOException {
        final GitHubProjectField reloaded = ProjectFixtures.field();
        when(gateway.field(NUMBER, ProjectFixtures.FIELD_ID)).thenReturn(Optional.of(reloaded));
        final ProjectField sut = new ProjectField(project, ProjectFixtures.field());

        final ProjectField result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheFieldIsGone() throws IOException {
        when(gateway.field(NUMBER, ProjectFixtures.FIELD_ID)).thenReturn(Optional.empty());
        final ProjectField sut = new ProjectField(project, ProjectFixtures.field());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenFieldResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new ProjectField(project, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenFieldProjectIsNull() {
        final GitHubProjectField response = ProjectFixtures.field();

        final Throwable thrown = catchThrowable(() -> new ProjectField(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("project");
    }

    @Test
    void constructor_shouldExposeTheViewResponse_whenViewIsLoaded() {
        final GitHubProjectView response = ProjectFixtures.view();

        final ProjectView sut = new ProjectView(project, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.number()).isEqualTo(ProjectFixtures.VIEW_NUMBER);
            softly.assertThat(sut.nodeId()).contains("PVTV_1");
            softly.assertThat(sut.name()).isEqualTo("Board");
            softly.assertThat(sut.layout()).isEqualTo(GitHubProjectViewLayout.BOARD);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void items_shouldStreamTheItems_whenTheyExist() throws IOException {
        final GitHubProjectItem listed = ProjectFixtures.item();
        when(gateway.viewItems(NUMBER, ProjectFixtures.VIEW_NUMBER)).thenReturn(Stream.of(listed));
        final ProjectView sut = new ProjectView(project, ProjectFixtures.view());

        final List<ProjectItem> items = sut.items().toList();

        assertThat(items)
                .singleElement()
                .satisfies(item -> assertSoftly(softly -> {
                    softly.assertThat(item.response()).isSameAs(listed);
                    softly.assertThat(item.project()).isSameAs(project);
                }));
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenViewProjectIsNull() {
        final GitHubProjectView response = ProjectFixtures.view();

        final Throwable thrown = catchThrowable(() -> new ProjectView(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("project");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenViewResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new ProjectView(project, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }
}
