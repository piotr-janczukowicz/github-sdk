package io.github.piotr_janczukowicz.github.sdk.pages;

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
import io.github.piotr_janczukowicz.github.api.pages.GitHubPageBuild;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPageBuildStatus;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesApi;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesBuildType;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSite;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSiteStatus;
import io.github.piotr_janczukowicz.github.api.pages.GitHubUpdatePagesSitePayload;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.DeploymentFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class PagesSiteTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubPagesSite response = DeploymentFixtures.pagesSite();
    private final PagesSite sut = new PagesSite(repository, response);

    @Test
    void accessors_shouldExposeTheResponse_whenSiteIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.htmlUrl()).contains("https://octocat.github.io/hello-world/");
            softly.assertThat(sut.status()).contains(GitHubPagesSiteStatus.BUILT);
            softly.assertThat(sut.buildType()).contains(GitHubPagesBuildType.LEGACY);
            softly.assertThat(sut.cname()).hasValue("example.com");
            softly.assertThat(sut.isPublic()).isTrue();
            softly.assertThat(sut.isHttpsEnforced()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenGitHubReportsNothing() {
        final PagesSite bare = new PagesSite(repository, DeploymentFixtures.barePagesSite());

        assertSoftly(softly -> {
            softly.assertThat(bare.cname()).isEmpty();
            softly.assertThat(bare.isHttpsEnforced()).isFalse();
        });
    }

    @Test
    void refresh_shouldReturnTheReloadedSite_whenItExists() throws IOException {
        final GitHubPagesSite reloaded = DeploymentFixtures.barePagesSite();
        final GitHubPagesApi api = github.pages();
        doReturn(Optional.of(reloaded)).when(api).getPages(OWNER, NAME);

        final PagesSite result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenThereIsNoSite() throws IOException {
        final GitHubPagesApi api = github.pages();
        doReturn(Optional.empty()).when(api).getPages(OWNER, NAME);

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("no Pages site");
    }

    @Test
    void update_shouldSendThePayloadAndReturnTheSite_whenCalled() throws IOException {
        final GitHubPagesSite reloaded = DeploymentFixtures.barePagesSite();
        final GitHubPagesApi api = github.pages();
        doReturn(Optional.of(reloaded)).when(api).getPages(OWNER, NAME);
        final GitHubUpdatePagesSitePayload payload = mock(GitHubUpdatePagesSitePayload.class);

        final PagesSite result = sut.update(payload);

        verify(api).updateInformationAboutPagesSite(OWNER, NAME, payload);
        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void update_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void delete_shouldTurnTheSiteOff_whenCalled() throws IOException {
        final GitHubPagesApi api = github.pages();

        sut.delete();

        verify(api).deletePagesSite(OWNER, NAME);
    }

    @Test
    void builds_shouldStreamTheBuilds_whenTheyExist() throws IOException {
        final GitHubPageBuild listed = DeploymentFixtures.pageBuild();
        when(github.pages().listPagesBuilds(eq(OWNER), eq(NAME), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));

        final List<GitHubPageBuild> builds = sut.builds().toList();

        assertThat(builds).containsExactly(listed);
    }

    @Test
    void latestBuild_shouldReturnTheLatestBuild_whenCalled() throws IOException {
        final GitHubPageBuild latest = DeploymentFixtures.pageBuild();
        final GitHubPagesApi api = github.pages();
        doReturn(latest).when(api).getLatestPagesBuild(OWNER, NAME);

        final GitHubPageBuild result = sut.latestBuild();

        assertThat(result).isSameAs(latest);
    }

    @Test
    void requestBuild_shouldReturnTheQueuedBuild_whenCalled() throws IOException {
        final GitHubPageBuildStatus queued = new GitHubPageBuildStatus("https://api.github.com/build", "queued");
        final GitHubPagesApi api = github.pages();
        doReturn(queued).when(api).requestPagesBuild(OWNER, NAME);

        final GitHubPageBuildStatus result = sut.requestBuild();

        assertThat(result).isSameAs(queued);
    }
}
