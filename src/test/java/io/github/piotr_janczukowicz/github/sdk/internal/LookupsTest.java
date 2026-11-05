package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class LookupsTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Lookups> constructor = Lookups.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void find_shouldReturnTheValue_whenTheLookupFindsIt() {
        final Optional<String> value = Lookups.find(() -> Optional.of("found"));

        assertThat(value).contains("found");
    }

    @Test
    void find_shouldBeEmpty_whenTheLookupFindsNothing() {
        final Optional<String> value = Lookups.find(Optional::empty);

        assertThat(value).isEmpty();
    }

    @Test
    void find_shouldThrowUncheckedIoException_whenTheLookupFails() {
        final Throwable thrown = catchThrowable(() -> Lookups.find(() -> {
            throw new IOException("transport failed");
        }));

        assertThat(thrown).isInstanceOf(UncheckedIOException.class);
    }
}
