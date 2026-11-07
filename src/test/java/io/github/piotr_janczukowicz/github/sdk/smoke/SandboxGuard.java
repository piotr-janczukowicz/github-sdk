package io.github.piotr_janczukowicz.github.sdk.smoke;

import java.util.Locale;

final class SandboxGuard {

    private SandboxGuard() {
        throw new UnsupportedOperationException("Utility class");
    }

    static void requireSandbox(final SmokeEnvironment env) {
        if (!env.repo().toLowerCase(Locale.ROOT).contains("smoke")) {
            throw new IllegalStateException(SmokeEnvironment.REPO_VARIABLE
                    + " must name a sandbox repository whose name contains \"smoke\", but was: " + env.owner() + "/"
                    + env.repo());
        }
    }
}
