package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.ratelimit.GitHubRateLimitOverview;
import java.time.Instant;

/**
 * The rate limit of the core REST API, as GitHub reported it at one moment. The object is immutable;
 * {@link #response()} gives the complete overview, including the search and other resources.
 */
public final class RateLimit {

    private final GitHubRateLimitOverview response;

    RateLimit(final GitHubRateLimitOverview response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the number of requests allowed in the current window.
     *
     * @return the limit
     */
    public int limit() {
        return response.resources().core().limit();
    }

    /**
     * Returns the number of requests left in the current window.
     *
     * @return the remaining requests
     */
    public int remaining() {
        return response.resources().core().remaining();
    }

    /**
     * Returns the moment the current window resets.
     *
     * @return the reset time
     */
    public Instant resetAt() {
        return Instant.ofEpochSecond(response.resources().core().reset());
    }

    /**
     * Returns the complete overview this object was built from.
     *
     * @return the response record
     */
    public GitHubRateLimitOverview response() {
        return response;
    }
}
