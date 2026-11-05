package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaborator;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaboratorBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@UnitTest
class CollaboratorTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCollaborator response = CollaboratorsFixtures.collaborator();
    private final Collaborator sut = new Collaborator(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenCollaboratorIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.login()).isEqualTo("hubot");
            softly.assertThat(sut.roleName()).hasValue("write");
            softly.assertThat(sut.isSiteAdmin()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void roleName_shouldBeEmpty_whenTheResponseHasNoRole() {
        final GitHubCollaborator withoutRole =
                GitHubCollaboratorBuilder.builder(response).roleName(null).build();
        final Collaborator collaborator = new Collaborator(repository, withoutRole);

        assertThat(collaborator.roleName()).isEmpty();
    }

    @Test
    void remove_shouldRemoveTheUser_whenCalled() throws IOException {
        sut.remove();

        verify(github.collaborators().collaborator()).removeCollaborator(OWNER, NAME, "hubot");
    }
}
