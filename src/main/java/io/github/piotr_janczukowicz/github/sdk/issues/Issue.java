package io.github.piotr_janczukowicz.github.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddAssigneesAssignee;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddAssigneesPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddLabelsLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubAddLabelsPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueEvent;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueUpdateState;
import io.github.piotr_janczukowicz.github.api.issues.GitHubRemoveAssigneesPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubSetLabelsLabel;
import io.github.piotr_janczukowicz.github.api.issues.GitHubSetLabelsPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubTimelineEvent;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubCreateForCommitCommentPayload;
import io.github.piotr_janczukowicz.github.api.reactions.GitHubReaction;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.reactions.Reaction;
import io.github.piotr_janczukowicz.github.sdk.reactions.ReactionContent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An issue of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Issue {

    private final Repository repository;
    private final GitHubIssue response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param repository the repository the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Issue(final Repository repository, final GitHubIssue response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the repository the issue belongs to.
     *
     * @return the repository
     */
    public Repository repository() {
        return repository;
    }

    /**
     * Returns the issue number within its repository.
     *
     * @return the number
     */
    public int number() {
        return response.number();
    }

    /**
     * Returns the issue title.
     *
     * @return the title
     */
    public String title() {
        return response.title();
    }

    /**
     * Returns the issue state.
     *
     * @return the state
     * @throws IllegalArgumentException if GitHub returns a state other than {@code open} or {@code closed}
     */
    public IssueState state() {
        return IssueState.from(response.state());
    }

    /**
     * Returns the labels of the issue as GitHub returned them.
     *
     * @return the labels, empty when there are none
     */
    public List<GitHubIssueLabel> labels() {
        return response.labels();
    }

    /**
     * Returns the milestone the issue belongs to.
     *
     * @return the milestone, or empty when the issue has none
     */
    public Optional<Milestone> milestone() {
        return Optional.ofNullable(response.milestone()).map(milestone -> new Milestone(repository, milestone));
    }

    /**
     * Returns the users the issue is assigned to.
     *
     * @return the assignees, empty when there are none
     */
    public List<GitHubSimpleUser> assignees() {
        return Optional.ofNullable(response.assignees()).orElse(List.of());
    }

    /**
     * Adds labels ({@code POST /repos/{owner}/{repo}/issues/{issue_number}/labels}) and loads the issue again. This
     * object is not changed.
     *
     * @param names the label names
     * @return the issue as GitHub holds it after the change
     * @throws IllegalArgumentException if no name is given or a name is blank
     * @throws NullPointerException if a name is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue addLabels(final String... names) throws IOException {
        final List<GitHubAddLabelsLabel> labels = Arguments.requireNotBlankValues(names, "names").stream()
                .map(GitHubAddLabelsLabel::ofName)
                .toList();
        github().issues()
                .labels()
                .addLabels(repository.owner(), repository.name(), number(), new GitHubAddLabelsPayload(labels));
        return refresh();
    }

    /**
     * Replaces the labels ({@code PUT /repos/{owner}/{repo}/issues/{issue_number}/labels}) and loads the issue again.
     * This object is not changed.
     *
     * @param names the label names; an empty list removes all labels
     * @return the issue as GitHub holds it after the change
     * @throws NullPointerException if {@code names} or a name is {@code null}
     * @throws IllegalArgumentException if a name is blank
     * @throws IOException if the transport fails
     */
    public Issue setLabels(final List<String> names) throws IOException {
        final List<GitHubSetLabelsLabel> labels = Arguments.requireNotBlankElements(names, "names").stream()
                .map(GitHubSetLabelsLabel::ofName)
                .toList();
        github().issues()
                .labels()
                .setLabels(repository.owner(), repository.name(), number(), new GitHubSetLabelsPayload(labels));
        return refresh();
    }

    /**
     * Removes a label ({@code DELETE /repos/{owner}/{repo}/issues/{issue_number}/labels/{name}}) and loads the issue
     * again. This object is not changed.
     *
     * @param name the label name
     * @return the issue as GitHub holds it after the change
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Issue removeLabel(final String name) throws IOException {
        github().issues()
                .labels()
                .removeLabel(repository.owner(), repository.name(), number(), requireNotBlank(name, "name"));
        return refresh();
    }

    /**
     * Assigns users ({@code POST /repos/{owner}/{repo}/issues/{issue_number}/assignees}). This object is not changed.
     *
     * @param logins the user logins
     * @return the issue as GitHub returned it after the change
     * @throws IllegalArgumentException if no login is given or a login is blank
     * @throws NullPointerException if a login is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue assign(final String... logins) throws IOException {
        final List<GitHubAddAssigneesAssignee> assignees = Arguments.requireNotBlankValues(logins, "logins").stream()
                .map(GitHubAddAssigneesAssignee::ofLogin)
                .toList();
        return new Issue(
                repository,
                github().issues()
                        .assignees()
                        .addAssignees(
                                repository.owner(),
                                repository.name(),
                                number(),
                                new GitHubAddAssigneesPayload(assignees)));
    }

    /**
     * Removes assigned users ({@code DELETE /repos/{owner}/{repo}/issues/{issue_number}/assignees}). This object is not
     * changed.
     *
     * @param logins the user logins
     * @return the issue as GitHub returned it after the change
     * @throws IllegalArgumentException if no login is given or a login is blank
     * @throws NullPointerException if a login is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue unassign(final String... logins) throws IOException {
        final List<String> removed = Arguments.requireNotBlankValues(logins, "logins");
        return new Issue(
                repository,
                github().issues()
                        .assignees()
                        .removeAssignees(
                                repository.owner(),
                                repository.name(),
                                number(),
                                new GitHubRemoveAssigneesPayload(removed)));
    }

    /**
     * Locks the conversation ({@code PUT /repos/{owner}/{repo}/issues/{issue_number}/lock}) and loads the issue again.
     * This object is not changed.
     *
     * @return the issue as GitHub holds it after the change
     * @throws IOException if the transport fails
     */
    public Issue lock() throws IOException {
        github().issues().issue().lock(repository.owner(), repository.name(), number());
        return refresh();
    }

    /**
     * Unlocks the conversation ({@code DELETE /repos/{owner}/{repo}/issues/{issue_number}/lock}) and loads the issue
     * again. This object is not changed.
     *
     * @return the issue as GitHub holds it after the change
     * @throws IOException if the transport fails
     */
    public Issue unlock() throws IOException {
        github().issues().issue().unlock(repository.owner(), repository.name(), number());
        return refresh();
    }

    /**
     * Returns the user who opened the issue.
     *
     * @return the author, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(response.user());
    }

    /**
     * Closes the issue ({@code PATCH /repos/{owner}/{repo}/issues/{issue_number}}) and loads it again. This object is
     * not changed.
     *
     * @return the closed issue
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the issue is gone after the update
     */
    public Issue close() throws IOException {
        return changeState(GitHubIssueUpdateState.CLOSED);
    }

    /**
     * Reopens the issue ({@code PATCH /repos/{owner}/{repo}/issues/{issue_number}}) and loads it again. This object is
     * not changed.
     *
     * @return the reopened issue
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the issue is gone after the update
     */
    public Issue reopen() throws IOException {
        return changeState(GitHubIssueUpdateState.OPEN);
    }

    /**
     * Adds a comment ({@code POST /repos/{owner}/{repo}/issues/{issue_number}/comments}).
     *
     * @param body the comment text in Markdown
     * @return the new comment as GitHub returned it
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IOException if the transport fails
     */
    public IssueComment comment(final String body) throws IOException {
        final GitHubCreateCommentPayload payload = new GitHubCreateCommentPayload(requireNonNull(body, "body"));
        return new IssueComment(
                this,
                github().issues().comments().createComment(repository.owner(), repository.name(), number(), payload));
    }

    /**
     * Lists the comments of the issue ({@code GET /repos/{owner}/{repo}/issues/{issue_number}/comments}). Pages are
     * fetched only as the stream is consumed, so {@code limit} stops further requests. Transport failures surface as
     * {@link java.io.UncheckedIOException}.
     *
     * @return the comments, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<IssueComment> comments() throws IOException {
        return github().issues().comments().listComments(repository.owner(), repository.name(), number()).stream()
                .map(response -> new IssueComment(this, response));
    }

    /**
     * Starts a change of several fields at once; nothing is sent until {@link IssueUpdate#send()}.
     *
     * @return a new update for this issue
     */
    public IssueUpdate update() {
        return new IssueUpdate(this);
    }

    /**
     * Loads the issue again ({@code GET /repos/{owner}/{repo}/issues/{issue_number}}). This object is not changed.
     *
     * @return the issue as GitHub holds it now
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the issue is gone
     */
    public Issue refresh() throws IOException {
        return repository
                .issue(number())
                .orElseThrow(() -> new IllegalStateException("Issue " + number() + " is gone"));
    }

    /**
     * Reacts to the issue ({@code POST /repos/{owner}/{repo}/issues/{issue_number}/reactions}). If the current user
     * already reacted with the same emoji, GitHub returns that reaction. This object is not changed.
     *
     * @param content the emoji
     * @return the reaction
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IOException if the transport fails
     */
    public Reaction react(final ReactionContent content) throws IOException {
        final GitHubCreateForCommitCommentPayload payload = new GitHubCreateForCommitCommentPayload(
                ClientValues.reactionToClient(requireNonNull(content, "content")));
        return reaction(github().reactions().createForIssue(repository.owner(), repository.name(), number(), payload));
    }

    /**
     * Lists the reactions to the issue ({@code GET /repos/{owner}/{repo}/issues/{issue_number}/reactions}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        return github().reactions().listForIssue(repository.owner(), repository.name(), number()).stream()
                .map(this::reaction);
    }

    /**
     * Lists the events of the issue, such as labeled, assigned or closed ({@code GET
     * /repos/{owner}/{repo}/issues/{issue_number}/events}). The client's records are passed through. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the events, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubIssueEvent> events() throws IOException {
        return github().issues().events().listEvents(repository.owner(), repository.name(), number()).stream();
    }

    /**
     * Lists the timeline of the issue: its events plus comments, commits and cross-references ({@code GET
     * /repos/{owner}/{repo}/issues/{issue_number}/timeline}). The client's records are passed through. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the timeline entries, oldest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubTimelineEvent> timeline() throws IOException {
        return github()
                .issues()
                .timeline()
                .listEventsForTimeline(repository.owner(), repository.name(), number())
                .stream();
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubIssue response() {
        return response;
    }

    Issue apply(final GitHubUpdatePayload payload) throws IOException {
        github().issues().issue().update(repository.owner(), repository.name(), number(), payload);
        return refresh();
    }

    private Reaction reaction(final GitHubReaction listed) {
        return new Reaction(
                listed,
                reactionId -> github().reactions()
                        .deleteForIssue(repository.owner(), repository.name(), number(), reactionId));
    }

    private Issue changeState(final GitHubIssueUpdateState state) throws IOException {
        return apply(GitHubUpdatePayloadBuilder.builder().state(state).build());
    }

    private GitHub github() {
        return Internals.github(repository);
    }
}
