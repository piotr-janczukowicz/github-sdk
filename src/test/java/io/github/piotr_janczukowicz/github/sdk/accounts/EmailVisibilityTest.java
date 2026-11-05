package io.github.piotr_janczukowicz.github.sdk.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.github.api.users.GitHubEmailVisibility;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class EmailVisibilityTest {

    @Test
    void toClient_shouldMapEveryVisibilityToTheClientValueOfTheSameName_whenCalled() {
        final boolean isEveryNameKept = Arrays.stream(EmailVisibility.values())
                .allMatch(visibility -> visibility.toClient().name().equals(visibility.name()));

        assertThat(isEveryNameKept).isTrue();
        assertThat(EmailVisibility.values()).hasSameSizeAs(GitHubEmailVisibility.values());
    }
}
