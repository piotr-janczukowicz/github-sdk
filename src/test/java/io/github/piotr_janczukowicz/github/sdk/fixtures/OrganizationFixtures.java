package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrgHook;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsVariable;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationActionsVariableVisibility;
import io.github.piotr_janczukowicz.github.api.teams.GitHubOrganizationInvitation;
import io.github.piotr_janczukowicz.github.api.teams.GitHubTeam;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;
import java.util.List;

public final class OrganizationFixtures {

    public static final String ORG = "acme";
    public static final String SLUG = "core";
    public static final long INVITATION_ID = 51L;
    public static final long HOOK_ID = 61L;

    private static final Instant MOMENT = Instant.parse("2026-11-05T10:00:00Z");

    private OrganizationFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubTeam listedTeam(final String slug) {
        final GitHubTeam listed = mock(GitHubTeam.class);
        lenient().when(listed.slug()).thenReturn(slug);
        return listed;
    }

    public static GitHubOrganizationInvitation invitation() {
        return new GitHubOrganizationInvitation(
                INVITATION_ID,
                "hubot",
                null,
                "direct_member",
                "2026-11-05T10:00:00Z",
                null,
                null,
                mock(GitHubSimpleUser.class),
                0L,
                "OI_kwDOA",
                "https://api.github.com/orgs/acme/invitations/51/teams",
                "member");
    }

    public static GitHubOrganizationInvitation emailInvitation() {
        return new GitHubOrganizationInvitation(
                INVITATION_ID,
                null,
                "new@example.com",
                "admin",
                "2026-11-05T10:00:00Z",
                "2026-11-05T11:00:00Z",
                "bounced",
                mock(GitHubSimpleUser.class),
                0L,
                "OI_kwDOA",
                "https://api.github.com/orgs/acme/invitations/51/teams",
                "member");
    }

    public static GitHubOrgHook hook() {
        final GitHubOrgHook hook = mock(GitHubOrgHook.class);
        lenient().when(hook.id()).thenReturn(HOOK_ID);
        lenient().when(hook.events()).thenReturn(List.of("push"));
        lenient().when(hook.active()).thenReturn(true);
        return hook;
    }

    public static GitHubOrganizationActionsVariable variable() {
        return new GitHubOrganizationActionsVariable(
                "DEPLOY_ENV", "staging", MOMENT, MOMENT, GitHubOrganizationActionsVariableVisibility.ALL, null);
    }
}
