package eu.nordlyse.eudi.crypto;

import eu.nordlyse.eudi.domain.PlaceOfBirth;
import eu.nordlyse.eudi.domain.ResidentAddress;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgeClaimsFactoryTest {

    @Test
    void computesAdultThresholdsFromBirthDate() {
        AgeClaimsFactory.AgeClaims claims = AgeClaimsFactory.fromBirthDate(
                LocalDate.of(1990, 5, 23),
                LocalDate.of(2026, 9, 9)
        );
        assertThat(claims.ageInYears()).isEqualTo(36);
        assertThat(claims.ageBirthYear()).isEqualTo(1990);
        assertThat(claims.ageEqualOrOver().get(18)).isTrue();
        assertThat(claims.ageEqualOrOver().get(65)).isFalse();
    }

    @Test
    void placeOfBirthRequiresAtLeastOneMember() {
        assertThat(new PlaceOfBirth(null, null, null).hasAnyValue()).isFalse();
        assertThat(new PlaceOfBirth("FR", null, null).asMap()).containsEntry("country", "FR");
    }

    @Test
    void residenceMapsToSdJwtAddressClaims() {
        ResidentAddress address = new ResidentAddress(
                "123 Via Appia, Rome",
                "IT",
                "Lazio",
                "Rome",
                "00100",
                "Via Appia",
                "123"
        );
        Map<String, String> mapped = address.asSdJwtAddress();
        assertThat(mapped)
                .containsEntry("country", "IT")
                .containsEntry("house_number", "123")
                .containsEntry("street_address", "Via Appia");
    }
}
