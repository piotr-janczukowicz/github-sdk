package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @MethodSource("listsWithoutBlankValues")
    void requireNotBlankElements_shouldReturnTheValues_whenTheListMayBeEmpty(final List<String> values) {
        final List<String> result = Arguments.requireNotBlankElements(values, "labels");

        assertThat(result).containsExactlyElementsOf(values);
    }

    private static Stream<List<String>> listsWithoutBlankValues() {
        return Stream.of(List.of(), List.of("a", "b"));
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

    @ParameterizedTest
    @ValueSource(strings = {"docs/readme.md", "release/v1.0"})
    void requireSafePath_shouldReturnThePath_whenItIsAnOrdinaryPathOrRef(final String value) {
        final String result = Arguments.requireSafePath(value, "path");

        assertThat(result).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/../b", "./b"})
    void requireSafePath_shouldThrowIllegalArgumentException_whenTheValueHasADotSegment(final String value) {
        final Throwable thrown = catchThrowable(() -> Arguments.requireSafePath(value, "path"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("path");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a\nb", "a\u007Fb"})
    void requireSafePath_shouldThrowIllegalArgumentException_whenTheValueHasAControlCharacter(final String value) {
        final Throwable thrown = catchThrowable(() -> Arguments.requireSafePath(value, "ref"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("control");
    }

    @Test
    void requireSafePath_shouldThrowIllegalArgumentException_whenTheValueIsBlank() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireSafePath(" ", "ref"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ref");
    }
}
