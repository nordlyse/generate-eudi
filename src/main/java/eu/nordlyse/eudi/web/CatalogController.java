package eu.nordlyse.eudi.web;

import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.service.StatusListService;
import eu.nordlyse.eudi.service.TypeMetadataService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CatalogController {

    private final TypeMetadataService typeMetadataService;
    private final StatusListService statusListService;
    private final EudiProperties properties;

    public CatalogController(
            TypeMetadataService typeMetadataService,
            StatusListService statusListService,
            EudiProperties properties
    ) {
        this.typeMetadataService = typeMetadataService;
        this.statusListService = statusListService;
        this.properties = properties;
    }

    @GetMapping("/catalog/vct")
    public Map<String, Object> typeMetadata(
            @RequestParam(name = "id", defaultValue = "urn:eudi:pid:1") String id
    ) {
        if (!properties.vct().equals(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Unknown vct"
            );
        }
        return typeMetadataService.pidTypeMetadata();
    }

    @GetMapping("/.well-known/jwt-vc-issuer")
    public Map<String, Object> issuerMetadata() {
        return typeMetadataService.issuerMetadata();
    }

    @GetMapping(value = "/statuslists/pid", produces = "application/statuslist+jwt")
    public String statusList() {
        return statusListService.compactJwt();
    }

    @GetMapping(value = "/statuslists/pid.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> statusListMetadata() {
        return statusListService.metadata();
    }
}
