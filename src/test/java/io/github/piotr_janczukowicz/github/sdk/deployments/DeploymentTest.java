package io.github.piotr_janczukowicz.github.sdk.deployments;

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
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateDeploymentStatusPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeployment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentApi;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentBuilder;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatus;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatusState;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentsStatusesApi;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.DeploymentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class DeploymentTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = DeploymentFixtures.DEPLOYMENT_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubDeployment response = DeploymentFixtures.deployment();
    private final Deployment sut = new Deployment(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenDeploymentIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.ref()).isEqualTo("main");
            softly.assertThat(sut.sha()).isEqualTo(DeploymentFixtures.SHA);
            softly.assertThat(sut.environment()).isEqualTo("staging");
            softly.assertThat(sut.description()).hasValue("Release");
            softly.assertThat(sut.creator()).hasValue("octocat");
            softly.assertThat(sut.isProduction()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenGitHubReportsNothing() {
        final Deployment bare = new Deployment(repository, DeploymentFixtures.bareDeployment());

        assertSoftly(softly -> {
            softly.assertThat(bare.description()).isEmpty();
            softly.assertThat(bare.creator()).isEmpty();
            softly.assertThat(bare.isProduction()).isFalse();
        });
    }

    @Test
    void statuses_shouldStreamTheStatuses_whenTheyExist() throws IOException {
        final GitHubDeploymentStatus listed = DeploymentFixtures.status();
        when(github
                        .deployments()
                        .statuses()
                        .listDeploymentStatuses(eq(OWNER), eq(NAME), eq(ID), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<GitHubDeploymentStatus> statuses = sut.statuses().toList();

        assertThat(statuses).containsExactly(listed);
    }

    @Test
    void latestStatus_shouldReturnTheFirstStatus_whenTheDeploymentHasStatuses() throws IOException {
        final GitHubDeploymentStatus newest = DeploymentFixtures.status();
        when(github
                        .deployments()
                        .statuses()
                        .listDeploymentStatuses(eq(OWNER), eq(NAME), eq(ID), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(newest));

        final Optional<GitHubDeploymentStatus> latest = sut.latestStatus();

        assertThat(latest).containsSame(newest);
    }

    @Test
    void latestStatus_shouldBeEmpty_whenTheDeploymentHasNoStatus() throws IOException {
        when(github
                        .deployments()
                        .statuses()
                        .listDeploymentStatuses(eq(OWNER), eq(NAME), eq(ID), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.empty());

        final Optional<GitHubDeploymentStatus> latest = sut.latestStatus();

        assertThat(latest).isEmpty();
    }

    @Test
    void createStatus_shouldSendOnlyTheState_whenNoDescriptionIsGiven() throws IOException {
        final GitHubDeploymentStatus created = DeploymentFixtures.status();
        final GitHubDeploymentsStatusesApi api = github.deployments().statuses();
        doReturn(created)
                .when(api)
                .createDeploymentStatus(eq(OWNER), eq(NAME), eq(ID), any(GitHubCreateDeploymentStatusPayload.class));
        final ArgumentCaptor<GitHubCreateDeploymentStatusPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDeploymentStatusPayload.class);

        final GitHubDeploymentStatus result = sut.createStatus(DeploymentState.IN_PROGRESS);

        verify(api).createDeploymentStatus(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubDeploymentStatusState.IN_PROGRESS);
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(result).isSameAs(created);
        });
    }

    @Test
    void createStatus_shouldSendTheDescription_whenOneIsGiven() throws IOException {
        final GitHubDeploymentsStatusesApi api = github.deployments().statuses();
        doReturn(DeploymentFixtures.status())
                .when(api)
                .createDeploymentStatus(eq(OWNER), eq(NAME), eq(ID), any(GitHubCreateDeploymentStatusPayload.class));
        final ArgumentCaptor<GitHubCreateDeploymentStatusPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDeploymentStatusPayload.class);

        sut.createStatus(DeploymentState.SUCCESS, "Done");

        verify(api).createDeploymentStatus(eq(OWNER), eq(NAME), eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().state()).isEqualTo(GitHubDeploymentStatusState.SUCCESS);
            softly.assertThat(payload.getValue().description()).isEqualTo("Done");
        });
    }

    @Test
    void createStatus_shouldThrowNullPointerException_whenStateIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createStatus((DeploymentState) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("state");
    }

    @Test
    void createStatus_shouldThrowNullPointerException_whenDescriptionIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createStatus(DeploymentState.SUCCESS, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("description");
    }

    @Test
    void createStatus_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createStatus((GitHubCreateDeploymentStatusPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void refresh_shouldReturnTheReloadedDeployment_whenItExists() throws IOException {
        final GitHubDeployment reloaded =
                GitHubDeploymentBuilder.builder(response).description("Changed").build();
        final GitHubDeploymentApi api = github.deployments().deployment();
        doReturn(Optional.of(reloaded)).when(api).getDeployment(OWNER, NAME, ID);

        final Deployment result = sut.refresh();

        assertThat(result.description()).hasValue("Changed");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheDeploymentIsGone() throws IOException {
        final GitHubDeploymentApi api = github.deployments().deployment();
        doReturn(Optional.empty()).when(api).getDeployment(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldDeleteTheDeployment_whenCalled() throws IOException {
        final GitHubDeploymentApi api = github.deployments().deployment();

        sut.delete();

        verify(api).deleteDeployment(OWNER, NAME, ID);
    }
}
