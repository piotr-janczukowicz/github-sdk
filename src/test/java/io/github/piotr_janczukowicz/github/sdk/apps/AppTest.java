package io.github.piotr_janczukowicz.github.sdk.apps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.apps.GitHubAppApi;
import io.github.piotr_janczukowicz.github.api.apps.GitHubAppsWebhooksApi;
import io.github.piotr_janczukowicz.github.api.apps.GitHubIntegration;
import io.github.piotr_janczukowicz.github.api.apps.GitHubIntegrationInstallationRequest;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubInstallation;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookDelivery;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookDeliveryItem;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubWebhookConfig;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.fixtures.AppFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class AppTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubAppApi api = github.apps().app();
    private final GitHubAppsWebhooksApi webhooks = github.apps().webhooks();
    private final GitHubIntegration response = AppFixtures.integration();
    private final App sut = new App(sdk, response);

    @Test
    void accessors_shouldExposeTheResponse_whenAppIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(AppFixtures.APP_ID);
            softly.assertThat(sut.slug()).isEqualTo(AppFixtures.SLUG);
            softly.assertThat(sut.name()).isEqualTo("Octo App");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> new App(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("sdk");
    }

    @Test
    void refresh_shouldReturnTheReloadedApp_whenItExists() throws IOException {
        final GitHubIntegration reloaded = AppFixtures.integration();
        when(api.getBySlug(AppFixtures.SLUG)).thenReturn(Optional.of(reloaded));

        final App result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheAppIsGone() throws IOException {
        when(api.getBySlug(AppFixtures.SLUG)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void installations_shouldWrapTheListedInstallations_whenTheyExist() throws IOException {
        final GitHubInstallation listed = AppFixtures.installation();
        when(api.listInstallations().stream()).thenReturn(Stream.of(listed));

        final List<Installation> installations = sut.installations().toList();

        assertThat(installations)
                .singleElement()
                .satisfies(installation -> assertThat(installation.response()).isSameAs(listed));
    }

    @Test
    void installation_shouldReturnTheInstallation_whenItExists() throws IOException {
        final GitHubInstallation loaded = AppFixtures.installation();
        when(api.getInstallation(AppFixtures.INSTALLATION_ID)).thenReturn(Optional.of(loaded));

        final Optional<Installation> found = sut.installation(AppFixtures.INSTALLATION_ID);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void organizationInstallation_shouldReturnTheInstallation_whenTheAppIsInstalled() throws IOException {
        final GitHubInstallation loaded = AppFixtures.installation();
        when(api.getOrgInstallation("acme")).thenReturn(loaded);

        final Optional<Installation> found = sut.organizationInstallation("acme");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void organizationInstallation_shouldBeEmpty_whenGitHubAnswersNotFound() throws IOException {
        doThrow(mock(GitHubNotFoundException.class)).when(api).getOrgInstallation("acme");

        final Optional<Installation> found = sut.organizationInstallation("acme");

        assertThat(found).isEmpty();
    }

    @Test
    void organizationInstallation_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.organizationInstallation(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void repositoryInstallation_shouldReturnTheInstallation_whenTheAppIsInstalled() throws IOException {
        final GitHubInstallation loaded = AppFixtures.installation();
        when(api.getRepoInstallation("octocat", "hello-world")).thenReturn(loaded);
        final Repository repository = sdk.repository("octocat", "hello-world");

        final Optional<Installation> found = sut.repositoryInstallation(repository);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void repositoryInstallation_shouldThrowNullPointerException_whenRepositoryIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.repositoryInstallation(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("repository");
    }

    @Test
    void userInstallation_shouldReturnTheInstallation_whenTheAppIsInstalled() throws IOException {
        final GitHubInstallation loaded = AppFixtures.installation();
        when(api.getUserInstallation("octocat")).thenReturn(Optional.of(loaded));

        final Optional<Installation> found = sut.userInstallation("octocat");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().response()).isSameAs(loaded);
    }

    @Test
    void installationRequests_shouldStreamTheRequests_whenTheyExist() throws IOException {
        final GitHubIntegrationInstallationRequest request = mock(GitHubIntegrationInstallationRequest.class);
        when(api.listInstallationRequests().stream()).thenReturn(Stream.of(request));

        final List<GitHubIntegrationInstallationRequest> requests =
                sut.installationRequests().toList();

        assertThat(requests).containsExactly(request);
    }

    @Test
    void webhookConfig_shouldReturnTheConfiguration_whenCalled() throws IOException {
        final GitHubWebhookConfig config = mock(GitHubWebhookConfig.class);
        when(webhooks.getWebhookConfigForApp()).thenReturn(config);

        final GitHubWebhookConfig result = sut.webhookConfig();

        assertThat(result).isSameAs(config);
    }

    @Test
    void webhookDeliveries_shouldStreamTheDeliveries_whenTheyExist() throws IOException {
        final GitHubHookDeliveryItem delivery = mock(GitHubHookDeliveryItem.class);
        when(webhooks.listWebhookDeliveries().stream()).thenReturn(Stream.of(delivery));

        final List<GitHubHookDeliveryItem> deliveries = sut.webhookDeliveries().toList();

        assertThat(deliveries).containsExactly(delivery);
    }

    @Test
    void webhookDelivery_shouldReturnTheDelivery_whenItExists() throws IOException {
        final GitHubHookDelivery delivery = mock(GitHubHookDelivery.class);
        when(webhooks.getWebhookDelivery(9L)).thenReturn(Optional.of(delivery));

        final Optional<GitHubHookDelivery> found = sut.webhookDelivery(9L);

        assertThat(found).containsSame(delivery);
    }

    @Test
    void redeliver_shouldAskGitHubToDeliverAgain_whenCalled() throws IOException {
        sut.redeliver(9L);

        verify(webhooks).redeliverWebhookDelivery(9L);
    }
}
