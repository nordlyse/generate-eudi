package eu.nordlyse.eudi.web;

import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final SecurityContextRepository securityContextRepository = mock(SecurityContextRepository.class);
    private final AuthController controller = new AuthController(
            authenticationManager,
            securityContextRepository,
            new EudiProperties(
                    "https://pid.generate-eudi.example",
                    "urn:eudi:pid:1",
                    "eu.europa.ec.eudi.pid.1",
                    "eu.europa.ec.eudi.pid.1",
                    "https://pid.generate-eudi.example/trust-anchors",
                    14,
                    "issuer.jwk.json",
                    "data",
                    "https://pid.generate-eudi.example/statuslists/pid",
                    "https://pid.generate-eudi.example/catalog/vct?id=urn:eudi:pid:1",
                    new EudiProperties.Officer("officer", "secret")
            )
    );

    @Test
    void loginRejectsNullUsernameOrPasswordWithoutAuthenticating() {
        assertThatThrownBy(() -> controller.login(new LoginRequest(null, "secret"), mock(), mock()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> controller.login(new LoginRequest("officer", null), mock(), mock()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getReason())
                .isEqualTo("Username and password are required");
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void meRejectsMissingPrincipal() {
        assertThatThrownBy(() -> controller.me(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutWithoutSessionStillSignsOut() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        Map<String, String> body = controller.logout(request);

        assertThat(body).containsEntry("status", "signed_out");
    }
}
