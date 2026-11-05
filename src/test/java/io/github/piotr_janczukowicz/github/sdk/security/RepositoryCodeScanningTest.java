package io.github.piotr_janczukowicz.github.sdk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAnalysis;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAnalysisDeletion;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningCodeScanningApi;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningCodeqlDatabase;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningDefaultSetup;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningDefaultSetupUpdate;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningDefaultSetupUpdateResponse;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningSarifsReceipt;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningSarifsStatus;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubListRecentAnalysesFilter;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubUploadSarifPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryCodeScanningTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
    private final RepositoryCodeScanning sut = repository.codeScanning();

    @Test
    void analyses_shouldStreamTheAnalyses_whenTheyExist() throws IOException {
        final GitHubCodeScanningAnalysis analysis = mock(GitHubCodeScanningAnalysis.class);
        when(api.listRecentAnalyses(OWNER, NAME).stream()).thenReturn(Stream.of(analysis));

        final List<GitHubCodeScanningAnalysis> analyses = sut.analyses().toList();

        assertThat(analyses).containsExactly(analysis);
    }

    @Test
    void analyses_shouldStreamTheMatches_whenAFilterIsGiven() throws IOException {
        final GitHubCodeScanningAnalysis analysis = mock(GitHubCodeScanningAnalysis.class);
        final GitHubListRecentAnalysesFilter filter = mock(GitHubListRecentAnalysesFilter.class);
        when(api.listRecentAnalyses(eq(OWNER), eq(NAME), eq(filter), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(analysis));

        final List<GitHubCodeScanningAnalysis> analyses = sut.analyses(filter).toList();

        assertThat(analyses).containsExactly(analysis);
    }

    @Test
    void analyses_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.analyses(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void analysis_shouldReturnTheAnalysis_whenItExists() throws IOException {
        final GitHubCodeScanningAnalysis analysis = mock(GitHubCodeScanningAnalysis.class);
        when(api.getAnalysis(OWNER, NAME, 4L)).thenReturn(Optional.of(analysis));

        final Optional<GitHubCodeScanningAnalysis> found = sut.analysis(4L);

        assertThat(found).containsSame(analysis);
    }

    @Test
    void sarif_shouldReturnTheSarifText_whenItExists() throws IOException {
        when(api.getAnalysisSarif(OWNER, NAME, 4L)).thenReturn(Optional.of("{}"));

        final Optional<String> sarif = sut.sarif(4L);

        assertThat(sarif).contains("{}");
    }

    @Test
    void deleteAnalysis_shouldReturnTheNextLinks_whenCalled() throws IOException {
        final GitHubCodeScanningAnalysisDeletion deletion = mock(GitHubCodeScanningAnalysisDeletion.class);
        when(api.deleteAnalysis(OWNER, NAME, 4L)).thenReturn(deletion);

        final GitHubCodeScanningAnalysisDeletion result = sut.deleteAnalysis(4L);

        assertThat(result).isSameAs(deletion);
    }

    @Test
    void uploadSarif_shouldReturnTheReceipt_whenCalled() throws IOException {
        final GitHubUploadSarifPayload payload = mock(GitHubUploadSarifPayload.class);
        final GitHubCodeScanningSarifsReceipt receipt = mock(GitHubCodeScanningSarifsReceipt.class);
        when(api.uploadSarif(OWNER, NAME, payload)).thenReturn(receipt);

        final GitHubCodeScanningSarifsReceipt result = sut.uploadSarif(payload);

        assertThat(result).isSameAs(receipt);
    }

    @Test
    void uploadSarif_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.uploadSarif(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void sarifStatus_shouldReturnTheStatus_whenItExists() throws IOException {
        final GitHubCodeScanningSarifsStatus status = mock(GitHubCodeScanningSarifsStatus.class);
        when(api.getSarif(OWNER, NAME, "abc")).thenReturn(Optional.of(status));

        final Optional<GitHubCodeScanningSarifsStatus> found = sut.sarifStatus("abc");

        assertThat(found).containsSame(status);
    }

    @Test
    void sarifStatus_shouldThrowIllegalArgumentException_whenIdIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.sarifStatus(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sarifId");
    }

    @Test
    void databases_shouldReturnTheDatabases_whenTheyExist() throws IOException {
        final GitHubCodeScanningCodeqlDatabase database = mock(GitHubCodeScanningCodeqlDatabase.class);
        when(api.listCodeqlDatabases(OWNER, NAME)).thenReturn(List.of(database));

        final List<GitHubCodeScanningCodeqlDatabase> databases = sut.databases();

        assertThat(databases).containsExactly(database);
    }

    @Test
    void database_shouldReturnTheDatabase_whenItExists() throws IOException {
        final GitHubCodeScanningCodeqlDatabase database = mock(GitHubCodeScanningCodeqlDatabase.class);
        when(api.getCodeqlDatabase(OWNER, NAME, "java")).thenReturn(Optional.of(database));

        final Optional<GitHubCodeScanningCodeqlDatabase> found = sut.database("java");

        assertThat(found).containsSame(database);
    }

    @Test
    void database_shouldThrowIllegalArgumentException_whenLanguageIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.database(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("language");
    }

    @Test
    void databaseDownloadLocation_shouldReturnTheLocation_whenCalled() throws IOException {
        final URI location = URI.create("https://example.com/db.zip");
        when(api.getCodeqlDatabaseDownloadLocation(OWNER, NAME, "java")).thenReturn(location);

        final URI result = sut.databaseDownloadLocation("java");

        assertThat(result).isEqualTo(location);
    }

    @Test
    void deleteDatabase_shouldDeleteTheDatabase_whenCalled() throws IOException {
        sut.deleteDatabase("java");

        verify(api).deleteCodeqlDatabase(OWNER, NAME, "java");
    }

    @Test
    void defaultSetup_shouldReturnTheSetup_whenCalled() throws IOException {
        final GitHubCodeScanningDefaultSetup setup = mock(GitHubCodeScanningDefaultSetup.class);
        when(api.getDefaultSetup(OWNER, NAME)).thenReturn(setup);

        final GitHubCodeScanningDefaultSetup result = sut.defaultSetup();

        assertThat(result).isSameAs(setup);
    }

    @Test
    void updateDefaultSetup_shouldReturnTheRun_whenGitHubStartsOne() throws IOException {
        final GitHubCodeScanningDefaultSetupUpdate update = mock(GitHubCodeScanningDefaultSetupUpdate.class);
        final GitHubCodeScanningDefaultSetupUpdateResponse response =
                mock(GitHubCodeScanningDefaultSetupUpdateResponse.class);
        when(api.updateDefaultSetup(OWNER, NAME, update)).thenReturn(Optional.of(response));

        final Optional<GitHubCodeScanningDefaultSetupUpdateResponse> result = sut.updateDefaultSetup(update);

        assertThat(result).containsSame(response);
    }

    @Test
    void updateDefaultSetup_shouldThrowNullPointerException_whenUpdateIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.updateDefaultSetup(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("update");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryCodeScanning(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
