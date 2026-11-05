package io.github.piotr_janczukowicz.github.sdk;

import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaborator;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubCollaboratorBuilder;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitation;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitationBuilder;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryInvitationPermission;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;

final class CollaboratorsFixtures {

    static final long INVITATION_ID = 9009L;

    private static final String USERS = "https://api.github.com/users/hubot";

    private CollaboratorsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitHubCollaborator collaborator() {
        return GitHubCollaboratorBuilder.builder()
                .login("hubot")
                .id(1L)
                .nodeId("U_kgDOA")
                .avatarUrl("https://avatars.githubusercontent.com/u/1")
                .url(USERS)
                .htmlUrl("https://github.com/hubot")
                .followersUrl(USERS + "/followers")
                .followingUrl(USERS + "/following{/other_user}")
                .gistsUrl(USERS + "/gists{/gist_id}")
                .starredUrl(USERS + "/starred{/owner}{/repo}")
                .subscriptionsUrl(USERS + "/subscriptions")
                .organizationsUrl(USERS + "/orgs")
                .reposUrl(USERS + "/repos")
                .eventsUrl(USERS + "/events{/privacy}")
                .receivedEventsUrl(USERS + "/received_events")
                .type("User")
                .siteAdmin(false)
                .roleName("write")
                .build();
    }

    static GitHubRepositoryInvitation invitation() {
        return GitHubRepositoryInvitationBuilder.builder()
                .id(INVITATION_ID)
                .repository(mock(GitHubMinimalRepository.class))
                .invitee(mock(GitHubSimpleUser.class))
                .inviter(mock(GitHubSimpleUser.class))
                .permissions(GitHubRepositoryInvitationPermission.WRITE)
                .createdAt(Instant.parse("2026-02-03T04:05:06Z"))
                .expired(false)
                .url("https://api.github.com/repos/octocat/hello-world/invitations/9009")
                .htmlUrl("https://github.com/octocat/hello-world/invitations")
                .nodeId("RI_kwDOA")
                .build();
    }
}
