package eu.nordlyse.eudi.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record ResidentAddress(
        String formatted,
        String country,
        String state,
        String city,
        String postalCode,
        String street,
        String houseNumber
) {
    public boolean hasAnyValue() {
        return notBlank(formatted)
                || notBlank(country)
                || notBlank(state)
                || notBlank(city)
                || notBlank(postalCode)
                || notBlank(street)
                || notBlank(houseNumber);
    }

    public Map<String, String> asSdJwtAddress() {
        Map<String, String> map = new LinkedHashMap<>();
        if (notBlank(formatted)) {
            map.put("formatted", formatted.trim());
        }
        if (notBlank(street)) {
            map.put("street_address", street.trim());
        }
        if (notBlank(houseNumber)) {
            map.put("house_number", houseNumber.trim());
        }
        if (notBlank(city)) {
            map.put("locality", city.trim());
        }
        if (notBlank(state)) {
            map.put("region", state.trim());
        }
        if (notBlank(postalCode)) {
            map.put("postal_code", postalCode.trim());
        }
        if (notBlank(country)) {
            map.put("country", country.trim().toUpperCase());
        }
        return map;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
