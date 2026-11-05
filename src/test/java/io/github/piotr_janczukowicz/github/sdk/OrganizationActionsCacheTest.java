package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheUsageByRepository;
import io.github.piotr_janczukowicz.github.api.actions.GitHubActionsCacheUsageOrgEnterprise;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrganizationFull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class OrganizationActionsCacheTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sdk = GitHubSdk.of(github);

    @Test
    void actionsCacheUsage_shouldReturnTheUsage_whenCalled() throws IOException {
        final GitHubActionsCacheUsageOrgEnterprise usage = mock(GitHubActionsCacheUsageOrgEnterprise.class);
        when(github.actions().cache().getOrgCacheUsage("acme")).thenReturn(usage);

        final GitHubActionsCacheUsageOrgEnterprise result = organization().actionsCacheUsage();

        assertThat(result).isSameAs(usage);
    }

    @Test
    void actionsCacheUsageByRepository_shouldStreamTheUsage_whenItExists() throws IOException {
        final GitHubActionsCacheUsageByRepository usage = mock(GitHubActionsCacheUsageByRepository.class);
        when(github.actions().cache().listOrgCacheUsageByRepository("acme").stream())
                .thenReturn(Stream.of(usage));

        final List<GitHubActionsCacheUsageByRepository> result =
                organization().actionsCacheUsageByRepository().toList();

        assertThat(result).containsExactly(usage);
    }

    private Organization organization() {
        final GitHubOrganizationFull response = mock(GitHubOrganizationFull.class);
        lenient().when(response.login()).thenReturn("acme");
        return new Organization(sdk, response);
    }
}
