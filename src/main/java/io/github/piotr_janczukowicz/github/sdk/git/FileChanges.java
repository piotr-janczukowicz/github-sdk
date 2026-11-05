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
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTree;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTreeEntry;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubUpdateRefPayload;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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
    private static final String TREE = "tree";
    private static final String IS_GONE = " is gone";
    private static final String EXECUTABLE_MODE = "100755";
    private static final String SYMLINK_MODE = "120000";
    private static final String SUBMODULE_MODE = "160000";

    private final Repository repository;
    private final String branch;
    private final Map<String, byte @Nullable []> changes = new LinkedHashMap<>();

    /**
     * Starts an empty collection of changes for one branch of a repository. Applications normally get it from
     * {@code Repository.commitTo(String)}. Nothing is sent until {@link #send(String)}.
     *
     * @param repository the repository the object belongs to
     * @param branch the branch the object belongs to
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a text argument is blank
     */
    public FileChanges(final Repository repository, final String branch) {
        this.repository = requireNonNull(repository, "repository");
        this.branch = Arguments.requireSafePath(branch, "branch");
    }

    /**
     * Writes a file, creating it or replacing its content.
     *
     * @param path the path in the repository
     * @param content the new content
     * @return these changes
     * @throws NullPointerException if {@code path} or {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank, has a control character, has a {@code .} or {@code ..}
     *     segment or has an empty segment
     */
    public FileChanges write(final String path, final byte[] content) {
        changes.put(
                Arguments.requireFilePath(path, "path"),
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
     * @throws IllegalArgumentException if {@code path} is blank, has a control character, has a {@code .} or {@code ..}
     *     segment or has an empty segment
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
     * @throws IllegalArgumentException if {@code path} is blank, has a control character, has a {@code .} or {@code ..}
     *     segment or has an empty segment
     */
    public FileChanges delete(final String path) {
        changes.put(Arguments.requireFilePath(path, "path"), null);
        return this;
    }

    /**
     * Commits all changes to the branch as one commit. The commit is built from git objects and the branch moves
     * without force, so a push that lands in between makes GitHub refuse the update and nothing is overwritten. A
     * rewritten file keeps the mode it has on the branch, so an executable script stays executable. The author and
     * committer are the authenticated user.
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
                .orElseThrow(() -> new IllegalStateException("Branch " + branch + IS_GONE))
                .object()
                .sha();
        final String baseTree = github.gitDatabase()
                .commits()
                .getCommit(owner, name, headSha)
                .orElseThrow(() -> new IllegalStateException("Commit " + headSha + IS_GONE))
                .tree()
                .sha();
        final String tree = github.gitDatabase()
                .trees()
                .createTree(owner, name, new GitHubCreateTreePayload(entries(github, baseTree), baseTree))
                .sha();
        final String commit = github.gitDatabase()
                .commits()
                .createCommit(
                        owner, name, new GitHubCreateCommitPayload(message, tree, List.of(headSha), null, null, null))
                .sha();
        github.gitDatabase()
                .refs()
                .updateRef(owner, name, BRANCH_REF + branch, new GitHubUpdateRefPayload(commit, false));
        return repository.commit(commit).orElseThrow(() -> new IllegalStateException("Commit " + commit + IS_GONE));
    }

    private List<GitHubCreateTreeEntry> entries(final GitHub github, final String baseTree) throws IOException {
        final List<GitHubCreateTreeEntry> entries = new ArrayList<>(changes.size());
        final Map<String, List<GitHubGitTreeEntry>> trees = new HashMap<>();
        for (final Map.Entry<String, byte @Nullable []> change : changes.entrySet()) {
            final byte[] content = change.getValue();
            if (content == null) {
                entries.add(entry(change.getKey(), GitHubCreateTreeEntryMode.FILE, OptionalNull.ofNull(), null));
                continue;
            }
            final GitHubCreateTreeEntryMode mode =
                    existingMode(github, baseTree, change.getKey().split("/"), 0, trees);
            final String text = inlineText(content);
            entries.add(
                    text == null
                            ? entry(change.getKey(), mode, OptionalNull.of(blob(github, content)), null)
                            : entry(change.getKey(), mode, null, text));
        }
        return entries;
    }

    private static GitHubCreateTreeEntry entry(
            final String path,
            final GitHubCreateTreeEntryMode mode,
            final @Nullable OptionalNull<String> sha,
            final @Nullable String content) {
        return new GitHubCreateTreeEntry(path, mode, GitHubCreateTreeEntryType.BLOB, sha, content);
    }

    /**
     * Returns the text of a file that GitHub can take inline in the tree, so no blob request is needed: valid UTF-8
     * without a NUL character, and not empty.
     */
    private static @Nullable String inlineText(final byte[] content) {
        if (content.length == 0) {
            return null;
        }
        for (final byte value : content) {
            if (value == 0) {
                return null;
            }
        }
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (final CharacterCodingException e) {
            return null;
        }
    }

    /**
     * Finds the mode the file has in the base tree, so that rewriting an executable file or a symbolic link keeps its
     * mode. A new file, and a path that is not a file there, gets the regular file mode. It walks the path one
     * directory at a time and reuses the trees it already loaded.
     */
    private GitHubCreateTreeEntryMode existingMode(
            final GitHub github,
            final String treeSha,
            final String[] segments,
            final int index,
            final Map<String, List<GitHubGitTreeEntry>> trees)
            throws IOException {
        final String segment = segments[index];
        final GitHubGitTreeEntry found = tree(github, treeSha, trees).stream()
                .filter(entry -> segment.equals(entry.path()))
                .findFirst()
                .orElse(null);
        if (found == null) {
            return GitHubCreateTreeEntryMode.FILE;
        }
        if (index == segments.length - 1) {
            return modeOf(found.mode(), segment);
        }
        if (!TREE.equals(found.type())) {
            return GitHubCreateTreeEntryMode.FILE;
        }
        return existingMode(github, found.sha(), segments, index + 1, trees);
    }

    private List<GitHubGitTreeEntry> tree(
            final GitHub github, final String sha, final Map<String, List<GitHubGitTreeEntry>> trees)
            throws IOException {
        final List<GitHubGitTreeEntry> cached = trees.get(sha);
        if (cached != null) {
            return cached;
        }
        final GitHubGitTree tree = github.gitDatabase()
                .trees()
                .getTree(repository.owner(), repository.name(), sha)
                .orElseThrow(() -> new IllegalStateException("Tree " + sha + IS_GONE));
        if (tree.truncated()) {
            throw new IllegalStateException("Tree " + sha + " is too large to read the modes of its files");
        }
        final List<GitHubGitTreeEntry> loaded = tree.tree();
        trees.put(sha, loaded);
        return loaded;
    }

    private static GitHubCreateTreeEntryMode modeOf(final String mode, final String segment) {
        return switch (mode) {
            case SUBMODULE_MODE ->
                throw new IllegalStateException("A submodule cannot be rewritten as a file: " + segment);
            case EXECUTABLE_MODE -> GitHubCreateTreeEntryMode.EXECUTABLE;
            case SYMLINK_MODE -> GitHubCreateTreeEntryMode.SYMLINK;
            default -> GitHubCreateTreeEntryMode.FILE;
        };
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
