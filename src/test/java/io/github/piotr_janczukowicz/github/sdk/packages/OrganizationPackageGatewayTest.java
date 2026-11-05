package io.github.piotr_janczukowicz.github.sdk.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PackageFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class OrganizationPackageGatewayTest {

    private static final String LOGIN = "octocat";
    private static final String NAME = PackageFixtures.NAME;
    private static final GitHubPackageType TYPE = GitHubPackageType.CONTAINER;
    private static final long ID = PackageFixtures.VERSION_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final OrganizationPackageGateway sut = new OrganizationPackageGateway(github, LOGIN);

    @Test
    void packages_shouldStreamThePackages_whenTheyExist() throws IOException {
        final GitHubPackage listed = PackageFixtures.registryPackage();
        when(github
                        .packages()
                        .listPackagesForOrg(
                                LOGIN,
                                io.github.piotr_janczukowicz.github.api.packages.GitHubListPackagesFilter.of(TYPE))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<GitHubPackage> packages = sut.packages(TYPE).toList();

        assertThat(packages).containsExactly(listed);
    }

    @Test
    void registryPackage_shouldReturnThePackage_whenItExists() throws IOException {
        final GitHubPackage loaded = PackageFixtures.registryPackage();
        when(github.packages().getPackageForOrg(LOGIN, TYPE, NAME)).thenReturn(Optional.of(loaded));

        final Optional<GitHubPackage> found = sut.registryPackage(TYPE, NAME);

        assertThat(found).containsSame(loaded);
    }

    @Test
    void deletePackage_shouldDeleteThePackage_whenCalled() throws IOException {
        sut.deletePackage(TYPE, NAME);

        verify(github.packages()).deletePackageForOrg(LOGIN, TYPE, NAME);
    }

    @Test
    void restorePackage_shouldRestoreThePackage_whenCalled() throws IOException {
        sut.restorePackage(TYPE, NAME);

        verify(github.packages()).restorePackageForOrg(LOGIN, TYPE, NAME);
    }

    @Test
    void versions_shouldStreamTheVersions_whenTheyExist() throws IOException {
        final GitHubPackageVersion version = PackageFixtures.version();
        when(github.packages().listPackageVersionsForOrg(LOGIN, TYPE, NAME).stream())
                .thenReturn(Stream.of(version));

        final List<GitHubPackageVersion> versions = sut.versions(TYPE, NAME).toList();

        assertThat(versions).containsExactly(version);
    }

    @Test
    void version_shouldReturnTheVersion_whenItExists() throws IOException {
        final GitHubPackageVersion loaded = PackageFixtures.version();
        when(github.packages().getPackageVersionForOrg(LOGIN, TYPE, NAME, ID)).thenReturn(Optional.of(loaded));

        final Optional<GitHubPackageVersion> found = sut.version(TYPE, NAME, ID);

        assertThat(found).containsSame(loaded);
    }

    @Test
    void deleteVersion_shouldDeleteTheVersion_whenCalled() throws IOException {
        sut.deleteVersion(TYPE, NAME, ID);

        verify(github.packages()).deletePackageVersionForOrg(LOGIN, TYPE, NAME, ID);
    }

    @Test
    void restoreVersion_shouldRestoreTheVersion_whenCalled() throws IOException {
        sut.restoreVersion(TYPE, NAME, ID);

        verify(github.packages()).restorePackageVersionForOrg(LOGIN, TYPE, NAME, ID);
    }
}
