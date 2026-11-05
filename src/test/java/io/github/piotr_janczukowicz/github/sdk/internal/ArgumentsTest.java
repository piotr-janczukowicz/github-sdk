package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ArgumentsTest {

    @Test
    void requireNotBlankValues_shouldReturnTheValues_whenAllAreGiven() {
        final List<String> values = Arguments.requireNotBlankValues(new String[] {"bug", "help wanted"}, "names");

        assertThat(values).containsExactly("bug", "help wanted");
    }

    @Test
    void requireNotBlankValues_shouldThrowIllegalArgumentException_whenNoValueIsGiven() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireNotBlankValues(new String[0], "names"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("names");
    }

    @Test
    void requireNotBlankValues_shouldThrowIllegalArgumentException_whenAValueIsBlank() {
        final Throwable thrown =
                catchThrowable(() -> Arguments.requireNotBlankValues(new String[] {"a", " "}, "names"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("names");
    }

    @Test
    void requireNotBlankValues_shouldThrowNullPointerException_whenAValueIsNull() {
        final Throwable thrown =
                catchThrowable(() -> Arguments.requireNotBlankValues(new String[] {"a", null}, "names"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("names");
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Arguments> constructor = Arguments.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
