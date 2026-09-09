package eu.nordlyse.eudi.crypto;

import com.nimbusds.jose.util.Base64URL;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

/**
 * IETF Token Status List packing: 1 bit per index, 0 = valid, 1 = revoked,
 * bits packed LSB-first in each byte, then zlib-compressed and base64url-encoded.
 */
public final class TokenStatusListCodec {

    private TokenStatusListCodec() {
    }

    public static String encode(boolean[] revokedFlags) {
        int size = Math.max(revokedFlags.length, 1);
        byte[] packed = new byte[(size + 7) / 8];
        for (int i = 0; i < revokedFlags.length; i++) {
            if (revokedFlags[i]) {
                packed[i / 8] |= (byte) (1 << (i % 8));
            }
        }
        return Base64URL.encode(zlib(packed)).toString();
    }

    public static boolean isRevoked(boolean[] revokedFlags, int index) {
        if (index < 0 || index >= revokedFlags.length) {
            throw new IllegalArgumentException("status index out of range");
        }
        return revokedFlags[index];
    }

    private static byte[] zlib(byte[] input) {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        byte[] buffer = new byte[Math.max(64, input.length * 2)];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (!deflater.finished()) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        deflater.end();
        return out.toByteArray();
    }
}
