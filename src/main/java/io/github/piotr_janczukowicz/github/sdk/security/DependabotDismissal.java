package io.github.piotr_janczukowicz.github.sdk.security;

import io.github.piotr_janczukowicz.github.api.dependabot.GitHubDependabotAlertDismissedReason;

/** Why a {@link DependabotAlert} is dismissed. */
public enum DependabotDismissal {
    /** A fix has been started. */
    FIX_STARTED,
    /** The alert is not accurate. */
    INACCURATE,
    /** Nobody has the time to fix it. */
    NO_BANDWIDTH,
    /** The vulnerable code is not used. */
    NOT_USED,
    /** The risk is tolerable. */
    TOLERABLE_RISK;

    GitHubDependabotAlertDismissedReason toClient() {
        return switch (this) {
            case FIX_STARTED -> GitHubDependabotAlertDismissedReason.FIX_STARTED;
            case INACCURATE -> GitHubDependabotAlertDismissedReason.INACCURATE;
            case NO_BANDWIDTH -> GitHubDependabotAlertDismissedReason.NO_BANDWIDTH;
            case NOT_USED -> GitHubDependabotAlertDismissedReason.NOT_USED;
            case TOLERABLE_RISK -> GitHubDependabotAlertDismissedReason.TOLERABLE_RISK;
        };
    }
}
