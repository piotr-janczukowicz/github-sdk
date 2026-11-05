package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatus;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatusState;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.deployments.Deployment;
import io.github.piotr_janczukowicz.github.sdk.deployments.DeploymentState;
import io.github.piotr_janczukowicz.github.sdk.deployments.Environment;
import io.github.piotr_janczukowicz.github.sdk.pages.PagesSite;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class DeploymentsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void deployment_shouldBeCreatedReportedListedAndDeleted_whenItIsManaged() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String base = repository.fetch().orElseThrow().response().defaultBranch();
        final String environment = ENV.name("env");
        final Deployment created = deploy(repository, base, environment);
        try {
            final GitHubDeploymentStatus pending = created.createStatus(DeploymentState.IN_PROGRESS, "Started");
            final GitHubDeploymentStatus done = created.createStatus(DeploymentState.SUCCESS);
            final GitHubDeploymentStatus latest = created.latestStatus().orElseThrow();
            final List<GitHubDeploymentStatus> statuses = created.statuses().toList();
            final boolean isListed =
                    repository.deployments().limit(20).anyMatch(deployment -> deployment.id() == created.id());
            created.createStatus(DeploymentState.INACTIVE);

            assertSoftly(softly -> {
                softly.assertThat(created.ref()).isEqualTo(base);
                softly.assertThat(created.environment()).isEqualTo(environment);
                softly.assertThat(pending.state()).isEqualTo(GitHubDeploymentStatusState.IN_PROGRESS);
                softly.assertThat(done.state()).isEqualTo(GitHubDeploymentStatusState.SUCCESS);
                softly.assertThat(latest.id()).isEqualTo(done.id());
                softly.assertThat(statuses).hasSizeGreaterThanOrEqualTo(2);
                softly.assertThat(isListed).isTrue();
            });
        } finally {
            created.delete();
        }
        assertThat(repository.deployment(created.id())).isEmpty();
    }

    private static Deployment deploy(final Repository repository, final String ref, final String environment)
            throws IOException {
        try {
            return repository.deploy(ref, environment);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Deployments permission, so GitHub refuses managing deployments");
            throw e;
        }
    }

    @Test
    void environment_shouldBeSavedListedReloadedAndDeleted_whenItIsManaged() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String name = ENV.name("saved");
        final Environment saved = save(repository, name);
        try {
            final List<Environment> listed = repository.environments().toList();
            final Environment reloaded = saved.refresh();

            assertSoftly(softly -> {
                softly.assertThat(saved.name()).isEqualTo(name);
                softly.assertThat(listed).extracting(Environment::name).contains(name);
                softly.assertThat(reloaded.name()).isEqualTo(name);
            });
        } finally {
            saved.delete();
        }
        assertThat(repository.environment(name)).isEmpty();
    }

    private static Environment save(final Repository repository, final String name) throws IOException {
        try {
            return repository.saveEnvironment(name);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Environments permission, so GitHub refuses managing environments");
            throw e;
        }
    }

    @Test
    void pages_shouldBeReadable_whenTheRepositoryHasNoSiteOrOne() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<PagesSite> site = readPages(repository);

        assertThat(site)
                .satisfiesAnyOf(
                        found -> assertThat(found).isEmpty(),
                        found -> assertThat(found.orElseThrow().htmlUrl()).isPresent());
    }

    private static Optional<PagesSite> readPages(final Repository repository) throws IOException {
        try {
            return repository.pages();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Pages permission, so GitHub refuses reading the Pages site");
            throw e;
        }
    }
}
