package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProject;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectField;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldDataType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItem;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemContentType;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectItemSimple;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectState;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectView;
import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectViewLayout;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.util.Map;

public final class ProjectFixtures {

    public static final String OWNER = "octocat";
    public static final long OWNER_ID = 583231L;
    public static final long NUMBER = 7L;
    public static final long ITEM_ID = 51L;
    public static final long FIELD_ID = 61L;
    public static final int VIEW_NUMBER = 3;

    private ProjectFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubProject project() {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        lenient().when(owner.login()).thenReturn(OWNER);
        lenient().when(owner.id()).thenReturn(OWNER_ID);
        final GitHubProject project = mock(GitHubProject.class);
        lenient().when(project.number()).thenReturn((int) NUMBER);
        lenient().when(project.nodeId()).thenReturn("PVT_1");
        lenient().when(project.title()).thenReturn("Roadmap");
        lenient().when(project.state()).thenReturn(GitHubProjectState.OPEN);
        lenient().when(project.isPublic()).thenReturn(true);
        lenient().when(project.owner()).thenReturn(owner);
        return project;
    }

    public static GitHubProjectItem item() {
        final GitHubProjectItem item = mock(GitHubProjectItem.class);
        lenient().when(item.id()).thenReturn(ITEM_ID);
        lenient().when(item.nodeId()).thenReturn("PVTI_1");
        lenient().when(item.contentType()).thenReturn(GitHubProjectItemContentType.ISSUE);
        lenient().when(item.content()).thenReturn(Map.of("title", "Fix it"));
        return item;
    }

    public static GitHubProjectItemSimple itemSimple() {
        final GitHubProjectItemSimple item = mock(GitHubProjectItemSimple.class);
        lenient().when(item.id()).thenReturn(ITEM_ID);
        return item;
    }

    public static GitHubProjectField field() {
        final GitHubProjectField field = mock(GitHubProjectField.class);
        lenient().when(field.id()).thenReturn(FIELD_ID);
        lenient().when(field.nodeId()).thenReturn("PVTF_1");
        lenient().when(field.name()).thenReturn("Status");
        lenient().when(field.dataType()).thenReturn(GitHubProjectFieldDataType.SINGLE_SELECT);
        return field;
    }

    public static GitHubProjectView view() {
        final GitHubProjectView view = mock(GitHubProjectView.class);
        lenient().when(view.number()).thenReturn(VIEW_NUMBER);
        lenient().when(view.nodeId()).thenReturn("PVTV_1");
        lenient().when(view.name()).thenReturn("Board");
        lenient().when(view.layout()).thenReturn(GitHubProjectViewLayout.BOARD);
        return view;
    }
}
