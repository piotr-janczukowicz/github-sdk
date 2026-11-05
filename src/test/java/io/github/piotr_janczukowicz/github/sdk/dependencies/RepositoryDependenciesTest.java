package io.github.piotr_janczukowicz.github.sdk.dependencies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubCreateRepositorySnapshotPayload;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubCreateRepositorySnapshotResponse;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubDependencyGraphDiffEntry;
import io.github.piotr_janczukowicz.github.api.dependencygraph.GitHubGenerateSbomReportResponse;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryDependenciesTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final RepositoryDependencies sut = repository.dependencies();

    @Test
    void requestSbomReport_shouldReturnTheResponse_whenCalled() throws IOException {
        final GitHubGenerateSbomReportResponse response =
                new GitHubGenerateSbomReportResponse("https://api.github.com/x");
        when(github.dependencyGraph().sboms().generateSbomReport(OWNER, NAME)).thenReturn(response);

        assertThat(sut.requestSbomReport()).isSameAs(response);
    }

    @Test
    void sbomReport_shouldReturnTheLocation_whenTheReportIsReady() throws IOException {
        final URI location = URI.create("https://example.com/sbom.json");
        when(github.dependencyGraph().sboms().fetchSbomReport(OWNER, NAME, "uuid-1"))
                .thenReturn(Optional.of(location));

        assertThat(sut.sbomReport("uuid-1")).contains(location);
    }

    @Test
    void sbomReport_shouldThrowIllegalArgumentException_whenReportIdIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.sbomReport(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reportId");
    }

    @Test
    void diff_shouldCompareTheRefs_whenCalled() throws IOException {
        final GitHubDependencyGraphDiffEntry entry = mock(GitHubDependencyGraphDiffEntry.class);
        when(github.dependencyGraph().dependencyReview().diffRange(OWNER, NAME, "main...feature"))
                .thenReturn(List.of(entry));

        final List<GitHubDependencyGraphDiffEntry> diff = sut.diff("main", "feature");

        assertThat(diff).containsExactly(entry);
    }

    @Test
    void diff_shouldThrowIllegalArgumentException_whenBaseIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.diff(" ", "feature"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("base");
    }

    @Test
    void diff_shouldThrowIllegalArgumentException_whenHeadIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.diff("main", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("head");
    }

    @Test
    void submit_shouldReturnTheResult_whenCalled() throws IOException {
        final GitHubCreateRepositorySnapshotPayload snapshot = mock(GitHubCreateRepositorySnapshotPayload.class);
        final GitHubCreateRepositorySnapshotResponse created = mock(GitHubCreateRepositorySnapshotResponse.class);
        when(github.dependencyGraph().dependencySubmission().createRepositorySnapshot(OWNER, NAME, snapshot))
                .thenReturn(created);

        final GitHubCreateRepositorySnapshotResponse result = sut.submit(snapshot);

        assertThat(result).isSameAs(created);
    }

    @Test
    void submit_shouldThrowNullPointerException_whenSnapshotIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.submit(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("snapshot");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryDependencies(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
