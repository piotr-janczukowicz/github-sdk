package io.github.piotr_janczukowicz.github.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdateWebhookPayload;
import io.github.piotr_janczukowicz.github.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link Webhook} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The delivery configuration (payload URL, content type, secret) is not part of
 * it, because GitHub replaces the whole configuration when any of it is sent; change that through the client.
 */
public final class WebhookUpdate {

    private static final String EVENTS = "events";

    private final Webhook webhook;
    private @Nullable List<String> events;
    private @Nullable List<String> addEvents;
    private @Nullable List<String> removeEvents;
    private @Nullable Boolean active;
    private final UpdateState state = new UpdateState();

    public WebhookUpdate(final Webhook webhook) {
        this.webhook = webhook;
    }

    /**
     * Replaces the events the webhook is subscribed to.
     *
     * @param events the event names
     * @return this update
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public WebhookUpdate events(final List<String> events) {
        this.events = List.copyOf(requireNonNull(events, EVENTS));
        return changed();
    }

    /**
     * Adds events to the subscription.
     *
     * @param events the event names
     * @return this update
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public WebhookUpdate addEvents(final List<String> events) {
        this.addEvents = List.copyOf(requireNonNull(events, EVENTS));
        return changed();
    }

    /**
     * Removes events from the subscription.
     *
     * @param events the event names
     * @return this update
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public WebhookUpdate removeEvents(final List<String> events) {
        this.removeEvents = List.copyOf(requireNonNull(events, EVENTS));
        return changed();
    }

    /**
     * Sets whether the webhook delivers events.
     *
     * @param active {@code true} to deliver events
     * @return this update
     */
    public WebhookUpdate active(final boolean active) {
        this.active = active;
        return changed();
    }

    /**
     * Sends the changes ({@code PATCH /repos/{owner}/{repo}/hooks/{hook_id}}). The webhook this update was made from is
     * not changed.
     *
     * @return the webhook as GitHub returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Webhook send() throws IOException {
        state.requireChange();
        return webhook.apply(new GitHubUpdateWebhookPayload(null, events, addEvents, removeEvents, active));
    }

    private WebhookUpdate changed() {
        state.mark();
        return this;
    }
}
