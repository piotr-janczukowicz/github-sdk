package io.github.piotr_janczukowicz.github.sdk.smoke;

import static java.util.Objects.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.client.GitHubCaller;
import io.github.piotr_janczukowicz.github.client.GitHubClient;
import io.github.piotr_janczukowicz.github.client.GitHubClients;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.http.client.authentication.AuthenticationProviders;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;

final class SmokeEnvironment {

    static final String TOKEN_VARIABLE = "SMOKE_GITHUB_TOKEN";
    static final String REPO_VARIABLE = "SMOKE_GITHUB_REPO";

    private static final String DEFAULT_REPO = "piotr-janczukowicz/github-api-smoke";
    private static final String USER_AGENT = "github-sdk-smoke/1.0";
    private static final String RUN_ID = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
    private static final SmokeEnvironment FROM_ENV = of(System.getenv());

    private final String token;
    private final String owner;
    private final String repo;
    private GitHubClient client;
    private GitHub github;
    private GitHubSdk sdk;

    private SmokeEnvironment(final String token, final String owner, final String repo) {
        this.token = token;
        this.owner = owner;
        this.repo = repo;
    }

    static SmokeEnvironment fromEnv() {
        return FROM_ENV;
    }

    static SmokeEnvironment of(final Map<String, String> env) {
        requireNonNull(env, "env");
        final String fullName = env.getOrDefault(REPO_VARIABLE, DEFAULT_REPO);
        final String[] parts = fullName.split("/", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(REPO_VARIABLE + " must be owner/name, but was: " + fullName);
        }
        return new SmokeEnvironment(env.getOrDefault(TOKEN_VARIABLE, ""), parts[0], parts[1]);
    }

    boolean configured() {
        return !token.isBlank();
    }

    String token() {
        return token;
    }

    String owner() {
        return owner;
    }

    String repo() {
        return repo;
    }

    String runId() {
        return RUN_ID;
    }

    String name(final String suffix) {
        return "smoke-" + RUN_ID + "-" + suffix;
    }

    void requireConfigured() {
        Assumptions.assumeTrue(configured(), TOKEN_VARIABLE + " is not set");
        SandboxGuard.requireSandbox(this);
    }

    synchronized GitHubClient client() {
        if (client == null) {
            client = GitHubClients.create(AuthenticationProviders.bearer(token), USER_AGENT);
        }
        return client;
    }

    synchronized GitHub github() {
        if (github == null) {
            github = new GitHub(GitHubCaller.of(client()));
        }
        return github;
    }

    synchronized GitHubSdk sdk() {
        if (sdk == null) {
            sdk = GitHubSdk.of(github());
        }
        return sdk;
    }

    @Override
    public String toString() {
        return "SmokeEnvironment[" + owner + "/" + repo + ", run " + RUN_ID + "]";
    }
}
