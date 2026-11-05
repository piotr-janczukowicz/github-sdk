package io.github.piotr_janczukowicz.github.sdk.packages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVisibility;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A package in GitHub Packages. The object is immutable, and {@link #response()} gives the complete response. The
 * package name is the key, together with its type.
 */
public final class RegistryPackage {

    private final PackageGateway gateway;
    private final GitHubPackage response;

    RegistryPackage(final PackageGateway gateway, final GitHubPackage response) {
        this.gateway = requireNonNull(gateway, "gateway");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the package name.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Returns the package type, for example container or maven.
     *
     * @return the type
     */
    public GitHubPackageType type() {
        return response.packageType();
    }

    /**
     * Returns who can see the package.
     *
     * @return the visibility
     */
    public GitHubPackageVisibility visibility() {
        return response.visibility();
    }

    /**
     * Returns the number of versions the package has.
     *
     * @return the version count
     */
    public int versionCount() {
        return response.versionCount();
    }

    /**
     * Loads the package again. This object is not changed.
     *
     * @return the package as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the package is gone
     */
    public RegistryPackage refresh() throws IOException {
        return gateway.registryPackage(type(), name())
                .map(loaded -> new RegistryPackage(gateway, loaded))
                .orElseThrow(() -> new IllegalStateException("Package " + name() + " is gone"));
    }

    /**
     * Lists the versions of the package, newest first ({@code GET
     * /orgs/{org}/packages/{package_type}/{package_name}/versions} or the user variants). The stream is lazy and can be
     * used once.
     *
     * @return the versions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryPackageVersion> versions() throws IOException {
        return gateway.versions(type(), name()).map(listed -> new RegistryPackageVersion(this, listed));
    }

    /**
     * Loads one version.
     *
     * @param id the version id
     * @return the version, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<RegistryPackageVersion> version(final long id) throws IOException {
        return gateway.version(type(), name(), id).map(loaded -> new RegistryPackageVersion(this, loaded));
    }

    /**
     * Deletes the whole package ({@code DELETE /orgs/{org}/packages/{package_type}/{package_name}} or the user
     * variants). GitHub refuses to delete a public package with more than 5000 downloads. A deleted package can be
     * restored for 30 days, if its name is still free.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gateway.deletePackage(type(), name());
    }

    /**
     * Restores the deleted package.
     *
     * @throws IOException if the transport fails
     */
    public void restore() throws IOException {
        gateway.restorePackage(type(), name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPackage response() {
        return response;
    }

    PackageGateway gateway() {
        return gateway;
    }
}
