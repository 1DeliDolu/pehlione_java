(() => {
  const API = {
    me: "/api/v1/me",
    categories: "/api/v1/categories",
    products: "/api/v1/products",
  };

  const LS_TOKEN = "auth.token";
  const APPAREL_SIZES = ["XS", "S", "M", "L", "XL", "XXL"];
  const SHOE_SIZES = ["36", "37", "38", "39", "40", "41", "42", "43", "44", "45"];
  const APPAREL_KEYWORDS = [
    "elbise",
    "giyim",
    "clothing",
    "apparel",
    "shirt",
    "tshirt",
    "pant",
    "jean",
    "jacket",
    "hoodie",
  ];
  const SHOE_KEYWORDS = ["ayakkabi", "shoe", "sneaker", "boot", "sandal"];

  let currentUser = null;
  let categories = [];
  let adminGranted = false;

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

  function escapeHtml(value) {
    return String(value ?? "").replace(
      /[&<>"']/g,
      (ch) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[ch],
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
        const contentType = response.headers.get("content-type") || "";
        if (
          contentType.includes("application/json") ||
          contentType.includes("problem+json")
        ) {
          const payload = await response.json();
          message =
            payload.detail || payload.message || payload.title || message;
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

    const responseType = response.headers.get("content-type") || "";
    if (
      responseType.includes("application/json") ||
      responseType.includes("problem+json")
    ) {
      return response.json();
    }
    return response.text();
  }

  function setResult(value, kind = "muted") {
    const resultEl = document.getElementById("adminCreateResult");
    if (!resultEl) {
      return;
    }
    if (typeof value === "string") {
      resultEl.textContent = value;
    } else {
      resultEl.textContent = JSON.stringify(value, null, 2);
    }
    resultEl.className = `small border rounded p-2 mb-0 ${
      kind === "error" ? "text-danger bg-light" : "bg-light"
    }`;
  }

  function classifyCategory(category) {
    const text = `${category && category.slug ? category.slug : ""} ${
      category && category.name ? category.name : ""
    }`.toLowerCase();
    if (SHOE_KEYWORDS.some((key) => text.includes(key))) {
      return "shoes";
    }
    if (APPAREL_KEYWORDS.some((key) => text.includes(key))) {
      return "apparel";
    }
    return "none";
  }

  function renderCategoryList() {
    const listEl = document.getElementById("adminCreateCategoryList");
    if (!listEl) {
      return;
    }

    if (!categories.length) {
      listEl.innerHTML = '<div class="muted">No category found.</div>';
      return;
    }

    listEl.innerHTML = categories
      .map(
        (category) => `
          <label class="form-check">
            <input class="form-check-input" type="checkbox" name="adminCreateCategory" value="${escapeHtml(category.id)}" data-size-type="${escapeHtml(classifyCategory(category))}">
            <span class="form-check-label">${escapeHtml(category.name || category.slug || `Category ${category.id}`)}</span>
          </label>
        `,
      )
      .join("");
  }

  function selectedCategoryCheckboxes() {
    return Array.from(
      document.querySelectorAll(
        '#adminCreateCategoryList input[name="adminCreateCategory"]:checked',
      ),
    );
  }

  function renderSizeRows() {
    const hintEl = document.getElementById("adminCreateSizeHint");
    const rowsEl = document.getElementById("adminCreateSizeRows");
    if (!hintEl || !rowsEl) {
      return;
    }

    const selected = selectedCategoryCheckboxes();
    const hasApparel = selected.some(
      (input) => input.dataset.sizeType === "apparel",
    );
    const hasShoes = selected.some((input) => input.dataset.sizeType === "shoes");

    if (!hasApparel && !hasShoes) {
      hintEl.textContent =
        "Selected categories do not require apparel/shoe size table.";
      rowsEl.innerHTML =
        '<tr><td colspan="2" class="small muted">No size matrix selected.</td></tr>';
      return;
    }

    const rows = [];
    if (hasApparel) {
      rows.push(
        '<tr class="table-light"><td colspan="2" class="small fw-semibold">Elbise Bedenleri (XS-S-M-L-XL)</td></tr>',
      );
      APPAREL_SIZES.forEach((size) => {
        rows.push(`
          <tr>
            <td>${size}</td>
            <td>
              <input type="number" min="0" step="1" value="0" class="form-control form-control-sm" data-size-key="APPAREL:${size}">
            </td>
          </tr>
        `);
      });
    }

    if (hasShoes) {
      rows.push(
        '<tr class="table-light"><td colspan="2" class="small fw-semibold">Ayakkabi Numaralari</td></tr>',
      );
      SHOE_SIZES.forEach((size) => {
        rows.push(`
          <tr>
            <td>${size}</td>
            <td>
              <input type="number" min="0" step="1" value="0" class="form-control form-control-sm" data-size-key="SHOE:${size}">
            </td>
          </tr>
        `);
      });
    }

    hintEl.textContent =
      "Size quantities entered here are merged into product description and stock.";
    rowsEl.innerHTML = rows.join("");
  }

  function collectSizeEntries() {
    const entries = [];
    let total = 0;
    const inputs = Array.from(
      document.querySelectorAll("#adminCreateSizeRows input[data-size-key]"),
    );
    inputs.forEach((input) => {
      const value = Number(input.value);
      if (!Number.isInteger(value) || value <= 0) {
        return;
      }
      const key = String(input.dataset.sizeKey || "").trim();
      if (!key) {
        return;
      }
      entries.push({ key, quantity: value });
      total += value;
    });
    return { entries, total };
  }

  function buildDescriptionWithSizes(baseDescription, sizeEntries) {
    if (!sizeEntries.length) {
      return baseDescription || null;
    }
    const lines = sizeEntries.map((entry) => `- ${entry.key}: ${entry.quantity}`);
    const sizeBlock = ["Size Matrix:", ...lines].join("\n");
    if (!baseDescription) {
      return sizeBlock;
    }
    return `${baseDescription}\n\n${sizeBlock}`;
  }

  function selectedCategoryIds() {
    return selectedCategoryCheckboxes()
      .map((input) => Number(input.value))
      .filter((value) => Number.isInteger(value) && value > 0);
  }

  function roleNames(user) {
    if (!user || !Array.isArray(user.roles)) {
      return [];
    }
    return user.roles.map((role) => String(role || "").trim()).filter(Boolean);
  }

  function renderUserMeta() {
    const emailEl = document.getElementById("adminCreateUserEmail");
    const rolesEl = document.getElementById("adminCreateUserRoles");
    if (emailEl) {
      emailEl.textContent =
        currentUser && currentUser.email ? currentUser.email : "-";
    }
    if (rolesEl) {
      const roles = roleNames(currentUser);
      rolesEl.textContent = roles.length ? roles.join(", ") : "-";
    }
  }

  function setCreateFormDisabled(disabled) {
    const form = document.getElementById("adminProductCreateForm");
    if (!form) {
      return;
    }
    Array.from(form.elements).forEach((element) => {
      element.disabled = disabled;
    });
  }

  function renderAdminAccess() {
    const alertEl = document.getElementById("adminCreateAuthAlert");
    if (!alertEl) {
      return;
    }
    alertEl.classList.toggle("d-none", adminGranted);
    setCreateFormDisabled(!adminGranted);
  }

  async function loadCurrentUser() {
    const token = getToken();
    if (!token) {
      window.location.assign("/login");
      return;
    }
    try {
      const me = await apiFetch(API.me);
      currentUser = me && typeof me === "object" ? me : null;
      adminGranted = roleNames(currentUser).includes("ROLE_ADMIN");
      renderUserMeta();
      renderAdminAccess();
    } catch (_) {
      window.location.assign("/login");
    }
  }

  async function loadCategories() {
    try {
      const data = await apiFetch(API.categories);
      categories = Array.isArray(data) ? data : [];
      renderCategoryList();
      renderSizeRows();
    } catch (error) {
      categories = [];
      renderCategoryList();
      setResult(`Category load failed: ${error.message}`, "error");
    }
  }

  async function submitCreate(event) {
    event.preventDefault();
    if (!adminGranted) {
      setResult("Admin role is required.", "error");
      return;
    }

    const sku = normalizeText(document.getElementById("adminCreateSku").value);
    const name = normalizeText(document.getElementById("adminCreateName").value);
    const descriptionBase = normalizeText(
      document.getElementById("adminCreateDescription").value,
    );
    const price = Number(document.getElementById("adminCreatePrice").value);
    const currency = String(
      document.getElementById("adminCreateCurrency").value || "",
    )
      .trim()
      .toUpperCase();
    const status = String(
      document.getElementById("adminCreateStatus").value || "ACTIVE",
    ).trim();
    const manualStock = Number(document.getElementById("adminCreateStock").value);
    const categoryIds = selectedCategoryIds();
    const sizes = collectSizeEntries();

    if (!sku || !name) {
      setResult("SKU and Name are required.", "error");
      return;
    }
    if (!Number.isFinite(price) || price <= 0) {
      setResult("Price must be greater than 0.", "error");
      return;
    }
    if (!/^[A-Z]{3}$/.test(currency)) {
      setResult("Currency must be ISO-4217 3 uppercase letters (e.g. EUR).", "error");
      return;
    }
    if (!Number.isInteger(manualStock) || manualStock < 0) {
      setResult("Manual stock must be 0 or greater.", "error");
      return;
    }

    const payload = {
      sku,
      name,
      description: buildDescriptionWithSizes(descriptionBase, sizes.entries),
      price,
      currency,
      stockQuantity: sizes.total > 0 ? sizes.total : manualStock,
      status,
      categoryIds,
    };

    try {
      const created = await apiFetch(API.products, {
        method: "POST",
        json: payload,
      });
      setResult(created);
    } catch (error) {
      setResult(`Create failed: ${error.message}`, "error");
    }
  }

  function bindEvents() {
    const categoryList = document.getElementById("adminCreateCategoryList");
    const form = document.getElementById("adminProductCreateForm");

    if (categoryList) {
      categoryList.addEventListener("change", renderSizeRows);
    }
    if (form) {
      form.addEventListener("submit", submitCreate);
    }
  }

  async function init() {
    if (!document.body || document.body.dataset.page !== "admin-product-create") {
      return;
    }
    bindEvents();
    await loadCurrentUser();
    if (!adminGranted) {
      return;
    }
    await loadCategories();
  }

  document.addEventListener("DOMContentLoaded", init);
})();
