package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtectionBuilder;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class BranchTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository("octocat", "hello-world");
    private final GitHubBranchWithProtection response = RepositoryContentFixtures.branch();
    private final Branch sut = new Branch(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenBranchIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.name()).isEqualTo("main");
            softly.assertThat(sut.isProtected()).isFalse();
            softly.assertThat(sut.commit().response()).isSameAs(response.commit());
            softly.assertThat(sut.commit().repository()).isSameAs(repository);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedBranch_whenGitHubHasIt() throws IOException {
        final GitHubBranchWithProtection reloaded = GitHubBranchWithProtectionBuilder.builder(response)
                .protectedBranch(true)
                .build();
        when(github.branches().branch().getBranch("octocat", "hello-world", "main"))
                .thenReturn(Optional.of(reloaded));

        final Branch refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
            softly.assertThat(sut.isProtected()).isFalse();
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheBranchIsGone() throws IOException {
        when(github.branches().branch().getBranch("octocat", "hello-world", "main"))
                .thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("main");
    }

    @Test
    void isProtected_shouldBeTrue_whenTheResponseSaysProtected() {
        final Branch protectedBranch = new Branch(
                repository,
                GitHubBranchWithProtectionBuilder.builder(response)
                        .protectedBranch(true)
                        .build());

        final boolean isProtected = protectedBranch.isProtected();

        assertThat(isProtected).isTrue();
    }
}
