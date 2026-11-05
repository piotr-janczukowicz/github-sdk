package io.github.piotr_janczukowicz.github.sdk.accounts;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.users.GitHubUpdateAuthenticatedUserPayload;
import io.github.piotr_janczukowicz.github.sdk.User;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to the profile of the authenticated account and sends them in one request. Only the fields that were
 * set are sent, and nothing is sent until {@link #send()}.
 */
public final class MeUpdate {

    private final Me me;
    private @Nullable String name;
    private @Nullable String email;
    private @Nullable String blog;
    private @Nullable String company;
    private @Nullable String location;
    private @Nullable String bio;
    private @Nullable Boolean hireable;
    private final UpdateState state = new UpdateState();

    MeUpdate(final Me me) {
        this.me = me;
    }

    /**
     * Sets the display name.
     *
     * @param name the new name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public MeUpdate name(final String name) {
        this.name = requireNonNull(name, "name");
        return changed();
    }

    /**
     * Sets the public email address.
     *
     * @param email the new address
     * @return this update
     * @throws NullPointerException if {@code email} is {@code null}
     */
    public MeUpdate email(final String email) {
        this.email = requireNonNull(email, "email");
        return changed();
    }

    /**
     * Sets the blog or website URL.
     *
     * @param blog the new URL
     * @return this update
     * @throws NullPointerException if {@code blog} is {@code null}
     */
    public MeUpdate blog(final String blog) {
        this.blog = requireNonNull(blog, "blog");
        return changed();
    }

    /**
     * Sets the company.
     *
     * @param company the new company
     * @return this update
     * @throws NullPointerException if {@code company} is {@code null}
     */
    public MeUpdate company(final String company) {
        this.company = requireNonNull(company, "company");
        return changed();
    }

    /**
     * Sets the location.
     *
     * @param location the new location
     * @return this update
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public MeUpdate location(final String location) {
        this.location = requireNonNull(location, "location");
        return changed();
    }

    /**
     * Sets the biography.
     *
     * @param bio the new biography
     * @return this update
     * @throws NullPointerException if {@code bio} is {@code null}
     */
    public MeUpdate bio(final String bio) {
        this.bio = requireNonNull(bio, "bio");
        return changed();
    }

    /**
     * Sets whether the account is available for hire.
     *
     * @param isHireable {@code true} when available
     * @return this update
     */
    public MeUpdate hireable(final boolean isHireable) {
        this.hireable = isHireable;
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /user}) and returns the profile GitHub answers with.
     *
     * @return the profile as GitHub holds it afterwards
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public User send() throws IOException {
        state.requireChange();
        return me.apply(
                new GitHubUpdateAuthenticatedUserPayload(name, email, blog, null, company, location, hireable, bio));
    }

    private MeUpdate changed() {
        state.mark();
        return this;
    }
}
