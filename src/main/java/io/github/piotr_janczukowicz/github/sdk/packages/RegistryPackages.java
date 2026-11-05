package io.github.piotr_janczukowicz.github.sdk.packages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The GitHub Packages of one organization, user or the authenticated user. {@code Organization.registryPackages(...)},
 * {@code User.registryPackages(...)} and {@code Me.registryPackages(...)} are the usual way in; this type exists so all
 * owner kinds share the same package objects. GitHub's REST API can list, read, delete and restore packages, but not
 * publish them, and it does not support fine-grained tokens: use a classic token with {@code read:packages} (and
 * {@code delete:packages} to delete).
 */
public final class RegistryPackages {

    private final PackageGateway gateway;

    private RegistryPackages(final PackageGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the packages of an organization; no request is made.
     *
     * @param sdk the SDK to call GitHub with
     * @param login the organization login
     * @return the packages handle
     * @throws NullPointerException if {@code sdk} or {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     */
    public static RegistryPackages forOrganization(final GitHubSdk sdk, final String login) {
        return new RegistryPackages(new OrganizationPackageGateway(
                Internals.github(requireNonNull(sdk, "sdk")), requireNotBlank(login, "login")));
    }

    /**
     * Returns the packages of a user; no request is made.
     *
     * @param sdk the SDK to call GitHub with
     * @param login the user login
     * @return the packages handle
     * @throws NullPointerException if {@code sdk} or {@code login} is {@code null}
     * @throws IllegalArgumentException if {@code login} is blank
     */
    public static RegistryPackages forUser(final GitHubSdk sdk, final String login) {
        return new RegistryPackages(
                new UserPackageGateway(Internals.github(requireNonNull(sdk, "sdk")), requireNotBlank(login, "login")));
    }

    /**
     * Returns the packages of the authenticated user; no request is made.
     *
     * @param sdk the SDK to call GitHub with
     * @return the packages handle
     * @throws NullPointerException if {@code sdk} is {@code null}
     */
    public static RegistryPackages forAuthenticatedUser(final GitHubSdk sdk) {
        return new RegistryPackages(new AuthenticatedPackageGateway(Internals.github(requireNonNull(sdk, "sdk"))));
    }

    /**
     * Lists the packages of one type ({@code GET /orgs/{org}/packages}, {@code GET /users/{username}/packages} or
     * {@code GET /user/packages}). GitHub requires the type. The stream is lazy and can be used once.
     *
     * @param type the package type
     * @return the packages
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryPackage> all(final GitHubPackageType type) throws IOException {
        return gateway.packages(requireNonNull(type, "type")).map(listed -> new RegistryPackage(gateway, listed));
    }

    /**
     * Loads one package ({@code GET /orgs/{org}/packages/{package_type}/{package_name}} or the user variants).
     *
     * @param type the package type
     * @param name the package name
     * @return the package, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code type} or {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<RegistryPackage> find(final GitHubPackageType type, final String name) throws IOException {
        return gateway.registryPackage(requireNonNull(type, "type"), requireNotBlank(name, "name"))
                .map(loaded -> new RegistryPackage(gateway, loaded));
    }
}
