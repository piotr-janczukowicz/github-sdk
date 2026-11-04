package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/** The state of an {@link Issue}. */
public enum IssueState {
    /** The issue is open. */
    OPEN,
    /** The issue is closed. */
    CLOSED,
    /** A value this library version does not know. */
    UNRECOGNIZED;

    /**
     * Maps the state GitHub returned.
     *
     * @param value the wire value, such as {@code open} or {@code closed}
     * @return the state, or {@link #UNRECOGNIZED} when the value is not known
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static IssueState from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "open" -> OPEN;
            case "closed" -> CLOSED;
            default -> UNRECOGNIZED;
        };
    }
}
