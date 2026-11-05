package io.github.piotr_janczukowicz.github.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.github.client.exception.GitHubForbiddenException;
import io.github.piotr_janczukowicz.github.sdk.User;
import io.github.piotr_janczukowicz.github.sdk.accounts.Me;
import io.github.piotr_janczukowicz.github.sdk.accounts.SigningKey;
import io.github.piotr_janczukowicz.github.sdk.accounts.SshKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Following, blocking and email changes are not run live, because they would affect real accounts. */
@Tag("smoke")
class MeSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void sshKey_shouldBeAddedListedReloadedAndDeleted_whenItIsManaged() throws IOException, GeneralSecurityException {
        final Me me = ENV.sdk().me();
        final String title = ENV.name("ssh");
        final SshKey added = addSshKey(me, title);
        try {
            final List<SshKey> listed = me.sshKeys().toList();
            final SshKey reloaded = added.refresh();

            assertSoftly(softly -> {
                softly.assertThat(added.title()).isEqualTo(title);
                softly.assertThat(listed).extracting(SshKey::id).contains(added.id());
                softly.assertThat(reloaded.id()).isEqualTo(added.id());
            });
        } finally {
            added.delete();
        }
        assertThat(isGone(me, added.id())).isTrue();
    }

    private static SshKey addSshKey(final Me me, final String title) throws IOException, GeneralSecurityException {
        try {
            return me.addSshKey(title, SmokeKeys.openSshPublicKey());
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no SSH keys permission, so GitHub refuses managing SSH keys");
            throw e;
        }
    }

    private static boolean isGone(final Me me, final long id) throws IOException {
        try {
            return me.sshKey(id).isEmpty();
        } catch (final GitHubForbiddenException e) {
            return true;
        }
    }

    @Test
    void signingKey_shouldBeAddedListedAndDeleted_whenItIsManaged() throws IOException, GeneralSecurityException {
        final Me me = ENV.sdk().me();
        final String title = ENV.name("signing");
        final SigningKey added = addSigningKey(me, title);
        try {
            final List<SigningKey> listed = me.signingKeys().toList();

            assertSoftly(softly -> {
                softly.assertThat(added.title()).isEqualTo(title);
                softly.assertThat(listed).extracting(SigningKey::id).contains(added.id());
            });
        } finally {
            added.delete();
        }
    }

    private static SigningKey addSigningKey(final Me me, final String title)
            throws IOException, GeneralSecurityException {
        try {
            return me.addSigningKey(title, SmokeKeys.openSshPublicKey());
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no SSH signing keys permission, so GitHub refuses managing them");
            throw e;
        }
    }

    @Test
    void account_shouldBeReadable_whenTheTokenAllowsIt() throws IOException {
        final Me me = ENV.sdk().me();
        final User profile = me.profile();

        final long emails = readEmails(me);

        assertSoftly(softly -> {
            softly.assertThat(profile.login()).isNotBlank();
            softly.assertThat(emails).isGreaterThanOrEqualTo(0);
        });
    }

    private static long readEmails(final Me me) throws IOException {
        try {
            return me.emails().count();
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no Email addresses permission");
            throw e;
        }
    }

    @Test
    void socialGraph_shouldBeReadable_whenTheTokenAllowsIt() throws IOException {
        final Me me = ENV.sdk().me();

        final boolean isRead = readSocialGraph(me, me.profile());

        assertThat(isRead).isTrue();
    }

    private static boolean readSocialGraph(final Me me, final User profile) throws IOException {
        try {
            me.followers().limit(5).toList();
            me.following().limit(5).toList();
            me.blocked().limit(5).toList();
            me.socialAccounts().toList();
            profile.followers().limit(5).toList();
            profile.sshKeys().limit(5).toList();
            profile.gpgKeys().limit(5).toList();
            profile.signingKeys().limit(5).toList();
            return true;
        } catch (final GitHubForbiddenException e) {
            Assumptions.abort("The token has no permission to read these account details");
            throw e;
        }
    }
}
