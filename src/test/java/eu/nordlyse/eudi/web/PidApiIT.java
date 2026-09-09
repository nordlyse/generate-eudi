package eu.nordlyse.eudi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nordlyse.eudi.web.dto.LoginRequest;
import eu.nordlyse.eudi.web.dto.PidIssueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "eudi.issuer-key-file=target/test-issuer.jwk.json")
class PidApiIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void officerCanIssuePidWithEncryptedHolderKey() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("officer", "eudi-officer-2026"))))
                .andExpect(status().isOk())
                .andReturn();
        String session = login.getResponse().getCookie("JSESSIONID").getValue();

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

        MvcResult issued = mockMvc.perform(post("/api/pid")
                        .cookie(login.getResponse().getCookie("JSESSIONID"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vct").value("urn:eudi:pid:1"))
                .andExpect(jsonPath("$.sdJwtVc").isNotEmpty())
                .andExpect(jsonPath("$.encryptedHolderKey").isNotEmpty())
                .andExpect(jsonPath("$.holderKeyRecoverySecret").isNotEmpty())
                .andExpect(jsonPath("$.mdoc.docType").value("eu.europa.ec.eudi.pid.1"))
                .andReturn();

        String body = issued.getResponse().getContentAsString();
        assertThat(body).contains("age_equal_or_over");
        assertThat(session).isNotBlank();
    }

    @Test
    void schemaListsCirAndRulebookFields() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("officer", "eudi-officer-2026"))))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/pid/schema")
                        .cookie(login.getResponse().getCookie("JSESSIONID")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='family_name')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='trust_anchor')]").exists())
                .andExpect(jsonPath("$[?(@.cirIdentifier=='age_over_18')]").exists());
    }
}
