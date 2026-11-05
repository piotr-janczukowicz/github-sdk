package io.github.piotr_janczukowicz.github.sdk.actions;

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
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The self-hosted runner calls for a repository. */
final class RepositoryRunnerGateway implements RunnerGateway {

    private final GitHub github;
    private final String owner;
    private final String name;

    RepositoryRunnerGateway(final GitHub github, final String owner, final String name) {
        this.github = github;
        this.owner = owner;
        this.name = name;
    }

    @Override
    public Stream<GitHubRunner> runners() throws IOException {
        return api().listSelfHostedRunnersForRepo(owner, name).stream();
    }

    @Override
    public Stream<GitHubRunner> runners(final String runnerName) throws IOException {
        return api()
                .listSelfHostedRunnersForRepo(
                        owner,
                        name,
                        new GitHubListSelfHostedRunnersForRepoFilter(runnerName),
                        GitHubPageRequest.byPage())
                .stream();
    }

    @Override
    public Optional<GitHubRunner> runner(final long id) throws IOException {
        return api().getSelfHostedRunnerForRepo(owner, name, id);
    }

    @Override
    public void delete(final long id) throws IOException {
        api().deleteSelfHostedRunnerFromRepo(owner, name, id);
    }

    @Override
    public Optional<GitHubRunnerLabels> labels(final long id) throws IOException {
        return api().listLabelsForSelfHostedRunnerForRepo(owner, name, id);
    }

    @Override
    public GitHubRunnerLabels addLabels(final long id, final List<String> labels) throws IOException {
        return api().addCustomLabelsToSelfHostedRunnerForRepo(
                        owner, name, id, new GitHubAddCustomLabelsToSelfHostedRunnerForRepoPayload(labels));
    }

    @Override
    public GitHubRunnerLabels replaceLabels(final long id, final List<String> labels) throws IOException {
        return api().replaceCustomLabelsForSelfHostedRunnerForRepo(
                        owner, name, id, new GitHubSetCustomLabelsForSelfHostedRunnerForRepoPayload(labels));
    }

    @Override
    public GitHubRunnerLabels removeLabel(final long id, final String label) throws IOException {
        return api().removeCustomLabelFromSelfHostedRunnerForRepo(owner, name, id, label);
    }

    @Override
    public GitHubRunnerLabels removeAllLabels(final long id) throws IOException {
        return api().removeAllCustomLabelsFromSelfHostedRunnerForRepo(owner, name, id);
    }

    @Override
    public List<GitHubRunnerApplication> applications() throws IOException {
        return api().listRunnerApplicationsForRepo(owner, name);
    }

    @Override
    public GitHubAuthenticationToken registrationToken() throws IOException {
        return api().createRegistrationTokenForRepo(owner, name);
    }

    @Override
    public GitHubAuthenticationToken removeToken() throws IOException {
        return api().createRemoveTokenForRepo(owner, name);
    }

    private GitHubActionsSelfHostedRunnersApi api() {
        return github.actions().selfHostedRunners();
    }
}
