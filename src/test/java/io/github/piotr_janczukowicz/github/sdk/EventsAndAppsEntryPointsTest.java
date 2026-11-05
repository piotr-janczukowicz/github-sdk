package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.apps.GitHubIntegration;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.users.GitHubUser;
import io.github.piotr_janczukowicz.github.sdk.apps.App;
import io.github.piotr_janczukowicz.github.sdk.apps.Marketplace;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AppFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class EventsAndAppsEntryPointsTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubEvent event = mock(GitHubEvent.class);

    @Test
    void events_shouldStreamTheRepositoryEvents_whenTheyExist() throws IOException {
        when(github.activity().events().listRepoEvents("octocat", "hello-world").stream())
                .thenReturn(Stream.of(event));

        final List<GitHubEvent> events =
                sdk.repository("octocat", "hello-world").events().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void networkEvents_shouldStreamTheNetworkEvents_whenTheyExist() throws IOException {
        when(github.activity().events().listPublicEventsForRepoNetwork("octocat", "hello-world").stream())
                .thenReturn(Stream.of(event));

        final List<GitHubEvent> events =
                sdk.repository("octocat", "hello-world").networkEvents().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void events_shouldStreamTheOrganizationEvents_whenTheyExist() throws IOException {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn("acme");
        when(github.activity().events().listPublicOrgEvents("acme").stream()).thenReturn(Stream.of(event));

        final List<GitHubEvent> events =
                new Organization(sdk, response).events().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void events_shouldStreamTheUserEvents_whenTheyExist() throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        lenient().when(response.login()).thenReturn("octocat");
        when(github.activity().events().listPublicEventsForUser("octocat").stream())
                .thenReturn(Stream.of(event));

        final List<GitHubEvent> events = new User(sdk, response).events().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void receivedEvents_shouldStreamTheReceivedEvents_whenTheyExist() throws IOException {
        final GitHubUser response = mock(GitHubUser.class);
        lenient().when(response.login()).thenReturn("octocat");
        when(github.activity().events().listReceivedPublicEventsForUser("octocat").stream())
                .thenReturn(Stream.of(event));

        final List<GitHubEvent> events =
                new User(sdk, response).receivedEvents().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void publicEvents_shouldStreamThePublicEvents_whenTheyExist() throws IOException {
        when(github.activity().events().listPublicEvents().stream()).thenReturn(Stream.of(event));

        final List<GitHubEvent> events = sdk.publicEvents().toList();

        assertThat(events).containsExactly(event);
    }

    @Test
    void app_shouldReturnTheAuthenticatedApp_whenCalled() throws IOException {
        final GitHubIntegration integration = AppFixtures.integration();
        when(github.apps().app().getAuthenticated()).thenReturn(integration);

        final App app = sdk.app();

        assertThat(app.response()).isSameAs(integration);
    }

    @Test
    void app_shouldReturnThePublicApp_whenTheSlugExists() throws IOException {
        final GitHubIntegration integration = AppFixtures.integration();
        when(github.apps().app().getBySlug(AppFixtures.SLUG)).thenReturn(Optional.of(integration));

        final Optional<App> found = sdk.app(AppFixtures.SLUG);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(integration);
    }

    @Test
    void app_shouldThrowIllegalArgumentException_whenSlugIsBlank() {
        final Throwable thrown = catchThrowable(() -> sdk.app(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("slug");
    }

    @Test
    void marketplace_shouldReturnTheListingWithoutRequest_whenCalled() {
        final Marketplace marketplace = sdk.marketplace();

        assertThat(marketplace).isNotNull();
    }
}
