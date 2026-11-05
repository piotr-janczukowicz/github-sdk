package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVisibility;

public final class PackageFixtures {

    public static final String NAME = "hello-image";
    public static final long VERSION_ID = 91L;

    private PackageFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubPackage registryPackage() {
        final GitHubPackage registryPackage = mock(GitHubPackage.class);
        lenient().when(registryPackage.name()).thenReturn(NAME);
        lenient().when(registryPackage.packageType()).thenReturn(GitHubPackageType.CONTAINER);
        lenient().when(registryPackage.visibility()).thenReturn(GitHubPackageVisibility.PUBLIC);
        lenient().when(registryPackage.versionCount()).thenReturn(3);
        return registryPackage;
    }

    public static GitHubPackageVersion version() {
        final GitHubPackageVersion version = mock(GitHubPackageVersion.class);
        lenient().when(version.id()).thenReturn(VERSION_ID);
        lenient().when(version.name()).thenReturn("sha256:abc");
        return version;
    }
}
