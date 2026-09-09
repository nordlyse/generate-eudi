package eu.nordlyse.eudi.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PlaceOfBirth(String country, String region, String locality) {

    public boolean hasAnyValue() {
        return notBlank(country) || notBlank(region) || notBlank(locality);
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (notBlank(country)) {
            map.put("country", country.trim().toUpperCase());
        }
        if (notBlank(region)) {
            map.put("region", region.trim());
        }
        if (notBlank(locality)) {
            map.put("locality", locality.trim());
        }
        return map;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
