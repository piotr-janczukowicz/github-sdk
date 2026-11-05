package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabelBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class LabelTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository("octocat", "hello-world");
    private final GitHubLabel record = RepositoryContentFixtures.label();
    private final Label sut = new Label(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenLabelIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.name()).isEqualTo("bug");
            softly.assertThat(sut.color()).isEqualTo("d73a4a");
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.isDefault()).isTrue();
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void description_shouldReturnTheDescription_whenTheRecordHasOne() {
        final Label described = new Label(
                repository,
                GitHubLabelBuilder.builder(record)
                        .description("Something broke")
                        .build());

        final Optional<String> description = described.description();

        assertThat(description).contains("Something broke");
    }

    @Test
    void refresh_shouldReturnTheReloadedLabel_whenGitHubHasIt() throws IOException {
        final GitHubLabel reloaded =
                GitHubLabelBuilder.builder(record).color("ffffff").build();
        when(github.issues().labels().getLabel("octocat", "hello-world", "bug")).thenReturn(Optional.of(reloaded));

        final Label refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.record()).isSameAs(reloaded);
            softly.assertThat(sut.color()).isEqualTo("d73a4a");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheLabelIsGone() throws IOException {
        when(github.issues().labels().getLabel("octocat", "hello-world", "bug")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("bug");
    }

    @Test
    void delete_shouldDeleteTheLabel_whenCalled() throws IOException {
        sut.delete();

        verify(github.issues().labels()).deleteLabel("octocat", "hello-world", "bug");
    }
}
