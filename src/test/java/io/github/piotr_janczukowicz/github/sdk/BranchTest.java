package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.github.api.GitHub;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtection;
import io.github.piotr_janczukowicz.github.api.branches.GitHubBranchWithProtectionBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class BranchTest {

    private final Repository repository = GitHubSdk.of(mock(GitHub.class)).repository("octocat", "hello-world");
    private final GitHubBranchWithProtection record = RepositoryContentFixtures.branch();
    private final Branch sut = new Branch(repository, record);

    @Test
    void accessors_shouldExposeTheRecord_whenBranchIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.name()).isEqualTo("main");
            softly.assertThat(sut.isProtected()).isFalse();
            softly.assertThat(sut.commit().record()).isSameAs(record.commit());
            softly.assertThat(sut.commit().repository()).isSameAs(repository);
            softly.assertThat(sut.record()).isSameAs(record);
        });
    }

    @Test
    void isProtected_shouldBeTrue_whenTheRecordSaysProtected() {
        final Branch protectedBranch = new Branch(
                repository,
                GitHubBranchWithProtectionBuilder.builder(record)
                        .protectedBranch(true)
                        .build());

        final boolean isProtected = protectedBranch.isProtected();

        assertThat(isProtected).isTrue();
    }
}
