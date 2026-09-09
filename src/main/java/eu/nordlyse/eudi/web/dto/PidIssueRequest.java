package eu.nordlyse.eudi.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PidIssueRequest {

    @NotBlank
    @Size(max = 200)
    private String familyName;

    @NotBlank
    @Size(max = 200)
    private String givenName;

    @NotNull
    private LocalDate birthDate;

    @Valid
    @NotNull
    private PlaceOfBirthRequest placeOfBirth = new PlaceOfBirthRequest();

    @NotNull
    @Size(min = 1)
    private List<@Pattern(regexp = "^[A-Z]{2}$") String> nationalities = new ArrayList<>();

    private String residentAddress;
    @Pattern(regexp = "^$|^[A-Z]{2}$")
    private String residentCountry;
    private String residentState;
    private String residentCity;
    private String residentPostalCode;
    private String residentStreet;
    private String residentHouseNumber;

    @Size(max = 100)
    private String personalAdministrativeNumber;

    private String portraitDataUrl;
    private boolean portraitOptOut;

    private String familyNameBirth;
    private String givenNameBirth;
    private Integer sex;

    @Email
    private String emailAddress;

    @Pattern(regexp = "^$|^\\+[1-9][0-9]{6,14}$")
    private String mobilePhoneNumber;

    private LocalDate expiryDate;

    @NotBlank
    private String issuingAuthority;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$")
    private String issuingCountry;

    private String documentNumber;

    @Pattern(regexp = "^$|^[A-Z]{2}(-[A-Z0-9]{1,8})?$")
    private String issuingJurisdiction;

    private String locationStatus;
    private LocalDate issuanceDate;
    private Map<Integer, Boolean> ageEqualOrOver;
    private Integer ageInYears;
    private Integer ageBirthYear;
    private String trustAnchor;
    private String attestationLegalCategory = "PID";

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public PlaceOfBirthRequest getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(PlaceOfBirthRequest placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public List<String> getNationalities() {
        return nationalities;
    }

    public void setNationalities(List<String> nationalities) {
        this.nationalities = nationalities;
    }

    public String getResidentAddress() {
        return residentAddress;
    }

    public void setResidentAddress(String residentAddress) {
        this.residentAddress = residentAddress;
    }

    public String getResidentCountry() {
        return residentCountry;
    }

    public void setResidentCountry(String residentCountry) {
        this.residentCountry = residentCountry;
    }

    public String getResidentState() {
        return residentState;
    }

    public void setResidentState(String residentState) {
        this.residentState = residentState;
    }

    public String getResidentCity() {
        return residentCity;
    }

    public void setResidentCity(String residentCity) {
        this.residentCity = residentCity;
    }

    public String getResidentPostalCode() {
        return residentPostalCode;
    }

    public void setResidentPostalCode(String residentPostalCode) {
        this.residentPostalCode = residentPostalCode;
    }

    public String getResidentStreet() {
        return residentStreet;
    }

    public void setResidentStreet(String residentStreet) {
        this.residentStreet = residentStreet;
    }

    public String getResidentHouseNumber() {
        return residentHouseNumber;
    }

    public void setResidentHouseNumber(String residentHouseNumber) {
        this.residentHouseNumber = residentHouseNumber;
    }

    public String getPersonalAdministrativeNumber() {
        return personalAdministrativeNumber;
    }

    public void setPersonalAdministrativeNumber(String personalAdministrativeNumber) {
        this.personalAdministrativeNumber = personalAdministrativeNumber;
    }

    public String getPortraitDataUrl() {
        return portraitDataUrl;
    }

    public void setPortraitDataUrl(String portraitDataUrl) {
        this.portraitDataUrl = portraitDataUrl;
    }

    public boolean isPortraitOptOut() {
        return portraitOptOut;
    }

    public void setPortraitOptOut(boolean portraitOptOut) {
        this.portraitOptOut = portraitOptOut;
    }

    public String getFamilyNameBirth() {
        return familyNameBirth;
    }

    public void setFamilyNameBirth(String familyNameBirth) {
        this.familyNameBirth = familyNameBirth;
    }

    public String getGivenNameBirth() {
        return givenNameBirth;
    }

    public void setGivenNameBirth(String givenNameBirth) {
        this.givenNameBirth = givenNameBirth;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getMobilePhoneNumber() {
        return mobilePhoneNumber;
    }

    public void setMobilePhoneNumber(String mobilePhoneNumber) {
        this.mobilePhoneNumber = mobilePhoneNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public String getIssuingCountry() {
        return issuingCountry;
    }

    public void setIssuingCountry(String issuingCountry) {
        this.issuingCountry = issuingCountry;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getIssuingJurisdiction() {
        return issuingJurisdiction;
    }

    public void setIssuingJurisdiction(String issuingJurisdiction) {
        this.issuingJurisdiction = issuingJurisdiction;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public LocalDate getIssuanceDate() {
        return issuanceDate;
    }

    public void setIssuanceDate(LocalDate issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    public Map<Integer, Boolean> getAgeEqualOrOver() {
        return ageEqualOrOver;
    }

    public void setAgeEqualOrOver(Map<Integer, Boolean> ageEqualOrOver) {
        this.ageEqualOrOver = ageEqualOrOver;
    }

    public Integer getAgeInYears() {
        return ageInYears;
    }

    public void setAgeInYears(Integer ageInYears) {
        this.ageInYears = ageInYears;
    }

    public Integer getAgeBirthYear() {
        return ageBirthYear;
    }

    public void setAgeBirthYear(Integer ageBirthYear) {
        this.ageBirthYear = ageBirthYear;
    }

    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    public String getAttestationLegalCategory() {
        return attestationLegalCategory;
    }

    public void setAttestationLegalCategory(String attestationLegalCategory) {
        this.attestationLegalCategory = attestationLegalCategory;
    }

    public static class PlaceOfBirthRequest {
        @Pattern(regexp = "^$|^[A-Z]{2}$")
        private String country;
        private String region;
        private String locality;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getLocality() {
            return locality;
        }

        public void setLocality(String locality) {
            this.locality = locality;
        }
    }
}
