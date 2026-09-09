package eu.nordlyse.eudi.web;

import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.service.PidIssuanceService;
import eu.nordlyse.eudi.service.PidSchemaService;
import eu.nordlyse.eudi.web.dto.PidIssueRequest;
import eu.nordlyse.eudi.web.dto.PidIssueResponse;
import eu.nordlyse.eudi.web.dto.SchemaField;
import eu.nordlyse.eudi.web.dto.UnlockRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PidController {

    private final PidIssuanceService issuanceService;
    private final PidSchemaService schemaService;
    private final EudiProperties properties;

    public PidController(
            PidIssuanceService issuanceService,
            PidSchemaService schemaService,
            EudiProperties properties
    ) {
        this.issuanceService = issuanceService;
        this.schemaService = schemaService;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "generate-eudi");
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return Map.of(
                "name", "generate-eudi",
                "vct", properties.vct(),
                "mdocDocType", properties.mdocDocType(),
                "mdocNamespace", properties.mdocNamespace(),
                "issuer", properties.issuer(),
                "standards", List.of(
                        "CIR (EU) 2024/2977",
                        "ARF Annex 3.01 PID Rulebook",
                        "ISO/IEC 18013-5",
                        "SD-JWT VC",
                        "Regulation (EU) 2024/1183"
                )
        );
    }

    @GetMapping("/pid/schema")
    public List<SchemaField> schema() {
        return schemaService.fields();
    }

    @GetMapping("/pid/stats")
    public Map<String, Object> stats() {
        return issuanceService.stats();
    }

    @PostMapping("/pid")
    public PidIssueResponse issue(@Valid @RequestBody PidIssueRequest request) {
        return issuanceService.issue(request);
    }

    @GetMapping("/pid")
    public List<PidIssueResponse> list() {
        return issuanceService.list();
    }

    @GetMapping("/pid/{id}")
    public PidIssueResponse get(@PathVariable String id) {
        return issuanceService.get(id);
    }

    @PostMapping("/pid/{id}/unlock")
    public Map<String, Object> unlock(@PathVariable String id, @RequestBody UnlockRequest request) {
        return issuanceService.unlock(id, request.recoverySecret());
    }
}
