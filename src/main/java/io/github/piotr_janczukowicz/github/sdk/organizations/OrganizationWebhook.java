package io.github.piotr_janczukowicz.github.sdk.organizations;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubOrgHook;
import io.github.piotr_janczukowicz.github.api.organizations.GitHubUpdateOrgWebhookPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubHookDeliveryItem;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

/**
 * A webhook of an organization, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response, including the delivery configuration, for every field the accessors here do not cover.
 */
public final class OrganizationWebhook {

    private final GitHubSdk sdk;
    private final String organization;
    private final GitHubOrgHook response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param organization the login of the organization the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a text argument is blank
     */
    public OrganizationWebhook(final GitHubSdk sdk, final String organization, final GitHubOrgHook response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.organization = requireNotBlank(organization, "organization");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the login of the organization the webhook belongs to.
     *
     * @return the organization login
     */
    public String organization() {
        return organization;
    }

    /**
     * Returns the webhook id.
     *
     * @return the id
     */
    public long id() {
        return response.id();
    }

    /**
     * Returns the events the webhook is subscribed to.
     *
     * @return the event names
     */
    public List<String> events() {
        return response.events();
    }

    /**
     * Tells whether the webhook delivers events.
     *
     * @return {@code true} when it is active
     */
    public boolean isActive() {
        return response.active();
    }

    /**
     * Loads the webhook again ({@code GET /orgs/{org}/hooks/{hook_id}}). This object is not changed.
     *
     * @return the webhook as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the webhook is gone
     */
    public OrganizationWebhook refresh() throws IOException {
        return github().organizations()
                .webhooks()
                .getWebhook(organization, id())
                .map(reloaded -> new OrganizationWebhook(sdk, organization, reloaded))
                .orElseThrow(() -> new IllegalStateException("Organization webhook " + id() + " is gone"));
    }

    /**
     * Changes the webhook ({@code PATCH /orgs/{org}/hooks/{hook_id}}). This object is not changed.
     *
     * @param payload the changes, as the client defines them
     * @return the webhook as GitHub holds it afterwards
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public OrganizationWebhook update(final GitHubUpdateOrgWebhookPayload payload) throws IOException {
        return new OrganizationWebhook(
                sdk,
                organization,
                github().organizations()
                        .webhooks()
                        .updateWebhook(organization, id(), requireNonNull(payload, "payload")));
    }

    /**
     * Asks GitHub to send a ping event to the webhook ({@code POST /orgs/{org}/hooks/{hook_id}/pings}).
     *
     * @throws IOException if the transport fails
     */
    public void ping() throws IOException {
        github().organizations().webhooks().pingWebhook(organization, id());
    }

    /**
     * Lists the recent deliveries ({@code GET /orgs/{org}/hooks/{hook_id}/deliveries}) as GitHub returns them. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the deliveries, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubHookDeliveryItem> deliveries() throws IOException {
        return github().organizations().webhooks().listWebhookDeliveries(organization, id()).stream();
    }

    /**
     * Deletes the webhook ({@code DELETE /orgs/{org}/hooks/{hook_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        github().organizations().webhooks().deleteWebhook(organization, id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubOrgHook response() {
        return response;
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
