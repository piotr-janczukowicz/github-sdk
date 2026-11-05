package io.github.piotr_janczukowicz.github.sdk.accounts;

import io.github.piotr_janczukowicz.github.api.users.GitHubEmailVisibility;

/** Who can see the primary email address of the authenticated account. */
public enum EmailVisibility {
    /** Everyone can see the address on the profile. */
    PUBLIC,
    /** Only the account owner can see the address. */
    PRIVATE;

    GitHubEmailVisibility toClient() {
        return switch (this) {
            case PUBLIC -> GitHubEmailVisibility.PUBLIC;
            case PRIVATE -> GitHubEmailVisibility.PRIVATE;
        };
    }
}
