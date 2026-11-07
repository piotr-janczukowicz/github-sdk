package io.github.piotr_janczukowicz.github.sdk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubAutomatedSecurityFixesStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubImmutableReleasesStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPrivateVulnerabilityReportingStatus;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositorySecurityTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubRepositoryApi api = github.repositories().repository();
    private final RepositorySecurity sut = repository.security();

    @Test
    void isVulnerabilityAlertsEnabled_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        doReturn(true).when(api).checkVulnerabilityAlerts(OWNER, NAME);

        final boolean isEnabled = sut.isVulnerabilityAlertsEnabled();

        assertThat(isEnabled).isTrue();
    }

    @Test
    void isVulnerabilityAlertsEnabled_shouldBeFalse_whenGitHubAnswersNo() throws IOException {
        doReturn(false).when(api).checkVulnerabilityAlerts(OWNER, NAME);

        final boolean isEnabled = sut.isVulnerabilityAlertsEnabled();

        assertThat(isEnabled).isFalse();
    }

    @Test
    void enableVulnerabilityAlerts_shouldEnableThem_whenCalled() throws IOException {
        sut.enableVulnerabilityAlerts();

        verify(api).enableVulnerabilityAlerts(OWNER, NAME);
    }

    @Test
    void disableVulnerabilityAlerts_shouldDisableThem_whenCalled() throws IOException {
        sut.disableVulnerabilityAlerts();

        verify(api).disableVulnerabilityAlerts(OWNER, NAME);
    }

    @Test
    void isAutomatedSecurityFixesEnabled_shouldBeTrue_whenEnabledAndNotPaused() throws IOException {
        doReturn(new GitHubAutomatedSecurityFixesStatus(true, false))
                .when(api)
                .checkAutomatedSecurityFixes(OWNER, NAME);

        final boolean isEnabled = sut.isAutomatedSecurityFixesEnabled();

        assertThat(isEnabled).isTrue();
    }

    @Test
    void isAutomatedSecurityFixesEnabled_shouldBeFalse_whenEnabledButPaused() throws IOException {
        doReturn(new GitHubAutomatedSecurityFixesStatus(true, true)).when(api).checkAutomatedSecurityFixes(OWNER, NAME);

        final boolean isEnabled = sut.isAutomatedSecurityFixesEnabled();

        assertThat(isEnabled).isFalse();
    }

    @Test
    void isAutomatedSecurityFixesEnabled_shouldBeFalse_whenNotEnabled() throws IOException {
        doReturn(new GitHubAutomatedSecurityFixesStatus(false, false))
                .when(api)
                .checkAutomatedSecurityFixes(OWNER, NAME);

        final boolean isEnabled = sut.isAutomatedSecurityFixesEnabled();

        assertThat(isEnabled).isFalse();
    }

    @Test
    void enableAutomatedSecurityFixes_shouldEnableThem_whenCalled() throws IOException {
        sut.enableAutomatedSecurityFixes();

        verify(api).enableAutomatedSecurityFixes(OWNER, NAME);
    }

    @Test
    void disableAutomatedSecurityFixes_shouldDisableThem_whenCalled() throws IOException {
        sut.disableAutomatedSecurityFixes();

        verify(api).disableAutomatedSecurityFixes(OWNER, NAME);
    }

    @Test
    void isPrivateVulnerabilityReportingEnabled_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        doReturn(new GitHubPrivateVulnerabilityReportingStatus(true))
                .when(api)
                .checkPrivateVulnerabilityReporting(OWNER, NAME);

        final boolean isEnabled = sut.isPrivateVulnerabilityReportingEnabled();

        assertThat(isEnabled).isTrue();
    }

    @Test
    void isPrivateVulnerabilityReportingEnabled_shouldBeFalse_whenNotEnabled() throws IOException {
        doReturn(new GitHubPrivateVulnerabilityReportingStatus(false))
                .when(api)
                .checkPrivateVulnerabilityReporting(OWNER, NAME);

        final boolean isEnabled = sut.isPrivateVulnerabilityReportingEnabled();

        assertThat(isEnabled).isFalse();
    }

    @Test
    void enablePrivateVulnerabilityReporting_shouldEnableIt_whenCalled() throws IOException {
        sut.enablePrivateVulnerabilityReporting();

        verify(api).enablePrivateVulnerabilityReporting(OWNER, NAME);
    }

    @Test
    void disablePrivateVulnerabilityReporting_shouldDisableIt_whenCalled() throws IOException {
        sut.disablePrivateVulnerabilityReporting();

        verify(api).disablePrivateVulnerabilityReporting(OWNER, NAME);
    }

    @Test
    void isImmutableReleasesEnabled_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        doReturn(new GitHubImmutableReleasesStatus(true, false)).when(api).checkImmutableReleases(OWNER, NAME);

        final boolean isEnabled = sut.isImmutableReleasesEnabled();

        assertThat(isEnabled).isTrue();
    }

    @Test
    void isImmutableReleasesEnabled_shouldBeFalse_whenNotEnabled() throws IOException {
        doReturn(new GitHubImmutableReleasesStatus(false, false)).when(api).checkImmutableReleases(OWNER, NAME);

        final boolean isEnabled = sut.isImmutableReleasesEnabled();

        assertThat(isEnabled).isFalse();
    }

    @Test
    void enableImmutableReleases_shouldEnableThem_whenCalled() throws IOException {
        sut.enableImmutableReleases();

        verify(api).enableImmutableReleases(OWNER, NAME);
    }

    @Test
    void disableImmutableReleases_shouldDisableThem_whenCalled() throws IOException {
        sut.disableImmutableReleases();

        verify(api).disableImmutableReleases(OWNER, NAME);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositorySecurity(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
