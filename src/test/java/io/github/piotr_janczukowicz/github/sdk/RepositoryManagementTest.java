package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContributor;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateDispatchEventPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateUsingTemplatePayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubReplaceAllTopicsPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryLanguages;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryTopics;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubTransferPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryManagementTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void archive_shouldSendArchivedAndReturnTheRepository_whenCalled() throws IOException {
        final GitHubFullRepository archived = mock(GitHubFullRepository.class);
        when(archived.name()).thenReturn(NAME);
        final GitHubRepositoryApi api = github.repositories().repository();
        when(api.update(eq(OWNER), eq(NAME), any(GitHubUpdatePayload.class))).thenReturn(archived);
        final ArgumentCaptor<GitHubUpdatePayload> payload = ArgumentCaptor.forClass(GitHubUpdatePayload.class);

        final Repository result = sut.archive();

        verify(api).update(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().archived()).isTrue();
            softly.assertThat(payload.getValue().name()).isNull();
            softly.assertThat(result.response()).isSameAs(archived);
        });
    }

    @Test
    void delete_shouldDeleteTheRepository_whenCalled() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();

        sut.delete();

        verify(api).delete(OWNER, NAME);
    }

    @Test
    void transfer_shouldKeepTheName_whenOnlyTheOwnerIsGiven() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        stubTransfer(api, "hubot", NAME);
        final ArgumentCaptor<GitHubTransferPayload> payload = ArgumentCaptor.forClass(GitHubTransferPayload.class);

        final Repository moved = sut.transfer("hubot");

        verify(api).transfer(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().newOwner()).isEqualTo("hubot");
            softly.assertThat(payload.getValue().newName()).isNull();
            softly.assertThat(moved.owner()).isEqualTo("hubot");
            softly.assertThat(moved.name()).isEqualTo(NAME);
        });
    }

    @Test
    void transfer_shouldRename_whenANewNameIsGiven() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        stubTransfer(api, "hubot", "renamed");
        final ArgumentCaptor<GitHubTransferPayload> payload = ArgumentCaptor.forClass(GitHubTransferPayload.class);

        final Repository moved = sut.transfer("hubot", "renamed");

        verify(api).transfer(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().newName()).isEqualTo("renamed");
            softly.assertThat(moved.name()).isEqualTo("renamed");
        });
    }

    @Test
    void transfer_shouldThrowIllegalArgumentException_whenOwnerIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.transfer(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("newOwner");
    }

    @Test
    void transfer_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.transfer("hubot", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("newName");
    }

    @Test
    void topics_shouldReturnTheTopicNames_whenCalled() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        doReturn(new GitHubRepositoryTopics(List.of("java", "sdk"))).when(api).getAllTopics(OWNER, NAME);

        final List<String> topics = sut.topics();

        assertThat(topics).containsExactly("java", "sdk");
    }

    @Test
    void replaceTopics_shouldSendTheTopicsAndReturnWhatGitHubHolds_whenCalled() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        doReturn(new GitHubRepositoryTopics(List.of("java")))
                .when(api)
                .replaceAllTopics(eq(OWNER), eq(NAME), any(GitHubReplaceAllTopicsPayload.class));
        final ArgumentCaptor<GitHubReplaceAllTopicsPayload> payload =
                ArgumentCaptor.forClass(GitHubReplaceAllTopicsPayload.class);

        final List<String> topics = sut.replaceTopics(List.of("java"));

        verify(api).replaceAllTopics(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().names()).containsExactly("java");
            softly.assertThat(topics).containsExactly("java");
        });
    }

    @Test
    void replaceTopics_shouldThrowNullPointerException_whenTopicsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.replaceTopics(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("topics");
    }

    @Test
    void languages_shouldReturnTheBytesPerLanguage_whenCalled() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        doReturn(new GitHubRepositoryLanguages(Map.of("Java", 1200))).when(api).listLanguages(OWNER, NAME);

        final Map<String, Integer> languages = sut.languages();

        assertThat(languages).containsEntry("Java", 1200);
    }

    @Test
    void contributors_shouldStreamTheContributors_whenTheyExist() throws IOException {
        final GitHubContributor contributor = mock(GitHubContributor.class);
        when(github.repositories().repository().listContributors(eq(OWNER), eq(NAME), any()).stream())
                .thenReturn(Stream.of(contributor));

        final List<GitHubContributor> contributors = sut.contributors().toList();

        assertThat(contributors).containsExactly(contributor);
    }

    @Test
    void dispatchEvent_shouldSendOnlyTheType_whenNoPayloadIsGiven() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        final ArgumentCaptor<GitHubCreateDispatchEventPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDispatchEventPayload.class);

        sut.dispatchEvent("deploy");

        verify(api).createDispatchEvent(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().eventType()).isEqualTo("deploy");
            softly.assertThat(payload.getValue().clientPayload()).isNullOrEmpty();
        });
    }

    @Test
    void dispatchEvent_shouldSendTheClientPayload_whenOneIsGiven() throws IOException {
        final GitHubRepositoryApi api = github.repositories().repository();
        final ArgumentCaptor<GitHubCreateDispatchEventPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateDispatchEventPayload.class);

        sut.dispatchEvent("deploy", Map.of("environment", "staging"));

        verify(api).createDispatchEvent(eq(OWNER), eq(NAME), payload.capture());
        assertThat(payload.getValue().clientPayload()).containsEntry("environment", "staging");
    }

    @Test
    void dispatchEvent_shouldThrowIllegalArgumentException_whenTypeIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.dispatchEvent(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("eventType");
    }

    @Test
    void dispatchEvent_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.dispatchEvent("deploy", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("clientPayload");
    }

    @Test
    void generate_shouldCreateFromTheTemplate_whenOwnerAndNameAreGiven() throws IOException {
        final GitHubFullRepository created = mock(GitHubFullRepository.class);
        when(created.name()).thenReturn("from-template");
        final GitHubRepositoryApi api = github.repositories().repository();
        doReturn(created)
                .when(api)
                .createUsingTemplate(eq(OWNER), eq(NAME), any(GitHubCreateUsingTemplatePayload.class));
        final ArgumentCaptor<GitHubCreateUsingTemplatePayload> payload =
                ArgumentCaptor.forClass(GitHubCreateUsingTemplatePayload.class);

        final Repository generated = sut.generate("hubot", "from-template");

        verify(api).createUsingTemplate(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().owner()).isEqualTo("hubot");
            softly.assertThat(payload.getValue().name()).isEqualTo("from-template");
            softly.assertThat(generated.owner()).isEqualTo("hubot");
            softly.assertThat(generated.name()).isEqualTo("from-template");
            softly.assertThat(generated.response()).isSameAs(created);
        });
    }

    @Test
    void generate_shouldThrowIllegalArgumentException_whenOwnerIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.generate(" ", "from-template"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("newOwner");
    }

    @Test
    void generate_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.generate("hubot", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("newName");
    }

    @Test
    void generate_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.generate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    private void stubTransfer(final GitHubRepositoryApi api, final String newOwner, final String newName)
            throws IOException {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        when(owner.login()).thenReturn(newOwner);
        final GitHubMinimalRepository moved = mock(GitHubMinimalRepository.class);
        when(moved.owner()).thenReturn(owner);
        when(moved.name()).thenReturn(newName);
        doReturn(moved).when(api).transfer(eq(OWNER), eq(NAME), any(GitHubTransferPayload.class));
    }
}
