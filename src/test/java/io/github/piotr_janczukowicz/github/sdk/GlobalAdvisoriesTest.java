package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubGlobalAdvisory;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubListGlobalAdvisoriesFilter;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisory;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GlobalAdvisoriesTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void globalAdvisories_shouldStreamTheAdvisories_whenTheyExist() throws IOException {
        final GitHubGlobalAdvisory advisory = mock(GitHubGlobalAdvisory.class);
        when(github.securityAdvisories().globalAdvisories().listGlobalAdvisories().stream())
                .thenReturn(Stream.of(advisory));

        final List<GitHubGlobalAdvisory> advisories = sdk.globalAdvisories().toList();

        assertThat(advisories).containsExactly(advisory);
    }

    @Test
    void globalAdvisories_shouldStreamTheMatches_whenAFilterIsGiven() throws IOException {
        final GitHubGlobalAdvisory advisory = mock(GitHubGlobalAdvisory.class);
        final GitHubListGlobalAdvisoriesFilter filter = mock(GitHubListGlobalAdvisoriesFilter.class);
        when(github.securityAdvisories().globalAdvisories().listGlobalAdvisories(filter).stream())
                .thenReturn(Stream.of(advisory));

        final List<GitHubGlobalAdvisory> advisories =
                sdk.globalAdvisories(filter).toList();

        assertThat(advisories).containsExactly(advisory);
    }

    @Test
    void globalAdvisories_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sdk.globalAdvisories(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void globalAdvisory_shouldReturnTheAdvisory_whenItExists() throws IOException {
        final GitHubGlobalAdvisory advisory = mock(GitHubGlobalAdvisory.class);
        when(github.securityAdvisories().globalAdvisories().getGlobalAdvisory("GHSA-aaaa-bbbb-cccc"))
                .thenReturn(Optional.of(advisory));

        final Optional<GitHubGlobalAdvisory> found = sdk.globalAdvisory("GHSA-aaaa-bbbb-cccc");

        assertThat(found).containsSame(advisory);
    }

    @Test
    void globalAdvisory_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        when(github.securityAdvisories().globalAdvisories().getGlobalAdvisory("GHSA-none-none-none"))
                .thenReturn(Optional.empty());

        final Optional<GitHubGlobalAdvisory> found = sdk.globalAdvisory("GHSA-none-none-none");

        assertThat(found).isEmpty();
    }

    @Test
    void globalAdvisory_shouldThrowNullPointerException_whenIdIsNull() {
        final Throwable thrown = catchThrowable(() -> sdk.globalAdvisory(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("ghsaId");
    }

    @Test
    void globalAdvisory_shouldThrowIllegalArgumentException_whenIdIsBlank() {
        final Throwable thrown = catchThrowable(() -> sdk.globalAdvisory(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ghsaId");
    }

    @Test
    void advisories_shouldStreamTheOrganizationAdvisories_whenTheyExist() throws IOException {
        final GitHubRepositoryAdvisory advisory = mock(GitHubRepositoryAdvisory.class);
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn("acme");
        when(github.securityAdvisories().repositoryAdvisories().listOrgRepositoryAdvisories("acme").stream())
                .thenReturn(Stream.of(advisory));

        final List<GitHubRepositoryAdvisory> advisories =
                new Organization(sdk, response).advisories().toList();

        assertThat(advisories).containsExactly(advisory);
    }
}
