package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.gists.Gist;
import io.github.piotr_janczukowicz.github.sdk.gists.GistComment;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class GistsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void gist_shouldBeCreatedChangedCommentedStarredAndDeleted_whenItIsManaged() throws IOException {
        final String first = ENV.name("one") + ".txt";
        final String renamed = ENV.name("two") + ".txt";
        final Gist created = create(first);
        try {
            final Gist updated = created.update()
                    .description("smoke " + ENV.runId())
                    .write(first, "changed")
                    .send();
            final Gist moved = updated.update().rename(first, renamed).send();
            final GistComment comment = moved.comment("first");
            final GistComment edited = comment.edit("second");
            final List<GistComment> comments = moved.comments().toList();
            moved.star();
            final boolean isStarred = moved.isStarred();
            final List<String> starred =
                    ENV.sdk().starredGists().map(Gist::id).limit(20).toList();
            moved.unstar();
            final boolean isStillStarred = moved.isStarred();
            final int revisions = (int) moved.commits().count();
            final boolean isOwnGist =
                    ENV.sdk().gists().limit(5).anyMatch(gist -> gist.id().equals(created.id()));
            edited.delete();
            final Optional<GistComment> afterDelete = moved.findComment(comment.id());

            assertSoftly(softly -> {
                softly.assertThat(created.isPublic()).isFalse();
                softly.assertThat(created.content(first)).hasValue("first version");
                softly.assertThat(updated.description()).hasValue("smoke " + ENV.runId());
                softly.assertThat(updated.content(first)).hasValue("changed");
                softly.assertThat(moved.files()).containsOnlyKeys(renamed);
                softly.assertThat(edited.body()).isEqualTo("second");
                softly.assertThat(comments).extracting(GistComment::id).contains(comment.id());
                softly.assertThat(isStarred).isTrue();
                softly.assertThat(starred).contains(created.id());
                softly.assertThat(isStillStarred).isFalse();
                softly.assertThat(revisions).isGreaterThanOrEqualTo(3);
                softly.assertThat(isOwnGist).isTrue();
                softly.assertThat(afterDelete).isEmpty();
            });
        } finally {
            created.delete();
        }
        final Optional<Gist> afterGistDelete = ENV.sdk().gist(created.id());
        assertThat(afterGistDelete).isEmpty();
    }

    private static Gist create(final String file) throws IOException {
        try {
            return ENV.sdk().createGist().file(file, "first version").send();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Gists permission, so GitHub refuses managing gists");
            throw e;
        }
    }

    @Test
    void gist_shouldBeEmpty_whenTheIdDoesNotExist() throws IOException {
        final Optional<Gist> gist = ENV.sdk().gist("0000000000000000000000000000000f");

        assertThat(gist).isEmpty();
    }
}
