package io.github.piotr_janczukowicz.github.sdk.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Loads an object that GitHub just listed, for the lazy streams: the list holds only a summary, so each item is looked
 * up again as the stream reaches it, and a stream cannot throw {@link IOException}.
 */
public final class Lookups {

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
    public static <T> T require(final IoLookup<T> lookup, final Supplier<String> missing) {
        try {
            return lookup.find().orElseThrow(() -> new IllegalStateException(missing.get()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Runs a lookup and returns what it found, or empty when GitHub no longer has the object, so a lazy stream can skip
     * an item that was deleted after it was listed.
     *
     * @param lookup the lookup, which may fail with {@link IOException}
     * @param <T> the type of the found object
     * @return the found object, or empty
     * @throws UncheckedIOException if the lookup failed with {@link IOException}
     */
    public static <T> Optional<T> find(final IoLookup<T> lookup) {
        try {
            return lookup.find();
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
    public interface IoLookup<T> {

        /**
         * Looks the object up.
         *
         * @return the object, or empty when GitHub does not have it
         * @throws IOException if the transport fails
         */
        Optional<T> find() throws IOException;
    }
}
