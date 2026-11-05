package io.github.piotr_janczukowicz.github.sdk.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsVariablesApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubUpdateOrgVariablePayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrgHook;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsVariable;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsVariableVisibility;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationsMembersApi;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationsWebhooksApi;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubUpdateOrgWebhookPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookDeliveryItem;
import io.github.piotr_janczukowicz.github.api.teams.GitHubOrganizationInvitation;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.fixtures.OrganizationFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class OrganizationObjectsTest {

    private static final String ORG = OrganizationFixtures.ORG;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void invitation_shouldExposeTheResponse_whenInvitationIsForAUser() {
        final GitHubOrganizationInvitation response = OrganizationFixtures.invitation();
        final OrganizationInvitation sut = new OrganizationInvitation(sdk, ORG, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.organization()).isEqualTo(ORG);
            softly.assertThat(sut.id()).isEqualTo(OrganizationFixtures.INVITATION_ID);
            softly.assertThat(sut.login()).hasValue("hubot");
            softly.assertThat(sut.email()).isEmpty();
            softly.assertThat(sut.role()).isEqualTo("direct_member");
            softly.assertThat(sut.isFailed()).isFalse();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void invitation_shouldExposeTheEmailAndFailure_whenInvitationIsForAnEmailThatBounced() {
        final OrganizationInvitation sut = new OrganizationInvitation(sdk, ORG, OrganizationFixtures.emailInvitation());

        assertSoftly(softly -> {
            softly.assertThat(sut.login()).isEmpty();
            softly.assertThat(sut.email()).hasValue("new@example.com");
            softly.assertThat(sut.isFailed()).isTrue();
        });
    }

    @Test
    void invitation_shouldCancelTheInvitation_whenCalled() throws IOException {
        final GitHubOrganizationsMembersApi api = github.organizations().members();

        new OrganizationInvitation(sdk, ORG, OrganizationFixtures.invitation()).cancel();

        verify(api).cancelInvitation(ORG, OrganizationFixtures.INVITATION_ID);
    }

    @Test
    void invitation_shouldThrowIllegalArgumentException_whenOrganizationIsBlank() {
        final GitHubOrganizationInvitation response = OrganizationFixtures.invitation();

        final Throwable thrown = catchThrowable(() -> new OrganizationInvitation(sdk, " ", response));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("organization");
    }

    @Test
    void webhook_shouldExposeTheResponse_whenWebhookIsLoaded() {
        final GitHubOrgHook response = OrganizationFixtures.hook();
        final OrganizationWebhook sut = new OrganizationWebhook(sdk, ORG, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.organization()).isEqualTo(ORG);
            softly.assertThat(sut.id()).isEqualTo(OrganizationFixtures.HOOK_ID);
            softly.assertThat(sut.events()).containsExactly("push");
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void webhook_shouldReturnTheReloadedWebhook_whenItExists() throws IOException {
        final GitHubOrgHook reloaded = OrganizationFixtures.hook();
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();
        doReturn(Optional.of(reloaded)).when(api).getWebhook(ORG, OrganizationFixtures.HOOK_ID);

        final OrganizationWebhook result = new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook()).refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void webhook_shouldThrowIllegalStateException_whenTheWebhookIsGone() throws IOException {
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();
        doReturn(Optional.empty()).when(api).getWebhook(ORG, OrganizationFixtures.HOOK_ID);
        final OrganizationWebhook sut = new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void webhook_shouldSendThePayloadAndReturnTheWebhook_whenUpdated() throws IOException {
        final GitHubOrgHook updated = OrganizationFixtures.hook();
        final GitHubUpdateOrgWebhookPayload payload = mock(GitHubUpdateOrgWebhookPayload.class);
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();
        doReturn(updated).when(api).updateWebhook(ORG, OrganizationFixtures.HOOK_ID, payload);

        final OrganizationWebhook result =
                new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook()).update(payload);

        assertThat(result.response()).isSameAs(updated);
    }

    @Test
    void webhook_shouldThrowNullPointerException_whenUpdatePayloadIsNull() {
        final OrganizationWebhook sut = new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook());

        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void webhook_shouldPingTheWebhook_whenCalled() throws IOException {
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();

        new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook()).ping();

        verify(api).pingWebhook(ORG, OrganizationFixtures.HOOK_ID);
    }

    @Test
    void webhook_shouldStreamTheDeliveries_whenTheyExist() throws IOException {
        final GitHubHookDeliveryItem delivery = mock(GitHubHookDeliveryItem.class);
        when(github.organizations().webhooks().listWebhookDeliveries(ORG, OrganizationFixtures.HOOK_ID).stream())
                .thenReturn(Stream.of(delivery));

        final List<GitHubHookDeliveryItem> deliveries = new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook())
                .deliveries()
                .toList();

        assertThat(deliveries).containsExactly(delivery);
    }

    @Test
    void webhook_shouldDeleteTheWebhook_whenCalled() throws IOException {
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();

        new OrganizationWebhook(sdk, ORG, OrganizationFixtures.hook()).delete();

        verify(api).deleteWebhook(ORG, OrganizationFixtures.HOOK_ID);
    }

    @Test
    void variable_shouldExposeTheResponse_whenVariableIsLoaded() {
        final GitHubOrganizationActionsVariable response = OrganizationFixtures.variable();
        final OrganizationVariable sut = new OrganizationVariable(sdk, ORG, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.organization()).isEqualTo(ORG);
            softly.assertThat(sut.name()).isEqualTo("DEPLOY_ENV");
            softly.assertThat(sut.value()).isEqualTo("staging");
            softly.assertThat(sut.visibility()).isEqualTo(GitHubOrganizationActionsVariableVisibility.ALL);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void variable_shouldReturnTheReloadedVariable_whenItExists() throws IOException {
        final GitHubOrganizationActionsVariable reloaded = OrganizationFixtures.variable();
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.of(reloaded)).when(api).getOrgVariable(ORG, "DEPLOY_ENV");

        final OrganizationVariable result =
                new OrganizationVariable(sdk, ORG, OrganizationFixtures.variable()).refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void variable_shouldThrowIllegalStateException_whenTheVariableIsGone() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.empty()).when(api).getOrgVariable(ORG, "DEPLOY_ENV");
        final OrganizationVariable sut = new OrganizationVariable(sdk, ORG, OrganizationFixtures.variable());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void variable_shouldSendTheValueAndReturnTheVariable_whenUpdated() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.of(OrganizationFixtures.variable())).when(api).getOrgVariable(ORG, "DEPLOY_ENV");
        final ArgumentCaptor<GitHubUpdateOrgVariablePayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateOrgVariablePayload.class);

        final OrganizationVariable result =
                new OrganizationVariable(sdk, ORG, OrganizationFixtures.variable()).update("production");

        verify(api).updateOrgVariable(any(), any(), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().value()).isEqualTo("production");
            softly.assertThat(payload.getValue().name()).isNull();
            softly.assertThat(result.name()).isEqualTo("DEPLOY_ENV");
        });
    }

    @Test
    void variable_shouldThrowNullPointerException_whenUpdateValueIsNull() {
        final OrganizationVariable sut = new OrganizationVariable(sdk, ORG, OrganizationFixtures.variable());

        final Throwable thrown = catchThrowable(() -> sut.update(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void variable_shouldDeleteTheVariable_whenCalled() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();

        new OrganizationVariable(sdk, ORG, OrganizationFixtures.variable()).delete();

        verify(api).deleteOrgVariable(ORG, "DEPLOY_ENV");
    }
}
