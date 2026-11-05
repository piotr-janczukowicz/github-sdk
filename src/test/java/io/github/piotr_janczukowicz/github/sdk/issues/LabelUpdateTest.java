package io.github.piotr_janczukowicz.github.sdk.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabelBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateLabelPayload;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RepositoryContentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class LabelUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubLabel response = RepositoryContentFixtures.label();
    private final Label label = new Label(GitHubSdk.of(github).repository(OWNER, NAME), response);

    @Test
    void send_shouldSendOnlyTheSetFieldsAndReturnTheResponse_whenNameAndColorAreSet() throws IOException {
        final GitHubLabel updated =
                GitHubLabelBuilder.builder(response).name("defect").build();
        when(github.issues().labels().updateLabel(eq(OWNER), eq(NAME), eq("bug"), any(GitHubUpdateLabelPayload.class)))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateLabelPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateLabelPayload.class);

        final Label result = label.update().name("defect").color("ffffff").send();

        verify(github.issues().labels()).updateLabel(eq(OWNER), eq(NAME), eq("bug"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().newName()).isEqualTo("defect");
            softly.assertThat(payload.getValue().color()).isEqualTo("ffffff");
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(label.name()).isEqualTo("bug");
        });
    }

    @Test
    void send_shouldSendTheDescription_whenItIsSet() throws IOException {
        when(github.issues().labels().updateLabel(eq(OWNER), eq(NAME), eq("bug"), any(GitHubUpdateLabelPayload.class)))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdateLabelPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateLabelPayload.class);

        label.update().description("Something broke").send();

        verify(github.issues().labels()).updateLabel(eq(OWNER), eq(NAME), eq("bug"), payload.capture());
        assertThat(payload.getValue().description()).isEqualTo("Something broke");
    }

    @Test
    void send_shouldThrowIllegalStateExceptionWithoutARequest_whenNothingIsSet() {
        final LabelUpdate update = label.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing");
        verifyNoInteractions(github.issues().labels());
    }

    @Test
    void name_shouldThrowNullPointerException_whenNameIsNull() {
        final LabelUpdate update = label.update();

        final Throwable thrown = catchThrowable(() -> update.name(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void color_shouldThrowNullPointerException_whenColorIsNull() {
        final LabelUpdate update = label.update();

        final Throwable thrown = catchThrowable(() -> update.color(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("color");
    }

    @Test
    void description_shouldThrowNullPointerException_whenDescriptionIsNull() {
        final LabelUpdate update = label.update();

        final Throwable thrown = catchThrowable(() -> update.description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("description");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenLabelIsNull() {
        final Throwable thrown = catchThrowable(() -> new LabelUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("label");
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> label.update().name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }
}
