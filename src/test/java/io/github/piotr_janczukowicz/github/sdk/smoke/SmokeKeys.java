package io.github.piotr_janczukowicz.github.sdk.smoke;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;

/** Creates throwaway SSH public keys for the smoke tests; the private half is never kept. */
final class SmokeKeys {

    static final String KEY_TYPE = "ssh-ed25519";

    private static final int KEY_LENGTH = 32;

    private SmokeKeys() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a new Ed25519 key pair and returns its public half in OpenSSH format.
     *
     * @return the public key line
     * @throws GeneralSecurityException if the JDK has no Ed25519 support
     * @throws IOException if the key cannot be encoded
     */
    static String openSshPublicKey() throws GeneralSecurityException, IOException {
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
