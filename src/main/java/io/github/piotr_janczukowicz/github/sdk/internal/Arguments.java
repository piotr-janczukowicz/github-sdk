package io.github.piotr_janczukowicz.github.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonEmpty;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.util.ArrayList;
import java.util.List;

/** Validates the arguments that several object packages take in the same shape. */
public final class Arguments {

    private static final char FIRST_PRINTABLE = ' ';
    private static final char DELETE = '';
    private static final String CURRENT = ".";
    private static final String PARENT = "..";

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
        return requireNonEmpty(requireNotBlankElements(values, name), name);
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

    /**
     * Checks a file path, ref, branch name or sha that becomes part of a request path, and returns it. A value with a
     * control character or with a {@code .} or {@code ..} segment is refused here, because HTTP clients normalize such
     * segments and the request could reach another endpoint.
     *
     * @param value the value
     * @param name the argument name used in the exception message
     * @return the value
     * @throws IllegalArgumentException if the value is blank, has a control character, or has a {@code .} or {@code ..}
     *     segment
     * @throws NullPointerException if the value is {@code null}
     */
    public static String requireSafePath(final String value, final String name) {
        final String checked = requireNotBlank(value, name);
        for (int index = 0; index < checked.length(); index++) {
            final char character = checked.charAt(index);
            if (character < FIRST_PRINTABLE || character == DELETE) {
                throw new IllegalArgumentException(name + " must not contain a control character");
            }
        }
        for (final String segment : checked.split("/", -1)) {
            if (CURRENT.equals(segment) || PARENT.equals(segment)) {
                throw new IllegalArgumentException(name + " must not have a . or .. segment: " + checked);
            }
        }
        return checked;
    }
}
