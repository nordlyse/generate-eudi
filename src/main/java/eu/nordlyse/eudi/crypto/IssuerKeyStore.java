package eu.nordlyse.eudi.crypto;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import eu.nordlyse.eudi.config.EudiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

public class IssuerKeyStore {

    private static final Logger log = LoggerFactory.getLogger(IssuerKeyStore.class);

    private final ECKey issuerKey;

    public IssuerKeyStore(EudiProperties properties) {
        this.issuerKey = loadOrCreate(Path.of(properties.issuerKeyFile()));
    }

    public ECKey issuerKey() {
        return issuerKey;
    }

    public ECKey publicJwk() {
        return issuerKey.toPublicJWK();
    }

    private static ECKey loadOrCreate(Path path) {
        try {
            if (Files.exists(path)) {
                return ECKey.parse(Files.readString(path));
            }
            Files.createDirectories(path.getParent());
            ECKey generated = new ECKeyGenerator(Curve.P_256)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID("generate-eudi-pid-provider-1")
                    .secureRandom(new SecureRandom())
                    .generate();
            Files.writeString(path, generated.toJSONString());
            log.info("Created PID provider signing key at {}", path.toAbsolutePath());
            return generated;
        } catch (IOException | java.text.ParseException | com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException("Unable to initialise PID provider signing key", ex);
        }
    }
}
