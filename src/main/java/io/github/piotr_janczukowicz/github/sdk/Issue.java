package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateCommentPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssue;
import io.github.piotr_janczukowicz.github.api.issues.GitHubIssueUpdateState;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An issue of a {@link Repository}, as GitHub returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #record()} gives the complete response for every field the accessors here do not cover.
 */
public final class Issue {

    private final Repository repository;
    private final GitHubIssue record;

    Issue(final Repository repository, final GitHubIssue record) {
        this.repository = requireNonNull(repository, "repository");
        this.record = requireNonNull(record, "record");
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
        return record.number();
    }

    /**
     * Returns the issue title.
     *
     * @return the title
     */
    public String title() {
        return record.title();
    }

    /**
     * Returns the issue state as GitHub names it, such as {@code open} or {@code closed}.
     *
     * @return the state
     */
    public String state() {
        return record.state();
    }

    /**
     * Returns the user who opened the issue.
     *
     * @return the author, or empty when GitHub does not return one
     */
    public Optional<GitHubSimpleUser> author() {
        return Optional.ofNullable(record.user());
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
        final GitHub github = repository.sdk().github();
        return new IssueComment(
                this,
                github.issues().comments().createComment(repository.owner(), repository.name(), number(), payload));
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
        final GitHub github = repository.sdk().github();
        return github.issues().comments().listComments(repository.owner(), repository.name(), number()).stream()
                .map(record -> new IssueComment(this, record));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitHubIssue record() {
        return record;
    }

    private Issue changeState(final GitHubIssueUpdateState state) throws IOException {
        final GitHub github = repository.sdk().github();
        final GitHubUpdatePayload payload =
                GitHubUpdatePayloadBuilder.builder().state(state).build();
        github.issues().issue().update(repository.owner(), repository.name(), number(), payload);
        return repository
                .issue(number())
                .orElseThrow(() -> new IllegalStateException("Issue " + number() + " is gone after the update"));
    }
}
