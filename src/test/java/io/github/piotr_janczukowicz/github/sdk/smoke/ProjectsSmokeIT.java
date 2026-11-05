package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.api.projects.GitHubProjectFieldDataType;
import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.projects.Project;
import io.github.piotr_janczukowicz.github.sdk.projects.ProjectField;
import io.github.piotr_janczukowicz.github.sdk.projects.ProjectItem;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ProjectsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String PROJECT_VARIABLE = "SMOKE_GITHUB_PROJECT";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void items_shouldBeAddedChangedListedAndRemoved_whenTheSandboxOwnerHasAProject() throws IOException {
        final String number = System.getenv(PROJECT_VARIABLE);
        Assumptions.assumeTrue(
                number != null && !number.isBlank(),
                PROJECT_VARIABLE + " is not set to the number of a project of the sandbox owner");
        final Project project = project(Long.parseLong(number.trim()));
        final String title = ENV.name("draft");
        final ProjectItem draft = addDraft(project, title);
        try {
            final List<ProjectField> fields = project.fields().toList();
            final Optional<ProjectField> text = fields.stream()
                    .filter(field -> field.dataType() == GitHubProjectFieldDataType.TEXT)
                    .findFirst();
            final boolean isListed = project.items().anyMatch(item -> item.id() == draft.id());
            final Project refreshed = project.refresh();
            final ProjectItem reloaded = draft.refresh();
            final ProjectItem changed =
                    text.isPresent() ? draft.setField(text.get().id(), "smoke") : reloaded;

            assertSoftly(softly -> {
                softly.assertThat(refreshed.number()).isEqualTo(project.number());
                softly.assertThat(fields).isNotEmpty();
                softly.assertThat(isListed).isTrue();
                softly.assertThat(reloaded.id()).isEqualTo(draft.id());
                softly.assertThat(changed.id()).isEqualTo(draft.id());
            });
        } finally {
            draft.delete();
        }

        assertThat(project.item(draft.id())).isEmpty();
    }

    private static Project project(final long number) throws IOException {
        return ENV.sdk()
                .user(ENV.owner())
                .orElseThrow()
                .project(number)
                .orElseThrow(() -> new IllegalStateException("Project " + number + " is not found"));
    }

    private static ProjectItem addDraft(final Project project, final String title) throws IOException {
        try {
            return project.addDraft(title, null);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("GitHub refuses project writes here: the token needs the Projects permission ("
                    + e.getMessage() + ")");
            throw e;
        }
    }
}
