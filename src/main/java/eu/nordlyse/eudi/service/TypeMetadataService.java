package eu.nordlyse.eudi.service;

import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.crypto.IssuerKeyStore;
import eu.nordlyse.eudi.web.dto.SchemaField;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TypeMetadataService {

    private final EudiProperties properties;
    private final PidSchemaService schemaService;
    private final IssuerKeyStore issuerKeyStore;

    public TypeMetadataService(
            EudiProperties properties,
            PidSchemaService schemaService,
            IssuerKeyStore issuerKeyStore
    ) {
        this.properties = properties;
        this.schemaService = schemaService;
        this.issuerKeyStore = issuerKeyStore;
    }

    public Map<String, Object> pidTypeMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vct", properties.vct());
        metadata.put("name", "Person Identification Data");
        metadata.put("description",
                "Natural-person PID as specified in CIR 2024/2977 and ARF Annex 3.01. Catalog entry for PID_15.");
        metadata.put("extends", "");
        metadata.put("display", List.of(Map.of(
                "lang", "en",
                "name", "EUDI PID",
                "description", "European Digital Identity Person Identification Data"
        )));
        List<Map<String, Object>> claims = new ArrayList<>();
        for (SchemaField field : schemaService.fields()) {
            if ("location_status".equals(field.cirIdentifier())) {
                continue;
            }
            claims.add(Map.of(
                    "path", pathFor(field),
                    "sd", "always",
                    "display", List.of(Map.of(
                            "lang", "en",
                            "label", field.cirIdentifier(),
                            "description", field.description()
                    ))
            ));
        }
        metadata.put("claims", claims);
        metadata.put("schema_uri", properties.typeMetadataUri());
        return metadata;
    }

    public Map<String, Object> issuerMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", properties.issuer());
        metadata.put("jwks", Map.of("keys", List.of(issuerKeyStore.publicJwk().toJSONObject())));
        return metadata;
    }

    private static List<String> pathFor(SchemaField field) {
        String claim = field.sdJwtClaim();
        if (claim.contains(".")) {
            return List.of(claim.split("\\."));
        }
        if (claim.contains("{")) {
            return List.of("place_of_birth");
        }
        return List.of(claim);
    }
}
