package eu.nordlyse.eudi.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nordlyse.eudi.crypto.PortraitProcessorTest;
import eu.nordlyse.eudi.web.dto.LoginRequest;
import eu.nordlyse.eudi.web.dto.PidIssueRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eudi.issuer-key-file=target/stage1-it/issuer.jwk.json",
        "eudi.data-directory=target/stage1-it/data"
})
class PidApiIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void officerCanIssueStandardFaithfulPid() throws Exception {
        MockHttpSession session = login();
        String documentNumber = "EUDI-FR-" + UUID.randomUUID().toString().substring(0, 8);
        PidIssueRequest request = sample(documentNumber);
        request.setResidentStreet("Via Appia");
        request.setResidentHouseNumber("123");
        request.setResidentCity("Rome");
        request.setResidentCountry("IT");

        MvcResult issued = mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"))
                .andExpect(jsonPath("$.statusListUri").value("https://pid.generate-eudi.example/statuslists/pid"))
                .andExpect(jsonPath("$.typeMetadataUri").isNotEmpty())
                .andExpect(jsonPath("$.issuedBy").value("officer"))
                .andExpect(jsonPath("$.mdoc.nameSpaces['eu.europa.ec.eudi.pid.1'].portrait_empty").value(true))
                .andExpect(jsonPath("$.mdoc.nameSpaces['eu.europa.ec.eudi.pid.1'].portrait").value(""))
                .andExpect(jsonPath("$.mdoc.nameSpaces['eu.europa.ec.eudi.pid.1'].portrait_encoding").value("bstr-base64"))
                .andReturn();

        JsonNode body = objectMapper.readTree(issued.getResponse().getContentAsString());
        assertThat(body.get("sdJwtVc").asText()).contains("~");
        assertThat(body.get("disclosures").toString()).contains("address.street_address");
        assertThat(body.get("disclosures").toString()).contains("age_equal_or_over.18");
        assertThat(body.get("mdoc").get("nameSpaces").get("eu.europa.ec.eudi.pid.1").get("portrait").asText())
                .doesNotContain("data:image");
        assertThat(body.get("sdJwtPayloadPreview").get("status").get("status_list").has("idx")).isTrue();

        MvcResult audit = mockMvc.perform(get("/api/pid/audit").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(audit.getResponse().getContentAsString())
                .contains("ISSUE")
                .contains("officer")
                .contains(documentNumber);
    }

    @Test
    void jpegPortraitIsStoredAsRawBytesNotDataUrl() throws Exception {
        MockHttpSession session = login();
        PidIssueRequest request = sample("EUDI-FR-JPEG-" + UUID.randomUUID().toString().substring(0, 6));
        request.setPortraitOptOut(false);
        request.setPortraitDataUrl("data:image/jpeg;base64,"
                + Base64.getEncoder().encodeToString(PortraitProcessorTest.TINY_JPEG));

        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mdoc.nameSpaces['eu.europa.ec.eudi.pid.1'].portrait_empty").value(false))
                .andExpect(jsonPath("$.mdoc.nameSpaces['eu.europa.ec.eudi.pid.1'].portrait").value(
                        Base64.getEncoder().encodeToString(PortraitProcessorTest.TINY_JPEG)))
                .andExpect(jsonPath("$.sdJwtPayloadPreview.picture").value(
                        Matchers.startsWith("data:image/jpeg;base64,")));
    }

    @Test
    void pngPortraitIsRejected() throws Exception {
        MockHttpSession session = login();
        PidIssueRequest request = sample("EUDI-FR-PNG");
        request.setPortraitOptOut(false);
        request.setPortraitDataUrl("data:image/png;base64,"
                + Base64.getEncoder().encodeToString(PortraitProcessorTest.PNG_HEADER));

        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void typeMetadataAndStatusListArePublic() throws Exception {
        mockMvc.perform(get("/catalog/vct").param("id", "urn:eudi:pid:1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"))
                .andExpect(jsonPath("$.claims").isArray())
                .andExpect(jsonPath("$.display[0].lang").value("en"));

        mockMvc.perform(get("/.well-known/jwt-vc-issuer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("https://pid.generate-eudi.example"))
                .andExpect(jsonPath("$.jwks.keys[0].kty").value("EC"));

        mockMvc.perform(get("/statuslists/pid.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encoding").value("ietf-token-status-list"));

        mockMvc.perform(get("/statuslists/pid"))
                .andExpect(status().isOk());
    }

    @Test
    void schemaListsCirAndRulebookFields() throws Exception {
        MockHttpSession session = login();
        mockMvc.perform(get("/api/pid/schema").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='family_name')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='trust_anchor')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='age_over_18')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='location_status')].sdJwtClaim").value("status.status_list"));
    }

    @Test
    void revokeUpdatesAuditAndFlag() throws Exception {
        MockHttpSession session = login();
        JsonNode issued = issue(session, sample("EUDI-FR-REV-" + UUID.randomUUID().toString().substring(0, 6)));
        String id = issued.get("id").asText();

        mockMvc.perform(post("/api/pid/" + id + "/revoke").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true));
    }

    @Test
    void healthAndMetaArePublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("generate-eudi"));

        mockMvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("generate-eudi"))
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"))
                .andExpect(jsonPath("$.mdocDocType").value("eu.europa.ec.eudi.pid.1"))
                .andExpect(jsonPath("$.standards").isArray());
    }

    @Test
    void unauthenticatedOfficerRoutesAreRejected() throws Exception {
        mockMvc.perform(get("/api/pid"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pid/schema"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/pid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listGetAndStatsExposeIssuedPidWithoutRecoverySecret() throws Exception {
        MockHttpSession session = login();
        JsonNode issued = issue(session, sample("EUDI-FR-GET-" + UUID.randomUUID().toString().substring(0, 6)));
        String id = issued.get("id").asText();
        assertThat(issued.get("holderKeyRecoverySecret").asText()).isNotBlank();

        MvcResult listed = mockMvc.perform(get("/api/pid").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listedPid = null;
        for (JsonNode item : objectMapper.readTree(listed.getResponse().getContentAsString())) {
            if (id.equals(item.get("id").asText())) {
                listedPid = item;
                break;
            }
        }
        assertThat(listedPid).isNotNull();
        assertThat(listedPid.has("holderKeyRecoverySecret")).isFalse();
        assertThat(listedPid.has("sdJwtVc")).isFalse();

        mockMvc.perform(get("/api/pid/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sdJwtVc").isNotEmpty())
                .andExpect(jsonPath("$.encryptedHolderKey").isNotEmpty())
                .andExpect(jsonPath("$.holderKeyRecoverySecret").doesNotExist());

        mockMvc.perform(get("/api/pid/stats").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuedCount").isNumber())
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"))
                .andExpect(jsonPath("$.statusListUri").isNotEmpty());
    }

    @Test
    void unlockReturnsPrivateJwkAndRejectsWrongSecret() throws Exception {
        MockHttpSession session = login();
        JsonNode issued = issue(session, sample("EUDI-FR-UNL-" + UUID.randomUUID().toString().substring(0, 6)));
        String id = issued.get("id").asText();
        String secret = issued.get("holderKeyRecoverySecret").asText();

        mockMvc.perform(post("/api/pid/" + id + "/unlock")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("recoverySecret", secret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kty").value("EC"))
                .andExpect(jsonPath("$.crv").value("P-256"))
                .andExpect(jsonPath("$.d").isNotEmpty());

        mockMvc.perform(post("/api/pid/" + id + "/unlock")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("recoverySecret", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void missingPidIsNotFoundForGetUnlockAndRevoke() throws Exception {
        MockHttpSession session = login();
        String missing = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/pid/" + missing).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("PID not found"));

        mockMvc.perform(post("/api/pid/" + missing + "/unlock")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("recoverySecret", "unused"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("PID not found"));

        mockMvc.perform(post("/api/pid/" + missing + "/revoke").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("PID not found"));
    }

    @Test
    void issueRejectsValidationAndRulebookErrors() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        PidIssueRequest emptyPlace = sample("EUDI-FR-PLACE");
        emptyPlace.getPlaceOfBirth().setCountry("");
        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyPlace)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("birth_place")));

        PidIssueRequest badSex = sample("EUDI-FR-SEX");
        badSex.setSex(7);
        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badSex)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("sex must be one of")));

        PidIssueRequest badExpiry = sample("EUDI-FR-EXP");
        badExpiry.setIssuanceDate(LocalDate.of(2026, 1, 10));
        badExpiry.setExpiryDate(LocalDate.of(2026, 1, 1));
        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badExpiry)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("expiry_date cannot be before issuance_date"));

        PidIssueRequest badJurisdiction = sample("EUDI-FR-JUR");
        badJurisdiction.setIssuingJurisdiction("DE-BE");
        mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badJurisdiction)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("issuing_jurisdiction must start with issuing_country"));
    }

    @Test
    void unknownTypeMetadataIsNotFoundAndDefaultVctIsServed() throws Exception {
        mockMvc.perform(get("/catalog/vct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"));

        mockMvc.perform(get("/catalog/vct").param("id", "urn:eudi:pid:999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Unknown vct"));
    }

    private JsonNode issue(MockHttpSession session, PidIssueRequest request) throws Exception {
        MvcResult issued = mockMvc.perform(post("/api/pid")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(issued.getResponse().getContentAsString());
    }

    private MockHttpSession login() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("officer", "eudi-officer-2026"))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession();
    }

    private static PidIssueRequest sample(String documentNumber) {
        PidIssueRequest request = new PidIssueRequest();
        request.setFamilyName("Dupont");
        request.setGivenName("Jean");
        request.setBirthDate(LocalDate.of(1980, 5, 23));
        PidIssueRequest.PlaceOfBirthRequest place = new PidIssueRequest.PlaceOfBirthRequest();
        place.setCountry("FR");
        request.setPlaceOfBirth(place);
        request.setNationalities(List.of("FR"));
        request.setPortraitOptOut(true);
        request.setIssuingAuthority("ANTS");
        request.setIssuingCountry("FR");
        request.setIssuingJurisdiction("FR-IDF");
        request.setEmailAddress("jean.dupont@example.eu");
        request.setMobilePhoneNumber("+33123456789");
        request.setSex(1);
        request.setDocumentNumber(documentNumber);
        return request;
    }
}
