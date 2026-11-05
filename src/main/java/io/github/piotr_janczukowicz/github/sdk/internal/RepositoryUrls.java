package io.github.piotr_janczukowicz.github.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/** Reads the owner and the name of a repository from the API URL that GitHub puts into issues and search hits. */
public final class RepositoryUrls {

    private RepositoryUrls() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads the coordinates of a repository from its API URL, such as
     * {@code https://api.github.com/repos/octocat/hello}. A trailing slash is ignored.
     *
     * @param url the repository URL
     * @return the owner and the name
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if the URL does not end with an owner and a name
     */
    public static Coordinates parse(final String url) {
        requireNonNull(url, "url");
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        final int nameStart = url.lastIndexOf('/', end - 1) + 1;
        if (nameStart == 0) {
            throw new IllegalArgumentException("Unexpected repository URL: " + url);
        }
        final int ownerStart = url.lastIndexOf('/', nameStart - 2) + 1;
        final String owner = url.substring(ownerStart, nameStart - 1);
        if (owner.isEmpty()) {
            throw new IllegalArgumentException("Unexpected repository URL: " + url);
        }
        return new Coordinates(owner, url.substring(nameStart, end));
    }

    /**
     * The owner and the name of a repository.
     *
     * @param owner the owner login
     * @param name the repository name
     */
    public record Coordinates(String owner, String name) {}
}
