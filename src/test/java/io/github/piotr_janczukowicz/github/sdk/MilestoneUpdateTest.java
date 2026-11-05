package io.github.piotr_janczukowicz.github.sdk;

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
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdateMilestonePayload;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestone;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestoneBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MilestoneUpdateTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";
    private static final int NUMBER = 3;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubMilestone response = RepositoryContentFixtures.milestone();
    private final Milestone milestone = new Milestone(GitHubSdk.of(github).repository(OWNER, NAME), response);

    @Test
    void send_shouldSendOnlyTheSetFieldsAndReturnTheResponse_whenTitleAndDueDateAreSet() throws IOException {
        final GitHubMilestone updated =
                GitHubMilestoneBuilder.builder(response).title("v1.1").build();
        final Instant due = Instant.parse("2026-12-01T00:00:00Z");
        when(github.issues()
                        .milestones()
                        .updateMilestone(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateMilestonePayload.class)))
                .thenReturn(updated);
        final ArgumentCaptor<GitHubUpdateMilestonePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateMilestonePayload.class);

        final Milestone result = milestone.update().title("v1.1").dueOn(due).send();

        verify(github.issues().milestones()).updateMilestone(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("v1.1");
            softly.assertThat(payload.getValue().dueOn()).isEqualTo(due);
            softly.assertThat(payload.getValue().state()).isNull();
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(milestone.title()).isEqualTo("v1.0");
        });
    }

    @Test
    void send_shouldSendTheDescription_whenItIsSet() throws IOException {
        when(github.issues()
                        .milestones()
                        .updateMilestone(eq(OWNER), eq(NAME), eq(NUMBER), any(GitHubUpdateMilestonePayload.class)))
                .thenReturn(response);
        final ArgumentCaptor<GitHubUpdateMilestonePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateMilestonePayload.class);

        milestone.update().description("First release").send();

        verify(github.issues().milestones()).updateMilestone(eq(OWNER), eq(NAME), eq(NUMBER), payload.capture());
        assertThat(payload.getValue().description()).isEqualTo("First release");
    }

    @Test
    void send_shouldThrowIllegalStateExceptionWithoutARequest_whenNothingIsSet() {
        final MilestoneUpdate update = milestone.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("nothing");
        verifyNoInteractions(github.issues().milestones());
    }

    @Test
    void title_shouldThrowNullPointerException_whenTitleIsNull() {
        final MilestoneUpdate update = milestone.update();

        final Throwable thrown = catchThrowable(() -> update.title(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("title");
    }

    @Test
    void description_shouldThrowNullPointerException_whenDescriptionIsNull() {
        final MilestoneUpdate update = milestone.update();

        final Throwable thrown = catchThrowable(() -> update.description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("description");
    }

    @Test
    void dueOn_shouldThrowNullPointerException_whenDueDateIsNull() {
        final MilestoneUpdate update = milestone.update();

        final Throwable thrown = catchThrowable(() -> update.dueOn(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("dueOn");
    }
}
