package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.packages.GitHubListPackagesFilter;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserApi;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PackageFixtures;
import io.github.piotr_janczukowicz.github.sdk.packages.RegistryPackage;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class OwnerRegistryPackagesTest {

    private static final GitHubPackageType TYPE = GitHubPackageType.CONTAINER;
    private static final String NAME = PackageFixtures.NAME;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void registryPackages_shouldListThePackagesOfTheOrganization_whenTheyExist() throws IOException {
        final GitHubPackage listed = PackageFixtures.registryPackage();
        when(github.packages().listPackagesForOrg("acme", GitHubListPackagesFilter.of(TYPE)).stream())
                .thenReturn(Stream.of(listed));
        final Organization sut = organization();

        final List<RegistryPackage> packages = sut.registryPackages(TYPE).toList();

        assertThat(packages)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(listed));
    }

    @Test
    void registryPackage_shouldLoadThePackageOfTheOrganization_whenItExists() throws IOException {
        final GitHubPackage loaded = PackageFixtures.registryPackage();
        when(github.packages().getPackageForOrg("acme", TYPE, NAME)).thenReturn(Optional.of(loaded));
        final Organization sut = organization();

        final Optional<RegistryPackage> found = sut.registryPackage(TYPE, NAME);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
        });
    }

    @Test
    void registryPackages_shouldListThePackagesOfTheUser_whenTheyExist() throws IOException {
        final GitHubPackage listed = PackageFixtures.registryPackage();
        when(github.packages().listPackagesForUser("octocat", GitHubListPackagesFilter.of(TYPE)).stream())
                .thenReturn(Stream.of(listed));
        final User sut = user();

        final List<RegistryPackage> packages = sut.registryPackages(TYPE).toList();

        assertThat(packages)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(listed));
    }

    @Test
    void registryPackage_shouldLoadThePackageOfTheUser_whenItExists() throws IOException {
        final GitHubPackage loaded = PackageFixtures.registryPackage();
        when(github.packages().getPackageForUser("octocat", TYPE, NAME)).thenReturn(Optional.of(loaded));
        final User sut = user();

        final Optional<RegistryPackage> found = sut.registryPackage(TYPE, NAME);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
        });
    }

    @Test
    void registryPackages_shouldListThePackagesOfTheAuthenticatedUser_whenTheyExist() throws IOException {
        final GitHubPackage listed = PackageFixtures.registryPackage();
        when(github.packages().listPackagesForAuthenticatedUser(GitHubListPackagesFilter.of(TYPE)).stream())
                .thenReturn(Stream.of(listed));

        final List<RegistryPackage> packages = sdk.me().registryPackages(TYPE).toList();

        assertThat(packages)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(listed));
    }

    @Test
    void registryPackage_shouldLoadThePackageOfTheAuthenticatedUser_whenItExists() throws IOException {
        final GitHubPackage loaded = PackageFixtures.registryPackage();
        when(github.packages().getPackageForAuthenticatedUser(TYPE, NAME)).thenReturn(Optional.of(loaded));

        final Optional<RegistryPackage> found = sdk.me().registryPackage(TYPE, NAME);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
        });
    }

    private Organization organization() {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn("acme");
        return new Organization(sdk, response);
    }

    private User user() throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        lenient().when(response.login()).thenReturn("octocat");
        final GitHubUserApi api = github.users().user();
        lenient().doReturn(response).when(api).getAuthenticatedUser();
        return new User(sdk, response);
    }
}
