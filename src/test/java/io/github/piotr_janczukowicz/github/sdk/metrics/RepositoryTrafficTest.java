package io.github.piotr_janczukowicz.github.sdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCloneTraffic;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubContentTraffic;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubGetClonesFilter;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubMetricsTrafficApi;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubReferrerTraffic;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubTrafficPer;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubViewTraffic;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryTrafficTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubMetricsTrafficApi api = github.metrics().traffic();
    private final RepositoryTraffic sut = repository.traffic();

    @Test
    void views_shouldReturnTheViewsPerDay_whenNoUnitIsGiven() throws IOException {
        final GitHubViewTraffic views = mock(GitHubViewTraffic.class);
        doReturn(views).when(api).getViews(OWNER, NAME);

        final GitHubViewTraffic result = sut.views();

        assertThat(result).isSameAs(views);
    }

    @Test
    void views_shouldReturnTheViewsPerUnit_whenAUnitIsGiven() throws IOException {
        final GitHubViewTraffic views = mock(GitHubViewTraffic.class);
        doReturn(views).when(api).getViews(OWNER, NAME, new GitHubGetClonesFilter(GitHubTrafficPer.WEEK));

        final GitHubViewTraffic result = sut.views(GitHubTrafficPer.WEEK);

        assertThat(result).isSameAs(views);
    }

    @Test
    void clones_shouldReturnTheClonesPerDay_whenNoUnitIsGiven() throws IOException {
        final GitHubCloneTraffic clones = mock(GitHubCloneTraffic.class);
        doReturn(clones).when(api).getClones(OWNER, NAME);

        final GitHubCloneTraffic result = sut.clones();

        assertThat(result).isSameAs(clones);
    }

    @Test
    void clones_shouldReturnTheClonesPerUnit_whenAUnitIsGiven() throws IOException {
        final GitHubCloneTraffic clones = mock(GitHubCloneTraffic.class);
        doReturn(clones).when(api).getClones(OWNER, NAME, new GitHubGetClonesFilter(GitHubTrafficPer.WEEK));

        final GitHubCloneTraffic result = sut.clones(GitHubTrafficPer.WEEK);

        assertThat(result).isSameAs(clones);
    }

    @Test
    void topPaths_shouldReturnThePaths_whenCalled() throws IOException {
        final GitHubContentTraffic path = mock(GitHubContentTraffic.class);
        doReturn(List.of(path)).when(api).getTopPaths(OWNER, NAME);

        final List<GitHubContentTraffic> paths = sut.topPaths();

        assertThat(paths).containsExactly(path);
    }

    @Test
    void topReferrers_shouldReturnTheReferrers_whenCalled() throws IOException {
        final GitHubReferrerTraffic referrer = mock(GitHubReferrerTraffic.class);
        doReturn(List.of(referrer)).when(api).getTopReferrers(OWNER, NAME);

        final List<GitHubReferrerTraffic> referrers = sut.topReferrers();

        assertThat(referrers).containsExactly(referrer);
    }

    @Test
    void views_shouldThrowNullPointerException_whenUnitIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.views(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("per");
    }

    @Test
    void clones_shouldThrowNullPointerException_whenUnitIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.clones(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("per");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryTraffic(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }
}
