package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeployment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentBuilder;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentPayloadValue;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatus;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatusBuilder;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubDeploymentStatusState;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubEnvironment;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubEnvironmentBuilder;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPageBuild;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesBuildType;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSite;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSiteBuilder;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSiteStatus;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;
import java.util.List;

public final class DeploymentFixtures {

    public static final long DEPLOYMENT_ID = 7007L;
    public static final String SHA = "a".repeat(40);

    private static final String API = "https://api.github.com/repos/octocat/hello-world";
    private static final Instant MOMENT = Instant.parse("2026-11-05T10:00:00Z");

    private DeploymentFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubDeployment deployment() {
        return GitHubDeploymentBuilder.builder()
                .url(API + "/deployments/7007")
                .id(DEPLOYMENT_ID)
                .nodeId("DE_kwDOA")
                .sha(SHA)
                .ref("main")
                .task("deploy")
                .payload(GitHubDeploymentPayloadValue.ofText(""))
                .environment("staging")
                .originalEnvironment("staging")
                .description("Release")
                .creator(creator())
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .statusesUrl(API + "/deployments/7007/statuses")
                .repositoryUrl(API)
                .productionEnvironment(true)
                .build();
    }

    public static GitHubDeployment bareDeployment() {
        return GitHubDeploymentBuilder.builder(deployment())
                .description(null)
                .creator(null)
                .productionEnvironment(null)
                .build();
    }

    public static GitHubDeploymentStatus status() {
        return GitHubDeploymentStatusBuilder.builder()
                .url(API + "/deployments/7007/statuses/1")
                .id(1L)
                .nodeId("DES_kwDOA")
                .state(GitHubDeploymentStatusState.SUCCESS)
                .creator(creator())
                .description("Done")
                .environment("staging")
                .targetUrl("https://example.com")
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .deploymentUrl(API + "/deployments/7007")
                .repositoryUrl(API)
                .build();
    }

    public static GitHubEnvironment environment() {
        return GitHubEnvironmentBuilder.builder()
                .id(8008L)
                .nodeId("EN_kwDOA")
                .name("staging")
                .url(API + "/environments/staging")
                .htmlUrl("https://github.com/octocat/hello-world/deployments/staging")
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .protectionRules(List.of())
                .build();
    }

    public static GitHubEnvironment bareEnvironment() {
        return GitHubEnvironmentBuilder.builder(environment())
                .protectionRules(null)
                .build();
    }

    public static GitHubPagesSite pagesSite() {
        return GitHubPagesSiteBuilder.builder()
                .url("https://api.github.com/repos/octocat/hello-world/pages")
                .status(GitHubPagesSiteStatus.BUILT)
                .htmlUrl("https://octocat.github.io/hello-world/")
                .cname("example.com")
                .buildType(GitHubPagesBuildType.LEGACY)
                .custom404(false)
                .sitePublic(true)
                .httpsEnforced(true)
                .build();
    }

    public static GitHubPagesSite barePagesSite() {
        return GitHubPagesSiteBuilder.builder(pagesSite())
                .cname(null)
                .httpsEnforced(null)
                .build();
    }

    public static GitHubPageBuild pageBuild() {
        return mock(GitHubPageBuild.class);
    }

    private static GitHubSimpleUser creator() {
        final GitHubSimpleUser user = mock(GitHubSimpleUser.class);
        lenient().when(user.login()).thenReturn("octocat");
        return user;
    }
}
