package io.github.piotr_janczukowicz.github.sdk.internal;

import java.io.IOException;

/** An action on a {@code long} that may fail with {@link IOException}. */
@FunctionalInterface
public interface IoLongConsumer {

    /**
     * Performs the action.
     *
     * @param value the value to act on
     * @throws IOException if the transport fails
     */
    void accept(long value) throws IOException;
}
