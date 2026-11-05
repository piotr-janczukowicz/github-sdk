package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.client.exception.GitHubNotFoundException;
import io.github.piotr_janczukowicz.github.sdk.Repository;
import io.github.piotr_janczukowicz.github.sdk.deploykeys.DeployKey;
import io.github.piotr_janczukowicz.github.sdk.notifications.Notification;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class DeployKeysAndNotificationsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String KEY_TYPE = "ssh-ed25519";
    private static final int KEY_LENGTH = 32;

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void deployKey_shouldBeAddedListedReloadedAndDeleted_whenItIsManaged()
            throws IOException, GeneralSecurityException {
        final Repository repository = ENV.sdk().repository(ENV.owner(), ENV.repo());
        final String title = ENV.name("key");
        final DeployKey added = add(repository, title, openSshPublicKey());
        try {
            final List<DeployKey> listed = repository.deployKeys().toList();
            final DeployKey reloaded = added.refresh();

            assertSoftly(softly -> {
                softly.assertThat(added.title()).isEqualTo(title);
                softly.assertThat(added.isReadOnly()).isTrue();
                softly.assertThat(added.key()).startsWith(KEY_TYPE);
                softly.assertThat(listed).extracting(DeployKey::id).contains(added.id());
                softly.assertThat(reloaded.id()).isEqualTo(added.id());
            });
        } finally {
            added.delete();
        }
        assertThat(repository.deployKey(added.id())).isEmpty();
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

        assertThat(notifications).allSatisfy(notification -> {
            assertThat(notification.id()).isNotBlank();
            assertThat(notification.title()).isNotBlank();
            assertThat(notification.repository().owner()).isNotBlank();
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

    private static String openSshPublicKey() throws GeneralSecurityException, IOException {
        final KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final byte[] encoded = pair.getPublic().getEncoded();
        final byte[] raw = Arrays.copyOfRange(encoded, encoded.length - KEY_LENGTH, encoded.length);
        final ByteArrayOutputStream blob = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(blob)) {
            final byte[] type = KEY_TYPE.getBytes(StandardCharsets.US_ASCII);
            out.writeInt(type.length);
            out.write(type);
            out.writeInt(raw.length);
            out.write(raw);
        }
        return KEY_TYPE + " " + Base64.getEncoder().encodeToString(blob.toByteArray());
    }
}
