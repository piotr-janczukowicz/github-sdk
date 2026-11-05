package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicense;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicenseSimple;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicensesApi;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GitHubSdkLicensesTest {

    private final GitHub github = mock(GitHub.class, RETURNS_DEEP_STUBS);
    private final GitHubSdk sut = GitHubSdk.of(github);

    @Test
    void licenses_shouldStreamTheCommonlyUsedLicenses_whenTheyExist() throws IOException {
        final GitHubLicenseSimple listed = mock(GitHubLicenseSimple.class);
        when(github.licenses().listCommonlyUsed(any(GitHubPageRequest.class)).stream())
                .thenReturn(Stream.of(listed));

        final List<GitHubLicenseSimple> licenses = sut.licenses().toList();

        assertThat(licenses).containsExactly(listed);
    }

    @Test
    void license_shouldReturnTheLicense_whenGitHubHasIt() throws IOException {
        final GitHubLicense license = mock(GitHubLicense.class);
        final GitHubLicensesApi api = github.licenses();
        doReturn(Optional.of(license)).when(api).get("mit");

        final Optional<GitHubLicense> found = sut.license("mit");

        assertThat(found).containsSame(license);
    }

    @Test
    void license_shouldBeEmpty_whenGitHubDoesNotHaveIt() throws IOException {
        final GitHubLicensesApi api = github.licenses();
        doReturn(Optional.empty()).when(api).get("none");

        final Optional<GitHubLicense> found = sut.license("none");

        assertThat(found).isEmpty();
    }

    @Test
    void license_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.license(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
    }

    @Test
    void utilities_shouldReturnTheUtilitiesWithoutRequest_whenCalled() {
        assertThat(sut.utilities()).isNotNull();
    }
}
