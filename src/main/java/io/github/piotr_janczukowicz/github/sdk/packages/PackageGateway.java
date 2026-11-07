package io.github.piotr_janczukowicz.github.sdk.packages;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The calls for the packages of one owner. GitHub has separate endpoints for an organization, a named user and the
 * authenticated user, and this interface hides which one is used.
 */
interface PackageGateway {

    Stream<GitHubPackage> packages(GitHubPackageType type) throws IOException;

    Optional<GitHubPackage> registryPackage(GitHubPackageType type, String name) throws IOException;

    void deletePackage(GitHubPackageType type, String name) throws IOException;

    void restorePackage(GitHubPackageType type, String name) throws IOException;

    Stream<GitHubPackageVersion> versions(GitHubPackageType type, String name) throws IOException;

    Optional<GitHubPackageVersion> version(GitHubPackageType type, String name, long id) throws IOException;

    void deleteVersion(GitHubPackageType type, String name, long id) throws IOException;

    void restoreVersion(GitHubPackageType type, String name, long id) throws IOException;
}
