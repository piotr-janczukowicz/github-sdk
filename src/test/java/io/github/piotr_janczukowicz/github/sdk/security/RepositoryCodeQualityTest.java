package io.github.piotr_janczukowicz.github.sdk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubCodeQualityApi;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubCodeQualityFinding;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubCodeQualitySetup;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubListFindingsForRepoFilter;
import io.github.piotr_janczukowicz.github.api.codequality.GitHubListFindingsForRepoState;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryCodeQualityTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCodeQualityApi api = github.codeQuality();
    private final RepositoryCodeQuality sut = repository.codeQuality();

    @Test
    void findings_shouldStreamTheFindings_whenTheyExist() throws IOException {
        final GitHubCodeQualityFinding finding = mock(GitHubCodeQualityFinding.class);
        when(api.listFindingsForRepo(eq(OWNER), eq(NAME), any(), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(finding));

        final List<GitHubCodeQualityFinding> findings = sut.findings().toList();

        assertThat(findings).containsExactly(finding);
    }

    @Test
    void findings_shouldSendTheState_whenOneIsGiven() throws IOException {
        when(api.listFindingsForRepo(eq(OWNER), eq(NAME), any(), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.empty());
        final ArgumentCaptor<GitHubListFindingsForRepoFilter> filter =
                ArgumentCaptor.forClass(GitHubListFindingsForRepoFilter.class);

        sut.findings(GitHubListFindingsForRepoState.DISMISSED).toList();

        verify(api).listFindingsForRepo(eq(OWNER), eq(NAME), filter.capture(), any(GitHubPageRequest.class));
        assertThat(filter.getValue().state()).isEqualTo(GitHubListFindingsForRepoState.DISMISSED);
    }

    @Test
    void findings_shouldThrowNullPointerException_whenStateIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.findings(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("state");
    }

    @Test
    void finding_shouldReturnTheFinding_whenItExists() throws IOException {
        final GitHubCodeQualityFinding finding = mock(GitHubCodeQualityFinding.class);
        when(api.getFinding(OWNER, NAME, 4)).thenReturn(Optional.of(finding));

        final Optional<GitHubCodeQualityFinding> found = sut.finding(4);

        assertThat(found).containsSame(finding);
    }

    @Test
    void setup_shouldReturnTheSetup_whenCodeQualityIsAvailable() throws IOException {
        final GitHubCodeQualitySetup setup = mock(GitHubCodeQualitySetup.class);
        when(api.getSetup(OWNER, NAME)).thenReturn(setup);

        final Optional<GitHubCodeQualitySetup> found = sut.setup();

        assertThat(found).containsSame(setup);
    }

    @Test
    void setup_shouldBeEmpty_whenGitHubAnswersNotFound() throws IOException {
        doThrow(mock(GitHubNotFoundException.class)).when(api).getSetup(OWNER, NAME);

        final Optional<GitHubCodeQualitySetup> found = sut.setup();

        assertThat(found).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryCodeQuality(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
