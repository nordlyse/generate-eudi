package eu.nordlyse.eudi.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nordlyse.eudi.crypto.PortraitProcessorTest;
import eu.nordlyse.eudi.web.dto.LoginRequest;
import eu.nordlyse.eudi.web.dto.PidIssueRequest;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
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
        Cookie session = login();
        String documentNumber = "EUDI-FR-" + UUID.randomUUID().toString().substring(0, 8);
        PidIssueRequest request = sample(documentNumber);
        request.setResidentStreet("Via Appia");
        request.setResidentHouseNumber("123");
        request.setResidentCity("Rome");
        request.setResidentCountry("IT");

        MvcResult issued = mockMvc.perform(post("/api/pid")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"))
                .andExpect(jsonPath("$.statusListUri").value("https://pid.generate-eudi.example/statuslists/pid"))
                .andExpect(jsonPath("$.typeMetadataUri").isNotEmpty())
                .andExpect(jsonPath("$.issuedBy").value("officer"))
                .andExpect(jsonPath("$.mdoc.portrait_empty").value(true))
                .andExpect(jsonPath("$.mdoc.portrait").value(""))
                .andExpect(jsonPath("$.mdoc.portrait_encoding").value("bstr-base64"))
                .andReturn();

        JsonNode body = objectMapper.readTree(issued.getResponse().getContentAsString());
        assertThat(body.get("sdJwtVc").asText()).contains("~");
        assertThat(body.get("disclosures").toString()).contains("address.street_address");
        assertThat(body.get("disclosures").toString()).contains("age_equal_or_over.18");
        assertThat(body.get("mdoc").get("portrait").asText()).doesNotContain("data:image");
        assertThat(body.get("sdJwtPayloadPreview").get("status").get("status_list").has("idx")).isTrue();

        MvcResult audit = mockMvc.perform(get("/api/pid/audit").cookie(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(audit.getResponse().getContentAsString())
                .contains("ISSUE")
                .contains("officer")
                .contains(documentNumber);
    }

    @Test
    void jpegPortraitIsStoredAsRawBytesNotDataUrl() throws Exception {
        Cookie session = login();
        PidIssueRequest request = sample("EUDI-FR-JPEG-" + UUID.randomUUID().toString().substring(0, 6));
        request.setPortraitOptOut(false);
        request.setPortraitDataUrl("data:image/jpeg;base64,"
                + Base64.getEncoder().encodeToString(PortraitProcessorTest.TINY_JPEG));

        mockMvc.perform(post("/api/pid")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mdoc.portrait_empty").value(false))
                .andExpect(jsonPath("$.mdoc.portrait").value(
                        Base64.getEncoder().encodeToString(PortraitProcessorTest.TINY_JPEG)))
                .andExpect(jsonPath("$.sdJwtPayloadPreview.picture").value(
                        Matchers.startsWith("data:image/jpeg;base64,")));
    }

    @Test
    void pngPortraitIsRejected() throws Exception {
        Cookie session = login();
        PidIssueRequest request = sample("EUDI-FR-PNG");
        request.setPortraitOptOut(false);
        request.setPortraitDataUrl("data:image/png;base64,"
                + Base64.getEncoder().encodeToString(PortraitProcessorTest.PNG_HEADER));

        mockMvc.perform(post("/api/pid")
                        .cookie(session)
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
        Cookie session = login();
        mockMvc.perform(get("/api/pid/schema").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='family_name')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='trust_anchor')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='age_over_18')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='location_status')].sdJwtClaim").value("status.status_list"));
    }

    @Test
    void revokeUpdatesAuditAndFlag() throws Exception {
        Cookie session = login();
        PidIssueRequest request = sample("EUDI-FR-REV-" + UUID.randomUUID().toString().substring(0, 6));
        MvcResult issued = mockMvc.perform(post("/api/pid")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String id = objectMapper.readTree(issued.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/pid/" + id + "/revoke").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true));
    }

    private Cookie login() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("officer", "eudi-officer-2026"))))
                .andExpect(status().isOk())
                .andReturn();
        return login.getResponse().getCookie("JSESSIONID");
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
