package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateDeploymentPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateDeploymentResult;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateOrUpdateEnvironmentPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeployment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentApi;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentMergedBranch;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentsEnvironmentsApi;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubEnvironment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubListDeploymentsFilter;
import io.github.piotr_janczukowicz.github.api.pages.GitHubCreatePagesSitePayload;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesApi;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSite;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSourcePath;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.deployments.Deployment;
import io.github.piotr_janczukowicz.github.sdk.deployments.Environment;
import io.github.piotr_janczukowicz.github.sdk.fixtures.DeploymentFixtures;
import io.github.piotr_janczukowicz.github.sdk.pages.PagesSite;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryDeploymentsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void deployments_shouldStreamTheDeployments_whenTheyExist() throws IOException {
        final GitHubDeployment listed = DeploymentFixtures.deployment();
        when(github
                        .deployments()
                        .deployment()
                        .listDeployments(
                                eq(OWNER),
                                eq(NAME),
                                any(GitHubListDeploymentsFilter.class),
                                any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<Deployment> deployments = sut.deployments().toList();

        assertThat(deployments).singleElement().satisfies(deployment -> {
            assertThat(deployment.response()).isSameAs(listed);
            assertThat(deployment.repository()).isSameAs(sut);
        });
    }

    @Test
    void deployments_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.deployments(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void deployment_shouldReturnTheDeployment_whenGitHubHasIt() throws IOException {
        final GitHubDeploymentApi api = github.deployments().deployment();
        doReturn(Optional.of(DeploymentFixtures.deployment())).when(api).getDeployment(OWNER, NAME, 7007L);

        final Optional<Deployment> found = sut.deployment(7007L);

        assertThat(found).isPresent();
    }

    @Test
    void deployment_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubDeploymentApi api = github.deployments().deployment();
        doReturn(Optional.empty()).when(api).getDeployment(OWNER, NAME, 1L);

        final Optional<Deployment> found = sut.deployment(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void deploy_shouldSwitchAutoMergeOff_whenOnlyRefAndEnvironmentAreGiven() throws IOException {
        final GitHubDeployment created = DeploymentFixtures.deployment();
        final GitHubDeploymentApi api = github.deployments().deployment();
        doReturn(GitHubCreateDeploymentResult.ofCreated(created))
                .when(api)
                .createDeployment(eq(OWNER), eq(NAME), any(GitHubCreateDeploymentPayload.class));
        final ArgumentCaptor<GitHubCreateDeploymentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDeploymentPayload.class);

        final Deployment deployment = sut.deploy("main", "staging");

        verify(api).createDeployment(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().ref()).isEqualTo("main");
            softly.assertThat(payload.getValue().environment()).isEqualTo("staging");
            softly.assertThat(payload.getValue().autoMerge()).isFalse();
            softly.assertThat(payload.getValue().requiredContexts()).isNull();
            softly.assertThat(deployment.response()).isSameAs(created);
            softly.assertThat(deployment.repository()).isSameAs(sut);
        });
    }

    @Test
    void deploy_shouldThrowIllegalStateException_whenGitHubMergesInsteadOfDeploying() throws IOException {
        final GitHubDeploymentApi api = github.deployments().deployment();
        doReturn(GitHubCreateDeploymentResult.ofMergedBranch(new GitHubDeploymentMergedBranch("Auto-merged main")))
                .when(api)
                .createDeployment(eq(OWNER), eq(NAME), any(GitHubCreateDeploymentPayload.class));
        final GitHubCreateDeploymentPayload payload = mock(GitHubCreateDeploymentPayload.class);

        final Throwable thrown = catchThrowable(() -> sut.deploy(payload));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Auto-merged main");
    }

    @Test
    void deploy_shouldThrowIllegalArgumentException_whenRefIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.deploy(" ", "staging"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ref");
    }

    @Test
    void deploy_shouldThrowIllegalArgumentException_whenEnvironmentIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.deploy("main", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("environment");
    }

    @Test
    void deploy_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.deploy(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void environments_shouldStreamTheEnvironments_whenTheyExist() throws IOException {
        final GitHubEnvironment listed = DeploymentFixtures.environment();
        when(github
                        .deployments()
                        .environments()
                        .getAllEnvironments(eq(OWNER), eq(NAME), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<Environment> environments = sut.environments().toList();

        assertThat(environments).singleElement().satisfies(environment -> {
            assertThat(environment.response()).isSameAs(listed);
            assertThat(environment.repository()).isSameAs(sut);
        });
    }

    @Test
    void environment_shouldReturnTheEnvironment_whenGitHubHasIt() throws IOException {
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();
        doReturn(Optional.of(DeploymentFixtures.environment())).when(api).getEnvironment(OWNER, NAME, "staging");

        final Optional<Environment> found = sut.environment("staging");

        assertThat(found).isPresent();
    }

    @Test
    void environment_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();
        doReturn(Optional.empty()).when(api).getEnvironment(OWNER, NAME, "none");

        final Optional<Environment> found = sut.environment("none");

        assertThat(found).isEmpty();
    }

    @Test
    void environment_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.environment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("environmentName");
    }

    @Test
    void saveEnvironment_shouldSendAnEmptyPayload_whenOnlyTheNameIsGiven() throws IOException {
        final GitHubEnvironment saved = DeploymentFixtures.environment();
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();
        doReturn(saved)
                .when(api)
                .createOrUpdateEnvironment(
                        eq(OWNER), eq(NAME), eq("staging"), any(GitHubCreateOrUpdateEnvironmentPayload.class));
        final ArgumentCaptor<GitHubCreateOrUpdateEnvironmentPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateOrUpdateEnvironmentPayload.class);

        final Environment environment = sut.saveEnvironment("staging");

        verify(api).createOrUpdateEnvironment(eq(OWNER), eq(NAME), eq("staging"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().waitTimer()).isNull();
            softly.assertThat(payload.getValue().reviewers()).isNullOrEmpty();
            softly.assertThat(environment.response()).isSameAs(saved);
        });
    }

    @Test
    void saveEnvironment_shouldSendThePayload_whenOneIsGiven() throws IOException {
        final GitHubCreateOrUpdateEnvironmentPayload payload = mock(GitHubCreateOrUpdateEnvironmentPayload.class);
        final GitHubDeploymentsEnvironmentsApi api = github.deployments().environments();
        doReturn(DeploymentFixtures.environment()).when(api).createOrUpdateEnvironment(OWNER, NAME, "staging", payload);

        final Environment environment = sut.saveEnvironment("staging", payload);

        assertThat(environment.name()).isEqualTo("staging");
    }

    @Test
    void saveEnvironment_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.saveEnvironment("staging", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void pages_shouldReturnTheSite_whenTheRepositoryHasOne() throws IOException {
        final GitHubPagesSite site = DeploymentFixtures.pagesSite();
        final GitHubPagesApi api = github.pages();
        doReturn(Optional.of(site)).when(api).getPages(OWNER, NAME);

        final Optional<PagesSite> found = sut.pages();

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(site);
    }

    @Test
    void pages_shouldBeEmpty_whenTheRepositoryHasNone() throws IOException {
        final GitHubPagesApi api = github.pages();
        doReturn(Optional.empty()).when(api).getPages(OWNER, NAME);

        final Optional<PagesSite> found = sut.pages();

        assertThat(found).isEmpty();
    }

    @Test
    void enablePages_shouldBuildFromTheRootOfTheBranch_whenOnlyTheBranchIsGiven() throws IOException {
        final GitHubPagesSite site = DeploymentFixtures.pagesSite();
        final GitHubPagesApi api = github.pages();
        doReturn(site).when(api).createPagesSite(eq(OWNER), eq(NAME), any(GitHubCreatePagesSitePayload.class));
        final ArgumentCaptor<GitHubCreatePagesSitePayload> payload =
                ArgumentCaptor.forClass(GitHubCreatePagesSitePayload.class);

        final PagesSite created = sut.enablePages("main");

        verify(api).createPagesSite(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().source().branch()).isEqualTo("main");
            softly.assertThat(payload.getValue().source().path()).isEqualTo(GitHubPagesSourcePath.ROOT);
            softly.assertThat(created.response()).isSameAs(site);
        });
    }

    @Test
    void enablePages_shouldThrowIllegalArgumentException_whenBranchIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.enablePages(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("branch");
    }

    @Test
    void enablePages_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.enablePages((GitHubCreatePagesSitePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }
}
