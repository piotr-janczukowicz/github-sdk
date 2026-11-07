package io.github.piotr_janczukowicz.github.sdk.actions;

import io.github.piotr_janczukowicz.github.api.actions.GitHubAuthenticationToken;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerApplication;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunnerLabels;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The self-hosted runner calls for one repository or organization. GitHub has separate endpoints for each, and this
 * interface hides which one is used.
 */
interface RunnerGateway {

    Stream<GitHubRunner> runners() throws IOException;

    Stream<GitHubRunner> runners(String name) throws IOException;

    Optional<GitHubRunner> runner(long id) throws IOException;

    void delete(long id) throws IOException;

    Optional<GitHubRunnerLabels> labels(long id) throws IOException;

    GitHubRunnerLabels addLabels(long id, List<String> labels) throws IOException;

    GitHubRunnerLabels replaceLabels(long id, List<String> labels) throws IOException;

    GitHubRunnerLabels removeLabel(long id, String label) throws IOException;

    GitHubRunnerLabels removeAllLabels(long id) throws IOException;

    List<GitHubRunnerApplication> applications() throws IOException;

    GitHubAuthenticationToken registrationToken() throws IOException;

    GitHubAuthenticationToken removeToken() throws IOException;
}
