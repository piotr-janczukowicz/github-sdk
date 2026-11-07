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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.checks.GitHubCheckSuite;
import io.github.piotr_janczukowicz.github.api.checks.GitHubChecksSuitesApi;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitComment;
import io.github.piotr_janczukowicz.github.api.commits.GitHubCommitsCommentsApi;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicenseContent;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicensesApi;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCommunityProfile;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubMetricsCommunityApi;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubAutolink;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateAutolinkPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoriesAutolinksApi;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckSuite;
import io.github.piotr_janczukowicz.github.sdk.content.Autolink;
import io.github.piotr_janczukowicz.github.sdk.content.CommitComment;
import io.github.piotr_janczukowicz.github.sdk.fixtures.ExtrasFixtures;
import io.github.piotr_janczukowicz.github.sdk.metrics.RepositoryStatistics;
import io.github.piotr_janczukowicz.github.sdk.metrics.RepositoryTraffic;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryExtrasTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository sut = GitHubSdk.of(github).repository(OWNER, NAME);

    @Test
    void traffic_shouldReturnTheTrafficWithoutRequest_whenCalled() {
        final RepositoryTraffic traffic = sut.traffic();

        assertThat(traffic).isNotNull();
        verifyNoInteractions(github);
    }

    @Test
    void statistics_shouldReturnTheStatisticsWithoutRequest_whenCalled() {
        final RepositoryStatistics statistics = sut.statistics();

        assertThat(statistics).isNotNull();
        verifyNoInteractions(github);
    }

    @Test
    void communityProfile_shouldReturnTheProfile_whenCalled() throws IOException {
        final GitHubCommunityProfile profile = mock(GitHubCommunityProfile.class);
        final GitHubMetricsCommunityApi api = github.metrics().community();
        doReturn(profile).when(api).getCommunityProfileMetrics(OWNER, NAME);

        final GitHubCommunityProfile result = sut.communityProfile();

        assertThat(result).isSameAs(profile);
    }

    @Test
    void commitComments_shouldStreamTheComments_whenTheyExist() throws IOException {
        final GitHubCommitComment listed = ExtrasFixtures.commitComment();
        when(github
                        .commits()
                        .comments()
                        .listCommitCommentsForRepo(eq(OWNER), eq(NAME), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(listed));

        final List<CommitComment> comments = sut.commitComments().toList();

        assertThat(comments)
                .singleElement()
                .satisfies(comment -> assertSoftly(softly -> {
                    softly.assertThat(comment.response()).isSameAs(listed);
                    softly.assertThat(comment.repository()).isSameAs(sut);
                }));
    }

    @Test
    void commitComment_shouldReturnTheComment_whenGitHubHasIt() throws IOException {
        final GitHubCommitsCommentsApi api = github.commits().comments();
        final GitHubCommitComment loaded = ExtrasFixtures.commitComment();
        doReturn(Optional.of(loaded)).when(api).getCommitComment(OWNER, NAME, ExtrasFixtures.COMMENT_ID);

        final Optional<CommitComment> found = sut.commitComment(ExtrasFixtures.COMMENT_ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void commitComment_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubCommitsCommentsApi api = github.commits().comments();
        doReturn(Optional.empty()).when(api).getCommitComment(OWNER, NAME, 1L);

        final Optional<CommitComment> found = sut.commitComment(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void autolinks_shouldStreamTheAutolinks_whenTheyExist() throws IOException {
        final GitHubAutolink listed = ExtrasFixtures.autolink();
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();
        doReturn(List.of(listed)).when(api).listAutolinks(OWNER, NAME);

        final List<Autolink> autolinks = sut.autolinks().toList();

        assertThat(autolinks)
                .singleElement()
                .satisfies(autolink -> assertSoftly(softly -> {
                    softly.assertThat(autolink.response()).isSameAs(listed);
                    softly.assertThat(autolink.repository()).isSameAs(sut);
                }));
    }

    @Test
    void autolink_shouldReturnTheAutolink_whenGitHubHasIt() throws IOException {
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();
        final GitHubAutolink loaded = ExtrasFixtures.autolink();
        doReturn(Optional.of(loaded)).when(api).getAutolink(OWNER, NAME, ExtrasFixtures.AUTOLINK_ID);

        final Optional<Autolink> found = sut.autolink(ExtrasFixtures.AUTOLINK_ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().repository()).isSameAs(sut);
        });
    }

    @Test
    void autolink_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();
        doReturn(Optional.empty()).when(api).getAutolink(OWNER, NAME, 1L);

        final Optional<Autolink> found = sut.autolink(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void addAutolink_shouldSendThePrefixAndTemplate_whenCalled() throws IOException {
        final GitHubAutolink created = ExtrasFixtures.autolink();
        final GitHubRepositoriesAutolinksApi api = github.repositories().autolinks();
        doReturn(created).when(api).createAutolink(eq(OWNER), eq(NAME), any(GitHubCreateAutolinkPayload.class));
        final ArgumentCaptor<GitHubCreateAutolinkPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateAutolinkPayload.class);

        final Autolink autolink = sut.addAutolink("TICKET-", "https://tickets.example.com/<num>");

        verify(api).createAutolink(eq(OWNER), eq(NAME), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().keyPrefix()).isEqualTo("TICKET-");
            softly.assertThat(payload.getValue().urlTemplate()).isEqualTo("https://tickets.example.com/<num>");
            softly.assertThat(autolink.response()).isSameAs(created);
        });
    }

    @Test
    void addAutolink_shouldThrowIllegalArgumentException_whenPrefixIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addAutolink(" ", "https://example.com/<num>"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("keyPrefix");
    }

    @Test
    void addAutolink_shouldThrowIllegalArgumentException_whenTemplateIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addAutolink("TICKET-", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("urlTemplate");
    }

    @Test
    void checkSuite_shouldReturnTheSuite_whenGitHubHasIt() throws IOException {
        final GitHubCheckSuite suite = ExtrasFixtures.checkSuite();
        final GitHubChecksSuitesApi api = github.checks().suites();
        doReturn(Optional.of(suite)).when(api).getSuite(OWNER, NAME, ExtrasFixtures.SUITE_ID);

        final Optional<CheckSuite> found = sut.checkSuite(ExtrasFixtures.SUITE_ID);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(suite);
        });
    }

    @Test
    void checkSuite_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubChecksSuitesApi api = github.checks().suites();
        doReturn(Optional.empty()).when(api).getSuite(OWNER, NAME, 1L);

        final Optional<CheckSuite> found = sut.checkSuite(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void license_shouldReturnTheLicense_whenTheRepositoryHasOne() throws IOException {
        final GitHubLicenseContent license = mock(GitHubLicenseContent.class);
        final GitHubLicensesApi api = github.licenses();
        doReturn(Optional.of(license)).when(api).getForRepo(OWNER, NAME);

        final Optional<GitHubLicenseContent> found = sut.license();

        assertThat(found).containsSame(license);
    }

    @Test
    void license_shouldBeEmpty_whenTheRepositoryHasNone() throws IOException {
        final GitHubLicensesApi api = github.licenses();
        doReturn(Optional.empty()).when(api).getForRepo(OWNER, NAME);

        final Optional<GitHubLicenseContent> found = sut.license();

        assertThat(found).isEmpty();
    }
}
