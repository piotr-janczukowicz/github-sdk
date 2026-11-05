package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubAuthenticationToken;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerApplication;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabels;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RunnerFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class SelfHostedRunnerTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = RunnerFixtures.RUNNER_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final Repository repository = sdk.repository(OWNER, NAME);
    private final SelfHostedRunners runners = repository.selfHostedRunners();
    private final GitHubRunner response = RunnerFixtures.runner();
    private final SelfHostedRunner sut = new SelfHostedRunner(runners, response);

    @Test
    void accessors_shouldExposeTheResponse_whenRunnerIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("build-1");
            softly.assertThat(sut.os()).isEqualTo("linux");
            softly.assertThat(sut.status()).isEqualTo("online");
            softly.assertThat(sut.isBusy()).isTrue();
            softly.assertThat(sut.labels()).hasSize(1);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new SelfHostedRunner(runners, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    @Test
    void refresh_shouldReturnTheReloadedRunner_whenItExists() throws IOException {
        final GitHubRunner reloaded = RunnerFixtures.runner();
        when(github.actions().selfHostedRunners().getSelfHostedRunnerForRepo(OWNER, NAME, ID))
                .thenReturn(Optional.of(reloaded));

        final SelfHostedRunner result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheRunnerIsGone() throws IOException {
        when(github.actions().selfHostedRunners().getSelfHostedRunnerForRepo(OWNER, NAME, ID))
                .thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void addLabels_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(github.actions()
                        .selfHostedRunners()
                        .addCustomLabelsToSelfHostedRunnerForRepo(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.addLabels(List.of("gpu"));

        assertThat(result).isSameAs(labels);
    }

    @Test
    void replaceLabels_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(github.actions()
                        .selfHostedRunners()
                        .replaceCustomLabelsForSelfHostedRunnerForRepo(eq(OWNER), eq(NAME), eq(ID), any()))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.replaceLabels(List.of("gpu"));

        assertThat(result).isSameAs(labels);
    }

    @Test
    void removeLabel_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(github.actions().selfHostedRunners().removeCustomLabelFromSelfHostedRunnerForRepo(OWNER, NAME, ID, "gpu"))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.removeLabel("gpu");

        assertThat(result).isSameAs(labels);
    }

    @Test
    void removeAllLabels_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(github.actions().selfHostedRunners().removeAllCustomLabelsFromSelfHostedRunnerForRepo(OWNER, NAME, ID))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.removeAllLabels();

        assertThat(result).isSameAs(labels);
    }

    @Test
    void labelMethods_shouldThrowNullPointerException_whenLabelsAreNull() {
        final Throwable addThrown = catchThrowable(() -> sut.addLabels(null));
        final Throwable replaceThrown = catchThrowable(() -> sut.replaceLabels(null));

        assertSoftly(softly -> {
            softly.assertThat(addThrown)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("labels");
            softly.assertThat(replaceThrown)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("labels");
        });
    }

    @Test
    void removeLabel_shouldThrowIllegalArgumentException_whenLabelIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.removeLabel(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("label");
    }

    @Test
    void delete_shouldDeleteTheRunner_whenCalled() throws IOException {
        sut.delete();

        verify(github.actions().selfHostedRunners()).deleteSelfHostedRunnerFromRepo(OWNER, NAME, ID);
    }

    @Test
    void all_shouldWrapTheRunners_whenTheyExist() throws IOException {
        final GitHubRunner listed = RunnerFixtures.runner();
        when(github.actions().selfHostedRunners().listSelfHostedRunnersForRepo(OWNER, NAME).stream())
                .thenReturn(Stream.of(listed));

        final List<SelfHostedRunner> all = runners.all().toList();

        assertThat(all)
                .singleElement()
                .satisfies(runner -> assertThat(runner.response()).isSameAs(listed));
    }

    @Test
    void named_shouldWrapTheMatches_whenANameIsGiven() throws IOException {
        final GitHubRunner listed = RunnerFixtures.runner();
        when(github
                        .actions()
                        .selfHostedRunners()
                        .listSelfHostedRunnersForRepo(eq(OWNER), eq(NAME), any(), any())
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<SelfHostedRunner> found = runners.named("build-1").toList();

        assertThat(found)
                .singleElement()
                .satisfies(runner -> assertThat(runner.response()).isSameAs(listed));
    }

    @Test
    void named_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> runners.named(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void find_shouldReturnTheRunner_whenItExists() throws IOException {
        final GitHubRunner loaded = RunnerFixtures.runner();
        when(github.actions().selfHostedRunners().getSelfHostedRunnerForRepo(OWNER, NAME, ID))
                .thenReturn(Optional.of(loaded));

        final Optional<SelfHostedRunner> found = runners.find(ID);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void applications_shouldReturnTheDownloads_whenCalled() throws IOException {
        final GitHubRunnerApplication application = mock(GitHubRunnerApplication.class);
        when(github.actions().selfHostedRunners().listRunnerApplicationsForRepo(OWNER, NAME))
                .thenReturn(List.of(application));

        final List<GitHubRunnerApplication> applications = runners.applications();

        assertThat(applications).containsExactly(application);
    }

    @Test
    void tokens_shouldReturnTheTokens_whenCalled() throws IOException {
        final GitHubAuthenticationToken registration = mock(GitHubAuthenticationToken.class);
        final GitHubAuthenticationToken removal = mock(GitHubAuthenticationToken.class);
        when(github.actions().selfHostedRunners().createRegistrationTokenForRepo(OWNER, NAME))
                .thenReturn(registration);
        when(github.actions().selfHostedRunners().createRemoveTokenForRepo(OWNER, NAME))
                .thenReturn(removal);

        final GitHubAuthenticationToken registrationResult = runners.registrationToken();
        final GitHubAuthenticationToken removalResult = runners.removeToken();

        assertSoftly(softly -> {
            softly.assertThat(registrationResult).isSameAs(registration);
            softly.assertThat(removalResult).isSameAs(removal);
        });
    }

    @Test
    void forOrganization_shouldReturnTheOrganizationRunners_whenCalled() throws IOException {
        final GitHubRunner listed = RunnerFixtures.runner();
        when(github.actions().selfHostedRunners().listSelfHostedRunnersForOrg("acme").stream())
                .thenReturn(Stream.of(listed));

        final List<SelfHostedRunner> all =
                SelfHostedRunners.forOrganization(sdk, "acme").all().toList();

        assertThat(all)
                .singleElement()
                .satisfies(runner -> assertThat(runner.response()).isSameAs(listed));
    }

    @Test
    void forOrganization_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> SelfHostedRunners.forOrganization(sdk, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void forOrganization_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> SelfHostedRunners.forOrganization(null, "acme"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void forRepository_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> SelfHostedRunners.forRepository(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
