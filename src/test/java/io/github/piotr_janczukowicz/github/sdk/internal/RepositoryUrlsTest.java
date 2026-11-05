package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class RepositoryUrlsTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://api.github.com/repos/octocat/hello-world",
                "https://api.github.com/repos/octocat/hello-world/",
                "https://ghe.example.com/api/v3/repos/octocat/hello-world//"
            })
    void parse_shouldReadOwnerAndName_whenTheUrlEndsWithThem(final String url) {
        final RepositoryUrls.Coordinates coordinates = RepositoryUrls.parse(url);

        assertThat(coordinates).isEqualTo(new RepositoryUrls.Coordinates("octocat", "hello-world"));
    }

    @Test
    void parse_shouldThrowIllegalArgumentException_whenTheUrlHasNoSlash() {
        final Throwable thrown = catchThrowable(() -> RepositoryUrls.parse("hello-world"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hello-world");
    }

    @Test
    void parse_shouldThrowIllegalArgumentException_whenTheUrlHasOnlySlashes() {
        final Throwable thrown = catchThrowable(() -> RepositoryUrls.parse("///"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<RepositoryUrls> constructor = RepositoryUrls.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
