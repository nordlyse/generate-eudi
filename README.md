# generate-eudi

Reference **Person Identification Data (PID)** issuer for the [European Digital Identity Wallet](https://ec.europa.eu/digital-building-blocks/sites/display/EUDIGITALIDENTITYWALLET/EU+Digital+Identity+Wallet+Home). Authorised officers fill CIR / ARF PID fields in English; the service returns:

- an **SD-JWT VC** (`vct`: `urn:eudi:pid:1`) with salted selective disclosures
- an **ISO/IEC 18013-5 aligned** attribute map (`docType` / namespace `eu.europa.ec.eudi.pid.1`) plus CBOR
- a **holder-specific P-256 key** bound in `cnf.jwk`, with the private JWK wrapped as **JWE** (`dir` + `A256GCM`)

This is a demonstration desk, not a notified PID Provider and not a production EUDI Wallet.

## Standards covered

| Source | What this sample implements |
| --- | --- |
| Regulation (EU) 2024/1183 | PID as the high-assurance identity attestation in the Wallet |
| CIR (EU) 2024/2977 | Mandatory/optional PID attributes and metadata |
| ARF Annex 3.01 PID Rulebook | SD-JWT claim names, mdoc identifiers, `place_of_birth`, age attestations, `trust_anchor`, `attestation_legal_category` |
| ISO/IEC 18013-5 | Namespace/docType `eu.europa.ec.eudi.pid.1` and attribute encoding (MSO/COSE is documented as out of scope for this sample) |
| SD-JWT VC | Issuer-signed JWT + `~` disclosures, `cnf` key binding, short technical validity |

Legal-person PID is out of scope of the natural-person PID Rulebook and of this sample.

## Run

Requires **Java 21** and **Maven 3.9+**.

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Demo officer credentials (change before any non-local use):

- Username: `officer`
- Password: `eudi-officer-2026`

## What happens at issuance

1. The officer authenticates.
2. CIR/ARF fields are validated (ISO country codes, sex vocabulary, portrait opt-out, jurisdiction prefix, and at least one `place_of_birth` member).
3. Age claims `age_in_years`, `age_birth_year`, and `age_equal_or_over.{12,14,16,18,21,65}` are derived from `birth_date`.
4. An EC P-256 holder key is generated. Only the public JWK is placed in `cnf`.
5. The private JWK is encrypted with a random 256-bit AES key. That wrapping key is shown **once** as the recovery secret.
6. The PID Provider signs the SD-JWT with ES256. The signing JWK is stored at `data/issuer-ec-p256.jwk.json`.

The PID private key must not be used to sign transactional data (PID Rulebook §5).

## PID fields

The in-app **PID data dictionary** lists every identifier with its SD-JWT claim and mdoc attribute. The HTTP equivalent is `GET /api/pid/schema` (officer session required).

Mandatory CIR attributes: `family_name`, `given_name`, `birth_date`, `birth_place`, `nationality`, plus metadata `issuing_authority` and `issuing_country`. Portrait is required unless the holder opts out.

## API (officer session)

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/login` | Create officer session |
| GET | `/api/pid/schema` | Full PID field catalogue |
| POST | `/api/pid` | Issue PID + encrypted holder key |
| GET | `/api/pid` | Session registry |
| GET | `/api/pid/{id}` | Issued artefacts (recovery secret is not stored) |
| POST | `/api/pid/{id}/unlock` | Unwrap holder JWK with the recovery secret |

Public: `GET /api/health`, `GET /api/meta`.

## Licenses

This project is licensed under the **Apache License 2.0**. Third-party libraries are limited to Apache-2.0 / MIT components:

- Spring Boot 3, Spring Security, Spring Validation — Apache 2.0
- Log4j2 (replaces Logback) — Apache 2.0
- Nimbus JOSE JWT — Apache 2.0
- Jackson (JSON + CBOR) — Apache 2.0

Tests use the Spring Boot test stack (JUnit is EPL 2.0). Runtime issuance does not depend on it.

## Configuration

See `src/main/resources/application.yml` (`eudi.issuer`, `eudi.vct`, officer credentials, technical validity window).

## Roadmap — what is done, what comes next

This sample already covers **natural-person PID data fields** and a teaching-grade SD-JWT. It does **not** yet produce a legally recognised PID or a Wallet Unit. Work is split into stages so later sessions can pick up one stage at a time.

Legal-person PID (company / public body identity) is **not** a missing field on this form. It is a separate attestation type with a different rulebook. It is listed as its own track at the end.

The PID holder key must never be used to sign transactional data (PID Rulebook §5). That stays true in every stage.

### Stage 0 — done (this repository)

Officer desk for a natural-person PID:

- CIR 2024/2977 + ARF PID Rulebook attributes and metadata in the UI and API
- Age claims derived from `birth_date` (`age_in_years`, `age_birth_year`, `age_equal_or_over.{12,14,16,18,21,65}`)
- SD-JWT VC (`vct`: `urn:eudi:pid:1`) with salted disclosures, `cnf.jwk`, short technical validity
- mdoc-aligned attribute map (`eu.europa.ec.eudi.pid.1`) plus CBOR hex — **attributes only**, not a full mdoc
- Server-generated P-256 holder key, public JWK in `cnf`, private JWK wrapped as JWE
- In-memory session registry; demo officer login

### Stage 1 — make the issued artefact more standard-faithful

Stay on the officer desk. Improve the document that is already produced.

1. Nested selective disclosure: disclose `address.*` and `age_equal_or_over.NN` one claim at a time, not as whole objects.
2. Portrait: accept JPEG only; store mdoc `portrait` as raw image bytes (PID_03 / ISO 39794-5 or 19794-5), not a data URL; encode opt-out as the specified empty portrait.
3. SD-JWT VC `status` (token status list or equivalent) instead of a plain `location_status` URL string.
4. Publish `vct` type metadata (`urn:eudi:pid:1` schema / display) as required by PID_15.
5. Persist issued PIDs and an officer audit log (who issued which `document_number`, when). Restart must not wipe the registry.

### Stage 2 — complete the ISO/IEC 18013-5 mdoc

CIR requires PID in **both** SD-JWT VC and mdoc. Stage 0 only maps attributes.

1. Build a real mdoc: per-element digests, Mobile Security Object (MSO), `IssuerAuth` as **COSE_Sign1**.
2. Put the holder device key in the MSO; keep namespace / `docType` `eu.europa.ec.eudi.pid.1`.
3. Encode dates as `full-date` / `tdate` per the PID Rulebook CDDL, not only ISO date strings in JSON.
4. Optional later: proximity session encryption (NFC / BLE DeviceEngagement). Needed for a phone presentation demo, not for “file on disk” issuance.

### Stage 3 — holder key is born in a wallet, not on the server

Today the server generates the holder key and shows a recovery secret. In EUDI the key is generated in the Wallet Secure Cryptographic Device (WSCD).

1. Add a minimal holder/wallet page that generates P-256 **in the browser** (or a stub WSCD) and never sends `d` to the issuer.
2. Wallet sends the public JWK plus **proof of possession** to the officer/issuer API.
3. Issuer binds that public key in `cnf` / MSO deviceKey and does not wrap a server-side private key.
4. Keep the rule: this key is only for PID key binding, not for signing contracts or payments.

### Stage 4 — issue into the wallet (OpenID4VCI)

A PID is issued when it arrives in a Wallet Unit, not when an officer downloads a file.

1. Implement OpenID4VCI credential offer + credential endpoint (pre-authorized or authorized code flow).
2. Wallet pulls the SD-JWT (and later the mdoc) over that protocol.
3. During the administrative validity period, re-issue short-lived **technical** PIDs without asking the user again (ARF distinction vs `expiry_date` / `issuance_date`).

### Stage 5 — identity proofing and issuer trust

Without this, the credential is still a demo even if Stages 1–4 are complete.

1. Bind issuance to a real identity source (civil register, notified eID, or a documented high-assurance proofing stub). Officer typing is not LoA high.
2. Issuer signing key in an HSM (or documented software-HSM stub); publish the public key as a machine-readable trust anchor.
3. Align `iss` / trust anchor with how CIR 2024/2980 trusted lists work (even if the key is not on the real EU list).
4. Revocation workflow: officer revoke → status list / MSO updates → relying party can check.

### Stage 6 — presentation and relying party (optional, but needed to prove issuance)

Issuance is only proven when someone can verify a presentation.

1. Demo relying-party verifier for SD-JWT (and mdoc if Stage 2 is done): check issuer signature, `cnf` / KB-JWT, expiry, status.
2. Selective disclosure UI in the wallet: e.g. share `age_equal_or_over.18` without `birthdate`.
3. Do not use the PID private key to sign transactional payloads from the relying party.

### Stage 7 — production EUDI Wallet Unit (out of this sample’s original scope)

Only after Stages 3–6. This is a certified product, not a README checkbox:

- WSCD / hardware-backed keys
- OpenID4VP + ISO 18013-5 proximity
- QEAA / Pub-EAA besides PID
- Unlinkability, consent, RP authentication
- Wallet and PID Provider certification / Member State notification

This repository will not become a notified Wallet or PID Provider by completing Stages 1–6 alone.

### Separate track — legal-person identification data

Not part of the natural-person PID form.

1. New rulebook / attribute set (legal name, registration number, VAT, seat, representative, …).
2. Separate `vct` / mdoc namespace from `urn:eudi:pid:1`.
3. Reuse Stages 1–6 patterns (dual encoding, wallet key, OpenID4VCI, trusted issuer) on that type.

### Suggested order of work

| Next session | Stage | Outcome |
| --- | --- | --- |
| 1 | Stage 1 | Better SD-JWT, portrait, status, persistence |
| 2 | Stage 2 | Verifiable mdoc file, not only a CBOR attribute map |
| 3 | Stage 3 + 4 | Key in a demo wallet; credential delivered by OpenID4VCI |
| 4 | Stage 5 + 6 | Proofing/trust stub + a verifier that accepts a presentation |
| later | Stage 7 / legal person | Only if the goal is a Wallet product or organisation identity |
