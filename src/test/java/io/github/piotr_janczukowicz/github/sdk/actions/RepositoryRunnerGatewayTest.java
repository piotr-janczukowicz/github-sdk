package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsSelfHostedRunnersApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubAddCustomLabelsToSelfHostedRunnerForRepoPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubAuthenticationToken;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListSelfHostedRunnersForRepoFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerApplication;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabels;
import io.github.piotr_janczukowicz.github.api.actions.GitHubSetCustomLabelsForSelfHostedRunnerForRepoPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RunnerFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryRunnerGatewayTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = RunnerFixtures.RUNNER_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubActionsSelfHostedRunnersApi api = github.actions().selfHostedRunners();
    private final RepositoryRunnerGateway sut = new RepositoryRunnerGateway(github, OWNER, NAME);

    @Test
    void runners_shouldStreamTheRunners_whenTheyExist() throws IOException {
        final GitHubRunner runner = RunnerFixtures.runner();
        when(api.listSelfHostedRunnersForRepo(OWNER, NAME).stream()).thenReturn(Stream.of(runner));

        final List<GitHubRunner> runners = sut.runners().toList();

        assertThat(runners).containsExactly(runner);
    }

    @Test
    void runners_shouldStreamTheMatches_whenANameIsGiven() throws IOException {
        final GitHubRunner runner = RunnerFixtures.runner();
        when(api
                        .listSelfHostedRunnersForRepo(
                                eq(OWNER),
                                eq(NAME),
                                eq(new GitHubListSelfHostedRunnersForRepoFilter("build-1")),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(runner));

        final List<GitHubRunner> runners = sut.runners("build-1").toList();

        assertThat(runners).containsExactly(runner);
    }

    @Test
    void runner_shouldReturnTheRunner_whenItExists() throws IOException {
        final GitHubRunner runner = RunnerFixtures.runner();
        when(api.getSelfHostedRunnerForRepo(OWNER, NAME, ID)).thenReturn(Optional.of(runner));

        final Optional<GitHubRunner> found = sut.runner(ID);

        assertThat(found).containsSame(runner);
    }

    @Test
    void delete_shouldDeleteTheRunner_whenCalled() throws IOException {
        sut.delete(ID);

        verify(api).deleteSelfHostedRunnerFromRepo(OWNER, NAME, ID);
    }

    @Test
    void labels_shouldReturnTheLabels_whenTheyExist() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(api.listLabelsForSelfHostedRunnerForRepo(OWNER, NAME, ID)).thenReturn(Optional.of(labels));

        final Optional<GitHubRunnerLabels> found = sut.labels(ID);

        assertThat(found).containsSame(labels);
    }

    @Test
    void addLabels_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(api.addCustomLabelsToSelfHostedRunnerForRepo(
                        eq(OWNER),
                        eq(NAME),
                        eq(ID),
                        eq(new GitHubAddCustomLabelsToSelfHostedRunnerForRepoPayload(List.of("gpu")))))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.addLabels(ID, List.of("gpu"));

        assertThat(result).isSameAs(labels);
    }

    @Test
    void replaceLabels_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(api.replaceCustomLabelsForSelfHostedRunnerForRepo(
                        eq(OWNER),
                        eq(NAME),
                        eq(ID),
                        eq(new GitHubSetCustomLabelsForSelfHostedRunnerForRepoPayload(List.of("gpu")))))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.replaceLabels(ID, List.of("gpu"));

        assertThat(result).isSameAs(labels);
    }

    @Test
    void removeLabel_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(api.removeCustomLabelFromSelfHostedRunnerForRepo(OWNER, NAME, ID, "gpu"))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.removeLabel(ID, "gpu");

        assertThat(result).isSameAs(labels);
    }

    @Test
    void removeAllLabels_shouldReturnTheLabels_whenCalled() throws IOException {
        final GitHubRunnerLabels labels = mock(GitHubRunnerLabels.class);
        when(api.removeAllCustomLabelsFromSelfHostedRunnerForRepo(OWNER, NAME, ID))
                .thenReturn(labels);

        final GitHubRunnerLabels result = sut.removeAllLabels(ID);

        assertThat(result).isSameAs(labels);
    }

    @Test
    void applications_shouldReturnTheDownloads_whenCalled() throws IOException {
        final GitHubRunnerApplication application = mock(GitHubRunnerApplication.class);
        when(api.listRunnerApplicationsForRepo(OWNER, NAME)).thenReturn(List.of(application));

        final List<GitHubRunnerApplication> applications = sut.applications();

        assertThat(applications).containsExactly(application);
    }

    @Test
    void registrationToken_shouldReturnTheToken_whenCalled() throws IOException {
        final GitHubAuthenticationToken token = mock(GitHubAuthenticationToken.class);
        when(api.createRegistrationTokenForRepo(OWNER, NAME)).thenReturn(token);

        final GitHubAuthenticationToken result = sut.registrationToken();

        assertThat(result).isSameAs(token);
    }

    @Test
    void removeToken_shouldReturnTheToken_whenCalled() throws IOException {
        final GitHubAuthenticationToken token = mock(GitHubAuthenticationToken.class);
        when(api.createRemoveTokenForRepo(OWNER, NAME)).thenReturn(token);

        final GitHubAuthenticationToken result = sut.removeToken();

        assertThat(result).isSameAs(token);
    }
}
