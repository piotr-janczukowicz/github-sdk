package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.deploykeys.DeployKey;
import io.github.piotr_janczukowicz.github.sdk.notifications.Notification;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class DeployKeysAndNotificationsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void deployKey_shouldBeAddedListedReloadedAndDeleted_whenItIsManaged()
            throws IOException, GeneralSecurityException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String title = ENV.name("key");
        final DeployKey added = add(repository, title, SmokeKeys.openSshPublicKey());
        try {
            final List<DeployKey> listed = repository.deployKeys().toList();
            final DeployKey reloaded = added.refresh();

            assertSoftly(softly -> {
                softly.assertThat(added.title()).isEqualTo(title);
                softly.assertThat(added.isReadOnly()).isTrue();
                softly.assertThat(added.key()).startsWith(SmokeKeys.KEY_TYPE);
                softly.assertThat(listed).extracting(DeployKey::id).contains(added.id());
                softly.assertThat(reloaded.id()).isEqualTo(added.id());
            });
        } finally {
            added.delete();
        }
        final Optional<DeployKey> afterDelete = repository.deployKey(added.id());
        assertThat(afterDelete).isEmpty();
    }

    private static DeployKey add(final Repository repository, final String title, final String key) throws IOException {
        try {
            return repository.addDeployKey(title, key);
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Deploy keys permission, so GitHub refuses managing deploy keys");
            throw e;
        }
    }

    @Test
    void notifications_shouldBeListed_whenTheTokenSupportsThem() throws IOException {
        final List<Notification> notifications = listNotifications();

        Assumptions.assumeFalse(notifications.isEmpty(), "The token has no notifications");

        assertSoftly(softly -> {
            softly.assertThat(notifications).extracting(Notification::id).noneMatch(String::isBlank);
            softly.assertThat(notifications).extracting(Notification::title).noneMatch(String::isBlank);
            softly.assertThat(notifications)
                    .extracting(notification -> notification.repository().owner())
                    .noneMatch(String::isBlank);
        });
    }

    private static List<Notification> listNotifications() throws IOException {
        try {
            return ENV.sdk().notifications().limit(5).toList();
        } catch (final GitHubForbiddenException | GitHubNotFoundException e) {
            Assumptions.abort("GitHub does not support fine-grained tokens for notifications");
            throw e;
        }
    }
}
