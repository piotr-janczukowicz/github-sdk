package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlert;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertItems;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningCodeScanningApi;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlert;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertsApi;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubListAlertsForRepoFilter;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubListSecretScanningAlertsFilter;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlert;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertWithMetadata;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertsApi;
import io.github.piotr_janczukowicz.github.sdk.fixtures.SecurityFixtures;
import io.github.piotr_janczukowicz.github.sdk.security.CodeScanningAlert;
import io.github.piotr_janczukowicz.github.sdk.security.DependabotAlert;
import io.github.piotr_janczukowicz.github.sdk.security.RepositorySecurity;
import io.github.piotr_janczukowicz.github.sdk.security.SecretScanningAlert;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryAlertsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void dependabotAlerts_shouldStreamTheAlerts_whenTheyExist() throws IOException {
        final GitHubDependabotAlert listed = SecurityFixtures.dependabotAlert();
        when(github
                        .dependabot()
                        .alerts()
                        .listAlertsForRepo(eq(OWNER), eq(NAME), any(GitHubListAlertsForRepoFilter.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<DependabotAlert> alerts = sut.dependabotAlerts().toList();

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.response()).isSameAs(listed);
            assertThat(alert.repository()).isSameAs(sut);
        });
    }

    @Test
    void dependabotAlerts_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.dependabotAlerts(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void dependabotAlert_shouldReturnTheAlert_whenGitHubHasIt() throws IOException {
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(Optional.of(SecurityFixtures.dependabotAlert()))
                .when(api)
                .getAlert(OWNER, NAME, SecurityFixtures.DEPENDABOT_NUMBER);

        final Optional<DependabotAlert> found = sut.dependabotAlert(SecurityFixtures.DEPENDABOT_NUMBER);

        assertThat(found).isPresent();
    }

    @Test
    void dependabotAlert_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubDependabotAlertsApi api = github.dependabot().alerts();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, 1);

        final Optional<DependabotAlert> found = sut.dependabotAlert(1);

        assertThat(found).isEmpty();
    }

    @Test
    void secretScanningAlerts_shouldLoadEachListedAlert_whenTheyExist() throws IOException {
        final GitHubSecretScanningAlert listed = mock(GitHubSecretScanningAlert.class);
        when(listed.number()).thenReturn(SecurityFixtures.SECRET_NUMBER);
        final GitHubSecretScanningAlertWithMetadata loaded = SecurityFixtures.secretAlert();
        when(github
                        .secretScanning()
                        .alerts()
                        .listRepoAlerts(eq(OWNER), eq(NAME), any(GitHubListSecretScanningAlertsFilter.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(Optional.of(loaded)).when(api).getAlert(OWNER, NAME, SecurityFixtures.SECRET_NUMBER);

        final List<SecretScanningAlert> alerts = sut.secretScanningAlerts().toList();

        assertThat(alerts)
                .singleElement()
                .satisfies(alert -> assertThat(alert.response()).isSameAs(loaded));
    }

    @Test
    void secretScanningAlerts_shouldSkipTheItem_whenAListedAlertIsGone() throws IOException {
        final GitHubSecretScanningAlert listed = mock(GitHubSecretScanningAlert.class);
        when(listed.number()).thenReturn(SecurityFixtures.SECRET_NUMBER);
        when(github
                        .secretScanning()
                        .alerts()
                        .listRepoAlerts(eq(OWNER), eq(NAME), any(GitHubListSecretScanningAlertsFilter.class))
                        .stream())
                .thenReturn(Stream.of(listed));
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, SecurityFixtures.SECRET_NUMBER);
        final Stream<SecretScanningAlert> alerts = sut.secretScanningAlerts();

        final long count = alerts.count();

        assertThat(count).isZero();
    }

    @Test
    void secretScanningAlerts_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.secretScanningAlerts(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void secretScanningAlert_shouldReturnTheAlert_whenGitHubHasIt() throws IOException {
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(Optional.of(SecurityFixtures.secretAlert()))
                .when(api)
                .getAlert(OWNER, NAME, SecurityFixtures.SECRET_NUMBER);

        final Optional<SecretScanningAlert> found = sut.secretScanningAlert(SecurityFixtures.SECRET_NUMBER);

        assertThat(found).isPresent();
    }

    @Test
    void secretScanningAlert_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubSecretScanningAlertsApi api = github.secretScanning().alerts();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, 1);

        final Optional<SecretScanningAlert> found = sut.secretScanningAlert(1);

        assertThat(found).isEmpty();
    }

    @Test
    void codeScanningAlerts_shouldLoadEachListedAlert_whenTheyExist() throws IOException {
        final GitHubCodeScanningAlertItems listed = mock(GitHubCodeScanningAlertItems.class);
        when(listed.number()).thenReturn(SecurityFixtures.CODE_NUMBER);
        final GitHubCodeScanningAlert loaded = SecurityFixtures.codeAlert();
        when(github.codeScanning().codeScanning().listAlertsForRepo(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(Optional.of(loaded)).when(api).getAlert(OWNER, NAME, SecurityFixtures.CODE_NUMBER);

        final List<CodeScanningAlert> alerts = sut.codeScanningAlerts().toList();

        assertThat(alerts)
                .singleElement()
                .satisfies(alert -> assertThat(alert.response()).isSameAs(loaded));
    }

    @Test
    void codeScanningAlerts_shouldSkipTheItem_whenAListedAlertIsGone() throws IOException {
        final GitHubCodeScanningAlertItems listed = mock(GitHubCodeScanningAlertItems.class);
        when(listed.number()).thenReturn(SecurityFixtures.CODE_NUMBER);
        when(github.codeScanning().codeScanning().listAlertsForRepo(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, SecurityFixtures.CODE_NUMBER);
        final Stream<CodeScanningAlert> alerts = sut.codeScanningAlerts();

        final long count = alerts.count();

        assertThat(count).isZero();
    }

    @Test
    void codeScanningAlert_shouldReturnTheAlert_whenGitHubHasIt() throws IOException {
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(Optional.of(SecurityFixtures.codeAlert()))
                .when(api)
                .getAlert(OWNER, NAME, SecurityFixtures.CODE_NUMBER);

        final Optional<CodeScanningAlert> found = sut.codeScanningAlert(SecurityFixtures.CODE_NUMBER);

        assertThat(found).isPresent();
    }

    @Test
    void codeScanningAlert_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubCodeScanningCodeScanningApi api = github.codeScanning().codeScanning();
        doReturn(Optional.empty()).when(api).getAlert(OWNER, NAME, 1);

        final Optional<CodeScanningAlert> found = sut.codeScanningAlert(1);

        assertThat(found).isEmpty();
    }

    @Test
    void security_shouldReturnTheSwitchesWithoutRequest_whenCalled() {
        final RepositorySecurity security = sut.security();

        assertThat(security).isNotNull();
    }
}
