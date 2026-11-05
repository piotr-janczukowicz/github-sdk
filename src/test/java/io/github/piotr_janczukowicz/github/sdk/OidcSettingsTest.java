package io.github.piotr_janczukowicz.github.sdk;

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
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsOidcApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsSelfHostedRunnersApi;
import io.github.piotr_janczukowicz.github.api.actions.GitHubOidcCustomPropertyInclusion;
import io.github.piotr_janczukowicz.github.api.actions.GitHubOidcCustomPropertyInclusionInput;
import io.github.piotr_janczukowicz.github.api.actions.GitHubOidcCustomSub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubOidcCustomSubRepo;
import io.github.piotr_janczukowicz.github.api.actions.GitHubRunner;
import io.github.piotr_janczukowicz.github.api.actions.GitHubSetCustomOidcSubClaimForRepoPayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubUpdateOidcCustomSubTemplateForOrgPayload;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.sdk.actions.SelfHostedRunner;
import io.github.piotr_janczukowicz.github.sdk.fixtures.RunnerFixtures;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class OidcSettingsTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);
    private final GitHubActionsOidcApi api = github.actions().oidc();
    private final Repository repository = sdk.repository("octocat", "hello-world");

    @Test
    void oidcSubjectClaim_shouldReturnTheCustomization_whenCalled() throws IOException {
        final GitHubOidcCustomSubRepo customization = mock(GitHubOidcCustomSubRepo.class);
        when(api.getCustomOidcSubClaimForRepo("octocat", "hello-world")).thenReturn(customization);

        final GitHubOidcCustomSubRepo result = repository.oidcSubjectClaim();

        assertThat(result).isSameAs(customization);
    }

    @Test
    void setOidcSubjectClaim_shouldSendTheClaimKeys_whenCalled() throws IOException {
        final ArgumentCaptor<GitHubSetCustomOidcSubClaimForRepoPayload> payload =
                ArgumentCaptor.forClass(GitHubSetCustomOidcSubClaimForRepoPayload.class);

        repository.setOidcSubjectClaim(false, List.of("repo", "context"), true);

        verify(api).setCustomOidcSubClaimForRepo(eq("octocat"), eq("hello-world"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().useDefault()).isFalse();
            softly.assertThat(payload.getValue().includeClaimKeys()).containsExactly("repo", "context");
        });
    }

    @Test
    void setOidcSubjectClaim_shouldSendTheImmutableSubjectFlag_whenItIsTrue() throws IOException {
        final ArgumentCaptor<GitHubSetCustomOidcSubClaimForRepoPayload> payload =
                ArgumentCaptor.forClass(GitHubSetCustomOidcSubClaimForRepoPayload.class);

        repository.setOidcSubjectClaim(true, List.of("repo"), true);

        verify(api).setCustomOidcSubClaimForRepo(eq("octocat"), eq("hello-world"), payload.capture());
        assertThat(payload.getValue().useImmutableSubject()).isTrue();
    }

    @Test
    void setOidcSubjectClaim_shouldThrowNullPointerException_whenClaimKeysAreNull() {
        final Throwable thrown = catchThrowable(() -> repository.setOidcSubjectClaim(true, null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("includeClaimKeys");
    }

    @Test
    void oidcSubjectTemplate_shouldReturnTheTemplate_whenCalled() throws IOException {
        final GitHubOidcCustomSub template = mock(GitHubOidcCustomSub.class);
        when(api.getOidcCustomSubTemplateForOrg("acme")).thenReturn(template);

        final GitHubOidcCustomSub result = organization().oidcSubjectTemplate();

        assertThat(result).isSameAs(template);
    }

    @Test
    void updateOidcSubjectTemplate_shouldSendTheClaimKeys_whenCalled() throws IOException {
        final ArgumentCaptor<GitHubUpdateOidcCustomSubTemplateForOrgPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateOidcCustomSubTemplateForOrgPayload.class);

        organization().updateOidcSubjectTemplate(List.of("repo"), null);

        verify(api).updateOidcCustomSubTemplateForOrg(eq("acme"), payload.capture());
        assertThat(payload.getValue().includeClaimKeys()).containsExactly("repo");
    }

    @Test
    void updateOidcSubjectTemplate_shouldSendTheImmutableSubjectFlag_whenItIsTrue() throws IOException {
        final ArgumentCaptor<GitHubUpdateOidcCustomSubTemplateForOrgPayload> payload =
                ArgumentCaptor.forClass(GitHubUpdateOidcCustomSubTemplateForOrgPayload.class);

        organization().updateOidcSubjectTemplate(List.of("repo"), true);

        verify(api).updateOidcCustomSubTemplateForOrg(eq("acme"), payload.capture());
        assertThat(payload.getValue().useImmutableSubject()).isTrue();
    }

    @Test
    void updateOidcSubjectTemplate_shouldThrowNullPointerException_whenClaimKeysAreNull() {
        final Organization organization = organization();

        final Throwable thrown = catchThrowable(() -> organization.updateOidcSubjectTemplate(null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("includeClaimKeys");
    }

    @Test
    void oidcCustomProperties_shouldReturnTheInclusions_whenTheyExist() throws IOException {
        final GitHubOidcCustomPropertyInclusion inclusion = mock(GitHubOidcCustomPropertyInclusion.class);
        when(api.listOidcCustomPropertyInclusionsForOrg("acme")).thenReturn(List.of(inclusion));

        final List<GitHubOidcCustomPropertyInclusion> result = organization().oidcCustomProperties();

        assertThat(result).containsExactly(inclusion);
    }

    @Test
    void includeOidcCustomProperty_shouldSendTheName_whenCalled() throws IOException {
        final GitHubOidcCustomPropertyInclusion inclusion = mock(GitHubOidcCustomPropertyInclusion.class);
        when(api.createOidcCustomPropertyInclusionForOrg(eq("acme"), any(GitHubOidcCustomPropertyInclusionInput.class)))
                .thenReturn(inclusion);
        final ArgumentCaptor<GitHubOidcCustomPropertyInclusionInput> input =
                ArgumentCaptor.forClass(GitHubOidcCustomPropertyInclusionInput.class);

        final GitHubOidcCustomPropertyInclusion result = organization().includeOidcCustomProperty("team");

        verify(api).createOidcCustomPropertyInclusionForOrg(eq("acme"), input.capture());
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(inclusion);
            softly.assertThat(input.getValue().customPropertyName()).isEqualTo("team");
        });
    }

    @Test
    void includeOidcCustomProperty_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Organization organization = organization();

        final Throwable thrown = catchThrowable(() -> organization.includeOidcCustomProperty(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("propertyName");
    }

    @Test
    void excludeOidcCustomProperty_shouldDeleteTheInclusion_whenCalled() throws IOException {
        organization().excludeOidcCustomProperty("team");

        verify(api).deleteOidcCustomPropertyInclusionForOrg("acme", "team");
    }

    @Test
    void selfHostedRunners_shouldListTheRunnersOfTheOrganization_whenTheyExist() throws IOException {
        final GitHubRunner runner = RunnerFixtures.runner();
        when(api().listSelfHostedRunnersForOrg("acme").stream()).thenReturn(Stream.of(runner));

        final List<SelfHostedRunner> runners =
                organization().selfHostedRunners().all().toList();

        assertThat(runners)
                .singleElement()
                .satisfies(found -> assertThat(found.response()).isSameAs(runner));
    }

    private GitHubActionsSelfHostedRunnersApi api() {
        return github.actions().selfHostedRunners();
    }

    private Organization organization() {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn("acme");
        return new Organization(sdk, response);
    }

    @Test
    void updateOidcSubjectTemplate_shouldThrowIllegalArgumentException_whenAKeyIsBlank() {
        final Organization organization = organization();
        final List<String> claimKeys = List.of("repo", " ");

        final Throwable thrown = catchThrowable(() -> organization.updateOidcSubjectTemplate(claimKeys, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setOidcSubjectClaim_shouldThrowIllegalArgumentException_whenAKeyIsBlank() {
        final List<String> claimKeys = List.of(" ");

        final Throwable thrown = catchThrowable(() -> repository.setOidcSubjectClaim(false, claimKeys, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
