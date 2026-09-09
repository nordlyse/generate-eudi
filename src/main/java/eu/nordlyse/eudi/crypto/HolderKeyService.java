package eu.nordlyse.eudi.crypto;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;

import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Component
public class HolderKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public HolderKeyBundle generateBoundHolderKey() {
        try {
            String kid = "holder-" + UUID.randomUUID();
            ECKey privateJwk = new ECKeyGenerator(Curve.P_256)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(kid)
                    .secureRandom(RANDOM)
                    .generate();

            byte[] wrappingKey = new byte[32];
            RANDOM.nextBytes(wrappingKey);

            JWEObject jwe = new JWEObject(
                    new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                            .contentType("jwk+json")
                            .keyID(kid)
                            .build(),
                    new Payload(privateJwk.toJSONString())
            );
            jwe.encrypt(new DirectEncrypter(new SecretKeySpec(wrappingKey, "AES")));

            return new HolderKeyBundle(
                    privateJwk.toPublicJWK().toJSONObject(),
                    jwe.serialize(),
                    Base64URL.encode(wrappingKey).toString(),
                    kid
            );
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to generate holder key", ex);
        }
    }

    public Map<String, Object> unlock(String encryptedHolderKey, String recoverySecret) {
        try {
            byte[] wrappingKey = Base64URL.from(recoverySecret).decode();
            if (wrappingKey.length != 32) {
                throw new IllegalArgumentException("Recovery secret must decode to 256 bits");
            }
            JWEObject jwe = JWEObject.parse(encryptedHolderKey);
            jwe.decrypt(new DirectDecrypter(new SecretKeySpec(wrappingKey, "AES")));
            ECKey key = ECKey.parse(jwe.getPayload().toString());
            return key.toJSONObject();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to decrypt holder key with the provided recovery secret", ex);
        }
    }

    public record HolderKeyBundle(
            Map<String, Object> publicJwk,
            String encryptedPrivateJwk,
            String recoverySecret,
            String keyId
    ) {
    }
}
