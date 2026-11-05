package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateLabelPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateMilestonePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubLabel;
import io.github.piotr_janczukowicz.github.api.search.GitHubMilestone;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryLabelsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubLabel label = RepositoryContentFixtures.label();
    private final GitHubMilestone milestone = RepositoryContentFixtures.milestone();

    @Test
    void label_shouldReturnTheLabel_whenGitHubHasIt() throws IOException {
        when(github.issues().labels().getLabel(OWNER, NAME, "bug")).thenReturn(Optional.of(label));

        final Optional<Label> found = sut.label("bug");

        assertThat(found).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(found.orElseThrow().record()).isSameAs(label);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void label_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.issues().labels().getLabel(OWNER, NAME, "gone")).thenReturn(Optional.empty());

        final Optional<Label> found = sut.label("gone");

        assertThat(found).isEmpty();
    }

    @Test
    void label_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.label(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void labels_shouldWrapEachLabel_whenTheRepositoryHasLabels() throws IOException {
        when(github.issues().labels().listLabelsForRepo(OWNER, NAME).stream()).thenReturn(Stream.of(label));

        final List<Label> labels = sut.labels().toList();

        assertThat(labels).extracting(Label::record).containsExactly(label);
    }

    @Test
    void createLabel_shouldSendNameAndColor_whenCalledWithBoth() throws IOException {
        when(github.issues().labels().createLabel(eq(OWNER), eq(NAME), any(GitHubCreateLabelPayload.class)))
                .thenReturn(label);
        final ArgumentCaptor<GitHubCreateLabelPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateLabelPayload.class);

        final Label created = sut.createLabel("bug", "d73a4a");

        verify(github.issues().labels()).createLabel(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("bug");
            softly.assertThat(payload.getValue().color()).isEqualTo("d73a4a");
            softly.assertThat(created.record()).isSameAs(label);
        });
    }

    @Test
    void createLabel_shouldSendThePayload_whenCalledWithAPayload() throws IOException {
        final GitHubCreateLabelPayload payload = new GitHubCreateLabelPayload("bug", null, "Something broke");
        when(github.issues().labels().createLabel(OWNER, NAME, payload)).thenReturn(label);

        final Label created = sut.createLabel(payload);

        assertThat(created.record()).isSameAs(label);
    }

    @Test
    void createLabel_shouldThrowIllegalArgumentException_whenColorIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createLabel("bug", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("color");
    }

    @Test
    void createLabel_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createLabel((GitHubCreateLabelPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void milestone_shouldReturnTheMilestone_whenGitHubHasIt() throws IOException {
        when(github.issues().milestones().getMilestone(OWNER, NAME, 3)).thenReturn(Optional.of(milestone));

        final Optional<Milestone> found = sut.milestone(3);

        assertThat(found).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(found.orElseThrow().record()).isSameAs(milestone);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void milestone_shouldReturnEmpty_whenGitHubAnswersNotFound() throws IOException {
        when(github.issues().milestones().getMilestone(OWNER, NAME, 9)).thenReturn(Optional.empty());

        final Optional<Milestone> found = sut.milestone(9);

        assertThat(found).isEmpty();
    }

    @Test
    void milestones_shouldWrapEachMilestone_whenTheRepositoryHasMilestones() throws IOException {
        when(github.issues().milestones().listMilestones(OWNER, NAME).stream()).thenReturn(Stream.of(milestone));

        final List<Milestone> milestones = sut.milestones().toList();

        assertThat(milestones).extracting(Milestone::record).containsExactly(milestone);
    }

    @Test
    void createMilestone_shouldSendTheTitle_whenCalledWithATitle() throws IOException {
        when(github.issues().milestones().createMilestone(eq(OWNER), eq(NAME), any(GitHubCreateMilestonePayload.class)))
                .thenReturn(milestone);
        final ArgumentCaptor<GitHubCreateMilestonePayload> payload =
                ArgumentCaptor.forClass(GitHubCreateMilestonePayload.class);

        final Milestone created = sut.createMilestone("v1.0");

        verify(github.issues().milestones()).createMilestone(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("v1.0");
            softly.assertThat(payload.getValue().state()).isNull();
            softly.assertThat(created.record()).isSameAs(milestone);
        });
    }

    @Test
    void createMilestone_shouldSendThePayload_whenCalledWithAPayload() throws IOException {
        final GitHubCreateMilestonePayload payload = new GitHubCreateMilestonePayload("v1.0", null, "First", null);
        when(github.issues().milestones().createMilestone(OWNER, NAME, payload)).thenReturn(milestone);

        final Milestone created = sut.createMilestone(payload);

        assertThat(created.record()).isSameAs(milestone);
    }

    @Test
    void createMilestone_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createMilestone(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void createMilestone_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createMilestone((GitHubCreateMilestonePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }
}
