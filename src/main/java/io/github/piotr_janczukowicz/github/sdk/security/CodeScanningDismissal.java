package io.github.piotr_janczukowicz.github.sdk.security;

import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertDismissedReason;

/** Why a {@link CodeScanningAlert} is dismissed. */
public enum CodeScanningDismissal {
    /** The alert is a false positive. */
    FALSE_POSITIVE,
    /** The problem will not be fixed. */
    WONT_FIX,
    /** The code is only used in tests. */
    USED_IN_TESTS,
    /** The problem is mitigated elsewhere. */
    MITIGATED;

    GitHubCodeScanningAlertDismissedReason toClient() {
        return switch (this) {
            case FALSE_POSITIVE -> GitHubCodeScanningAlertDismissedReason.FALSE_POSITIVE;
            case WONT_FIX -> GitHubCodeScanningAlertDismissedReason.WONT_FIX;
            case USED_IN_TESTS -> GitHubCodeScanningAlertDismissedReason.USED_IN_TESTS;
            case MITIGATED -> GitHubCodeScanningAlertDismissedReason.MITIGATED;
        };
    }
}
