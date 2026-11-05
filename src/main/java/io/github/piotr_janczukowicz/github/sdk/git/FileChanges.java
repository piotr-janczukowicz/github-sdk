package io.github.piotr_janczukowicz.github.sdk.git;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateBlobPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateCommitPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreeEntry;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreeEntryMode;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreeEntryType;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateTreePayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubUpdateRefPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Collects file writes and deletions for one branch of a {@link Repository} and commits them together. Nothing is sent
 * until {@link #send(String)}. When the same path is chosen more than once, the last choice counts.
 */
public final class FileChanges {

    private static final String BRANCH_REF = "heads/";

    private final Repository repository;
    private final String branch;
    private final Map<String, byte @Nullable []> changes = new LinkedHashMap<>();

    public FileChanges(final Repository repository, final String branch) {
        this.repository = requireNonNull(repository, "repository");
        this.branch = requireNotBlank(branch, "branch");
    }

    /**
     * Writes a file, creating it or replacing its content.
     *
     * @param path the path in the repository
     * @param content the new content
     * @return these changes
     * @throws NullPointerException if {@code path} or {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public FileChanges write(final String path, final byte[] content) {
        changes.put(
                requireNotBlank(path, "path"),
                requireNonNull(content, "content").clone());
        return this;
    }

    /**
     * Writes a text file, encoded as UTF-8. See {@link #write(String, byte[])}.
     *
     * @param path the path in the repository
     * @param text the new content
     * @return these changes
     * @throws NullPointerException if {@code path} or {@code text} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public FileChanges write(final String path, final String text) {
        return write(path, requireNonNull(text, "text").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Deletes a file.
     *
     * @param path the path in the repository
     * @return these changes
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public FileChanges delete(final String path) {
        changes.put(requireNotBlank(path, "path"), null);
        return this;
    }

    /**
     * Commits all changes to the branch as one commit. The commit is built from git objects and the branch moves
     * without force, so a push that lands in between makes GitHub refuse the update and nothing is overwritten. The
     * author and committer are the authenticated user.
     *
     * @param message the commit message
     * @return the new commit
     * @throws NullPointerException if {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank
     * @throws IllegalStateException if no change was chosen, the branch is gone, or GitHub does not have the commit it
     *     just created
     * @throws IOException if the transport fails
     */
    public Commit send(final String message) throws IOException {
        requireNotBlank(message, "message");
        if (changes.isEmpty()) {
            throw new IllegalStateException("There is nothing to commit");
        }
        final GitHub github = Internals.github(repository);
        final String owner = repository.owner();
        final String name = repository.name();
        final String headSha = github.gitDatabase()
                .refs()
                .getRef(owner, name, BRANCH_REF + branch)
                .orElseThrow(() -> new IllegalStateException("Branch " + branch + " is gone"))
                .object()
                .sha();
        final String baseTree = github.gitDatabase()
                .commits()
                .getCommit(owner, name, headSha)
                .orElseThrow(() -> new IllegalStateException("Commit " + headSha + " is gone"))
                .tree()
                .sha();
        final String tree = github.gitDatabase()
                .trees()
                .createTree(owner, name, new GitHubCreateTreePayload(entries(github), baseTree))
                .sha();
        final String commit = github.gitDatabase()
                .commits()
                .createCommit(
                        owner, name, new GitHubCreateCommitPayload(message, tree, List.of(headSha), null, null, null))
                .sha();
        github.gitDatabase()
                .refs()
                .updateRef(owner, name, BRANCH_REF + branch, new GitHubUpdateRefPayload(commit, false));
        return repository.commit(commit).orElseThrow(() -> new IllegalStateException("Commit " + commit + " is gone"));
    }

    private List<GitHubCreateTreeEntry> entries(final GitHub github) throws IOException {
        final List<GitHubCreateTreeEntry> entries = new ArrayList<>(changes.size());
        for (final Map.Entry<String, byte @Nullable []> change : changes.entrySet()) {
            final byte[] content = change.getValue();
            final OptionalNull<String> sha =
                    content == null ? OptionalNull.ofNull() : OptionalNull.of(blob(github, content));
            entries.add(new GitHubCreateTreeEntry(
                    change.getKey(), GitHubCreateTreeEntryMode.FILE, GitHubCreateTreeEntryType.BLOB, sha, null));
        }
        return entries;
    }

    private String blob(final GitHub github, final byte[] content) throws IOException {
        return github.gitDatabase()
                .blobs()
                .createBlob(
                        repository.owner(),
                        repository.name(),
                        new GitHubCreateBlobPayload(Base64.getEncoder().encodeToString(content), "base64"))
                .sha();
    }
}
