package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

@UnitTest
class LookupsTest {

    @Test
    void require_shouldReturnTheValue_whenTheLookupFindsIt() {
        final String value = Lookups.require(() -> Optional.of("found"), () -> "missing");

        assertThat(value).isEqualTo("found");
    }

    @Test
    void require_shouldNotBuildTheMessage_whenTheLookupFindsTheValue() {
        final AtomicInteger built = new AtomicInteger();

        Lookups.require(() -> Optional.of("found"), () -> "missing" + built.incrementAndGet());

        assertThat(built).hasValue(0);
    }

    @Test
    void require_shouldThrowIllegalStateException_whenTheLookupIsEmpty() {
        final Throwable thrown = catchThrowable(() -> Lookups.require(Optional::empty, () -> "Thing 7 is gone"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Thing 7 is gone");
    }

    @Test
    void require_shouldThrowUncheckedIoException_whenTheLookupFails() {
        final IOException failure = new IOException("transport failed");

        final Throwable thrown = catchThrowable(() -> Lookups.require(
                () -> {
                    throw failure;
                },
                () -> "missing"));

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCause(failure);
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Lookups> constructor = Lookups.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
