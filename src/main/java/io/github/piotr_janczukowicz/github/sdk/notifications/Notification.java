package io.github.piotr_janczukowicz.github.sdk.notifications;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.activity.GitHubSetThreadSubscriptionPayload;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThread;
import io.github.piotr_janczukowicz.github.api.activity.GitHubThreadSubscription;
import io.github.piotr_janczukowicz.github.sdk.GitHubSdk;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A notification thread of the authenticated user, as GitHub returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover. The client types the
 * reason and the subject type as text, so they are kept as GitHub wrote them, for example {@code mention} and
 * {@code PullRequest}.
 */
public final class Notification {

    private final GitHubSdk sdk;
    private final GitHubThread response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Notification(final GitHubSdk sdk, final GitHubThread response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the thread id.
     *
     * @return the id
     */
    public String id() {
        return response.id();
    }

    /**
     * Returns the title of the subject, for example the title of the issue.
     *
     * @return the title
     */
    public String title() {
        return response.subject().title();
    }

    /**
     * Returns the type of the subject, as GitHub wrote it, for example {@code Issue}, {@code PullRequest} or
     * {@code Release}.
     *
     * @return the subject type
     */
    public String type() {
        return response.subject().type();
    }

    /**
     * Returns why the user got the notification, as GitHub wrote it, for example {@code mention},
     * {@code review_requested} or {@code subscribed}.
     *
     * @return the reason
     */
    public String reason() {
        return response.reason();
    }

    /**
     * Tells whether the notification is still unread.
     *
     * @return {@code true} when it is unread
     */
    public boolean isUnread() {
        return response.unread();
    }

    /**
     * Returns the repository the notification is about. The repository is a handle that is not loaded; call
     * {@link Repository#fetch()} to load it.
     *
     * @return the repository handle
     */
    public Repository repository() {
        return sdk.repository(
                response.repository().owner().login(), response.repository().name());
    }

    /**
     * Loads the notification again ({@code GET /notifications/threads/{thread_id}}). This object is not changed.
     *
     * @return the notification as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the notification is gone
     */
    public Notification refresh() throws IOException {
        return sdk.notification(id()).orElseThrow(() -> new IllegalStateException("Notification " + id() + " is gone"));
    }

    /**
     * Marks the notification as read ({@code PATCH /notifications/threads/{thread_id}}). This object is not changed.
     *
     * @throws IOException if the transport fails
     */
    public void markRead() throws IOException {
        github().activity().notifications().markThreadAsRead(threadId());
    }

    /**
     * Marks the notification as done, which removes it from the inbox ({@code DELETE
     * /notifications/threads/{thread_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void markDone() throws IOException {
        github().activity().notifications().markThreadAsDone(threadId());
    }

    /**
     * Loads the subscription of the user to this thread ({@code GET /notifications/threads/{thread_id}/subscription})
     * as GitHub returns it.
     *
     * @return the subscription, or empty when the user has none or GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubThreadSubscription> subscription() throws IOException {
        return github().activity().notifications().getThreadSubscriptionForAuthenticatedUser(threadId());
    }

    /**
     * Subscribes the user to the thread, so later activity notifies them ({@code PUT
     * /notifications/threads/{thread_id}/subscription}).
     *
     * @return the subscription as GitHub holds it afterwards
     * @throws IOException if the transport fails
     */
    public GitHubThreadSubscription subscribe() throws IOException {
        return putSubscription(false);
    }

    /**
     * Mutes the thread, so later activity does not notify the user ({@code PUT
     * /notifications/threads/{thread_id}/subscription}).
     *
     * @return the subscription as GitHub holds it afterwards
     * @throws IOException if the transport fails
     */
    public GitHubThreadSubscription ignore() throws IOException {
        return putSubscription(true);
    }

    /**
     * Removes the subscription of the user to the thread ({@code DELETE
     * /notifications/threads/{thread_id}/subscription}).
     *
     * @throws IOException if the transport fails
     */
    public void unsubscribe() throws IOException {
        github().activity().notifications().deleteThreadSubscription(threadId());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubThread response() {
        return response;
    }

    private GitHubThreadSubscription putSubscription(final boolean isIgnored) throws IOException {
        return github().activity()
                .notifications()
                .setThreadSubscription(threadId(), new GitHubSetThreadSubscriptionPayload(isIgnored));
    }

    private long threadId() {
        return Long.parseLong(response.id());
    }

    private GitHub github() {
        return Internals.github(sdk);
    }
}
