package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsSecretsApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsVariablesApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubCreateOrgVariablePayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubOrgVariableVisibility;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubCreateOrgWebhookPayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubCreateOrganizationInvitationPayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrgHook;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrgMembership;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsSecret;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationInvitationRole;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationMembershipRole;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationsMembersApi;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationsWebhooksApi;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubSetOrganizationMembershipPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubCreateTeamPayload;
import io.github.piotr_janczukowicz.github.api.teams.GitHubOrganizationInvitation;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamApi;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeamFull;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.fixtures.OrganizationFixtures;
import io.github.piotr_janczukowicz.github.sdk.organizations.InvitationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationInvitation;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationRole;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationVariable;
import io.github.piotr_janczukowicz.github.sdk.organizations.OrganizationWebhook;
import io.github.piotr_janczukowicz.github.sdk.organizations.VariableVisibility;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class OrganizationManagementTest {

    private static final String ORG = OrganizationFixtures.ORG;

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final Organization sut = organization();

    @Test
    void isMember_shouldReportGitHubsAnswer_whenCalled() throws IOException {
        final GitHubOrganizationsMembersApi api = github.organizations().members();
        doReturn(true).when(api).isMember(ORG, "hubot");

        final boolean isMember = sut.isMember("hubot");

        assertThat(isMember).isTrue();
    }

    @Test
    void membership_shouldReturnTheMembership_whenTheUserHasOne() throws IOException {
        final GitHubOrgMembership membership = mock(GitHubOrgMembership.class);
        final GitHubOrganizationsMembersApi api = github.organizations().members();
        doReturn(Optional.of(membership)).when(api).getMembership(ORG, "hubot");

        final Optional<GitHubOrgMembership> found = sut.membership("hubot");

        assertThat(found).containsSame(membership);
    }

    @Test
    void changeMembership_shouldSendTheRoleAndReturnTheMembership_whenCalled() throws IOException {
        final GitHubOrgMembership membership = mock(GitHubOrgMembership.class);
        final GitHubOrganizationsMembersApi api = github.organizations().members();
        doReturn(membership)
                .when(api)
                .setMembership(eq(ORG), eq("hubot"), any(GitHubSetOrganizationMembershipPayload.class));
        final ArgumentCaptor<GitHubSetOrganizationMembershipPayload> payload =
                ArgumentCaptor.forClass(GitHubSetOrganizationMembershipPayload.class);

        final GitHubOrgMembership result = sut.changeMembership("hubot", OrganizationRole.ADMIN);

        verify(api).setMembership(eq(ORG), eq("hubot"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().role()).isEqualTo(GitHubOrganizationMembershipRole.ADMIN);
            softly.assertThat(result).isSameAs(membership);
        });
    }

    @Test
    void changeMembership_shouldThrowNullPointerException_whenRoleIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.changeMembership("hubot", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("role");
    }

    @Test
    void removeMember_shouldRemoveTheUser_whenCalled() throws IOException {
        final GitHubOrganizationsMembersApi api = github.organizations().members();

        sut.removeMember("hubot");

        verify(api).removeMember(ORG, "hubot");
    }

    @Test
    void isMember_shouldThrowIllegalArgumentException_whenLoginIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.isMember(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("login");
    }

    @Test
    void outsideCollaborators_shouldStreamTheCollaborators_whenTheyExist() throws IOException {
        final GitHubSimpleUser collaborator = mock(GitHubSimpleUser.class);
        when(github
                        .organizations()
                        .outsideCollaborators()
                        .listOutsideCollaborators(eq(ORG), any(GitHubPageRequest.class))
                        .stream())
                .thenReturn(Stream.of(collaborator));

        final List<GitHubSimpleUser> collaborators = sut.outsideCollaborators().toList();

        assertThat(collaborators).containsExactly(collaborator);
    }

    @Test
    void inviteByEmail_shouldSendTheEmailAndRole_whenCalled() throws IOException {
        final GitHubOrganizationsMembersApi api = github.organizations().members();
        doReturn(OrganizationFixtures.emailInvitation())
                .when(api)
                .createInvitation(eq(ORG), any(GitHubCreateOrganizationInvitationPayload.class));
        final ArgumentCaptor<GitHubCreateOrganizationInvitationPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateOrganizationInvitationPayload.class);

        final OrganizationInvitation invitation = sut.inviteByEmail("new@example.com", InvitationRole.ADMIN);

        verify(api).createInvitation(eq(ORG), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().email()).isEqualTo("new@example.com");
            softly.assertThat(payload.getValue().inviteeId()).isNull();
            softly.assertThat(payload.getValue().role()).isEqualTo(GitHubOrganizationInvitationRole.ADMIN);
            softly.assertThat(invitation.organization()).isEqualTo(ORG);
        });
    }

    @Test
    void inviteUser_shouldSendTheUserIdAndRole_whenCalled() throws IOException {
        final GitHubOrganizationsMembersApi api = github.organizations().members();
        doReturn(OrganizationFixtures.invitation())
                .when(api)
                .createInvitation(eq(ORG), any(GitHubCreateOrganizationInvitationPayload.class));
        final ArgumentCaptor<GitHubCreateOrganizationInvitationPayload> payload =
                ArgumentCaptor.forClass(GitHubCreateOrganizationInvitationPayload.class);

        sut.inviteUser(42L, InvitationRole.DIRECT_MEMBER);

        verify(api).createInvitation(eq(ORG), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().inviteeId()).isEqualTo(42L);
            softly.assertThat(payload.getValue().email()).isNull();
            softly.assertThat(payload.getValue().role()).isEqualTo(GitHubOrganizationInvitationRole.DIRECT_MEMBER);
        });
    }

    @Test
    void inviteByEmail_shouldThrowIllegalArgumentException_whenEmailIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.inviteByEmail(" ", InvitationRole.ADMIN));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
    }

    @Test
    void inviteUser_shouldThrowNullPointerException_whenRoleIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.inviteUser(42L, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("role");
    }

    @Test
    void invitations_shouldStreamThePendingInvitations_whenTheyExist() throws IOException {
        final GitHubOrganizationInvitation listed = OrganizationFixtures.invitation();
        when(github.organizations().members().listPendingInvitations(ORG).stream())
                .thenReturn(Stream.of(listed));

        final List<OrganizationInvitation> invitations = sut.invitations().toList();

        assertThat(invitations).singleElement().satisfies(invitation -> {
            assertThat(invitation.response()).isSameAs(listed);
            assertThat(invitation.organization()).isEqualTo(ORG);
        });
    }

    @Test
    void createTeam_shouldSendOnlyTheName_whenNoPayloadIsGiven() throws IOException {
        final GitHubTeamFull created = mock(GitHubTeamFull.class);
        final GitHubTeamApi api = github.teams().team();
        doReturn(created).when(api).create(eq(ORG), any(GitHubCreateTeamPayload.class));
        final ArgumentCaptor<GitHubCreateTeamPayload> payload = ArgumentCaptor.forClass(GitHubCreateTeamPayload.class);

        final Team team = sut.createTeam("core");

        verify(api).create(eq(ORG), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("core");
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(team.response()).isSameAs(created);
            softly.assertThat(team.organization()).isSameAs(sut);
        });
    }

    @Test
    void createTeam_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createTeam(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void createTeam_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createTeam((GitHubCreateTeamPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void webhooks_shouldStreamTheWebhooks_whenTheyExist() throws IOException {
        final GitHubOrgHook listed = OrganizationFixtures.hook();
        when(github.organizations().webhooks().listWebhooks(eq(ORG), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));

        final List<OrganizationWebhook> webhooks = sut.webhooks().toList();

        assertThat(webhooks).singleElement().satisfies(webhook -> {
            assertThat(webhook.response()).isSameAs(listed);
            assertThat(webhook.organization()).isEqualTo(ORG);
        });
    }

    @Test
    void webhook_shouldReturnTheWebhook_whenGitHubHasIt() throws IOException {
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();
        doReturn(Optional.of(OrganizationFixtures.hook())).when(api).getWebhook(ORG, OrganizationFixtures.HOOK_ID);

        final Optional<OrganizationWebhook> found = sut.webhook(OrganizationFixtures.HOOK_ID);

        assertThat(found).isPresent();
    }

    @Test
    void webhook_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();
        doReturn(Optional.empty()).when(api).getWebhook(ORG, 1L);

        final Optional<OrganizationWebhook> found = sut.webhook(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void createWebhook_shouldSendThePayloadAndReturnTheWebhook_whenCalled() throws IOException {
        final GitHubCreateOrgWebhookPayload payload = mock(GitHubCreateOrgWebhookPayload.class);
        final GitHubOrgHook created = OrganizationFixtures.hook();
        final GitHubOrganizationsWebhooksApi api = github.organizations().webhooks();
        doReturn(created).when(api).createWebhook(ORG, payload);

        final OrganizationWebhook webhook = sut.createWebhook(payload);

        assertThat(webhook.response()).isSameAs(created);
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createWebhook(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void variables_shouldStreamTheVariables_whenTheyExist() throws IOException {
        when(github.actions().variables().listOrgVariables(eq(ORG), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(OrganizationFixtures.variable()));

        final List<OrganizationVariable> variables = sut.variables().toList();

        assertThat(variables).singleElement().satisfies(variable -> {
            assertThat(variable.name()).isEqualTo("DEPLOY_ENV");
            assertThat(variable.organization()).isEqualTo(ORG);
        });
    }

    @Test
    void variable_shouldReturnTheVariable_whenGitHubHasIt() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.of(OrganizationFixtures.variable())).when(api).getOrgVariable(ORG, "DEPLOY_ENV");

        final Optional<OrganizationVariable> found = sut.variable("DEPLOY_ENV");

        assertThat(found).isPresent();
    }

    @Test
    void variable_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.empty()).when(api).getOrgVariable(ORG, "NONE");

        final Optional<OrganizationVariable> found = sut.variable("NONE");

        assertThat(found).isEmpty();
    }

    @Test
    void createVariable_shouldSendTheVariableAndReturnIt_whenCalled() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.of(OrganizationFixtures.variable())).when(api).getOrgVariable(ORG, "DEPLOY_ENV");
        final ArgumentCaptor<GitHubCreateOrgVariablePayload> payload =
                ArgumentCaptor.forClass(GitHubCreateOrgVariablePayload.class);

        final OrganizationVariable variable = sut.createVariable("DEPLOY_ENV", "staging", VariableVisibility.PRIVATE);

        verify(api).createOrgVariable(eq(ORG), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("DEPLOY_ENV");
            softly.assertThat(payload.getValue().value()).isEqualTo("staging");
            softly.assertThat(payload.getValue().visibility()).isEqualTo(GitHubOrgVariableVisibility.PRIVATE);
            softly.assertThat(variable.name()).isEqualTo("DEPLOY_ENV");
        });
    }

    @Test
    void createVariable_shouldThrowIllegalStateException_whenTheVariableIsGone() throws IOException {
        final GitHubActionsVariablesApi api = github.actions().variables();
        doReturn(Optional.empty()).when(api).getOrgVariable(ORG, "DEPLOY_ENV");

        final Throwable thrown =
                catchThrowable(() -> sut.createVariable("DEPLOY_ENV", "staging", VariableVisibility.ALL));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void createVariable_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createVariable(" ", "staging", VariableVisibility.ALL));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("variableName");
    }

    @Test
    void createVariable_shouldThrowNullPointerException_whenValueIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createVariable("DEPLOY_ENV", null, VariableVisibility.ALL));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void createVariable_shouldThrowNullPointerException_whenVisibilityIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createVariable("DEPLOY_ENV", "staging", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("visibility");
    }

    @Test
    void secrets_shouldStreamTheSecrets_whenTheyExist() throws IOException {
        final GitHubOrganizationActionsSecret secret = mock(GitHubOrganizationActionsSecret.class);
        when(github.actions().secrets().listOrgSecrets(eq(ORG), any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(secret));

        final List<GitHubOrganizationActionsSecret> secrets = sut.secrets().toList();

        assertThat(secrets).containsExactly(secret);
    }

    @Test
    void deleteSecret_shouldDeleteTheSecret_whenCalled() throws IOException {
        final GitHubActionsSecretsApi api = github.actions().secrets();

        sut.deleteSecret("TOKEN");

        verify(api).deleteOrgSecret(ORG, "TOKEN");
    }

    @Test
    void deleteSecret_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.deleteSecret(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("secretName");
    }

    private Organization organization() {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn(ORG);
        return new Organization(sdk, response);
    }
}
