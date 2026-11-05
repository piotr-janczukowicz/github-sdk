package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryVisibility;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void send_shouldSendOnlyTheSetFieldsAndReturnTheRepository_whenSomeFieldsAreSet() throws IOException {
        final GitHubFullRepository updated = mock(GitHubFullRepository.class);
        when(updated.name()).thenReturn(NAME);
        final GitHubRepositoryApi api = github.repositories().repository();
        when(api.update(eq(OWNER), eq(NAME), any(GitHubUpdatePayload.class))).thenReturn(updated);
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Repository result =
                repository.update().description("A greeting").wiki(false).send();

        verify(api).update(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("A greeting");
            softly.assertThat(payload.getValue().hasWiki()).isFalse();
            softly.assertThat(payload.getValue().name()).isNull();
            softly.assertThat(payload.getValue().archived()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(result.owner()).isEqualTo(OWNER);
            softly.assertThat(result.name()).isEqualTo(NAME);
        });
    }

    @Test
    void send_shouldSendEveryField_whenAllAreSet() throws IOException {
        final GitHubFullRepository updated = mock(GitHubFullRepository.class);
        when(updated.name()).thenReturn("renamed");
        final GitHubRepositoryApi api = github.repositories().repository();
        when(api.update(eq(OWNER), eq(NAME), any(GitHubUpdatePayload.class))).thenReturn(updated);
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Repository result = repository
                .update()
                .name("renamed")
                .description("A greeting")
                .homepage("https://example.com")
                .visibility(GitHubRepositoryVisibility.PRIVATE)
                .defaultBranch("trunk")
                .issues(true)
                .projects(false)
                .wiki(false)
                .template(true)
                .allowSquashMerge(true)
                .allowMergeCommit(false)
                .allowRebaseMerge(true)
                .allowAutoMerge(true)
                .deleteBranchOnMerge(true)
                .allowUpdateBranch(true)
                .allowForking(false)
                .webCommitSignoffRequired(true)
                .send();

        verify(api).update(eq(OWNER), eq(NAME), payload.capture());
        final GitHubUpdatePayload sent = payload.getValue();
        assertSoftly(softly -> {
            softly.assertThat(sent.name()).isEqualTo("renamed");
            softly.assertThat(sent.description()).isEqualTo("A greeting");
            softly.assertThat(sent.homepage()).isEqualTo("https://example.com");
            softly.assertThat(sent.visibility()).isEqualTo(GitHubRepositoryVisibility.PRIVATE);
            softly.assertThat(sent.defaultBranch()).isEqualTo("trunk");
            softly.assertThat(sent.hasIssues()).isTrue();
            softly.assertThat(sent.hasProjects()).isFalse();
            softly.assertThat(sent.hasWiki()).isFalse();
            softly.assertThat(sent.isTemplate()).isTrue();
            softly.assertThat(sent.allowSquashMerge()).isTrue();
            softly.assertThat(sent.allowMergeCommit()).isFalse();
            softly.assertThat(sent.allowRebaseMerge()).isTrue();
            softly.assertThat(sent.allowAutoMerge()).isTrue();
            softly.assertThat(sent.deleteBranchOnMerge()).isTrue();
            softly.assertThat(sent.allowUpdateBranch()).isTrue();
            softly.assertThat(sent.allowForking()).isFalse();
            softly.assertThat(sent.webCommitSignoffRequired()).isTrue();
            softly.assertThat(result.name()).isEqualTo("renamed");
            softly.assertThat(repository.name()).isEqualTo(NAME);
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingIsSet() {
        final RepositoryUpdate update = repository.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing to update");
        verifyNoInteractions(github);
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final RepositoryUpdate update = repository.update();

        final Throwable thrown = catchThrowable(() -> update.name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void description_shouldThrowNullPointerException_whenDescriptionIsNull() {
        final RepositoryUpdate update = repository.update();

        final Throwable thrown = catchThrowable(() -> update.description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("description");
    }

    @Test
    void homepage_shouldThrowNullPointerException_whenHomepageIsNull() {
        final RepositoryUpdate update = repository.update();

        final Throwable thrown = catchThrowable(() -> update.homepage(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("homepage");
    }

    @Test
    void visibility_shouldThrowNullPointerException_whenVisibilityIsNull() {
        final RepositoryUpdate update = repository.update();

        final Throwable thrown = catchThrowable(() -> update.visibility(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("visibility");
    }

    @Test
    void defaultBranch_shouldThrowIllegalArgumentException_whenBranchIsBlank() {
        final RepositoryUpdate update = repository.update();

        final Throwable thrown = catchThrowable(() -> update.defaultBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("defaultBranch");
    }
}
