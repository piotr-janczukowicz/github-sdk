package io.github.piotr_janczukowicz.github.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubHook;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdateWebhookPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A webhook of a {@link Repository}, as GitHub returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover. GitHub does not return the secret, but the payload
 * URL can embed credentials or tokens, so do not log {@link #payloadUrl()} or {@link #response()}.
 */
public final class Webhook {

    private final Repository repository;
    private final GitHubHook response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Webhook(final Repository repository, final GitHubHook response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the webhook belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
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
     * Tells whether the webhook delivers events.
     *
     * @return {@code true} when active
     */
    public boolean isActive() {
        return response.active();
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
     * Returns the URL GitHub delivers the events to.
     *
     * @return the payload URL, or empty when the configuration has none
     */
    public Optional<String> payloadUrl() {
        return Optional.ofNullable(response.config().url());
    }

    /**
     * Starts a change of several fields at once; nothing is sent until {@link WebhookUpdate#send()}.
     *
     * @return a new update for this webhook
     */
    public WebhookUpdate update() {
        return new WebhookUpdate(this);
    }

    /**
     * Loads the webhook again ({@code GET /repos/{owner}/{repo}/hooks/{hook_id}}). This object is not changed.
     *
     * @return the webhook as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the webhook is gone
     */
    public Webhook refresh() throws IOException {
        return repository.webhook(id()).orElseThrow(() -> new IllegalStateException("Webhook " + id() + " is gone"));
    }

    /**
     * Deletes the webhook ({@code DELETE /repos/{owner}/{repo}/hooks/{hook_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.github(repository)
                .repositories()
                .webhooks()
                .deleteWebhook(repository.owner(), repository.name(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubHook response() {
        return response;
    }

    Webhook apply(final GitHubUpdateWebhookPayload payload) throws IOException {
        return new Webhook(
                repository,
                Internals.github(repository)
                        .repositories()
                        .webhooks()
                        .updateWebhook(repository.owner(), repository.name(), id(), payload));
    }
}
