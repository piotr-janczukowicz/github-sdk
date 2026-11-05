package io.github.piotr_janczukowicz.github.sdk.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubListForAuthenticatedUserFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepository;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class MeRepositoriesTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Me sut = GitHubSdk.of(github).me();

    @Test
    void repositories_shouldReturnHandles_whenTheAccountHasRepositories() throws IOException {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn("octocat");
        final GitHubRepository listed = mock(GitHubRepository.class);
        when(listed.owner()).thenReturn(owner);
        when(listed.name()).thenReturn("private-notes");
        when(github
                        .repositories()
                        .repository()
                        .listForAuthenticatedUser(
                                any(GitHubListForAuthenticatedUserFilter.class), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<Repository> repositories = sut.repositories().toList();

        assertThat(repositories)
                .singleElement()
                .satisfies(repository -> assertSoftly(softly -> {
                    softly.assertThat(repository.owner()).isEqualTo("octocat");
                    softly.assertThat(repository.name()).isEqualTo("private-notes");
                }));
    }

    @Test
    void repositories_shouldUseTheFilter_whenCalledWithOne() throws IOException {
        final GitHubListForAuthenticatedUserFilter filter = GitHubListForAuthenticatedUserFilter.empty();
        when(github
                        .repositories()
                        .repository()
                        .listForAuthenticatedUser(eq(filter), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.empty());

        final List<Repository> repositories = sut.repositories(filter).toList();

        assertThat(repositories).isEmpty();
    }

    @Test
    void repositories_shouldThrowNullPointerException_whenTheFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.repositories(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }
}
