package io.github.piotr_janczukowicz.github.sdk.security;

import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlertResolution;

/** How a {@link SecretScanningAlert} is resolved. */
public enum SecretResolution {
    /** The detected string is not a secret. */
    FALSE_POSITIVE,
    /** The secret will not be revoked. */
    WONT_FIX,
    /** The secret was revoked. */
    REVOKED,
    /** The secret is only used in tests. */
    USED_IN_TESTS;

    GitHubSecretScanningAlertResolution toClient() {
        return switch (this) {
            case FALSE_POSITIVE -> GitHubSecretScanningAlertResolution.FALSE_POSITIVE;
            case WONT_FIX -> GitHubSecretScanningAlertResolution.WONT_FIX;
            case REVOKED -> GitHubSecretScanningAlertResolution.REVOKED;
            case USED_IN_TESTS -> GitHubSecretScanningAlertResolution.USED_IN_TESTS;
        };
    }
}
