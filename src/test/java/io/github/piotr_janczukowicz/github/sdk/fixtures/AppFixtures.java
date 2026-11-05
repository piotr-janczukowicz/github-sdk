package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.apps.GitHubIntegration;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubInstallation;
import java.time.Instant;

public final class AppFixtures {

    public static final long APP_ID = 71L;
    public static final long INSTALLATION_ID = 81L;
    public static final String SLUG = "octo-app";

    private AppFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubIntegration integration() {
        final GitHubIntegration app = mock(GitHubIntegration.class);
        lenient().when(app.id()).thenReturn(APP_ID);
        lenient().when(app.slug()).thenReturn(SLUG);
        lenient().when(app.name()).thenReturn("Octo App");
        return app;
    }

    public static GitHubInstallation installation() {
        return installationSuspendedAt(null);
    }

    public static GitHubInstallation installationSuspendedAt(final Instant suspendedAt) {
        final GitHubInstallation installation = mock(GitHubInstallation.class);
        lenient().when(installation.id()).thenReturn(INSTALLATION_ID);
        lenient().when(installation.appSlug()).thenReturn(SLUG);
        lenient().when(installation.targetId()).thenReturn(5L);
        lenient().when(installation.targetType()).thenReturn("Organization");
        lenient().when(installation.suspendedAt()).thenReturn(suspendedAt);
        return installation;
    }
}
