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

    @Test
    void requireNotBlankElements_shouldReturnTheValues_whenTheListMayBeEmpty() {
        final List<String> empty = Arguments.requireNotBlankElements(List.of(), "labels");
        final List<String> some = Arguments.requireNotBlankElements(List.of("a", "b"), "labels");

        assertThat(empty).isEmpty();
        assertThat(some).containsExactly("a", "b");
    }

    @Test
    void requireNotBlankElements_shouldThrowIllegalArgumentException_whenAValueIsBlank() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireNotBlankElements(List.of("a", " "), "labels"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("labels");
    }

    @Test
    void requireNotBlankElements_shouldThrowNullPointerException_whenTheListIsNull() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireNotBlankElements(null, "labels"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("labels");
    }

    @Test
    void requireNotBlankValues_shouldThrowIllegalArgumentException_whenTheListIsEmpty() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireNotBlankValues(List.of(), "labels"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("labels");
    }

    @Test
    void requireNotBlankValues_shouldReturnTheValues_whenTheListHasValues() {
        final List<String> values = Arguments.requireNotBlankValues(List.of("gpu"), "labels");

        assertThat(values).containsExactly("gpu");
    }
}
