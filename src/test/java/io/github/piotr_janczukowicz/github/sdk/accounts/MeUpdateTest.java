package io.github.piotr_janczukowicz.github.sdk.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.users.GitHubAuthenticatedUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateAuthenticatedUserPayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.User;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MeUpdateTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Me me = GitHubSdk.of(github).me();

    @Test
    void send_shouldSendOnlyTheSetFieldsAndReturnTheAnsweredProfile_whenSomeFieldsAreSet() throws IOException {
        final GitHubAuthenticatedUser reloaded = mock(GitHubAuthenticatedUser.class);
        final GitHubUserApi api = github.users().user();
        doReturn(reloaded).when(api).updateAuthenticatedUser(any(GitHubUpdateAuthenticatedUserPayload.class));
        final ArgumentCaptor<GitHubUpdateAuthenticatedUserPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAuthenticatedUserPayload.class);

        final User profile = me.update().name("Octo Cat").bio("Hello").send();

        verify(api).updateAuthenticatedUser(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("Octo Cat");
            softly.assertThat(payload.getValue().bio()).isEqualTo("Hello");
            softly.assertThat(payload.getValue().company()).isNull();
            softly.assertThat(profile.response()).isSameAs(reloaded);
        });
    }

    @Test
    void send_shouldSendEveryField_whenAllAreSet() throws IOException {
        final GitHubUserApi api = github.users().user();
        final GitHubAuthenticatedUser updated = mock(GitHubAuthenticatedUser.class);
        doReturn(updated).when(api).updateAuthenticatedUser(any(GitHubUpdateAuthenticatedUserPayload.class));
        final ArgumentCaptor<GitHubUpdateAuthenticatedUserPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateAuthenticatedUserPayload.class);

        me.update()
                .name("Octo Cat")
                .email("octo@example.com")
                .blog("https://example.com")
                .company("GitHub")
                .location("Internet")
                .bio("Hello")
                .hireable(true)
                .send();

        verify(api).updateAuthenticatedUser(payload.capture());
        final GitHubUpdateAuthenticatedUserPayload sent = payload.getValue();
        assertSoftly(softly -> {
            softly.assertThat(sent.name()).isEqualTo("Octo Cat");
            softly.assertThat(sent.email()).isEqualTo("octo@example.com");
            softly.assertThat(sent.blog()).isEqualTo("https://example.com");
            softly.assertThat(sent.company()).isEqualTo("GitHub");
            softly.assertThat(sent.location()).isEqualTo("Internet");
            softly.assertThat(sent.bio()).isEqualTo("Hello");
            softly.assertThat(sent.hireable()).isTrue();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingIsSet() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing to update");
        verifyNoInteractions(github);
    }

    @Test
    void name_shouldThrowNullPointerException_whenNameIsNull() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(() -> update.name(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void email_shouldThrowNullPointerException_whenEmailIsNull() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(() -> update.email(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("email");
    }

    @Test
    void blog_shouldThrowNullPointerException_whenBlogIsNull() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(() -> update.blog(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("blog");
    }

    @Test
    void company_shouldThrowNullPointerException_whenCompanyIsNull() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(() -> update.company(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("company");
    }

    @Test
    void location_shouldThrowNullPointerException_whenLocationIsNull() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(() -> update.location(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("location");
    }

    @Test
    void bio_shouldThrowNullPointerException_whenBioIsNull() {
        final MeUpdate update = me.update();

        final Throwable thrown = catchThrowable(() -> update.bio(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("bio");
    }

    @Test
    void anyUpdate_shouldNotSendAnything_beforeSendIsCalled() {
        me.update().name("Octo Cat");

        verifyNoInteractions(github);
    }
}
