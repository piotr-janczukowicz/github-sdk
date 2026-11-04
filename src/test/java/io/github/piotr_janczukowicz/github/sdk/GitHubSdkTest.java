package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class GitHubSdkTest {

    @Test
    void of_shouldThrowNullPointerException_whenGithubIsNull() {
        final Throwable thrown = catchThrowable(() -> GitHubSdk.of(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("github");
    }

    @Test
    void repository_shouldReturnHandleWithoutRequest_whenCoordinatesAreValid() {
        final GitHub github = mock(GitHub.class);
        final GitHubSdk sut = GitHubSdk.of(github);

        final Repository repository = sut.repository("octocat", "hello-world");

        assertSoftly(softly -> {
            softly.assertThat(repository.owner()).isEqualTo("octocat");
            softly.assertThat(repository.name()).isEqualTo("hello-world");
        });
        verifyNoInteractions(github);
    }

    @Test
    void repository_shouldThrowIllegalArgumentException_whenOwnerIsBlank() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class));

        final Throwable thrown = catchThrowable(() -> sut.repository(" ", "hello-world"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("owner");
    }

    @Test
    void repository_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final GitHubSdk sut = GitHubSdk.of(mock(GitHub.class));

        final Throwable thrown = catchThrowable(() -> sut.repository("octocat", ""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }
}
