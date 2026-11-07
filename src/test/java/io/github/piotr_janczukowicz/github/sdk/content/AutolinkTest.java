package io.github.piotr_janczukowicz.github.sdk.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubAutolink;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesAutolinksApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ExtrasFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class AutolinkTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final long ID = ExtrasFixtures.AUTOLINK_ID;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubAutolink response = ExtrasFixtures.autolink();
    private final Autolink sut = new Autolink(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAutolinkIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.id()).isEqualTo(ID);
            softly.assertThat(sut.keyPrefix()).isEqualTo("TICKET-");
            softly.assertThat(sut.urlTemplate()).isEqualTo("https://tickets.example.com/<num>");
            softly.assertThat(sut.isAlphanumeric()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedAutolink_whenItExists() throws IOException {
        final GitHubAutolink reloaded = ExtrasFixtures.autolink();
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();
        doReturn(Optional.of(reloaded)).when(api).getAutolink(OWNER, NAME, ID);

        final Autolink result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAutolinkIsGone() throws IOException {
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();
        doReturn(Optional.empty()).when(api).getAutolink(OWNER, NAME, ID);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldDeleteTheAutolink_whenCalled() throws IOException {
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();

        sut.delete();

        verify(api).deleteAutolink(OWNER, NAME, ID);
    }
}
