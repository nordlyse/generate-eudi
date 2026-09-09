const EU_COUNTRIES = [
    ["AT", "Austria"], ["BE", "Belgium"], ["BG", "Bulgaria"], ["HR", "Croatia"],
    ["CY", "Cyprus"], ["CZ", "Czechia"], ["DK", "Denmark"], ["EE", "Estonia"],
    ["FI", "Finland"], ["FR", "France"], ["DE", "Germany"], ["GR", "Greece"],
    ["HU", "Hungary"], ["IE", "Ireland"], ["IT", "Italy"], ["LV", "Latvia"],
    ["LT", "Lithuania"], ["LU", "Luxembourg"], ["MT", "Malta"], ["NL", "Netherlands"],
    ["PL", "Poland"], ["PT", "Portugal"], ["RO", "Romania"], ["SK", "Slovakia"],
    ["SI", "Slovenia"], ["ES", "Spain"], ["SE", "Sweden"], ["IS", "Iceland"],
    ["LI", "Liechtenstein"], ["NO", "Norway"], ["CH", "Switzerland"],
    ["QU", "Unknown nationality (QU)"], ["QS", "Stateless (QS)"]
];

const SECTIONS = [
    ["identity", "Holder identity"],
    ["birth", "Birth & nationality"],
    ["residence", "Residence"],
    ["civil", "Civil & contact"],
    ["issuance", "Issuance metadata"],
    ["age", "Age attestations"],
    ["portrait", "Portrait"],
    ["review", "Review & issue"]
];

const state = {
    user: null,
    view: "issue",
    section: "identity",
    error: "",
    schema: [],
    stats: {},
    list: [],
    result: null,
    resultTab: "overview",
    form: blankForm()
};

function blankForm() {
    const today = new Date().toISOString().slice(0, 10);
    const expiry = new Date();
    expiry.setFullYear(expiry.getFullYear() + 10);
    return {
        familyName: "",
        givenName: "",
        birthDate: "",
        placeOfBirth: { country: "", region: "", locality: "" },
        nationalities: "FR",
        residentAddress: "",
        residentCountry: "",
        residentState: "",
        residentCity: "",
        residentPostalCode: "",
        residentStreet: "",
        residentHouseNumber: "",
        personalAdministrativeNumber: "",
        portraitDataUrl: "",
        portraitOptOut: false,
        familyNameBirth: "",
        givenNameBirth: "",
        sex: "",
        emailAddress: "",
        mobilePhoneNumber: "",
        expiryDate: expiry.toISOString().slice(0, 10),
        issuingAuthority: "Demonstration PID Provider",
        issuingCountry: "FR",
        documentNumber: "",
        issuingJurisdiction: "FR-IDF",
        locationStatus: "https://pid.generate-eudi.example/status",
        issuanceDate: today,
        trustAnchor: "https://pid.generate-eudi.example/trust-anchors",
        attestationLegalCategory: "PID"
    };
}

function sampleForm() {
    return {
        ...blankForm(),
        familyName: "Dupont",
        givenName: "Jean",
        birthDate: "1980-05-23",
        placeOfBirth: { country: "FR", region: "Île-de-France", locality: "Paris" },
        nationalities: "FR",
        residentAddress: "123 Via Appia, 00100 Rome, Italy",
        residentCountry: "IT",
        residentState: "Lazio",
        residentCity: "Rome",
        residentPostalCode: "00100",
        residentStreet: "Via Appia",
        residentHouseNumber: "123",
        personalAdministrativeNumber: "FR-1980-000441",
        familyNameBirth: "Dupont",
        givenNameBirth: "Jean",
        sex: "1",
        emailAddress: "jean.dupont@example.eu",
        mobilePhoneNumber: "+33123456789",
        issuingAuthority: "Agence Nationale des Titres Sécurisés",
        issuingCountry: "FR",
        issuingJurisdiction: "FR-IDF",
        portraitOptOut: true
    };
}

document.addEventListener("mousemove", (event) => {
    const spot = document.getElementById("spotlight");
    spot.style.left = event.clientX + "px";
    spot.style.top = event.clientY + "px";
});

async function api(path, options = {}) {
    const response = await fetch(path, {
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || data.error || "Request failed");
    }
    return data;
}

async function boot() {
    try {
        state.user = await api("/api/auth/me");
        await loadWorkspace();
    } catch {
        state.user = null;
    }
    render();
}

async function loadWorkspace() {
    const [schema, stats, list] = await Promise.all([
        api("/api/pid/schema"),
        api("/api/pid/stats"),
        api("/api/pid")
    ]);
    state.schema = schema;
    state.stats = stats;
    state.list = list;
}

function el(html) {
    const template = document.createElement("template");
    template.innerHTML = html.trim();
    return template.content;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function countryOptions(selected) {
    return EU_COUNTRIES.map(([code, name]) =>
        `<option value="${code}" ${selected === code ? "selected" : ""}>${code} — ${name}</option>`
    ).join("");
}

function badge(kind) {
    if (kind === "req") return `<span class="badge req">Mandatory</span>`;
    return `<span class="badge opt">Optional</span>`;
}

function field(id, label, kind, control) {
    return `<div>
        <div class="field-head"><label for="${id}">${label}</label>${badge(kind)}</div>
        ${control}
    </div>`;
}

function render() {
    const app = document.getElementById("app");
    app.innerHTML = "";
    app.append(state.user ? appShell() : loginView());
    bind();
}

function loginView() {
    return el(`
        <div class="wrap">
            ${topbar(false)}
            <section class="hero">
                <div class="kicker">EUDI Wallet · PID Rulebook · CIR 2024/2977</div>
                <h2>Issue a <span class="gradient-text">European Digital Identity</span></h2>
                <p class="lede">An authorised officer console for preparing Person Identification Data in SD-JWT VC and ISO/IEC 18013-5 aligned encodings, then sealing a holder-specific encrypted key.</p>
            </section>
            <form class="glass login-card" id="login-form">
                <div class="chip-row">
                    <span class="chip glow">Officer access</span>
                    <span class="chip">Demo issuer</span>
                </div>
                <label for="username">Username</label>
                <input id="username" name="username" autocomplete="username" value="officer">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="current-password" value="eudi-officer-2026">
                <p class="error" id="login-error"></p>
                <button class="btn full" type="submit">Enter the PID desk</button>
            </form>
        </div>
    `);
}

function topbar(signedIn) {
    return `
        <header class="topbar">
            <div class="brand">
                <div class="logo" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 64 64" fill="none">
                        <circle cx="32" cy="32" r="16" stroke="#7c5cff" stroke-width="3"/>
                        <circle cx="32" cy="32" r="4" fill="#5eead4"/>
                    </svg>
                </div>
                <div>
                    <h1>generate-eudi</h1>
                    <p>European Digital Identity PID issuer</p>
                </div>
            </div>
            <div class="chip-row">
                <span class="chip">vct urn:eudi:pid:1</span>
                <span class="chip">eu.europa.ec.eudi.pid.1</span>
                ${signedIn ? `<span class="chip glow">${escapeHtml(state.user.username)}</span>
                <button class="btn ghost" id="logout" type="button">Sign out</button>` : ""}
            </div>
        </header>
    `;
}

function appShell() {
    return el(`
        <div class="wrap">
            ${topbar(true)}
            <div class="stat-grid">
                <div class="glass stat"><b>${state.stats.issuedCount ?? 0}</b><span>PIDs issued this session</span></div>
                <div class="glass stat"><b>ES256</b><span>Issuer & holder P-256 keys</span></div>
                <div class="glass stat"><b>SD-JWT + mdoc</b><span>Dual encoding as required by CIR</span></div>
            </div>
            <div class="view-tabs">
                <button class="tab ${state.view === "issue" ? "active" : ""}" data-view="issue">Issue PID</button>
                <button class="tab ${state.view === "registry" ? "active" : ""}" data-view="registry">Issued registry</button>
                <button class="tab ${state.view === "schema" ? "active" : ""}" data-view="schema">PID data dictionary</button>
            </div>
            ${state.view === "issue" ? issueView() : state.view === "registry" ? registryView() : schemaView()}
        </div>
    `);
}

function issueView() {
    if (state.result) return resultView();
    return `
        <div class="shell">
            <aside class="side glass panel">
                ${SECTIONS.map(([id, label], index) => `
                    <button class="nav-btn ${state.section === id ? "active" : ""}" data-section="${id}" type="button">
                        <span class="n">${index + 1}</span>${label}
                    </button>
                `).join("")}
                <div class="actions">
                    <button class="btn ghost" id="sample" type="button">Load sample</button>
                    <button class="btn ghost" id="reset" type="button">Reset</button>
                </div>
            </aside>
            <section class="glass panel">${sectionForm()}</section>
            <aside class="preview glass id-card">
                <div class="eu">European Union · PID</div>
                ${stars()}
                <div style="display:flex;gap:12px;align-items:flex-start;margin-top:8px">
                    ${state.form.portraitDataUrl && !state.form.portraitOptOut
                        ? `<img class="portrait-preview" src="${state.form.portraitDataUrl}" alt="Portrait preview">`
                        : `<div class="portrait-preview"></div>`}
                    <div>
                        <h4>${escapeHtml(state.form.givenName || "Given name")} ${escapeHtml(state.form.familyName || "Family name")}</h4>
                        <div class="meta">
                            Birth ${escapeHtml(state.form.birthDate || "YYYY-MM-DD")}<br>
                            Nationality ${escapeHtml(state.form.nationalities || "—")}<br>
                            Issuer ${escapeHtml(state.form.issuingCountry || "—")} · ${escapeHtml(state.form.issuingAuthority || "")}
                        </div>
                    </div>
                </div>
                <p class="meta" style="margin-top:18px">Technical PIDs are short-lived. Administrative validity is separate (issuance / expiry dates).</p>
            </aside>
        </div>
    `;
}

function stars() {
    return `<svg class="stars" width="54" height="54" viewBox="0 0 54 54" fill="#fbbf24" aria-hidden="true">
        <g transform="translate(27 27)">
            ${Array.from({ length: 12 }, (_, i) => {
                const a = (i * 30) * Math.PI / 180;
                return `<circle cx="${Math.cos(a) * 16}" cy="${Math.sin(a) * 16}" r="1.6"/>`;
            }).join("")}
        </g>
    </svg>`;
}

function sectionForm() {
    const f = state.form;
    const forms = {
        identity: `
            <h3>Holder identity</h3>
            <p class="hint">CIR 2024/2977 mandatory attributes: family_name, given_name, birth_date.</p>
            <div class="grid-2">
                ${field("givenName", "given_name", "req", `<input id="givenName" value="${escapeHtml(f.givenName)}">`)}
                ${field("familyName", "family_name", "req", `<input id="familyName" value="${escapeHtml(f.familyName)}">`)}
            </div>
            ${field("birthDate", "birth_date", "req", `<input id="birthDate" type="date" value="${escapeHtml(f.birthDate)}">`)}
        `,
        birth: `
            <h3>Birth & nationality</h3>
            <p class="hint">place_of_birth needs at least one of country, region or locality. Nationality uses ISO 3166-1 alpha-2 codes.</p>
            <div class="grid-2">
                ${field("pobCountry", "place_of_birth.country", "req", `<select id="pobCountry"><option value=""></option>${countryOptions(f.placeOfBirth.country)}</select>`)}
                ${field("pobRegion", "place_of_birth.region", "opt", `<input id="pobRegion" value="${escapeHtml(f.placeOfBirth.region)}">`)}
            </div>
            ${field("pobLocality", "place_of_birth.locality", "opt", `<input id="pobLocality" value="${escapeHtml(f.placeOfBirth.locality)}">`)}
            ${field("nationalities", "nationality / nationalities", "req", `<input id="nationalities" value="${escapeHtml(f.nationalities)}" placeholder="FR,BE">`)}
        `,
        residence: `
            <h3>Residence</h3>
            <p class="hint">Optional CIR Table 2 address attributes, encoded as OpenID address claims in SD-JWT VC.</p>
            ${field("residentAddress", "resident_address", "opt", `<input id="residentAddress" value="${escapeHtml(f.residentAddress)}">`)}
            <div class="grid-2">
                ${field("residentStreet", "resident_street", "opt", `<input id="residentStreet" value="${escapeHtml(f.residentStreet)}">`)}
                ${field("residentHouseNumber", "resident_house_number", "opt", `<input id="residentHouseNumber" value="${escapeHtml(f.residentHouseNumber)}">`)}
                ${field("residentCity", "resident_city", "opt", `<input id="residentCity" value="${escapeHtml(f.residentCity)}">`)}
                ${field("residentPostalCode", "resident_postal_code", "opt", `<input id="residentPostalCode" value="${escapeHtml(f.residentPostalCode)}">`)}
                ${field("residentState", "resident_state", "opt", `<input id="residentState" value="${escapeHtml(f.residentState)}">`)}
                ${field("residentCountry", "resident_country", "opt", `<select id="residentCountry"><option value=""></option>${countryOptions(f.residentCountry)}</select>`)}
            </div>
        `,
        civil: `
            <h3>Civil status & contact</h3>
            <p class="hint">Optional attributes including ISO/IEC 5218-compatible sex codes extended by CIR.</p>
            <div class="grid-2">
                ${field("givenNameBirth", "given_name_birth", "opt", `<input id="givenNameBirth" value="${escapeHtml(f.givenNameBirth)}">`)}
                ${field("familyNameBirth", "family_name_birth", "opt", `<input id="familyNameBirth" value="${escapeHtml(f.familyNameBirth)}">`)}
                ${field("sex", "sex", "opt", `<select id="sex">
                    <option value=""></option>
                    <option value="0" ${f.sex === "0" ? "selected" : ""}>0 — not known</option>
                    <option value="1" ${f.sex === "1" ? "selected" : ""}>1 — male</option>
                    <option value="2" ${f.sex === "2" ? "selected" : ""}>2 — female</option>
                    <option value="3" ${f.sex === "3" ? "selected" : ""}>3 — other</option>
                    <option value="4" ${f.sex === "4" ? "selected" : ""}>4 — inter</option>
                    <option value="5" ${f.sex === "5" ? "selected" : ""}>5 — diverse</option>
                    <option value="6" ${f.sex === "6" ? "selected" : ""}>6 — open</option>
                    <option value="9" ${f.sex === "9" ? "selected" : ""}>9 — not applicable</option>
                </select>`)}
                ${field("personalAdministrativeNumber", "personal_administrative_number", "opt", `<input id="personalAdministrativeNumber" value="${escapeHtml(f.personalAdministrativeNumber)}">`)}
                ${field("emailAddress", "email_address", "opt", `<input id="emailAddress" type="email" value="${escapeHtml(f.emailAddress)}">`)}
                ${field("mobilePhoneNumber", "mobile_phone_number", "opt", `<input id="mobilePhoneNumber" placeholder="+33612345678" value="${escapeHtml(f.mobilePhoneNumber)}">`)}
            </div>
        `,
        issuance: `
            <h3>Issuance metadata</h3>
            <p class="hint">issuing_authority and issuing_country are mandatory. Administrative dates differ from the short technical validity of the SD-JWT.</p>
            ${field("issuingAuthority", "issuing_authority", "req", `<input id="issuingAuthority" value="${escapeHtml(f.issuingAuthority)}">`)}
            <div class="grid-2">
                ${field("issuingCountry", "issuing_country", "req", `<select id="issuingCountry">${countryOptions(f.issuingCountry)}</select>`)}
                ${field("issuingJurisdiction", "issuing_jurisdiction", "opt", `<input id="issuingJurisdiction" value="${escapeHtml(f.issuingJurisdiction)}">`)}
                ${field("issuanceDate", "issuance_date", "opt", `<input id="issuanceDate" type="date" value="${escapeHtml(f.issuanceDate)}">`)}
                ${field("expiryDate", "expiry_date", "opt", `<input id="expiryDate" type="date" value="${escapeHtml(f.expiryDate)}">`)}
                ${field("documentNumber", "document_number", "opt", `<input id="documentNumber" placeholder="Auto-generated if empty" value="${escapeHtml(f.documentNumber)}">`)}
                ${field("attestationLegalCategory", "attestation_legal_category", "opt", `<input id="attestationLegalCategory" value="${escapeHtml(f.attestationLegalCategory)}">`)}
            </div>
            ${field("trustAnchor", "trust_anchor", "opt", `<input id="trustAnchor" value="${escapeHtml(f.trustAnchor)}">`)}
            ${field("locationStatus", "location_status", "opt", `<input id="locationStatus" value="${escapeHtml(f.locationStatus)}">`)}
        `,
        age: `
            <h3>Age attestations</h3>
            <p class="hint">age_in_years, age_birth_year and age_equal_or_over.{12,14,16,18,21,65} are derived from birth_date at issuance. Officers do not need to type them.</p>
            <p class="lede">These claims exist so a relying party can learn that a holder is over 18 without receiving the birth date.</p>
        `,
        portrait: `
            <h3>Portrait</h3>
            <p class="hint">CIR requires a facial image unless the user opts out. JPEG data is stored as an SD-JWT picture data URL and as mdoc portrait.</p>
            <label class="check"><input id="portraitOptOut" type="checkbox" ${f.portraitOptOut ? "checked" : ""}> User opts out of portrait (PID_03 empty portrait)</label>
            ${field("portrait", "portrait", "req", `<input id="portrait" type="file" accept="image/jpeg,image/png">`)}
            ${f.portraitDataUrl && !f.portraitOptOut ? `<img class="portrait-preview" src="${f.portraitDataUrl}" alt="Uploaded portrait">` : ""}
        `,
        review: `
            <h3>Review & issue</h3>
            <p class="hint">Issuing creates an SD-JWT VC, an mdoc-aligned CBOR payload, a holder P-256 key bound in cnf.jwk, and an AES-256-GCM wrapped private key unique to this user.</p>
            <p class="error">${escapeHtml(state.error)}</p>
            <div class="actions">
                <button class="btn" id="issue" type="button">Issue encrypted EUDI PID</button>
            </div>
        `
    };
    return forms[state.section];
}

function resultView() {
    const r = state.result;
    const tabs = [
        ["overview", "Overview"],
        ["sdjwt", "SD-JWT VC"],
        ["mdoc", "mdoc attributes"],
        ["key", "Holder key"]
    ];
    return `
        <section class="glass panel">
            <div class="chip-row">
                <span class="chip glow">Issued</span>
                <span class="chip">${escapeHtml(r.documentNumber)}</span>
                <span class="chip">${escapeHtml(r.vct)}</span>
            </div>
            <h3 style="margin-top:14px">${escapeHtml(r.givenName)} ${escapeHtml(r.familyName)}</h3>
            <div class="tabs">
                ${tabs.map(([id, label]) => `<button class="tab ${state.resultTab === id ? "active" : ""}" data-rtab="${id}" type="button">${label}</button>`).join("")}
            </div>
            ${resultTab()}
            <div class="actions">
                <button class="btn ghost" id="download-sdjwt" type="button">Download SD-JWT</button>
                <button class="btn ghost" id="download-key" type="button">Download encrypted key</button>
                <button class="btn" id="another" type="button">Issue another PID</button>
            </div>
        </section>
    `;
}

function formatInstant(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return escapeHtml(value);
    return date.toISOString().replace("T", " ").replace(/\.\d+Z$/, " UTC");
}

function resultTab() {
    const r = state.result;
    if (state.resultTab === "sdjwt") {
        return `<div class="pre">${escapeHtml(r.sdJwtVc)}</div>
            <p class="hint">Selective disclosures are appended after the issuer-signed JWT, separated by ~ as specified by SD-JWT VC.</p>
            <div class="pre">${escapeHtml(JSON.stringify(r.sdJwtPayloadPreview, null, 2))}</div>`;
    }
    if (state.resultTab === "mdoc") {
        return `<div class="pre">${escapeHtml(JSON.stringify(r.mdoc, null, 2))}</div>
            <p class="hint">CBOR hex of the namespaced attribute map</p>
            <div class="pre">${escapeHtml(r.mdocCborHex)}</div>`;
    }
    if (state.resultTab === "key") {
        return `
            <div class="warn">${escapeHtml(r.holderKeyWarning)}</div>
            <label>One-time recovery secret</label>
            <div class="secret">${escapeHtml(r.holderKeyRecoverySecret)}</div>
            <label>Encrypted holder private JWK (JWE dir + A256GCM)</label>
            <div class="pre">${escapeHtml(r.encryptedHolderKey)}</div>
            <label>Public JWK bound in cnf</label>
            <div class="pre">${escapeHtml(JSON.stringify(r.holderPublicJwk, null, 2))}</div>
        `;
    }
    return `
        <p class="lede">Document ${escapeHtml(r.documentNumber)} was issued at ${formatInstant(r.issuedAt)}. Technical expiry ${formatInstant(r.technicalExpiresAt)}. Administrative expiry ${escapeHtml(r.administrativeExpiryDate)}.</p>
        <p class="hint">${(r.disclosures || []).length} selectively disclosable claims were salted and hashed into the issuer-signed JWT.</p>
    `;
}

function registryView() {
    if (!state.list.length) {
        return `<section class="glass panel"><h3>Issued registry</h3><p class="empty">No PIDs in this session yet.</p></section>`;
    }
    return `<section class="glass panel">
        <h3>Issued registry</h3>
        ${state.list.map((item) => `
            <div class="list-item" data-open="${item.id}">
                <div>
                    <h4 style="margin:0">${escapeHtml(item.givenName)} ${escapeHtml(item.familyName)}</h4>
                    <div class="meta">${escapeHtml(item.documentNumber)} · ${escapeHtml(item.issuedAt)}</div>
                </div>
                <span class="chip">${escapeHtml(item.vct)}</span>
            </div>
        `).join("")}
        <div id="unlock-box"></div>
    </section>`;
}

function schemaView() {
    return `<section class="glass panel">
        <h3>PID data dictionary</h3>
        <p class="hint">All Person Identification Data identifiers from CIR 2024/2977 and the ARF PID Rulebook, with SD-JWT VC claim names and ISO/IEC 18013-5 attribute identifiers.</p>
        <table class="table">
            <thead><tr><th>CIR identifier</th><th>SD-JWT claim</th><th>mdoc attribute</th><th>Presence</th><th>Description</th></tr></thead>
            <tbody>
                ${state.schema.map((row) => `<tr>
                    <td class="mono">${escapeHtml(row.cirIdentifier)}</td>
                    <td class="mono">${escapeHtml(row.sdJwtClaim)}</td>
                    <td class="mono">${escapeHtml(row.mdocAttribute)}</td>
                    <td>${escapeHtml(row.presence)}</td>
                    <td>${escapeHtml(row.description)}</td>
                </tr>`).join("")}
            </tbody>
        </table>
    </section>`;
}

function readFormFromDom() {
    const f = state.form;
    const val = (id) => {
        const node = document.getElementById(id);
        return node ? node.value : undefined;
    };
    const assign = (id, key) => {
        const value = val(id);
        if (value !== undefined) f[key] = value;
    };
    assign("givenName", "givenName");
    assign("familyName", "familyName");
    assign("birthDate", "birthDate");
    if (document.getElementById("pobCountry")) {
        f.placeOfBirth.country = val("pobCountry");
        f.placeOfBirth.region = val("pobRegion");
        f.placeOfBirth.locality = val("pobLocality");
        f.nationalities = val("nationalities");
    }
    assign("residentAddress", "residentAddress");
    assign("residentStreet", "residentStreet");
    assign("residentHouseNumber", "residentHouseNumber");
    assign("residentCity", "residentCity");
    assign("residentPostalCode", "residentPostalCode");
    assign("residentState", "residentState");
    assign("residentCountry", "residentCountry");
    assign("givenNameBirth", "givenNameBirth");
    assign("familyNameBirth", "familyNameBirth");
    assign("sex", "sex");
    assign("personalAdministrativeNumber", "personalAdministrativeNumber");
    assign("emailAddress", "emailAddress");
    assign("mobilePhoneNumber", "mobilePhoneNumber");
    assign("issuingAuthority", "issuingAuthority");
    assign("issuingCountry", "issuingCountry");
    assign("issuingJurisdiction", "issuingJurisdiction");
    assign("issuanceDate", "issuanceDate");
    assign("expiryDate", "expiryDate");
    assign("documentNumber", "documentNumber");
    assign("attestationLegalCategory", "attestationLegalCategory");
    assign("trustAnchor", "trustAnchor");
    assign("locationStatus", "locationStatus");
    const opt = document.getElementById("portraitOptOut");
    if (opt) f.portraitOptOut = opt.checked;
}

function payload() {
    const f = state.form;
    const nationalities = f.nationalities.split(",").map((item) => item.trim().toUpperCase()).filter(Boolean);
    return {
        familyName: f.familyName,
        givenName: f.givenName,
        birthDate: f.birthDate,
        placeOfBirth: {
            country: f.placeOfBirth.country || null,
            region: f.placeOfBirth.region || null,
            locality: f.placeOfBirth.locality || null
        },
        nationalities,
        residentAddress: emptyToNull(f.residentAddress),
        residentCountry: emptyToNull(f.residentCountry),
        residentState: emptyToNull(f.residentState),
        residentCity: emptyToNull(f.residentCity),
        residentPostalCode: emptyToNull(f.residentPostalCode),
        residentStreet: emptyToNull(f.residentStreet),
        residentHouseNumber: emptyToNull(f.residentHouseNumber),
        personalAdministrativeNumber: emptyToNull(f.personalAdministrativeNumber),
        portraitDataUrl: f.portraitOptOut ? null : emptyToNull(f.portraitDataUrl),
        portraitOptOut: f.portraitOptOut,
        familyNameBirth: emptyToNull(f.familyNameBirth),
        givenNameBirth: emptyToNull(f.givenNameBirth),
        sex: f.sex === "" ? null : Number(f.sex),
        emailAddress: emptyToNull(f.emailAddress),
        mobilePhoneNumber: emptyToNull(f.mobilePhoneNumber),
        expiryDate: emptyToNull(f.expiryDate),
        issuingAuthority: f.issuingAuthority,
        issuingCountry: f.issuingCountry,
        documentNumber: emptyToNull(f.documentNumber),
        issuingJurisdiction: emptyToNull(f.issuingJurisdiction),
        locationStatus: emptyToNull(f.locationStatus),
        issuanceDate: emptyToNull(f.issuanceDate),
        trustAnchor: emptyToNull(f.trustAnchor),
        attestationLegalCategory: emptyToNull(f.attestationLegalCategory)
    };
}

function emptyToNull(value) {
    return value ? value : null;
}

function download(name, content) {
    const blob = new Blob([content], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
}

function bind() {
    const login = document.getElementById("login-form");
    if (login) {
        login.addEventListener("submit", async (event) => {
            event.preventDefault();
            try {
                await api("/api/auth/login", {
                    method: "POST",
                    body: JSON.stringify({
                        username: document.getElementById("username").value,
                        password: document.getElementById("password").value
                    })
                });
                state.user = await api("/api/auth/me");
                await loadWorkspace();
                render();
            } catch (error) {
                document.getElementById("login-error").textContent = error.message;
            }
        });
    }

    document.getElementById("logout")?.addEventListener("click", async () => {
        await api("/api/auth/logout", { method: "POST" }).catch(() => undefined);
        state.user = null;
        render();
    });

    document.querySelectorAll("[data-view]").forEach((button) => {
        button.addEventListener("click", () => {
            readFormFromDom();
            state.view = button.dataset.view;
            render();
        });
    });

    document.querySelectorAll("[data-section]").forEach((button) => {
        button.addEventListener("click", () => {
            readFormFromDom();
            state.section = button.dataset.section;
            render();
        });
    });

    document.getElementById("sample")?.addEventListener("click", () => {
        state.form = sampleForm();
        render();
    });
    document.getElementById("reset")?.addEventListener("click", () => {
        state.form = blankForm();
        state.result = null;
        render();
    });
    document.getElementById("another")?.addEventListener("click", () => {
        state.result = null;
        state.form = blankForm();
        state.section = "identity";
        render();
    });
    document.querySelectorAll("[data-rtab]").forEach((button) => {
        button.addEventListener("click", () => {
            state.resultTab = button.dataset.rtab;
            render();
        });
    });
    document.getElementById("download-sdjwt")?.addEventListener("click", () => {
        download(state.result.documentNumber + ".sdjwt.txt", state.result.sdJwtVc);
    });
    document.getElementById("download-key")?.addEventListener("click", () => {
        download(state.result.documentNumber + ".holder.jwe.txt", state.result.encryptedHolderKey);
    });
    document.getElementById("portrait")?.addEventListener("change", (event) => {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = () => {
            state.form.portraitDataUrl = reader.result;
            render();
        };
        reader.readAsDataURL(file);
    });
    document.getElementById("portraitOptOut")?.addEventListener("change", (event) => {
        state.form.portraitOptOut = event.target.checked;
    });
    document.getElementById("issue")?.addEventListener("click", async () => {
        readFormFromDom();
        state.error = "";
        try {
            state.result = await api("/api/pid", { method: "POST", body: JSON.stringify(payload()) });
            state.resultTab = "overview";
            await loadWorkspace();
            render();
        } catch (error) {
            state.error = error.message;
            render();
        }
    });
    document.querySelectorAll("[data-open]").forEach((row) => {
        row.addEventListener("click", async () => {
            const detail = await api("/api/pid/" + row.dataset.open);
            state.result = { ...detail, holderKeyRecoverySecret: detail.holderKeyRecoverySecret || "(shown only at issuance)" };
            state.view = "issue";
            state.resultTab = "overview";
            render();
        });
    });

    document.querySelectorAll("input, select, textarea").forEach((node) => {
        node.addEventListener("change", readFormFromDom);
        node.addEventListener("blur", readFormFromDom);
    });
}

boot();
