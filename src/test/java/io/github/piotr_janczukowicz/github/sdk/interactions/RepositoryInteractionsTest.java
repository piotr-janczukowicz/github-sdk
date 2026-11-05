package io.github.piotr_janczukowicz.github.sdk.interactions;

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
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionExpiry;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionGroup;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionLimit;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionLimitResponse;
import io.github.piotr_janczukowicz.github.api.interactions.GitHubInteractionsReposApi;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryInteractionsTest {

    private static final String OWNER = "octocat";
    private static final String NAME = "hello-world";

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final Repository repository = GitHubSdk.of(github).repository(OWNER, NAME);
    private final GitHubInteractionsReposApi api = github.interactions().repos();
    private final RepositoryInteractions sut = repository.interactions();

    @Test
    void current_shouldReturnTheLimit_whenOneIsActive() throws IOException {
        final GitHubInteractionLimitResponse active = mock(GitHubInteractionLimitResponse.class);
        when(api.getRestrictionsForRepo(OWNER, NAME)).thenReturn(Optional.of(active));

        final Optional<GitHubInteractionLimitResponse> current = sut.current();

        assertThat(current).containsSame(active);
    }

    @Test
    void current_shouldBeEmpty_whenNoLimitIsActive() throws IOException {
        when(api.getRestrictionsForRepo(OWNER, NAME)).thenReturn(Optional.empty());

        assertThat(sut.current()).isEmpty();
    }

    @Test
    void restrict_shouldLimitForOneDay_whenNoExpiryIsGiven() throws IOException {
        final GitHubInteractionLimitResponse set = mock(GitHubInteractionLimitResponse.class);
        when(api.setRestrictionsForRepo(eq(OWNER), eq(NAME), any())).thenReturn(set);
        final ArgumentCaptor<GitHubInteractionLimit> limit = ArgumentCaptor.forClass(GitHubInteractionLimit.class);

        final GitHubInteractionLimitResponse result = sut.restrict(InteractionGroup.CONTRIBUTORS_ONLY);

        verify(api).setRestrictionsForRepo(eq(OWNER), eq(NAME), limit.capture());
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(set);
            softly.assertThat(limit.getValue().limit()).isEqualTo(GitHubInteractionGroup.CONTRIBUTORS_ONLY);
            softly.assertThat(limit.getValue().expiry()).isEqualTo(GitHubInteractionExpiry.ONE_DAY);
        });
    }

    @Test
    void restrict_shouldSendTheExpiry_whenOneIsGiven() throws IOException {
        final GitHubInteractionLimitResponse set = mock(GitHubInteractionLimitResponse.class);
        when(api.setRestrictionsForRepo(eq(OWNER), eq(NAME), any())).thenReturn(set);
        final ArgumentCaptor<GitHubInteractionLimit> limit = ArgumentCaptor.forClass(GitHubInteractionLimit.class);

        sut.restrict(InteractionGroup.EXISTING_USERS, InteractionExpiry.SIX_MONTHS);

        verify(api).setRestrictionsForRepo(eq(OWNER), eq(NAME), limit.capture());
        assertSoftly(softly -> {
            softly.assertThat(limit.getValue().limit()).isEqualTo(GitHubInteractionGroup.EXISTING_USERS);
            softly.assertThat(limit.getValue().expiry()).isEqualTo(GitHubInteractionExpiry.SIX_MONTHS);
        });
    }

    @Test
    void restrict_shouldThrowNullPointerException_whenGroupIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.restrict(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("group");
    }

    @Test
    void restrict_shouldThrowNullPointerException_whenExpiryIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.restrict(InteractionGroup.EXISTING_USERS, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("expiry");
    }

    @Test
    void remove_shouldRemoveTheLimit_whenCalled() throws IOException {
        sut.remove();

        verify(api).removeRestrictionsForRepo(OWNER, NAME);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> new RepositoryInteractions(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void toClient_shouldMapEveryValueByName_whenGroupsAndExpiriesAreConverted() {
        final boolean areGroupsMapped = Arrays.stream(InteractionGroup.values())
                .allMatch(group -> group.toClient().name().equals(group.name()));
        final boolean areExpiriesMapped = Arrays.stream(InteractionExpiry.values())
                .allMatch(expiry -> expiry.toClient().name().equals(expiry.name()));

        assertSoftly(softly -> {
            softly.assertThat(areGroupsMapped).isTrue();
            softly.assertThat(areExpiriesMapped).isTrue();
        });
    }
}
