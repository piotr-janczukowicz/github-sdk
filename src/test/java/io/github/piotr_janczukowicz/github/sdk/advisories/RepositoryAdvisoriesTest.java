package io.github.piotr_janczukowicz.github.sdk.advisories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubCreatePrivateVulnerabilityReportPayload;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubCreateRepositoryAdvisoryPayload;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisory;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubSecurityAdvisoriesRepositoryAdvisoriesApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AdvisoryFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryAdvisoriesTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final String GHSA = AdvisoryFixtures.GHSA;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubSecurityAdvisoriesRepositoryAdvisoriesApi api =
            github.securityAdvisories().repositoryAdvisories();
    private final RepositoryAdvisories sut = repository.advisories();

    @Test
    void all_shouldWrapTheAdvisories_whenTheyExist() throws IOException {
        final GitHubRepositoryAdvisory listed = AdvisoryFixtures.advisory();
        when(api.listRepositoryAdvisories(OWNER, NAME).stream()).thenReturn(Stream.of(listed));

        final List<Advisory> advisories = sut.all().toList();

        assertThat(advisories)
                .singleElement()
                .satisfies(advisory -> assertSoftly(softly -> {
                    softly.assertThat(advisory.response()).isSameAs(listed);
                    softly.assertThat(advisory.repository()).isSameAs(repository);
                }));
    }

    @Test
    void find_shouldReturnTheAdvisory_whenItExists() throws IOException {
        final GitHubRepositoryAdvisory loaded = AdvisoryFixtures.advisory();
        when(api.getRepositoryAdvisory(OWNER, NAME, GHSA)).thenReturn(Optional.of(loaded));

        final Optional<Advisory> found = sut.find(GHSA);

        assertThat(found)
                .hasValueSatisfying(advisory -> assertSoftly(softly -> {
                    softly.assertThat(advisory.response()).isSameAs(loaded);
                    softly.assertThat(advisory.repository()).isSameAs(repository);
                }));
    }

    @Test
    void find_shouldBeEmpty_whenGitHubHasNoSuchAdvisory() throws IOException {
        when(api.getRepositoryAdvisory(OWNER, NAME, GHSA)).thenReturn(Optional.empty());

        final Optional<Advisory> found = sut.find(GHSA);

        assertThat(found).isEmpty();
    }

    @Test
    void find_shouldThrowNullPointerException_whenIdIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.find(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("ghsaId");
    }

    @Test
    void find_shouldThrowIllegalArgumentException_whenIdIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.find(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ghsaId");
    }

    @Test
    void create_shouldReturnTheNewAdvisory_whenCalled() throws IOException {
        final GitHubCreateRepositoryAdvisoryPayload payload = mock(GitHubCreateRepositoryAdvisoryPayload.class);
        final GitHubRepositoryAdvisory created = AdvisoryFixtures.advisory();
        when(api.createRepositoryAdvisory(OWNER, NAME, payload)).thenReturn(created);

        final Advisory result = sut.create(payload);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void create_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.create(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void report_shouldReturnTheNewAdvisory_whenCalled() throws IOException {
        final GitHubCreatePrivateVulnerabilityReportPayload payload =
                mock(GitHubCreatePrivateVulnerabilityReportPayload.class);
        final GitHubRepositoryAdvisory created = AdvisoryFixtures.advisory();
        when(api.createPrivateVulnerabilityReport(OWNER, NAME, payload)).thenReturn(created);

        final Advisory result = sut.report(payload);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void report_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.report(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryAdvisories(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
