package io.github.piotr_janczukowicz.github.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

@UnitTest
class InternalsTest {

    @Test
    void github_shouldReturnTheClientOfTheRepository_whenTheRepositoryComesFromTheSdk() {
        final GitHub github = mock(GitHub.class);
        final Repository repository = GitHubSdk.of(github).repository("octocat", "hello-world");

        final GitHub result = Internals.github(repository);

        assertThat(result).isSameAs(github);
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Internals> constructor = Internals.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
