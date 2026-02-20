(() => {
  const LS_TOKEN = "auth.token";

  let productId = null;
  let currentUser = null;
  let categories = [];
  let adminGranted = false;
  let currentProduct = null;

  // ---------------------------------------------------------------------------
  // Utilities
  // ---------------------------------------------------------------------------

  function getToken() {
    return localStorage.getItem(LS_TOKEN);
  }

  function normalizeText(value) {
    if (value == null) return null;
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
        const ct = response.headers.get("content-type") || "";
        if (ct.includes("application/json") || ct.includes("problem+json")) {
          const payload = await response.json();
          message =
            payload.detail || payload.message || payload.title || message;
        } else {
          const text = await response.text();
          if (text) message = text;
        }
      } catch (_) {
        // keep fallback
      }
      throw new Error(message);
    }

    if (response.status === 204) return null;

    const ct = response.headers.get("content-type") || "";
    if (ct.includes("application/json") || ct.includes("problem+json")) {
      return response.json();
    }
    return response.text();
  }

  // ---------------------------------------------------------------------------
  // DOM helpers
  // ---------------------------------------------------------------------------

  function setResult(value, kind = "muted") {
    const el = document.getElementById("adminEditResult");
    if (!el) return;
    el.textContent =
      typeof value === "string" ? value : JSON.stringify(value, null, 2);
    el.className = `small border rounded p-2 mb-0 ${
      kind === "error" ? "text-danger bg-light" : "bg-light"
    }`;
  }

  function setImageResult(message, isError = false) {
    const el = document.getElementById("adminEditImageResult");
    if (!el) return;
    el.textContent = message;
    el.className = `small mt-2 ${isError ? "text-danger" : "text-success"}`;
  }

  function setFormDisabled(disabled) {
    const form = document.getElementById("adminProductEditForm");
    if (!form) return;
    Array.from(form.elements).forEach((el) => {
      el.disabled = disabled;
    });
    const uploadBtn = document.getElementById("adminEditImageUploadBtn");
    const urlBtn = document.getElementById("adminEditImageUrlBtn");
    if (uploadBtn) uploadBtn.disabled = disabled;
    if (urlBtn) urlBtn.disabled = disabled;
  }

  function roleNames(user) {
    if (!user || !Array.isArray(user.roles)) return [];
    return user.roles.map((r) => String(r || "").trim()).filter(Boolean);
  }

  // ---------------------------------------------------------------------------
  // Auth / user
  // ---------------------------------------------------------------------------

  function renderUserMeta() {
    const emailEl = document.getElementById("adminEditUserEmail");
    const rolesEl = document.getElementById("adminEditUserRoles");
    if (emailEl) emailEl.textContent = currentUser?.email ?? "-";
    if (rolesEl) {
      const roles = roleNames(currentUser);
      rolesEl.textContent = roles.length ? roles.join(", ") : "-";
    }
  }

  function renderAdminAccess() {
    const alertEl = document.getElementById("adminEditAuthAlert");
    if (alertEl) alertEl.classList.toggle("d-none", adminGranted);
    setFormDisabled(!adminGranted);
  }

  async function loadCurrentUser() {
    const token = getToken();
    if (!token) {
      window.location.assign("/login");
      return;
    }
    try {
      const me = await apiFetch("/api/v1/me");
      currentUser = me && typeof me === "object" ? me : null;
      adminGranted = roleNames(currentUser).includes("ROLE_ADMIN");
      renderUserMeta();
      renderAdminAccess();
    } catch (_) {
      window.location.assign("/login");
    }
  }

  // ---------------------------------------------------------------------------
  // Categories
  // ---------------------------------------------------------------------------

  function renderCategoryList(checkedIds = []) {
    const listEl = document.getElementById("adminEditCategoryList");
    if (!listEl) return;

    if (!categories.length) {
      listEl.innerHTML = '<div class="muted">No category found.</div>';
      return;
    }

    const checkedSet = new Set(checkedIds.map(Number));

    listEl.innerHTML = categories
      .map(
        (cat) => `
          <label class="form-check">
            <input class="form-check-input" type="checkbox"
              name="adminEditCategory"
              value="${escapeHtml(cat.id)}"
              ${checkedSet.has(Number(cat.id)) ? "checked" : ""}>
            <span class="form-check-label">${escapeHtml(cat.name || cat.slug || `Category ${cat.id}`)}</span>
          </label>
        `,
      )
      .join("");
  }

  function selectedCategoryIds() {
    return Array.from(
      document.querySelectorAll(
        '#adminEditCategoryList input[name="adminEditCategory"]:checked',
      ),
    )
      .map((el) => Number(el.value))
      .filter((v) => Number.isInteger(v) && v > 0);
  }

  async function loadCategories(checkedIds = []) {
    try {
      const data = await apiFetch("/api/v1/categories");
      categories = Array.isArray(data) ? data : [];
      renderCategoryList(checkedIds);
    } catch (error) {
      categories = [];
      renderCategoryList([]);
      setResult(`Category load failed: ${error.message}`, "error");
    }
  }

  // ---------------------------------------------------------------------------
  // Product load & form populate
  // ---------------------------------------------------------------------------

  function populateForm(product) {
    currentProduct = product;

    const skuEl = document.getElementById("editProductSkuDisplay");
    if (skuEl) skuEl.textContent = product.sku ?? "-";

    const nameEl = document.getElementById("adminEditName");
    if (nameEl) nameEl.value = product.name ?? "";

    const descEl = document.getElementById("adminEditDescription");
    if (descEl) descEl.value = product.description ?? "";

    const priceEl = document.getElementById("adminEditPrice");
    if (priceEl) priceEl.value = product.price ?? "";

    const currencyEl = document.getElementById("adminEditCurrency");
    if (currencyEl) currencyEl.value = product.currency ?? "EUR";

    const stockEl = document.getElementById("adminEditStock");
    if (stockEl) stockEl.value = product.stockQuantity ?? 0;

    const statusEl = document.getElementById("adminEditStatus");
    if (statusEl) statusEl.value = product.status ?? "DRAFT";

    const catIds = Array.isArray(product.categories)
      ? product.categories.map((c) => c.id ?? c)
      : [];

    return catIds;
  }

  async function loadProduct() {
    const loadErrEl = document.getElementById("adminEditLoadError");
    try {
      const product = await apiFetch(`/api/v1/products/${productId}`);
      const catIds = populateForm(product);
      await loadCategories(catIds);
      renderImages(product.images ?? []);
    } catch (error) {
      if (loadErrEl) {
        loadErrEl.textContent = `Urun yuklenemedi: ${error.message}`;
        loadErrEl.classList.remove("d-none");
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Images gallery
  // ---------------------------------------------------------------------------

  function renderImages(images) {
    const gallery = document.getElementById("adminEditImagesGallery");
    if (!gallery) return;

    if (!images.length) {
      gallery.innerHTML =
        '<div class="col-12 small muted">No images yet.</div>';
      return;
    }

    gallery.innerHTML = images
      .map(
        (img) => `
          <div class="col-6 col-md-3" id="imgCard-${escapeHtml(img.id)}">
            <div class="border rounded p-1 text-center position-relative">
              <img
                src="${escapeHtml(img.url)}"
                alt="${escapeHtml(img.altText ?? "")}"
                class="img-fluid rounded mb-1"
                style="max-height:120px;object-fit:cover;"
                onerror="this.src='/img/placeholder.png'"
              />
              ${img.primary ? '<span class="badge bg-success mb-1 d-block">Primary</span>' : ""}
              <div class="small text-truncate muted">${escapeHtml(img.altText ?? "")}</div>
              <button
                class="btn btn-sm btn-outline-danger mt-1 w-100"
                type="button"
                data-delete-image-id="${escapeHtml(img.id)}"
              >
                <i class="bi bi-trash"></i> Delete
              </button>
            </div>
          </div>
        `,
      )
      .join("");
  }

  async function reloadImages() {
    try {
      const data = await apiFetch(
        `/api/v1/products/${productId}/images?size=50`,
      );
      const images = data?.content ?? data ?? [];
      renderImages(Array.isArray(images) ? images : []);
    } catch (error) {
      setImageResult(`Image reload failed: ${error.message}`, true);
    }
  }

  async function deleteImage(imageId) {
    try {
      await apiFetch(`/api/v1/products/${productId}/images/${imageId}`, {
        method: "DELETE",
      });
      document.getElementById(`imgCard-${imageId}`)?.remove();
      setImageResult("Image deleted.");
    } catch (error) {
      setImageResult(`Delete failed: ${error.message}`, true);
    }
  }

  async function uploadImageFile() {
    const fileInput = document.getElementById("adminEditImageFile");
    const altInput = document.getElementById("adminEditImageAlt");
    if (!fileInput?.files?.length) {
      setImageResult("Please select a file first.", true);
      return;
    }
    const formData = new FormData();
    Array.from(fileInput.files).forEach((f) => formData.append("files", f));
    const altText = normalizeText(altInput?.value);
    if (altText) formData.append("altText", altText);

    try {
      const token = getToken();
      const response = await fetch(
        `/api/v1/products/${productId}/images/upload`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: formData,
        },
      );
      if (!response.ok) {
        let msg = `HTTP ${response.status}`;
        try {
          const p = await response.json();
          msg = p.detail || p.message || p.title || msg;
        } catch (_) {}
        throw new Error(msg);
      }
      const updated = await response.json();
      renderImages(updated.images ?? []);
      fileInput.value = "";
      if (altInput) altInput.value = "";
      setImageResult("Image uploaded successfully.");
    } catch (error) {
      setImageResult(`Upload failed: ${error.message}`, true);
    }
  }

  async function addImageByUrl() {
    const urlInput = document.getElementById("adminEditImageUrl");
    const altInput = document.getElementById("adminEditImageUrlAlt");
    const url = normalizeText(urlInput?.value);
    if (!url) {
      setImageResult("Please enter an image URL.", true);
      return;
    }
    try {
      const updated = await apiFetch(`/api/v1/products/${productId}/images`, {
        method: "POST",
        json: { url, altText: normalizeText(altInput?.value) },
      });
      renderImages(updated.images ?? []);
      if (urlInput) urlInput.value = "";
      if (altInput) altInput.value = "";
      setImageResult("Image URL added successfully.");
    } catch (error) {
      setImageResult(`Add URL failed: ${error.message}`, true);
    }
  }

  // ---------------------------------------------------------------------------
  // Form submit (PUT /api/v1/products/{id})
  // ---------------------------------------------------------------------------

  async function submitEdit(event) {
    event.preventDefault();
    if (!adminGranted) {
      setResult("Admin role is required.", "error");
      return;
    }

    const name = normalizeText(document.getElementById("adminEditName")?.value);
    const description = normalizeText(
      document.getElementById("adminEditDescription")?.value,
    );
    const price = Number(document.getElementById("adminEditPrice")?.value);
    const currency = String(
      document.getElementById("adminEditCurrency")?.value ?? "",
    )
      .trim()
      .toUpperCase();
    const status = String(
      document.getElementById("adminEditStatus")?.value ?? "DRAFT",
    ).trim();
    const stock = Number(document.getElementById("adminEditStock")?.value ?? 0);
    const categoryIds = selectedCategoryIds();

    if (!name) {
      setResult("Name is required.", "error");
      return;
    }
    if (!Number.isFinite(price) || price <= 0) {
      setResult("Price must be greater than 0.", "error");
      return;
    }
    if (!/^[A-Z]{3}$/.test(currency)) {
      setResult("Currency must be 3 uppercase letters (e.g. EUR).", "error");
      return;
    }
    if (!Number.isInteger(stock) || stock < 0) {
      setResult("Stock must be 0 or greater.", "error");
      return;
    }

    const payload = {
      name,
      description,
      price,
      currency,
      stockQuantity: stock,
      status,
      categoryIds,
    };

    const submitBtn = document.getElementById("adminEditSubmitBtn");
    if (submitBtn) submitBtn.disabled = true;

    try {
      const updated = await apiFetch(`/api/v1/products/${productId}`, {
        method: "PUT",
        json: payload,
      });
      setResult(updated);
      // Refresh SKU display / sidebar meta
      if (updated?.sku) {
        const skuEl = document.getElementById("editProductSkuDisplay");
        if (skuEl) skuEl.textContent = updated.sku;
      }
    } catch (error) {
      setResult(`Update failed: ${error.message}`, "error");
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  }

  // ---------------------------------------------------------------------------
  // Event binding
  // ---------------------------------------------------------------------------

  function bindEvents() {
    const form = document.getElementById("adminProductEditForm");
    if (form) form.addEventListener("submit", submitEdit);

    const uploadBtn = document.getElementById("adminEditImageUploadBtn");
    if (uploadBtn) uploadBtn.addEventListener("click", uploadImageFile);

    const urlBtn = document.getElementById("adminEditImageUrlBtn");
    if (urlBtn) urlBtn.addEventListener("click", addImageByUrl);

    const gallery = document.getElementById("adminEditImagesGallery");
    if (gallery) {
      gallery.addEventListener("click", (event) => {
        const btn = event.target.closest("[data-delete-image-id]");
        if (btn) {
          const imageId = btn.dataset.deleteImageId;
          if (imageId) deleteImage(imageId);
        }
      });
    }
  }

  // ---------------------------------------------------------------------------
  // Init
  // ---------------------------------------------------------------------------

  async function init() {
    if (!document.body || document.body.dataset.page !== "admin-product-edit") {
      return;
    }

    productId = document.body.dataset.productId;
    if (!productId) {
      const loadErrEl = document.getElementById("adminEditLoadError");
      if (loadErrEl) {
        loadErrEl.textContent =
          "Product ID not found in URL. Navigate from the admin panel.";
        loadErrEl.classList.remove("d-none");
      }
      return;
    }

    bindEvents();
    await loadCurrentUser();
    if (!adminGranted) return;
    await loadProduct();
  }

  document.addEventListener("DOMContentLoaded", init);
})();
