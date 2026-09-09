package eu.nordlyse.eudi.service;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.crypto.IssuerKeyStore;
import eu.nordlyse.eudi.crypto.TokenStatusListCodec;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatusListService {

    private static final JOSEObjectType STATUS_LIST_JWT = new JOSEObjectType("statuslist+jwt");

    private final EudiProperties properties;
    private final CredentialRegistry registry;
    private final IssuerKeyStore issuerKeyStore;

    public StatusListService(
            EudiProperties properties,
            CredentialRegistry registry,
            IssuerKeyStore issuerKeyStore
    ) {
        this.properties = properties;
        this.registry = registry;
        this.issuerKeyStore = issuerKeyStore;
    }

    public String compactJwt() {
        try {
            Instant now = Instant.now();
            boolean[] flags = registry.revocationFlags();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .subject(properties.statusListUri())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(24 * 3600L)))
                    .claim("ttl", 3600)
                    .claim("status_list", Map.of(
                            "bits", 1,
                            "lst", TokenStatusListCodec.encode(flags)
                    ))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256)
                            .type(STATUS_LIST_JWT)
                            .keyID(issuerKeyStore.issuerKey().getKeyID())
                            .build(),
                    claims
            );
            jwt.sign(new ECDSASigner(issuerKeyStore.issuerKey()));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token status list", ex);
        }
    }

    public Map<String, Object> metadata() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uri", properties.statusListUri());
        body.put("bits", 1);
        body.put("entries", registry.nextStatusIndex());
        body.put("encoding", "ietf-token-status-list");
        return body;
    }
}
