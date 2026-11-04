package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.branches.GitHubListBranchesFilter;
import io.github.piotr_janczukowicz.github.api.branches.GitHubShortBranch;
import io.github.piotr_janczukowicz.github.api.commits.GitHubListCommitsFilter;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubListPullRequestsFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateOrUpdateFileContentsPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubGetContentFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPullRequestSimple;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A repository, identified by its owner and name. A handle from {@link GitHubSdk#repository(String, String)} holds no
 * data from GitHub; {@link #fetch()} returns a loaded repository. It is the starting point for the operations that
 * belong to a repository.
 */
public final class Repository {

    private final GitHubSdk sdk;
    private final String owner;
    private final String name;
    private final @Nullable GitHubFullRepository record;

    Repository(
            final GitHubSdk sdk, final String owner, final String name, final @Nullable GitHubFullRepository record) {
        this.sdk = sdk;
        this.owner = owner;
        this.name = name;
        this.record = record;
    }

    /**
     * Returns the account that owns the repository.
     *
     * @return the owner login
     */
    public String owner() {
        return owner;
    }

    /**
     * Returns the repository name.
     *
     * @return the name without the owner
     */
    public String name() {
        return name;
    }

    /**
     * Loads the repository ({@code GET /repos/{owner}/{repo}}). This object is not changed.
     *
     * @return the loaded repository, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Repository> fetch() throws IOException {
        return sdk.github()
                .repositories()
                .repository()
                .get(owner, name)
                .map(loaded -> new Repository(sdk, owner, name, loaded));
    }

    /**
     * Returns the complete response of a loaded repository.
     *
     * @return the response record
     * @throws IllegalStateException if this is a handle that was not loaded with {@link #fetch()}
     */
    public GitHubFullRepository record() {
        if (record == null) {
            throw new IllegalStateException("Repository " + owner + "/" + name + " is not loaded; call fetch()");
        }
        return record;
    }

    /**
     * Creates an issue with a title ({@code POST /repos/{owner}/{repo}/issues}).
     *
     * @param title the issue title
     * @return the new issue as GitHub returned it
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final String title) throws IOException {
        return createIssue(GitHubCreatePayloadBuilder.builder()
                .title(requireNonNull(title, "title"))
                .build());
    }

    /**
     * Creates an issue ({@code POST /repos/{owner}/{repo}/issues}).
     *
     * @param payload the issue to create
     * @return the new issue as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final GitHubCreatePayload payload) throws IOException {
        return new Issue(this, sdk.github().issues().issue().create(owner, name, requireNonNull(payload, "payload")));
    }

    /**
     * Loads one issue of this repository ({@code GET /repos/{owner}/{repo}/issues/{issue_number}}).
     *
     * @param number the issue number
     * @return the issue, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Issue> issue(final int number) throws IOException {
        return sdk.github().issues().issue().get(owner, name, number).map(record -> new Issue(this, record));
    }

    /**
     * Loads one pull request of this repository ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}}).
     *
     * @param number the pull request number
     * @return the pull request, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PullRequest> pullRequest(final int number) throws IOException {
        return sdk.github()
                .pullRequests()
                .pullRequest()
                .get(owner, name, number)
                .map(loaded -> new PullRequest(this, loaded));
    }

    /**
     * Lists the open pull requests ({@code GET /repos/{owner}/{repo}/pulls}), the way GitHub lists them by default. See
     * {@link #pullRequests(GitHubListPullRequestsFilter)}.
     *
     * @return the pull requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequest> pullRequests() throws IOException {
        return pullRequests(GitHubListPullRequestsFilter.empty());
    }

    /**
     * Lists pull requests ({@code GET /repos/{owner}/{repo}/pulls}). GitHub's list holds only a summary of each pull
     * request, so every listed pull request is loaded with one more request as the stream reaches it; a {@code limit}
     * on the stream bounds the cost. A transport failure during iteration is an {@link java.io.UncheckedIOException},
     * and a listed pull request that is gone before it is loaded is an {@link IllegalStateException}.
     *
     * @param filter the list filter
     * @return the pull requests
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequest> pullRequests(final GitHubListPullRequestsFilter filter) throws IOException {
        return sdk
                .github()
                .pullRequests()
                .pullRequest()
                .list(owner, name, requireNonNull(filter, "filter"), GitHubPageRequest.byPage())
                .stream()
                .map(this::loadPullRequest);
    }

    /**
     * Creates a pull request ({@code POST /repos/{owner}/{repo}/pulls}).
     *
     * @param payload the pull request to create
     * @return the new pull request as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequest createPullRequest(final GitHubCreatePullRequestPayload payload) throws IOException {
        return new PullRequest(
                this,
                sdk.github().pullRequests().pullRequest().create(owner, name, requireNonNull(payload, "payload")));
    }

    /**
     * Loads one branch ({@code GET /repos/{owner}/{repo}/branches/{branch}}).
     *
     * @param name the branch name
     * @return the branch, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Branch> branch(final String name) throws IOException {
        return sdk.github()
                .branches()
                .branch()
                .getBranch(owner, this.name, requireNotBlank(name, "name"))
                .map(loaded -> new Branch(this, loaded));
    }

    /**
     * Lists the branches ({@code GET /repos/{owner}/{repo}/branches}). See {@link #branches(GitHubListBranchesFilter)}.
     *
     * @return the branches
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Branch> branches() throws IOException {
        return branches(GitHubListBranchesFilter.empty());
    }

    /**
     * Lists branches ({@code GET /repos/{owner}/{repo}/branches}). GitHub's list holds only a summary of each branch,
     * so every listed branch is loaded with one more request as the stream reaches it; a {@code limit} on the stream
     * bounds the cost. A transport failure during iteration is an {@link java.io.UncheckedIOException}, and a listed
     * branch that is gone before it is loaded is an {@link IllegalStateException}.
     *
     * @param filter the list filter
     * @return the branches
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Branch> branches(final GitHubListBranchesFilter filter) throws IOException {
        return sdk
                .github()
                .branches()
                .branch()
                .listBranches(owner, name, requireNonNull(filter, "filter"), GitHubPageRequest.byPage())
                .stream()
                .map(this::loadBranch);
    }

    /**
     * Loads one commit ({@code GET /repos/{owner}/{repo}/commits/{ref}}).
     *
     * @param sha the commit hash, a branch name or a tag name
     * @return the commit, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code sha} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Commit> commit(final String sha) throws IOException {
        return sdk.github()
                .commits()
                .commit()
                .getCommit(owner, name, requireNotBlank(sha, "sha"))
                .map(loaded -> new Commit(this, loaded));
    }

    /**
     * Lists the commits of the default branch ({@code GET /repos/{owner}/{repo}/commits}). See
     * {@link #commits(GitHubListCommitsFilter)}.
     *
     * @return the commits, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits() throws IOException {
        return commits(GitHubListCommitsFilter.empty());
    }

    /**
     * Lists commits ({@code GET /repos/{owner}/{repo}/commits}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the list filter
     * @return the commits, newest first
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits(final GitHubListCommitsFilter filter) throws IOException {
        return sdk
                .github()
                .commits()
                .commit()
                .listCommits(owner, name, requireNonNull(filter, "filter"), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Commit(this, listed));
    }

    /**
     * Loads one file on the default branch ({@code GET /repos/{owner}/{repo}/contents/{path}}).
     *
     * @param path the path within the repository
     * @return the file, or empty when GitHub answers HTTP 404 or the path is a directory, symbolic link or submodule
     * @throws IllegalArgumentException if {@code path} is blank
     * @throws IOException if the transport fails
     */
    public Optional<RepositoryFile> file(final String path) throws IOException {
        return read(requireNotBlank(path, "path"), null);
    }

    /**
     * Loads one file at a ref ({@code GET /repos/{owner}/{repo}/contents/{path}}).
     *
     * @param path the path within the repository
     * @param ref the branch, tag or commit
     * @return the file, or empty when GitHub answers HTTP 404 or the path is a directory, symbolic link or submodule
     * @throws IllegalArgumentException if {@code path} or {@code ref} is blank
     * @throws IOException if the transport fails
     */
    public Optional<RepositoryFile> file(final String path, final String ref) throws IOException {
        return read(requireNotBlank(path, "path"), requireNotBlank(ref, "ref"));
    }

    /**
     * Creates or replaces a file on the default branch ({@code PUT /repos/{owner}/{repo}/contents/{path}}) and loads it
     * again. An existing file is replaced using its current blob hash.
     *
     * @param path the path within the repository
     * @param message the commit message
     * @param content the new content
     * @return the file as GitHub holds it after the write
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code path} or {@code message} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the file is gone after the write
     */
    public RepositoryFile writeFile(final String path, final String message, final byte[] content) throws IOException {
        return write(path, message, content, null);
    }

    /**
     * Creates or replaces a file on a branch ({@code PUT /repos/{owner}/{repo}/contents/{path}}) and loads it again. An
     * existing file is replaced using its current blob hash on that branch.
     *
     * @param path the path within the repository
     * @param message the commit message
     * @param content the new content
     * @param branch the branch to commit to
     * @return the file as GitHub holds it after the write
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code path}, {@code message} or {@code branch} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the file is gone after the write
     */
    public RepositoryFile writeFile(final String path, final String message, final byte[] content, final String branch)
            throws IOException {
        return write(path, message, content, requireNotBlank(branch, "branch"));
    }

    /**
     * Returns the SDK this repository belongs to.
     *
     * @return the SDK
     */
    GitHubSdk sdk() {
        return sdk;
    }

    private PullRequest loadPullRequest(final GitHubPullRequestSimple listed) {
        try {
            return pullRequest(listed.number())
                    .orElseThrow(() -> new IllegalStateException("Pull request " + listed.number() + " is gone"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Branch loadBranch(final GitHubShortBranch listed) {
        try {
            return branch(listed.name())
                    .orElseThrow(() -> new IllegalStateException("Branch " + listed.name() + " is gone"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<RepositoryFile> read(final String path, final @Nullable String ref) throws IOException {
        return sdk.github()
                .repositories()
                .contents()
                .getContent(owner, name, path, new GitHubGetContentFilter(ref))
                .flatMap(result -> result instanceof final GitHubContentFile file
                        ? Optional.of(new RepositoryFile(this, file))
                        : Optional.empty());
    }

    private RepositoryFile write(
            final String path, final String message, final byte[] content, final @Nullable String branch)
            throws IOException {
        requireNotBlank(path, "path");
        requireNotBlank(message, "message");
        requireNonNull(content, "content");
        final String sha = read(path, branch).map(RepositoryFile::sha).orElse(null);
        sdk.github()
                .repositories()
                .contents()
                .createOrUpdateFileContents(
                        owner,
                        name,
                        path,
                        new GitHubCreateOrUpdateFileContentsPayload(
                                message, Base64.getEncoder().encodeToString(content), sha, branch, null, null));
        return read(path, branch)
                .orElseThrow(() -> new IllegalStateException("File " + path + " is gone after the write"));
    }
}
