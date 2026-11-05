package io.github.piotr_janczukowicz.github.sdk.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCache;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheList;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheRetentionLimitForRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheStorageLimitForRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheUsageByRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubDeleteActionsCacheByKeyFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubGetActionsCacheListFilter;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryActionsCacheTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubActionsCacheApi api = github.actions().cache();
    private final RepositoryActionsCache sut = repository.actionsCache();

    @Test
    void caches_shouldWrapTheCaches_whenTheyExist() throws IOException {
        final GitHubActionsCache listed = cache(7L);
        when(api.listActionsCaches(OWNER, NAME).stream()).thenReturn(Stream.of(listed));

        final List<ActionsCache> caches = sut.caches().toList();

        assertThat(caches).singleElement().satisfies(cache -> {
            assertThat(cache.id()).isEqualTo(7L);
            assertThat(cache.key()).isEqualTo("maven-key");
            assertThat(cache.ref()).isEqualTo("refs/heads/main");
            assertThat(cache.response()).isSameAs(listed);
        });
    }

    @Test
    void caches_shouldPassTheFilter_whenOneIsGiven() throws IOException {
        final GitHubGetActionsCacheListFilter filter = mock(GitHubGetActionsCacheListFilter.class);
        final GitHubActionsCache listed = cache(9L);
        when(api.listActionsCaches(eq(OWNER), eq(NAME), eq(filter), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));

        final List<ActionsCache> caches = sut.caches(filter).toList();

        assertThat(caches).extracting(ActionsCache::id).containsExactly(9L);
    }

    @Test
    void caches_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.caches(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void delete_shouldDeleteTheCache_whenCalled() throws IOException {
        final GitHubActionsCache listed = cache(7L);
        when(api.listActionsCaches(OWNER, NAME).stream()).thenReturn(Stream.of(listed));
        final ActionsCache cache = sut.caches().findFirst().orElseThrow();

        cache.delete();

        verify(api).deleteActionsCacheById(OWNER, NAME, 7L);
    }

    @Test
    void deleteByKey_shouldReturnTheDeletedCaches_whenCalled() throws IOException {
        final GitHubActionsCacheList deleted = mock(GitHubActionsCacheList.class);
        final GitHubActionsCache removed = cache(8L);
        when(deleted.actionsCaches()).thenReturn(List.of(removed));
        when(api.deleteActionsCacheByKey(eq(OWNER), eq(NAME), any(GitHubDeleteActionsCacheByKeyFilter.class)))
                .thenReturn(deleted);
        final ArgumentCaptor<GitHubDeleteActionsCacheByKeyFilter> filter =
                ArgumentCaptor.forClass(GitHubDeleteActionsCacheByKeyFilter.class);

        final List<ActionsCache> result = sut.deleteByKey("maven-key");

        verify(api).deleteActionsCacheByKey(eq(OWNER), eq(NAME), filter.capture());
        assertSoftly(softly -> {
            softly.assertThat(result).extracting(ActionsCache::id).containsExactly(8L);
            softly.assertThat(filter.getValue().key()).isEqualTo("maven-key");
        });
    }

    @Test
    void deleteByKey_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.deleteByKey(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
    }

    @Test
    void usage_shouldReturnTheUsage_whenCalled() throws IOException {
        final GitHubActionsCacheUsageByRepository usage = mock(GitHubActionsCacheUsageByRepository.class);
        when(api.getRepoCacheUsage(OWNER, NAME)).thenReturn(usage);

        final GitHubActionsCacheUsageByRepository result = sut.usage();

        assertThat(result).isSameAs(usage);
    }

    @Test
    void retentionDays_shouldReturnTheDays_whenGitHubReportsThem() throws IOException {
        final GitHubActionsCacheRetentionLimitForRepository limit =
                mock(GitHubActionsCacheRetentionLimitForRepository.class);
        when(limit.maxCacheRetentionDays()).thenReturn(14);
        when(api.getRepoCacheRetentionLimit(OWNER, NAME)).thenReturn(limit);

        final OptionalInt days = sut.retentionDays();

        assertThat(days).hasValue(14);
    }

    @Test
    void retentionDays_shouldBeEmpty_whenGitHubReportsNone() throws IOException {
        final GitHubActionsCacheRetentionLimitForRepository limit =
                mock(GitHubActionsCacheRetentionLimitForRepository.class);
        when(limit.maxCacheRetentionDays()).thenReturn(null);
        when(api.getRepoCacheRetentionLimit(OWNER, NAME)).thenReturn(limit);

        final OptionalInt days = sut.retentionDays();

        assertThat(days).isEmpty();
    }

    @Test
    void setRetentionDays_shouldSendTheDays_whenCalled() throws IOException {
        final ArgumentCaptor<GitHubActionsCacheRetentionLimitForRepository> limit =
                ArgumentCaptor.forClass(GitHubActionsCacheRetentionLimitForRepository.class);

        sut.setRetentionDays(10);

        verify(api).setRepoCacheRetentionLimit(eq(OWNER), eq(NAME), limit.capture());
        assertThat(limit.getValue().maxCacheRetentionDays()).isEqualTo(10);
    }

    @Test
    void setRetentionDays_shouldThrowIllegalArgumentException_whenDaysAreBelowOne() {
        final Throwable thrown = catchThrowable(() -> sut.setRetentionDays(0));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("days");
    }

    @Test
    void storageGigabytes_shouldReturnTheGigabytes_whenGitHubReportsThem() throws IOException {
        final GitHubActionsCacheStorageLimitForRepository limit =
                mock(GitHubActionsCacheStorageLimitForRepository.class);
        when(limit.maxCacheSizeGb()).thenReturn(5);
        when(api.getRepoCacheStorageLimit(OWNER, NAME)).thenReturn(limit);

        final OptionalInt gigabytes = sut.storageGigabytes();

        assertThat(gigabytes).hasValue(5);
    }

    @Test
    void storageGigabytes_shouldBeEmpty_whenGitHubReportsNone() throws IOException {
        final GitHubActionsCacheStorageLimitForRepository limit =
                mock(GitHubActionsCacheStorageLimitForRepository.class);
        when(limit.maxCacheSizeGb()).thenReturn(null);
        when(api.getRepoCacheStorageLimit(OWNER, NAME)).thenReturn(limit);

        final OptionalInt gigabytes = sut.storageGigabytes();

        assertThat(gigabytes).isEmpty();
    }

    @Test
    void setStorageGigabytes_shouldSendTheGigabytes_whenCalled() throws IOException {
        final ArgumentCaptor<GitHubActionsCacheStorageLimitForRepository> limit =
                ArgumentCaptor.forClass(GitHubActionsCacheStorageLimitForRepository.class);

        sut.setStorageGigabytes(3);

        verify(api).setRepoCacheStorageLimit(eq(OWNER), eq(NAME), limit.capture());
        assertThat(limit.getValue().maxCacheSizeGb()).isEqualTo(3);
    }

    @Test
    void setStorageGigabytes_shouldThrowIllegalArgumentException_whenGigabytesAreBelowOne() {
        final Throwable thrown = catchThrowable(() -> sut.setStorageGigabytes(0));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("gigabytes");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryActionsCache(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void actionsCache_constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new ActionsCache(sut, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }

    private static GitHubActionsCache cache(final long id) {
        final GitHubActionsCache cache = mock(GitHubActionsCache.class);
        lenient().when(cache.id()).thenReturn(id);
        lenient().when(cache.key()).thenReturn("maven-key");
        lenient().when(cache.ref()).thenReturn("refs/heads/main");
        return cache;
    }
}
