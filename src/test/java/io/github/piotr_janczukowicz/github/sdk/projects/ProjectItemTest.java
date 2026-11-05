package io.github.piotr_janczukowicz.github.sdk.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItem;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemContentType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubUpdateProjectItemPayload;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ProjectFixtures;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectItemTest {

    private static final long NUMBER = ProjectFixtures.NUMBER;
    private static final long ID = ProjectFixtures.ITEM_ID;
    private static final long FIELD = ProjectFixtures.FIELD_ID;

    private final ProjectGateway gateway = mock(ProjectGateway.class);
    private final Project project = new Project(gateway, ProjectFixtures.project());
    private final GitHubProjectItem response = ProjectFixtures.item();
    private final ProjectItem sut = new ProjectItem(project, response);

    @Test
    void content_shouldBeEmpty_whenTheResponseHasNone() {
        final GitHubProjectItem bare = mock(GitHubProjectItem.class);
        when(bare.content()).thenReturn(null);

        assertThat(new ProjectItem(project, bare).content()).isEmpty();
    }

    @Test
    void accessors_shouldExposeTheResponse_whenItemIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.nodeId()).contains("PVTI_1");
            softly.assertThat(sut.contentType()).isEqualTo(GitHubProjectItemContentType.ISSUE);
            softly.assertThat(sut.content()).containsEntry("title", "Fix it");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenProjectIsNull() {
        final Throwable thrown = catchThrowable(() -> new ProjectItem(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("project");
    }

    @Test
    void setField_shouldSendTheValueAndReturnTheItem_whenCalled() throws IOException {
        final GitHubProjectItem updated = ProjectFixtures.item();
        when(gateway.updateItem(eq(NUMBER), eq(ID), any())).thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateProjectItemPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateProjectItemPayload.class);

        final ProjectItem result = sut.setField(FIELD, "Done");

        verify(gateway).updateItem(eq(NUMBER), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(payload.getValue().fields()).singleElement().satisfies(field -> {
                assertThat(field.id()).isEqualTo(FIELD);
                assertThat(field.value()).isEqualTo(OptionalNull.of("Done"));
            });
        });
    }

    @Test
    void clearField_shouldSendANullValue_whenCalled() throws IOException {
        final GitHubProjectItem updated = ProjectFixtures.item();
        when(gateway.updateItem(eq(NUMBER), eq(ID), any())).thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateProjectItemPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateProjectItemPayload.class);

        sut.clearField(FIELD);

        verify(gateway).updateItem(eq(NUMBER), eq(ID), payload.capture());
        assertThat(payload.getValue().fields())
                .singleElement()
                .satisfies(field -> assertThat(field.value()).isEqualTo(OptionalNull.ofNull()));
    }

    @Test
    void setField_shouldThrowNullPointerException_whenValueIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.setField(FIELD, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void refresh_shouldReturnTheReloadedItem_whenItExists() throws IOException {
        final GitHubProjectItem reloaded = ProjectFixtures.item();
        when(gateway.item(NUMBER, ID)).thenReturn(Optional.of(reloaded));

        final ProjectItem result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheItemIsGone() throws IOException {
        when(gateway.item(NUMBER, ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldRemoveTheItem_whenCalled() throws IOException {
        sut.delete();

        verify(gateway).deleteItem(NUMBER, ID);
    }
}
