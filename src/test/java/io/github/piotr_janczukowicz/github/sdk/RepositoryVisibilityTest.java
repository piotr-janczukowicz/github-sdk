package io.github.piotr_janczukowicz.github.sdk;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryVisibility;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryVisibilityTest {

    @Test
    void toClient_shouldMapEveryVisibilityToTheClientValue_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(RepositoryVisibility.PUBLIC.toClient()).isEqualTo(GitHubRepositoryVisibility.PUBLIC);
            softly.assertThat(RepositoryVisibility.PRIVATE.toClient()).isEqualTo(GitHubRepositoryVisibility.PRIVATE);
            softly.assertThat(RepositoryVisibility.INTERNAL.toClient()).isEqualTo(GitHubRepositoryVisibility.INTERNAL);
        });
    }
}
