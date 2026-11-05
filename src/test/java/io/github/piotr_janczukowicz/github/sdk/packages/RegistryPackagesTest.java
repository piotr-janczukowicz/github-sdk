package io.github.piotr_janczukowicz.github.sdk.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.packages.GitHubListPackagesFilter;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PackageFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistryPackagesTest {

    private static final GitHubPackageType TYPE = GitHubPackageType.CONTAINER;
    private static final String NAME = PackageFixtures.NAME;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void all_shouldWrapThePackagesOfTheOrganization_whenTheyExist() throws IOException {
        final GitHubPackage listed = PackageFixtures.registryPackage();
        when(github.packages().listPackagesForOrg("acme", GitHubListPackagesFilter.of(TYPE)).stream())
                .thenReturn(Stream.of(listed));

        final List<RegistryPackage> packages =
                RegistryPackages.forOrganization(sdk, "acme").all(TYPE).toList();

        assertThat(packages)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(listed));
    }

    @Test
    void find_shouldWrapThePackageOfTheUser_whenItExists() throws IOException {
        final GitHubPackage loaded = PackageFixtures.registryPackage();
        when(github.packages().getPackageForUser("octocat", TYPE, NAME)).thenReturn(Optional.of(loaded));

        final Optional<RegistryPackage> found =
                RegistryPackages.forUser(sdk, "octocat").find(TYPE, NAME);

        assertThat(found).get().extracting(RegistryPackage::response).isSameAs(loaded);
    }

    @Test
    void find_shouldBeEmpty_whenTheAuthenticatedUserHasNoSuchPackage() throws IOException {
        when(github.packages().getPackageForAuthenticatedUser(TYPE, NAME)).thenReturn(Optional.empty());

        final Optional<RegistryPackage> found =
                RegistryPackages.forAuthenticatedUser(sdk).find(TYPE, NAME);

        assertThat(found).isEmpty();
    }

    @Test
    void find_shouldBeEmpty_whenTheOrganizationHasNoSuchPackage() throws IOException {
        when(github.packages().getPackageForOrg("acme", TYPE, NAME)).thenReturn(Optional.empty());

        final Optional<RegistryPackage> found =
                RegistryPackages.forOrganization(sdk, "acme").find(TYPE, NAME);

        assertThat(found).isEmpty();
    }

    @Test
    void find_shouldBeEmpty_whenTheUserHasNoSuchPackage() throws IOException {
        when(github.packages().getPackageForUser("octocat", TYPE, NAME)).thenReturn(Optional.empty());

        final Optional<RegistryPackage> found =
                RegistryPackages.forUser(sdk, "octocat").find(TYPE, NAME);

        assertThat(found).isEmpty();
    }

    @Test
    void find_shouldThrowNullPointerException_whenTypeIsNull() {
        final RegistryPackages sut = RegistryPackages.forAuthenticatedUser(sdk);

        final Throwable thrown = catchThrowable(() -> sut.find(null, NAME));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    @Test
    void find_shouldThrowNullPointerException_whenNameIsNull() {
        final RegistryPackages sut = RegistryPackages.forAuthenticatedUser(sdk);

        final Throwable thrown = catchThrowable(() -> sut.find(TYPE, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void forOrganization_shouldThrowNullPointerException_whenLoginIsNull() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forOrganization(sdk, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("login");
    }

    @Test
    void forUser_shouldThrowNullPointerException_whenLoginIsNull() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forUser(sdk, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("login");
    }

    @Test
    void all_shouldThrowNullPointerException_whenTypeIsNull() {
        final RegistryPackages sut = RegistryPackages.forAuthenticatedUser(sdk);

        final Throwable thrown = catchThrowable(() -> sut.all(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    @Test
    void find_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final RegistryPackages sut = RegistryPackages.forAuthenticatedUser(sdk);

        final Throwable thrown = catchThrowable(() -> sut.find(TYPE, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void forOrganization_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forOrganization(sdk, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void forUser_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forUser(sdk, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void forAuthenticatedUser_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forAuthenticatedUser(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void forOrganization_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forOrganization(null, "acme"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void forUser_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> RegistryPackages.forUser(null, "octocat"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }
}
