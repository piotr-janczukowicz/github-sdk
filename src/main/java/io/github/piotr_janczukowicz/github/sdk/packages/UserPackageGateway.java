package io.github.piotr_janczukowicz.github.sdk.packages;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.packages.GitHubListPackagesFilter;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/** The package calls for a named user. */
final class UserPackageGateway implements PackageGateway {

    private final GitHub github;
    private final String login;

    UserPackageGateway(final GitHub github, final String login) {
        this.github = github;
        this.login = login;
    }

    @Override
    public Stream<GitHubPackage> packages(final GitHubPackageType type) throws IOException {
        return github.packages().listPackagesForUser(login, GitHubListPackagesFilter.of(type)).stream();
    }

    @Override
    public Optional<GitHubPackage> registryPackage(final GitHubPackageType type, final String name) throws IOException {
        return github.packages().getPackageForUser(login, type, name);
    }

    @Override
    public void deletePackage(final GitHubPackageType type, final String name) throws IOException {
        github.packages().deletePackageForUser(login, type, name);
    }

    @Override
    public void restorePackage(final GitHubPackageType type, final String name) throws IOException {
        github.packages().restorePackageForUser(login, type, name);
    }

    @Override
    public Stream<GitHubPackageVersion> versions(final GitHubPackageType type, final String name) throws IOException {
        return github.packages().listPackageVersionsForUser(login, type, name).stream();
    }

    @Override
    public Optional<GitHubPackageVersion> version(final GitHubPackageType type, final String name, final long id)
            throws IOException {
        return github.packages().getPackageVersionForUser(login, type, name, id);
    }

    @Override
    public void deleteVersion(final GitHubPackageType type, final String name, final long id) throws IOException {
        github.packages().deletePackageVersionForUser(login, type, name, id);
    }

    @Override
    public void restoreVersion(final GitHubPackageType type, final String name, final long id) throws IOException {
        github.packages().restorePackageVersionForUser(login, type, name, id);
    }
}
