package eu.nordlyse.eudi.service;

import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.crypto.AgeClaimsFactory;
import eu.nordlyse.eudi.crypto.HolderKeyService;
import eu.nordlyse.eudi.crypto.IssuerKeyStore;
import eu.nordlyse.eudi.crypto.MdocEncoder;
import eu.nordlyse.eudi.crypto.PortraitProcessor;
import eu.nordlyse.eudi.crypto.SdJwtVcIssuer;
import eu.nordlyse.eudi.domain.IssuedCredential;
import eu.nordlyse.eudi.domain.PidDocument;
import eu.nordlyse.eudi.domain.PlaceOfBirth;
import eu.nordlyse.eudi.domain.ResidentAddress;
import eu.nordlyse.eudi.web.dto.PidIssueRequest;
import eu.nordlyse.eudi.web.dto.PidIssueResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class PidIssuanceService {

    private static final Set<Integer> SEX_VALUES = Set.of(0, 1, 2, 3, 4, 5, 6, 9);

    private final EudiProperties properties;
    private final HolderKeyService holderKeyService;
    private final SdJwtVcIssuer sdJwtVcIssuer;
    private final MdocEncoder mdocEncoder;
    private final IssuerKeyStore issuerKeyStore;
    private final CredentialRegistry registry;
    private final AuditLogService auditLog;
    private final Clock clock;

    public PidIssuanceService(
            EudiProperties properties,
            HolderKeyService holderKeyService,
            SdJwtVcIssuer sdJwtVcIssuer,
            MdocEncoder mdocEncoder,
            IssuerKeyStore issuerKeyStore,
            CredentialRegistry registry,
            AuditLogService auditLog
    ) {
        this.properties = properties;
        this.holderKeyService = holderKeyService;
        this.sdJwtVcIssuer = sdJwtVcIssuer;
        this.mdocEncoder = mdocEncoder;
        this.issuerKeyStore = issuerKeyStore;
        this.registry = registry;
        this.auditLog = auditLog;
        this.clock = Clock.systemUTC();
    }

    public PidIssueResponse issue(PidIssueRequest request, String officer) {
        PidDocument document = toDocument(request);
        Instant issuedAt = clock.instant();
        Instant technicalExpiry = issuedAt.plusSeconds(properties.technicalValidityDays() * 24L * 3600L);
        int statusIndex = registry.nextStatusIndex();

        HolderKeyService.HolderKeyBundle holderKey = holderKeyService.generateBoundHolderKey();
        SdJwtVcIssuer.IssuedSdJwt sdJwt = sdJwtVcIssuer.issue(
                document,
                holderKey.publicJwk(),
                issuedAt,
                technicalExpiry,
                statusIndex
        );
        MdocEncoder.EncodedMdoc mdoc = mdocEncoder.encode(document);

        IssuedCredential stored = new IssuedCredential(
                UUID.randomUUID().toString(),
                issuedAt,
                technicalExpiry,
                document,
                sdJwt.compact(),
                sdJwt.payloadPreview(),
                mdoc.json(),
                mdoc.cborHex(),
                holderKey.publicJwk(),
                holderKey.encryptedPrivateJwk(),
                issuerKeyStore.publicJwk().toJSONObject(),
                statusIndex,
                false,
                officer
        );
        registry.save(stored);
        auditLog.record(
                officer,
                "ISSUE",
                stored.id(),
                document.documentNumber(),
                "Issued PID with status index " + statusIndex
        );
        return toResponse(stored, sdJwt.disclosures(), holderKey.recoverySecret(), true);
    }

    public List<PidIssueResponse> list() {
        return registry.list().stream()
                .map(item -> toResponse(item, List.of(), null, false))
                .toList();
    }

    public PidIssueResponse get(String id) {
        return registry.find(id)
                .map(item -> toResponse(item, List.of(), null, true))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PID not found"));
    }

    public Map<String, Object> unlock(String id, String recoverySecret) {
        IssuedCredential credential = registry.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PID not found"));
        try {
            return holderKeyService.unlock(credential.encryptedHolderKey(), recoverySecret);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public PidIssueResponse revoke(String id, String officer) {
        IssuedCredential credential = registry.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PID not found"));
        IssuedCredential revoked = credential.revoke();
        registry.save(revoked);
        auditLog.record(
                officer,
                "REVOKE",
                revoked.id(),
                revoked.document().documentNumber(),
                "Revoked PID at status index " + revoked.statusIndex()
        );
        return toResponse(revoked, List.of(), null, true);
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("issuedCount", registry.size());
        stats.put("vct", properties.vct());
        stats.put("mdocDocType", properties.mdocDocType());
        stats.put("issuer", properties.issuer());
        stats.put("statusListUri", properties.statusListUri());
        stats.put("typeMetadataUri", properties.typeMetadataUri());
        return stats;
    }

    private PidDocument toDocument(PidIssueRequest request) {
        PlaceOfBirth placeOfBirth = new PlaceOfBirth(
                upper(request.getPlaceOfBirth() == null ? null : request.getPlaceOfBirth().getCountry()),
                trim(request.getPlaceOfBirth() == null ? null : request.getPlaceOfBirth().getRegion()),
                trim(request.getPlaceOfBirth() == null ? null : request.getPlaceOfBirth().getLocality())
        );
        if (!placeOfBirth.hasAnyValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "birth_place requires at least one of country, region or locality");
        }

        List<String> nationalities = request.getNationalities().stream()
                .map(this::upper)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (nationalities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one nationality is required");
        }

        if (request.getSex() != null && !SEX_VALUES.contains(request.getSex())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sex must be one of 0,1,2,3,4,5,6,9");
        }

        byte[] portrait = PortraitProcessor.parse(request.getPortraitDataUrl(), request.isPortraitOptOut());

        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate issuanceDate = request.getIssuanceDate() == null ? today : request.getIssuanceDate();
        LocalDate expiryDate = request.getExpiryDate() == null ? issuanceDate.plusYears(10) : request.getExpiryDate();
        if (expiryDate.isBefore(issuanceDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiry_date cannot be before issuance_date");
        }

        AgeClaimsFactory.AgeClaims computed = AgeClaimsFactory.fromBirthDate(request.getBirthDate(), today);
        Map<Integer, Boolean> ages = request.getAgeEqualOrOver() == null || request.getAgeEqualOrOver().isEmpty()
                ? computed.ageEqualOrOver()
                : new LinkedHashMap<>(request.getAgeEqualOrOver());
        Integer ageInYears = request.getAgeInYears() == null ? computed.ageInYears() : request.getAgeInYears();
        Integer ageBirthYear = request.getAgeBirthYear() == null ? computed.ageBirthYear() : request.getAgeBirthYear();

        String documentNumber = notBlank(request.getDocumentNumber())
                ? request.getDocumentNumber().trim()
                : generateDocumentNumber(request.getIssuingCountry());

        String trustAnchor = notBlank(request.getTrustAnchor()) ? request.getTrustAnchor().trim() : properties.trustAnchor();
        String category = notBlank(request.getAttestationLegalCategory()) ? request.getAttestationLegalCategory() : "PID";

        String jurisdiction = upper(request.getIssuingJurisdiction());
        String issuingCountry = upper(request.getIssuingCountry());
        if (notBlank(jurisdiction) && !jurisdiction.startsWith(issuingCountry)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "issuing_jurisdiction must start with issuing_country");
        }

        ResidentAddress residence = new ResidentAddress(
                trim(request.getResidentAddress()),
                upper(request.getResidentCountry()),
                trim(request.getResidentState()),
                trim(request.getResidentCity()),
                trim(request.getResidentPostalCode()),
                trim(request.getResidentStreet()),
                trim(request.getResidentHouseNumber())
        );

        return new PidDocument(
                request.getFamilyName().trim(),
                request.getGivenName().trim(),
                request.getBirthDate(),
                placeOfBirth,
                List.copyOf(nationalities),
                residence,
                trim(request.getPersonalAdministrativeNumber()),
                portrait,
                request.isPortraitOptOut(),
                trim(request.getFamilyNameBirth()),
                trim(request.getGivenNameBirth()),
                request.getSex(),
                trim(request.getEmailAddress()),
                trim(request.getMobilePhoneNumber()),
                expiryDate,
                request.getIssuingAuthority().trim(),
                issuingCountry,
                documentNumber,
                jurisdiction,
                issuanceDate,
                ages,
                ageInYears,
                ageBirthYear,
                trustAnchor,
                category
        );
    }

    private PidIssueResponse toResponse(
            IssuedCredential credential,
            List<SdJwtVcIssuer.Disclosure> disclosures,
            String recoverySecret,
            boolean includeArtifacts
    ) {
        List<PidIssueResponse.DisclosureView> disclosureViews = disclosures.stream()
                .map(item -> new PidIssueResponse.DisclosureView(item.path(), item.claim(), item.encoded()))
                .toList();
        String warning = recoverySecret == null ? null
                : "Store this recovery secret with the holder. It is shown only at issuance and is required to unwrap the encrypted holder private key. The PID private key must not be used to sign transactional data.";
        return new PidIssueResponse(
                credential.id(),
                credential.document().documentNumber(),
                credential.issuedAt(),
                credential.technicalExpiresAt(),
                credential.document().expiryDate(),
                properties.vct(),
                properties.mdocDocType(),
                includeArtifacts ? credential.sdJwtVc() : null,
                includeArtifacts ? credential.sdJwtPayloadPreview() : null,
                includeArtifacts ? disclosureViews : List.of(),
                includeArtifacts ? credential.mdoc() : null,
                includeArtifacts ? credential.mdocCborHex() : null,
                credential.holderPublicJwk(),
                includeArtifacts ? credential.encryptedHolderKey() : null,
                recoverySecret,
                warning,
                includeArtifacts ? credential.issuerPublicJwk() : null,
                credential.document().givenName(),
                credential.document().familyName(),
                credential.statusIndex(),
                properties.statusListUri(),
                properties.typeMetadataUri(),
                credential.revoked(),
                credential.issuedBy()
        );
    }

    private String generateDocumentNumber(String country) {
        int n = ThreadLocalRandom.current().nextInt(1000000, 10000000);
        return "EUDI-" + upper(country) + "-" + n;
    }

    private String upper(String value) {
        return value == null || value.isBlank() ? value : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
