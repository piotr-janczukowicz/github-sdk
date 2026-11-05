package io.github.piotr_janczukowicz.github.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonEmpty;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.util.ArrayList;
import java.util.List;

/** Validates the arguments that several object packages take in the same shape. */
public final class Arguments {

    private Arguments() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks a varargs array of names or logins and returns it as an immutable list.
     *
     * @param values the values, at least one
     * @param name the argument name used in the exception message
     * @return the values as an immutable list
     * @throws IllegalArgumentException if no value is given or a value is blank
     * @throws NullPointerException if the array or a value is {@code null}
     */
    public static List<String> requireNotBlankValues(final String[] values, final String name) {
        final String[] given = requireNonEmpty(values, name);
        final List<String> checked = new ArrayList<>(given.length);
        for (final String value : given) {
            checked.add(requireNotBlank(value, name));
        }
        return List.copyOf(checked);
    }

    /**
     * Checks a list of names or logins that must not be empty, and returns it as an immutable list.
     *
     * @param values the values, at least one
     * @param name the argument name used in the exception message
     * @return the values as an immutable list
     * @throws IllegalArgumentException if the list is empty or a value is blank
     * @throws NullPointerException if the list or a value is {@code null}
     */
    public static List<String> requireNotBlankValues(final List<String> values, final String name) {
        final List<String> checked = requireNotBlankElements(values, name);
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return checked;
    }

    /**
     * Checks a list of names or logins that may be empty, and returns it as an immutable list.
     *
     * @param values the values
     * @param name the argument name used in the exception message
     * @return the values as an immutable list
     * @throws IllegalArgumentException if a value is blank
     * @throws NullPointerException if the list or a value is {@code null}
     */
    public static List<String> requireNotBlankElements(final List<String> values, final String name) {
        final List<String> checked =
                new ArrayList<>(requireNonNull(values, name).size());
        for (final String value : values) {
            checked.add(requireNotBlank(value, name));
        }
        return List.copyOf(checked);
    }
}
