package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.releases.GitHubCreateReleasePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseNotesContent;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryTag;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.content.Release;
import io.github.piotr_janczukowicz.github.sdk.content.ReleaseAsset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("smoke")
class ReleasesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void release_shouldRoundTrip_whenADraftIsCreatedListedRefreshedAndDeleted() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String tag = ENV.name("release");
        final Release created = repository.createRelease(GitHubCreateReleasePayloadBuilder.builder()
                .tagName(tag)
                .name("smoke release")
                .draft(true)
                .build());
        boolean isDeleted = false;
        try {
            final Release renamed = created.update().name("smoke renamed").send();
            final Release refreshed = renamed.refresh();
            final boolean isListed = repository
                    .releases()
                    .limit(50)
                    .anyMatch(listed ->
                            listed.response().id() == created.response().id());
            refreshed.delete();
            isDeleted = true;
            final boolean isStillListed = repository
                    .releases()
                    .limit(50)
                    .anyMatch(listed ->
                            listed.response().id() == created.response().id());

            assertSoftly(softly -> {
                softly.assertThat(created.isDraft()).isTrue();
                softly.assertThat(renamed.isDraft()).isTrue();
                softly.assertThat(renamed.name()).contains("smoke renamed");
                softly.assertThat(refreshed.name()).contains("smoke renamed");
                softly.assertThat(isListed).isTrue();
                softly.assertThat(isStillListed).isFalse();
            });
        } finally {
            if (!isDeleted) {
                created.delete();
            }
        }
    }

    @Test
    void latestRelease_shouldNeverBeADraftAndTagsShouldRespectLimit_whenTheyAreRead() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final Optional<Release> latest = repository.latestRelease();
        final List<GitHubRepositoryTag> tags = repository.tags().limit(3).toList();

        assertSoftly(softly -> {
            softly.assertThat(latest.map(Release::isDraft).orElse(false)).isFalse();
            softly.assertThat(tags).doesNotContainNull();
        });
    }

    @Test
    void asset_shouldRoundTrip_whenAFileIsUploadedListedRelabeledDownloadedAndDeleted(@TempDir final Path directory)
            throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final Release release = repository.createRelease(GitHubCreateReleasePayloadBuilder.builder()
                .tagName(ENV.name("asset"))
                .name("smoke asset release")
                .draft(true)
                .build());
        try {
            final Path file = Files.writeString(directory.resolve("smoke.txt"), "smoke content");
            final ReleaseAsset uploaded = release.uploadAsset(file);
            final boolean isListed = release.refresh().assets().anyMatch(listed -> listed.id() == uploaded.id());
            final ReleaseAsset relabeled =
                    uploaded.update().label("Smoke label").send();
            final Path downloaded = relabeled.download(directory.resolve("downloaded.txt"));
            final String content = Files.readString(downloaded);
            relabeled.delete();
            final Optional<ReleaseAsset> afterDelete = repository.releaseAsset(uploaded.id());

            assertSoftly(softly -> {
                softly.assertThat(uploaded.name()).isEqualTo("smoke.txt");
                softly.assertThat(isListed).isTrue();
                softly.assertThat(relabeled.label()).contains("Smoke label");
                softly.assertThat(content).isEqualTo("smoke content");
                softly.assertThat(afterDelete).isEmpty();
            });
        } finally {
            release.delete();
        }
    }

    @Test
    void generateReleaseNotes_shouldReturnABody_whenATagNameIsGiven() throws IOException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());

        final GitHubReleaseNotesContent notes = repository.generateReleaseNotes(ENV.name("notes"));

        assertThat(notes.body()).isNotNull();
    }
}
