package io.github.piotr_janczukowicz.github.sdk.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.apps.GitHubUserMarketplacePurchase;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.api.users.GitHubUserApi;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class MeEventsTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final Me sut = sdk.me();

    @Test
    void events_shouldStreamTheEventsOfTheAccount_whenTheyExist() throws IOException {
        final GitHubEvent event = mock(GitHubEvent.class);
        signedInAs("octocat");
        when(github.activity().events().listEventsForAuthenticatedUser("octocat").stream())
                .thenReturn(Stream.of(event));

        final List<GitHubEvent> events = sut.events().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void receivedEvents_shouldStreamTheReceivedEventsOfTheAccount_whenTheyExist() throws IOException {
        final GitHubEvent event = mock(GitHubEvent.class);
        signedInAs("octocat");
        when(github.activity().events().listReceivedEventsForUser("octocat").stream())
                .thenReturn(Stream.of(event));

        final List<GitHubEvent> events = sut.receivedEvents().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void marketplaceSubscriptions_shouldStreamTheSubscriptions_whenTheyExist() throws IOException {
        final GitHubUserMarketplacePurchase purchase = mock(GitHubUserMarketplacePurchase.class);
        when(github.apps().marketplace().listSubscriptionsForAuthenticatedUser(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(purchase));

        final List<GitHubUserMarketplacePurchase> purchases =
                sut.marketplaceSubscriptions().toList();

        assertThat(purchases).containsExactly(purchase);
    }

    private void signedInAs(final String login) throws IOException {
        final GitHubUser view = mock(GitHubUser.class);
        lenient().when(view.login()).thenReturn(login);
        final GitHubUserApi api = github.users().user();
        doReturn(view).when(api).getAuthenticatedUser();
    }
}
