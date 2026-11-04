package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/** The state of an {@link Issue}: GitHub knows exactly these two. */
public enum IssueState {
    /** The issue is open. */
    OPEN,
    /** The issue is closed. */
    CLOSED;

    /**
     * Maps the state GitHub returned.
     *
     * @param value the wire value, {@code open} or {@code closed}
     * @return the state
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is neither {@code open} nor {@code closed}
     */
    public static IssueState from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "open" -> OPEN;
            case "closed" -> CLOSED;
            default -> throw new IllegalArgumentException("Unknown issue state: " + value);
        };
    }
}
