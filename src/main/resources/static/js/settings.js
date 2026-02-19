(() => {
    const API = {
        me: "/api/v1/me",
        addresses: "/api/v1/addresses",
        addressById: (id) => `/api/v1/addresses/${id}`,
        changeMyPassword: "/api/v1/me/password",
    };

    const LS_TOKEN = "auth.token";
    const LS_PROFILE_CARD_PREFIX = "profile.card.v1";

    let currentUser = null;
    let addresses = [];

    function getToken() {
        return localStorage.getItem(LS_TOKEN);
    }

    function normalizeText(value) {
        if (value == null) {
            return null;
        }
        const trimmed = String(value).trim();
        return trimmed.length ? trimmed : null;
    }

    function normalizeDigits(value) {
        if (value == null) {
            return "";
        }
        return String(value).replace(/\D/g, "");
    }

    function escapeHtml(value) {
        return String(value ?? "").replace(
            /[&<>"']/g,
            (char) =>
                ({
                    "&": "&amp;",
                    "<": "&lt;",
                    ">": "&gt;",
                    '"': "&quot;",
                    "'": "&#39;",
                })[char],
        );
    }

    async function apiFetch(url, options = {}) {
        const opts = { ...options };
        const headers = new Headers(opts.headers || {});
        headers.set("Accept", "application/json");

        if (opts.json !== undefined) {
            headers.set("Content-Type", "application/json");
            opts.body = JSON.stringify(opts.json);
            delete opts.json;
        }

        const token = getToken();
        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        const response = await fetch(url, { ...opts, headers });
        if (!response.ok) {
            let message = `HTTP ${response.status}`;
            try {
                const type = response.headers.get("content-type") || "";
                if (type.includes("application/json") || type.includes("problem+json")) {
                    const body = await response.json();
                    message = body.detail || body.message || body.title || message;
                } else {
                    const text = await response.text();
                    if (text) {
                        message = text;
                    }
                }
            } catch (_) {
                // keep fallback
            }
            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json") || contentType.includes("problem+json")) {
            return response.json();
        }
        return response.text();
    }

    function toUserPage() {
        window.location.assign("/login");
    }

    function renderUserMeta() {
        const emailEl = document.getElementById("settingsUserEmail");
        const rolesEl = document.getElementById("settingsUserRoles");
        if (emailEl) {
            emailEl.textContent = currentUser && currentUser.email ? currentUser.email : "-";
        }
        if (rolesEl) {
            const roles = currentUser && Array.isArray(currentUser.roles) ? currentUser.roles : [];
            rolesEl.textContent = roles.length ? roles.join(", ") : "-";
        }
    }

    async function loadCurrentUser() {
        if (!getToken()) {
            toUserPage();
            return;
        }
        try {
            const me = await apiFetch(API.me);
            currentUser = me && typeof me === "object" ? me : null;
            if (!currentUser || !currentUser.email) {
                toUserPage();
                return;
            }
            renderUserMeta();
        } catch (_) {
            toUserPage();
        }
    }

    function parseGermanLine1(line1) {
        const raw = normalizeText(line1) || "";
        const match = raw.match(/^(.*?)(?:\s+(\d+[a-zA-Z0-9\-\/]*)$)/);
        if (!match) {
            return { street: raw, houseNumber: "" };
        }
        return {
            street: normalizeText(match[1]) || "",
            houseNumber: normalizeText(match[2]) || "",
        };
    }

    function buildGermanAddressPayload(values) {
        const street = normalizeText(values.street);
        const houseNumber = normalizeText(values.houseNumber);
        const line1 = [street, houseNumber].filter(Boolean).join(" ");
        const postalCode = normalizeText(values.postalCode);
        const countryCode = (normalizeText(values.countryCode) || "DE").toUpperCase();

        const payload = {
            label: normalizeText(values.label),
            fullName: normalizeText(values.fullName),
            phone: normalizeText(values.phone),
            line1,
            line2: normalizeText(values.line2),
            city: normalizeText(values.city),
            state: normalizeText(values.state),
            postalCode,
            countryCode,
            makeDefault: !!values.makeDefault,
        };

        if (!payload.fullName || !payload.line1 || !payload.city || !payload.postalCode || !payload.countryCode) {
            throw new Error("Please fill all required address fields.");
        }
        if (payload.countryCode !== "DE") {
            throw new Error("Country code must be DE.");
        }
        if (!/^[0-9]{5}$/.test(payload.postalCode)) {
            throw new Error("German postal code must be exactly 5 digits.");
        }

        return payload;
    }

    function baseAddressPayload(payload) {
        return {
            label: payload.label,
            fullName: payload.fullName,
            phone: payload.phone,
            line1: payload.line1,
            line2: payload.line2,
            city: payload.city,
            state: payload.state,
            postalCode: payload.postalCode,
            countryCode: payload.countryCode,
        };
    }

    function setAddressResult(message, kind = "muted") {
        const resultEl = document.getElementById("addressResult");
        if (!resultEl) {
            return;
        }
        resultEl.textContent = message;
        resultEl.className = `small mt-2 ${kind === "error" ? "text-danger" : kind === "success" ? "text-success" : "muted"}`;
    }

    function clearAddressForm() {
        const form = document.getElementById("addressForm");
        if (!form) {
            return;
        }
        document.getElementById("addressId").value = "";
        document.getElementById("addrLabel").value = "";
        document.getElementById("addrFullName").value = "";
        document.getElementById("addrPhone").value = "";
        document.getElementById("addrCountry").value = "DE";
        document.getElementById("addrStreet").value = "";
        document.getElementById("addrHouseNumber").value = "";
        document.getElementById("addrLine2").value = "";
        document.getElementById("addrCity").value = "";
        document.getElementById("addrState").value = "";
        document.getElementById("addrPostal").value = "";
        document.getElementById("addrDefault").checked = false;
    }

    function fillAddressForm(address) {
        if (!address) {
            return;
        }
        const parsed = parseGermanLine1(address.line1);
        document.getElementById("addressId").value = String(address.id || "");
        document.getElementById("addrLabel").value = address.label || "";
        document.getElementById("addrFullName").value = address.fullName || "";
        document.getElementById("addrPhone").value = address.phone || "";
        document.getElementById("addrCountry").value = address.countryCode || "DE";
        document.getElementById("addrStreet").value = parsed.street || "";
        document.getElementById("addrHouseNumber").value = parsed.houseNumber || "";
        document.getElementById("addrLine2").value = address.line2 || "";
        document.getElementById("addrCity").value = address.city || "";
        document.getElementById("addrState").value = address.state || "";
        document.getElementById("addrPostal").value = address.postalCode || "";
        document.getElementById("addrDefault").checked = !!address.isDefault;
        setAddressResult("Address loaded into form.");
    }

    function renderAddressRows() {
        const body = document.getElementById("addressRows");
        if (!body) {
            return;
        }
        if (!addresses.length) {
            body.innerHTML = '<tr><td colspan="5" class="small muted">No address found.</td></tr>';
            return;
        }

        body.innerHTML = addresses
            .map(
                (address) => `
                  <tr>
                    <td>${escapeHtml(address.label || "-")}</td>
                    <td>${escapeHtml(address.fullName || "-")}</td>
                    <td>
                      <div>${escapeHtml(address.line1 || "-")} ${escapeHtml(address.line2 || "")}</div>
                      <div class="small muted">${escapeHtml(address.postalCode || "-")} ${escapeHtml(address.city || "-")} (${escapeHtml(address.countryCode || "-")})</div>
                    </td>
                    <td>${address.isDefault ? '<span class="badge text-bg-success">Default</span>' : '<span class="badge text-bg-secondary">No</span>'}</td>
                    <td class="text-nowrap">
                      <button class="btn btn-sm btn-outline-dark" type="button" data-action="edit" data-id="${escapeHtml(address.id)}">Edit</button>
                      <button class="btn btn-sm btn-outline-success" type="button" data-action="default" data-id="${escapeHtml(address.id)}">Default</button>
                    </td>
                  </tr>
                `,
            )
            .join("");
    }

    async function loadAddresses() {
        try {
            const data = await apiFetch(API.addresses);
            addresses = Array.isArray(data) ? data : [];
            renderAddressRows();
            setAddressResult(`${addresses.length} address loaded.`, "success");
        } catch (error) {
            setAddressResult(`Could not load addresses: ${error.message}`, "error");
        }
    }

    async function setDefaultAddress(id) {
        try {
            await apiFetch(`${API.addresses}/${id}/default`, { method: "POST" });
            await loadAddresses();
            const updated = addresses.find((item) => Number(item.id) === Number(id));
            if (updated) {
                fillAddressForm(updated);
            }
            setAddressResult("Default address updated.", "success");
        } catch (error) {
            setAddressResult(`Could not set default address: ${error.message}`, "error");
        }
    }

    async function handleAddressSave(event) {
        event.preventDefault();

        let payload;
        try {
            payload = buildGermanAddressPayload({
                label: document.getElementById("addrLabel").value,
                fullName: document.getElementById("addrFullName").value,
                phone: document.getElementById("addrPhone").value,
                countryCode: document.getElementById("addrCountry").value,
                street: document.getElementById("addrStreet").value,
                houseNumber: document.getElementById("addrHouseNumber").value,
                line2: document.getElementById("addrLine2").value,
                city: document.getElementById("addrCity").value,
                state: document.getElementById("addrState").value,
                postalCode: document.getElementById("addrPostal").value,
                makeDefault: document.getElementById("addrDefault").checked,
            });
        } catch (error) {
            setAddressResult(error.message, "error");
            return;
        }

        const editingId = Number(document.getElementById("addressId").value);

        try {
            let targetId = null;
            if (Number.isFinite(editingId) && editingId > 0) {
                await apiFetch(API.addressById(editingId), {
                    method: "PUT",
                    json: baseAddressPayload(payload),
                });
                targetId = editingId;
            } else {
                const created = await apiFetch(API.addresses, {
                    method: "POST",
                    json: baseAddressPayload(payload),
                });
                targetId = created && Number.isFinite(Number(created.id)) ? Number(created.id) : null;
            }

            if (payload.makeDefault && Number.isFinite(targetId)) {
                await apiFetch(`${API.addresses}/${targetId}/default`, { method: "POST" });
            }

            await loadAddresses();
            const updated = addresses.find((item) => Number(item.id) === Number(targetId));
            if (updated) {
                fillAddressForm(updated);
            }
            setAddressResult("Address saved successfully.", "success");
        } catch (error) {
            setAddressResult(`Could not save address: ${error.message}`, "error");
        }
    }

    function bindAddressEvents() {
        const form = document.getElementById("addressForm");
        const rows = document.getElementById("addressRows");
        const refreshBtn = document.getElementById("refreshAddressesBtn");
        const newBtn = document.getElementById("addressNewBtn");

        if (!form || !rows || !refreshBtn || !newBtn) {
            return;
        }

        form.addEventListener("submit", handleAddressSave);
        refreshBtn.addEventListener("click", loadAddresses);
        newBtn.addEventListener("click", () => {
            clearAddressForm();
            setAddressResult("New address form ready.");
        });

        rows.addEventListener("click", async (event) => {
            const btn = event.target.closest("button[data-action]");
            if (!btn) {
                return;
            }
            const id = Number(btn.dataset.id);
            if (!Number.isFinite(id)) {
                return;
            }
            const action = btn.dataset.action;
            if (action === "edit") {
                const selected = addresses.find((item) => Number(item.id) === id);
                if (selected) {
                    fillAddressForm(selected);
                }
                return;
            }
            if (action === "default") {
                await setDefaultAddress(id);
            }
        });
    }

    function profileCardStorageKey() {
        if (!currentUser || !currentUser.email) {
            return null;
        }
        return `${LS_PROFILE_CARD_PREFIX}:${String(currentUser.email).toLowerCase()}`;
    }

    function maskCardNumber(cardNumber) {
        const digits = normalizeDigits(cardNumber);
        if (digits.length < 4) {
            return "****";
        }
        return `**** **** **** ${digits.slice(-4)}`;
    }

    function getSavedCard() {
        const key = profileCardStorageKey();
        if (!key) {
            return null;
        }
        try {
            const parsed = JSON.parse(localStorage.getItem(key));
            if (!parsed || typeof parsed !== "object") {
                return null;
            }
            return {
                cardHolderName: normalizeText(parsed.cardHolderName),
                cardNumber: normalizeDigits(parsed.cardNumber),
                expiryMonth: Number(parsed.expiryMonth),
                expiryYear: Number(parsed.expiryYear),
            };
        } catch (_) {
            return null;
        }
    }

    function setCardResult(message, kind = "muted") {
        const resultEl = document.getElementById("cardResult");
        if (!resultEl) {
            return;
        }
        resultEl.textContent = message;
        resultEl.className = `small mt-2 ${kind === "error" ? "text-danger" : kind === "success" ? "text-success" : "muted"}`;
    }

    function renderSavedCard() {
        const infoEl = document.getElementById("cardSavedInfo");
        if (!infoEl) {
            return;
        }
        const saved = getSavedCard();
        if (!saved || !saved.cardNumber) {
            infoEl.textContent = "No saved card";
            return;
        }
        const month = Number.isFinite(saved.expiryMonth)
            ? String(saved.expiryMonth).padStart(2, "0")
            : "--";
        infoEl.textContent = `${saved.cardHolderName || "Card"} | ${maskCardNumber(saved.cardNumber)} | ${month}/${saved.expiryYear || "----"}`;
    }

    function loadSavedCardToForm() {
        const saved = getSavedCard();
        document.getElementById("cardHolderNameInput").value = saved && saved.cardHolderName ? saved.cardHolderName : "";
        document.getElementById("cardNumberInput").value = saved && saved.cardNumber ? saved.cardNumber : "";
        document.getElementById("cardExpMonthInput").value = saved && Number.isFinite(saved.expiryMonth) ? String(saved.expiryMonth) : "";
        document.getElementById("cardExpYearInput").value = saved && Number.isFinite(saved.expiryYear) ? String(saved.expiryYear) : "";
    }

    function validateCardPayload(payload) {
        if (!payload.cardHolderName || !payload.cardNumber || !payload.expiryMonthRaw || !payload.expiryYearRaw) {
            throw new Error("Please fill all required card fields.");
        }
        if (!/^\d{13,19}$/.test(payload.cardNumber)) {
            throw new Error("Card number must be 13-19 digits.");
        }

        const month = Number(payload.expiryMonthRaw);
        const year = Number(payload.expiryYearRaw);
        if (!Number.isInteger(month) || month < 1 || month > 12) {
            throw new Error("Expiration month must be between 1 and 12.");
        }
        if (!Number.isInteger(year) || payload.expiryYearRaw.length !== 4) {
            throw new Error("Expiration year must be 4 digits.");
        }

        const now = new Date();
        const thisYear = now.getFullYear();
        const thisMonth = now.getMonth() + 1;
        if (year < thisYear || year > thisYear + 30) {
            throw new Error("Expiration year is not valid.");
        }
        if (year === thisYear && month < thisMonth) {
            throw new Error("Card is expired.");
        }

        return {
            cardHolderName: payload.cardHolderName,
            cardNumber: payload.cardNumber,
            expiryMonth: month,
            expiryYear: year,
        };
    }

    function collectCardPayload() {
        return validateCardPayload({
            cardHolderName: normalizeText(document.getElementById("cardHolderNameInput").value),
            cardNumber: normalizeDigits(document.getElementById("cardNumberInput").value),
            expiryMonthRaw: normalizeDigits(document.getElementById("cardExpMonthInput").value),
            expiryYearRaw: normalizeDigits(document.getElementById("cardExpYearInput").value),
        });
    }

    function bindCardEvents() {
        const form = document.getElementById("cardForm");
        const loadBtn = document.getElementById("cardLoadBtn");
        const clearBtn = document.getElementById("cardClearBtn");

        if (!form || !loadBtn || !clearBtn) {
            return;
        }

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            const key = profileCardStorageKey();
            if (!key) {
                setCardResult("User context missing.", "error");
                return;
            }

            try {
                const payload = collectCardPayload();
                localStorage.setItem(key, JSON.stringify(payload));
                renderSavedCard();
                setCardResult("Card saved for checkout.", "success");
            } catch (error) {
                setCardResult(error.message, "error");
            }
        });

        clearBtn.addEventListener("click", () => {
            const key = profileCardStorageKey();
            if (key) {
                localStorage.removeItem(key);
            }
            loadSavedCardToForm();
            renderSavedCard();
            setCardResult("Saved card cleared.", "success");
        });

        loadBtn.addEventListener("click", () => {
            loadSavedCardToForm();
            setCardResult("Saved card loaded into form.");
        });
    }

    function setPasswordResult(message, kind = "muted") {
        const resultEl = document.getElementById("passwordResult");
        if (!resultEl) {
            return;
        }
        resultEl.textContent = message;
        resultEl.className = `small mt-2 ${kind === "error" ? "text-danger" : kind === "success" ? "text-success" : "muted"}`;
    }

    function bindPasswordEvents() {
        const form = document.getElementById("passwordForm");
        if (!form) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const currentPassword = document.getElementById("settingsCurrentPassword").value || "";
            const newPassword = document.getElementById("settingsNewPassword").value || "";
            const confirmNewPassword = document.getElementById("settingsConfirmPassword").value || "";

            if (!currentPassword || !newPassword || !confirmNewPassword) {
                setPasswordResult("Please fill all password fields.", "error");
                return;
            }
            if (newPassword.length < 8) {
                setPasswordResult("New password must be at least 8 characters.", "error");
                return;
            }

            try {
                await apiFetch(API.changeMyPassword, {
                    method: "POST",
                    json: { currentPassword, newPassword, confirmNewPassword },
                });
                document.getElementById("settingsCurrentPassword").value = "";
                document.getElementById("settingsNewPassword").value = "";
                document.getElementById("settingsConfirmPassword").value = "";
                setPasswordResult("Password updated successfully.", "success");
            } catch (error) {
                setPasswordResult(`Could not update password: ${error.message}`, "error");
            }
        });
    }

    async function init() {
        await loadCurrentUser();
        if (!currentUser) {
            return;
        }

        const page = document.body ? document.body.dataset.page : null;
        if (page === "settings-address") {
            bindAddressEvents();
            clearAddressForm();
            await loadAddresses();
            return;
        }

        if (page === "settings-karte") {
            bindCardEvents();
            loadSavedCardToForm();
            renderSavedCard();
            return;
        }

        if (page === "settings-password") {
            bindPasswordEvents();
            setPasswordResult("Fill form and submit to change password.");
        }
    }

    document.addEventListener("DOMContentLoaded", init);
})();
