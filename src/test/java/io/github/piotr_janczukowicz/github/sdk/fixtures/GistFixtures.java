package io.github.piotr_janczukowicz.github.sdk.fixtures;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGist;
import io.github.piotr_janczukowicz.github.api.gists.GitHubBaseGistBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistComment;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistCommentBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimple;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleBuilder;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleFile;
import io.github.piotr_janczukowicz.github.api.gists.GitHubGistSimpleFileBuilder;
import io.github.piotr_janczukowicz.github.api.search.GitHubAuthorAssociation;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import java.time.Instant;
import java.util.Map;

public final class GistFixtures {

    public static final String ID = "aa5a315d61ae9438b18d";
    public static final String FILE = "hello.txt";
    public static final long COMMENT_ID = 4004L;

    private static final String API = "https://api.github.com/gists/" + ID;
    private static final Instant MOMENT = Instant.parse("2026-11-05T10:00:00Z");

    private GistFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static GitHubGistSimple gist() {
        return GitHubGistSimpleBuilder.builder()
                .id(ID)
                .nodeId("G_kwDOA")
                .url(API)
                .htmlUrl("https://gist.github.com/" + ID)
                .description("A greeting")
                .isPublic(true)
                .owner(owner())
                .files(Map.of(FILE, file("hello", false)))
                .build();
    }

    public static GitHubGistSimple gistWithFiles(final Map<String, GitHubGistSimpleFile> files) {
        return GitHubGistSimpleBuilder.builder(gist()).files(files).build();
    }

    public static GitHubGistSimple bareGist() {
        return GitHubGistSimpleBuilder.builder()
                .id(ID)
                .url(API)
                .htmlUrl("https://gist.github.com/" + ID)
                .build();
    }

    public static GitHubGistSimpleFile file(final String content, final boolean isTruncated) {
        return GitHubGistSimpleFileBuilder.builder()
                .filename(FILE)
                .content(content)
                .truncated(isTruncated)
                .build();
    }

    public static GitHubBaseGist baseGist() {
        return GitHubBaseGistBuilder.builder()
                .id(ID)
                .nodeId("G_kwDOA")
                .url(API)
                .forksUrl(API + "/forks")
                .commitsUrl(API + "/commits")
                .commentsUrl(API + "/comments")
                .gitPullUrl("https://gist.github.com/" + ID + ".git")
                .gitPushUrl("https://gist.github.com/" + ID + ".git")
                .htmlUrl("https://gist.github.com/" + ID)
                .isPublic(true)
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .comments(0)
                .files(Map.of())
                .build();
    }

    public static GitHubGistComment comment() {
        return GitHubGistCommentBuilder.builder()
                .id(COMMENT_ID)
                .nodeId("GC_kwDOA")
                .url(API + "/comments/4004")
                .body("Nice")
                .user(owner())
                .createdAt(MOMENT)
                .updatedAt(MOMENT)
                .authorAssociation(GitHubAuthorAssociation.OWNER)
                .build();
    }

    public static GitHubGistComment commentWithoutAuthor() {
        return GitHubGistCommentBuilder.builder(comment()).user(null).build();
    }

    private static GitHubSimpleUser owner() {
        final GitHubSimpleUser owner = mock(GitHubSimpleUser.class);
        lenient().when(owner.login()).thenReturn("octocat");
        return owner;
    }
}
