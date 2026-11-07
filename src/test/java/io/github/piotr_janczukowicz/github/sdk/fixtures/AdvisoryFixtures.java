package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisory;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisorySeverity;
import io.github.piotr_janczukowicz.github.api.securityadvisories.GitHubRepositoryAdvisoryState;

public final class AdvisoryFixtures {

    public static final String GHSA = "GHSA-aaaa-bbbb-cccc";

    private AdvisoryFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubRepositoryAdvisory advisory() {
        final GitHubRepositoryAdvisory advisory = mock(GitHubRepositoryAdvisory.class);
        lenient().when(advisory.ghsaId()).thenReturn(GHSA);
        lenient().when(advisory.summary()).thenReturn("Injection in parser");
        lenient().when(advisory.severity()).thenReturn(GitHubRepositoryAdvisorySeverity.HIGH);
        lenient().when(advisory.state()).thenReturn(GitHubRepositoryAdvisoryState.DRAFT);
        return advisory;
    }
}
