(() => {
  const API = {
    productById: (id) => `/api/v1/products/${id}`,
    productImages: (id) => `/api/v1/products/${id}/images`,
  };

  const IMAGE_PAGE_SIZE = 6;
  let productId = null;
  let imagePageIndex = 0;

  const els = {
    root: null,
    name: null,
    sku: null,
    price: null,
    categories: null,
    description: null,
    imageGrid: null,
    imagePageInfo: null,
    prevBtn: null,
    nextBtn: null,
  };

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

  function escapeAttr(value) {
    return escapeHtml(value).replace(/`/g, "&#96;");
  }

  async function apiFetch(url) {
    const response = await fetch(url, {
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      let message = `HTTP ${response.status}`;
      try {
        const type = response.headers.get("content-type") || "";
        if (type.includes("json") || type.includes("problem+json")) {
          const payload = await response.json();
          message = payload.detail || payload.message || payload.title || message;
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
    return response.json();
  }

  function parseProductId() {
    if (els.root) {
      const attr = Number(els.root.dataset.productId);
      if (Number.isInteger(attr) && attr > 0) {
        return attr;
      }
    }
    const match = String(window.location.pathname || "").match(/\/products\/(\d+)$/);
    if (match) {
      return Number(match[1]);
    }
    return null;
  }

  function formatMoney(amount, currency = "EUR") {
    const value = Number(amount || 0);
    try {
      return new Intl.NumberFormat("de-DE", {
        style: "currency",
        currency,
      }).format(value);
    } catch (_) {
      return `${value.toFixed(2)} ${currency}`;
    }
  }

  function renderProduct(product) {
    els.name.textContent = product.name || "Product";
    els.sku.textContent = `SKU: ${product.sku || "-"}`;
    els.price.textContent = formatMoney(product.price, product.currency);
    els.description.textContent = product.description || "No description.";

    const categories = Array.isArray(product.categories) ? product.categories : [];
    if (!categories.length) {
      els.categories.innerHTML = '<span class="badge text-bg-light border">No category</span>';
    } else {
      els.categories.innerHTML = categories
        .map((category) => `<span class="badge text-bg-light border">${escapeHtml(category.name || category.slug || "-")}</span>`)
        .join(" ");
    }
  }

  function renderImages(items, pageMeta) {
    if (!items.length) {
      els.imageGrid.innerHTML =
        '<div class="col-12"><div class="small muted">No image found.</div></div>';
    } else {
      els.imageGrid.innerHTML = items
        .map(
          (image) => `
            <div class="col-12 col-sm-6 col-lg-4">
              <div class="border rounded p-2 h-100 bg-white">
                <img src="${escapeAttr(image.url || "")}" alt="${escapeAttr(image.altText || "Product image")}" class="w-100 rounded mb-2" style="height:220px;object-fit:cover;" />
                <div class="small d-flex justify-content-between align-items-center">
                  <span class="text-truncate">${escapeHtml(image.altText || "Image")}</span>
                  ${image.primary ? '<span class="badge text-bg-success">Primary</span>' : ""}
                </div>
              </div>
            </div>
          `,
        )
        .join("");
    }

    if (pageMeta) {
      els.imagePageInfo.textContent = `Page ${pageMeta.number + 1}/${Math.max(1, pageMeta.totalPages)} | total ${pageMeta.totalElements}`;
      els.prevBtn.disabled = pageMeta.first;
      els.nextBtn.disabled = pageMeta.last;
    } else {
      els.imagePageInfo.textContent = `${items.length} images`;
      els.prevBtn.disabled = true;
      els.nextBtn.disabled = true;
    }
  }

  async function loadImages() {
    try {
      const url = `${API.productImages(productId)}?page=${imagePageIndex}&size=${IMAGE_PAGE_SIZE}&sort=sortOrder,asc`;
      const data = await apiFetch(url);
      const items = Array.isArray(data && data.items) ? data.items : [];
      const page = data && data.page ? data.page : null;
      if (page && Number.isInteger(page.number)) {
        imagePageIndex = page.number;
      }
      renderImages(items, page);
    } catch (error) {
      els.imageGrid.innerHTML = `<div class="col-12"><div class="alert alert-danger">${escapeHtml(error.message)}</div></div>`;
      els.imagePageInfo.textContent = "Image load failed";
      els.prevBtn.disabled = true;
      els.nextBtn.disabled = true;
    }
  }

  async function loadProduct() {
    try {
      const product = await apiFetch(API.productById(productId));
      renderProduct(product);
    } catch (error) {
      els.name.textContent = "Product not found";
      els.sku.textContent = "-";
      els.price.textContent = "-";
      els.categories.innerHTML = "";
      els.description.textContent = error.message;
    }
  }

  function cacheElements() {
    els.root = document.getElementById("productDetailRoot");
    els.name = document.getElementById("pdName");
    els.sku = document.getElementById("pdSku");
    els.price = document.getElementById("pdPrice");
    els.categories = document.getElementById("pdCategories");
    els.description = document.getElementById("pdDescription");
    els.imageGrid = document.getElementById("pdImageGrid");
    els.imagePageInfo = document.getElementById("pdImagePageInfo");
    els.prevBtn = document.getElementById("pdPrevBtn");
    els.nextBtn = document.getElementById("pdNextBtn");
  }

  function bindEvents() {
    els.prevBtn.addEventListener("click", async () => {
      if (imagePageIndex <= 0) {
        return;
      }
      imagePageIndex -= 1;
      await loadImages();
    });
    els.nextBtn.addEventListener("click", async () => {
      imagePageIndex += 1;
      await loadImages();
    });
  }

  async function init() {
    cacheElements();
    productId = parseProductId();
    if (!productId || !els.name || !els.imageGrid) {
      return;
    }
    bindEvents();
    await loadProduct();
    await loadImages();
  }

  document.addEventListener("DOMContentLoaded", init);
})();
