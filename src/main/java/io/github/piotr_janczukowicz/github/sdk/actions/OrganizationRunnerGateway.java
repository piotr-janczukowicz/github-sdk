package io.github.piotr_janczukowicz.github.sdk.actions;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsSelfHostedRunnersApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubAddCustomLabelsToSelfHostedRunnerForOrgPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubAuthenticationToken;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListSelfHostedRunnersForOrgFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerApplication;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabels;
import io.github.piotr_janczukowicz.github.api.actions.GitHubSetCustomLabelsForSelfHostedRunnerForOrgPayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The self-hosted runner calls for an organization. */
final class OrganizationRunnerGateway implements RunnerGateway {

    private final GitHub github;
    private final String login;

    OrganizationRunnerGateway(final GitHub github, final String login) {
        this.github = github;
        this.login = login;
    }

    @Override
    public Stream<GitHubRunner> runners() throws IOException {
        return api().listSelfHostedRunnersForOrg(login).stream();
    }

    @Override
    public Stream<GitHubRunner> runners(final String name) throws IOException {
        return api()
                .listSelfHostedRunnersForOrg(
                        login, new GitHubListSelfHostedRunnersForOrgFilter(name), GitHubPageRequest.byPage())
                .stream();
    }

    @Override
    public Optional<GitHubRunner> runner(final long id) throws IOException {
        return api().getSelfHostedRunnerForOrg(login, id);
    }

    @Override
    public void delete(final long id) throws IOException {
        api().deleteSelfHostedRunnerFromOrg(login, id);
    }

    @Override
    public Optional<GitHubRunnerLabels> labels(final long id) throws IOException {
        return api().listLabelsForSelfHostedRunnerForOrg(login, id);
    }

    @Override
    public GitHubRunnerLabels addLabels(final long id, final List<String> labels) throws IOException {
        return api().addCustomLabelsToSelfHostedRunnerForOrg(
                        login, id, new GitHubAddCustomLabelsToSelfHostedRunnerForOrgPayload(labels));
    }

    @Override
    public GitHubRunnerLabels replaceLabels(final long id, final List<String> labels) throws IOException {
        return api().replaceCustomLabelsForSelfHostedRunnerForOrg(
                        login, id, new GitHubSetCustomLabelsForSelfHostedRunnerForOrgPayload(labels));
    }

    @Override
    public GitHubRunnerLabels removeLabel(final long id, final String label) throws IOException {
        return api().removeCustomLabelFromSelfHostedRunnerForOrg(login, id, label);
    }

    @Override
    public GitHubRunnerLabels removeAllLabels(final long id) throws IOException {
        return api().removeAllCustomLabelsFromSelfHostedRunnerForOrg(login, id);
    }

    @Override
    public List<GitHubRunnerApplication> applications() throws IOException {
        return api().listRunnerApplicationsForOrg(login);
    }

    @Override
    public GitHubAuthenticationToken registrationToken() throws IOException {
        return api().createRegistrationTokenForOrg(login);
    }

    @Override
    public GitHubAuthenticationToken removeToken() throws IOException {
        return api().createRemoveTokenForOrg(login);
    }

    private GitHubActionsSelfHostedRunnersApi api() {
        return github.actions().selfHostedRunners();
    }
}
