package io.github.piotr_janczukowicz.github.sdk.packages;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.packages.GitHubListPackagesFilter;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/** The package calls for the authenticated user. */
final class AuthenticatedPackageGateway implements PackageGateway {

    private final GitHub github;

    AuthenticatedPackageGateway(final GitHub github) {
        this.github = github;
    }

    @Override
    public Stream<GitHubPackage> packages(final GitHubPackageType type) throws IOException {
        return github.packages().listPackagesForAuthenticatedUser(GitHubListPackagesFilter.of(type)).stream();
    }

    @Override
    public Optional<GitHubPackage> registryPackage(final GitHubPackageType type, final String name) throws IOException {
        return github.packages().getPackageForAuthenticatedUser(type, name);
    }

    @Override
    public void deletePackage(final GitHubPackageType type, final String name) throws IOException {
        github.packages().deletePackageForAuthenticatedUser(type, name);
    }

    @Override
    public void restorePackage(final GitHubPackageType type, final String name) throws IOException {
        github.packages().restorePackageForAuthenticatedUser(type, name);
    }

    @Override
    public Stream<GitHubPackageVersion> versions(final GitHubPackageType type, final String name) throws IOException {
        return github.packages().listPackageVersionsForAuthenticatedUser(type, name).stream();
    }

    @Override
    public Optional<GitHubPackageVersion> version(final GitHubPackageType type, final String name, final long id)
            throws IOException {
        return github.packages().getPackageVersionForAuthenticatedUser(type, name, id);
    }

    @Override
    public void deleteVersion(final GitHubPackageType type, final String name, final long id) throws IOException {
        github.packages().deletePackageVersionForAuthenticatedUser(type, name, id);
    }

    @Override
    public void restoreVersion(final GitHubPackageType type, final String name, final long id) throws IOException {
        github.packages().restorePackageVersionForAuthenticatedUser(type, name, id);
    }
}
