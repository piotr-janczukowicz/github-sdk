package io.github.piotr_janczukowicz.github.sdk.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackage;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVisibility;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PackageFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistryPackageTest {

    private static final String NAME = PackageFixtures.NAME;
    private static final GitHubPackageType TYPE = GitHubPackageType.CONTAINER;
    private static final long ID = PackageFixtures.VERSION_ID;

    private final PackageGateway gateway = mock(PackageGateway.class);
    private final GitHubPackage response = PackageFixtures.registryPackage();
    private final RegistryPackage sut = new RegistryPackage(gateway, response);

    @Test
    void accessors_shouldExposeTheResponse_whenPackageIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo(NAME);
            softly.assertThat(sut.type()).isEqualTo(TYPE);
            softly.assertThat(sut.visibility()).isEqualTo(GitHubPackageVisibility.PUBLIC);
            softly.assertThat(sut.versionCount()).isEqualTo(3);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenGatewayIsNull() {
        final Throwable thrown = catchThrowable(() -> new RegistryPackage(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("gateway");
    }

    @Test
    void refresh_shouldReturnTheReloadedPackage_whenItExists() throws IOException {
        final GitHubPackage reloaded = PackageFixtures.registryPackage();
        when(gateway.registryPackage(TYPE, NAME)).thenReturn(Optional.of(reloaded));

        final RegistryPackage result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenThePackageIsGone() throws IOException {
        when(gateway.registryPackage(TYPE, NAME)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void versions_shouldWrapTheVersions_whenTheyExist() throws IOException {
        final GitHubPackageVersion listed = PackageFixtures.version();
        when(gateway.versions(TYPE, NAME)).thenReturn(Stream.of(listed));

        final List<RegistryPackageVersion> versions = sut.versions().toList();

        assertThat(versions)
                .singleElement()
                .satisfies(version -> assertSoftly(softly -> {
                    softly.assertThat(version.response()).isSameAs(listed);
                    softly.assertThat(version.registryPackage()).isSameAs(sut);
                }));
    }

    @Test
    void version_shouldReturnTheVersion_whenItExists() throws IOException {
        final GitHubPackageVersion loaded = PackageFixtures.version();
        when(gateway.version(TYPE, NAME, ID)).thenReturn(Optional.of(loaded));

        final Optional<RegistryPackageVersion> found = sut.version(ID);

        assertThat(found).get().extracting(RegistryPackageVersion::response).isSameAs(loaded);
    }

    @Test
    void version_shouldBeEmpty_whenTheVersionIsMissing() throws IOException {
        when(gateway.version(TYPE, NAME, ID)).thenReturn(Optional.empty());

        final Optional<RegistryPackageVersion> found = sut.version(ID);

        assertThat(found).isEmpty();
    }

    @Test
    void delete_shouldDeleteThePackage_whenCalled() throws IOException {
        sut.delete();

        verify(gateway).deletePackage(TYPE, NAME);
    }

    @Test
    void restore_shouldRestoreThePackage_whenCalled() throws IOException {
        sut.restore();

        verify(gateway).restorePackage(TYPE, NAME);
    }
}
