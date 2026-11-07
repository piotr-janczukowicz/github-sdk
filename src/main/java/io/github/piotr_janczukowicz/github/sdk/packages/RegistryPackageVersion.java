package io.github.piotr_janczukowicz.github.sdk.packages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import java.io.IOException;

/**
 * A version of a {@link RegistryPackage}. The object is immutable, and {@link #response()} gives the complete response,
 * including the registry specific metadata.
 */
public final class RegistryPackageVersion {

    private final RegistryPackage registryPackage;
    private final GitHubPackageVersion response;

    RegistryPackageVersion(final RegistryPackage registryPackage, final GitHubPackageVersion response) {
        this.registryPackage = requireNonNull(registryPackage, "registryPackage");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the package the version belongs to.
     *
     * @return the package
     */
    public RegistryPackage registryPackage() {
        return registryPackage;
    }

    /**
     * Returns the version id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the version name, for example a tag or a digest.
     *
     * @return the name
     */
    public String name() {
        return response.name();
    }

    /**
     * Loads the version again. This object is not changed.
     *
     * @return the version as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the version is gone
     */
    public RegistryPackageVersion refresh() throws IOException {
        return registryPackage
                .version(id())
                .orElseThrow(() -> new IllegalStateException("Package version " + id() + " is gone"));
    }

    /**
     * Deletes the version ({@code DELETE /orgs/{org}/packages/{package_type}/{package_name}/versions/{version_id}} or
     * the user variants). GitHub refuses to delete the only version of a public package or a version of a public
     * package with more than 5000 downloads. A deleted version can be restored for 30 days.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        registryPackage.gateway().deleteVersion(registryPackage.type(), registryPackage.name(), id());
    }

    /**
     * Restores the deleted version.
     *
     * @throws IOException if the transport fails
     */
    public void restore() throws IOException {
        registryPackage.gateway().restoreVersion(registryPackage.type(), registryPackage.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubPackageVersion response() {
        return response;
    }
}
