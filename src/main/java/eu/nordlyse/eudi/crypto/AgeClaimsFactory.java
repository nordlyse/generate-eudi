package eu.nordlyse.eudi.crypto;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AgeClaimsFactory {

    public static final List<Integer> DEFAULT_THRESHOLDS = List.of(12, 14, 16, 18, 21, 65);

    private AgeClaimsFactory() {
    }

    public static AgeClaims fromBirthDate(LocalDate birthDate, LocalDate today) {
        int years = Period.between(birthDate, today).getYears();
        Map<Integer, Boolean> over = new LinkedHashMap<>();
        for (Integer threshold : DEFAULT_THRESHOLDS) {
            over.put(threshold, years >= threshold);
        }
        return new AgeClaims(years, birthDate.getYear(), over);
    }

    public record AgeClaims(int ageInYears, int ageBirthYear, Map<Integer, Boolean> ageEqualOrOver) {
    }
}
