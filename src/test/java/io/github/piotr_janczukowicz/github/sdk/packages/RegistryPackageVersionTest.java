package io.github.piotr_janczukowicz.github.sdk.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageType;
import io.github.piotr_janczukowicz.github.api.packages.GitHubPackageVersion;
import io.github.piotr_janczukowicz.github.sdk.fixtures.PackageFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistryPackageVersionTest {

    private static final String NAME = PackageFixtures.NAME;
    private static final GitHubPackageType TYPE = GitHubPackageType.CONTAINER;
    private static final long ID = PackageFixtures.VERSION_ID;

    private final PackageGateway gateway = mock(PackageGateway.class);
    private final RegistryPackage registryPackage = new RegistryPackage(gateway, PackageFixtures.registryPackage());
    private final GitHubPackageVersion response = PackageFixtures.version();
    private final RegistryPackageVersion sut = new RegistryPackageVersion(registryPackage, response);

    @Test
    void accessors_shouldExposeTheResponse_whenVersionIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.registryPackage()).isSameAs(registryPackage);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("sha256:abc");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenPackageIsNull() {
        final Throwable thrown = catchThrowable(() -> new RegistryPackageVersion(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("registryPackage");
    }

    @Test
    void refresh_shouldReturnTheReloadedVersion_whenItExists() throws IOException {
        final GitHubPackageVersion reloaded = PackageFixtures.version();
        when(gateway.version(TYPE, NAME, ID)).thenReturn(Optional.of(reloaded));

        final RegistryPackageVersion result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheVersionIsGone() throws IOException {
        when(gateway.version(TYPE, NAME, ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldDeleteTheVersion_whenCalled() throws IOException {
        sut.delete();

        verify(gateway).deleteVersion(TYPE, NAME, ID);
    }

    @Test
    void restore_shouldRestoreTheVersion_whenCalled() throws IOException {
        sut.restore();

        verify(gateway).restoreVersion(TYPE, NAME, ID);
    }
}
