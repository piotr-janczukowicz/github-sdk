package io.github.piotr_janczukowicz.github.sdk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Loads an object that GitHub just listed, for the lazy streams: the list holds only a summary, so each item is looked
 * up again as the stream reaches it, and a stream cannot throw {@link IOException}.
 */
final class Lookups {

    private Lookups() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Runs a lookup and returns what it found.
     *
     * @param lookup the lookup, which may fail with {@link IOException}
     * @param missing the message for the failure when nothing was found; built only then
     * @param <T> the type of the found object
     * @return the found object
     * @throws IllegalStateException if the lookup found nothing, so the listed item is gone
     * @throws UncheckedIOException if the lookup failed with {@link IOException}
     */
    static <T> T require(final IoLookup<T> lookup, final Supplier<String> missing) {
        try {
            return lookup.find().orElseThrow(() -> new IllegalStateException(missing.get()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A lookup that may fail with {@link IOException}.
     *
     * @param <T> the type of the found object
     */
    @FunctionalInterface
    interface IoLookup<T> {

        /**
         * Looks the object up.
         *
         * @return the object, or empty when GitHub does not have it
         * @throws IOException if the transport fails
         */
        Optional<T> find() throws IOException;
    }
}
