(() => {
  const API = {
    productById: (id) => `/api/v1/products/${id}`,
    productImages: (id) => `/api/v1/products/${id}/images`,
  };

  const LS_CART = "cart.v1";
  const IMAGE_PAGE_SIZE = 6;
  let productId = null;
  let imagePageIndex = 0;
  let currentProduct = null;

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
    addToCartBtn: null,
    cartActionBox: null,
    continueShoppingBtn: null,
    goToCartBtn: null,
    cartCount: null,
    cartList: null,
    cartEmpty: null,
    cartSubtotal: null,
    cartCurrency: null,
    clearCartBtn: null,
    openCheckoutBtn: null,
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

  function getCart() {
    try {
      const parsed = JSON.parse(localStorage.getItem(LS_CART));
      if (parsed && Array.isArray(parsed.items)) {
        return parsed;
      }
    } catch (_) {
      // keep fallback cart
    }
    return { currency: "EUR", items: [] };
  }

  function setCart(cart) {
    localStorage.setItem(LS_CART, JSON.stringify(cart));
    renderCart();
  }

  function cartItemCount() {
    return getCart().items.reduce((sum, item) => sum + Number(item.qty || 0), 0);
  }

  function cartSubtotal() {
    return getCart().items.reduce(
      (sum, item) => sum + Number(item.price || 0) * Number(item.qty || 0),
      0,
    );
  }

  function addToCart(product) {
    const cart = getCart();
    const existing = cart.items.find((item) => Number(item.id) === Number(product.id));
    if (existing) {
      existing.qty += 1;
    } else {
      cart.items.push({
        id: product.id,
        name: product.name,
        price: Number(product.price || 0),
        currency: product.currency || "EUR",
        qty: 1,
      });
    }
    cart.currency = product.currency || cart.currency || "EUR";
    setCart(cart);
    if (els.cartActionBox) {
      els.cartActionBox.classList.remove("d-none");
    }
  }

  function removeFromCart(productId) {
    const cart = getCart();
    cart.items = cart.items.filter((item) => Number(item.id) !== Number(productId));
    setCart(cart);
  }

  function changeCartQty(productId, delta) {
    const cart = getCart();
    const item = cart.items.find((entry) => Number(entry.id) === Number(productId));
    if (!item) {
      return;
    }
    item.qty = Number(item.qty || 0) + delta;
    if (item.qty <= 0) {
      cart.items = cart.items.filter((entry) => Number(entry.id) !== Number(productId));
    }
    setCart(cart);
  }

  function clearCart() {
    setCart({ currency: "EUR", items: [] });
  }

  function renderCart() {
    const cart = getCart();
    if (els.cartCount) {
      els.cartCount.textContent = String(cartItemCount());
    }
    if (!els.cartList || !els.cartEmpty || !els.cartSubtotal || !els.cartCurrency) {
      return;
    }

    els.cartList.innerHTML = "";
    if (!cart.items.length) {
      els.cartEmpty.classList.remove("d-none");
    } else {
      els.cartEmpty.classList.add("d-none");
      cart.items.forEach((item) => {
        const row = document.createElement("div");
        row.className = "border rounded-xl p-2 bg-white";
        row.innerHTML = `
          <div class="d-flex justify-content-between align-items-start">
            <div class="me-2">
              <div class="fw-semibold">${escapeHtml(item.name || "Product")}</div>
              <div class="small muted">${formatMoney(item.price, item.currency)} x ${Number(item.qty || 0)}</div>
            </div>
            <button class="btn btn-sm btn-outline-danger" type="button" data-cart-action="remove" data-id="${escapeAttr(item.id)}">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
          <div class="d-flex justify-content-between align-items-center mt-2">
            <div class="btn-group btn-group-sm" role="group">
              <button class="btn btn-outline-dark" type="button" data-cart-action="dec" data-id="${escapeAttr(item.id)}">-</button>
              <button class="btn btn-outline-dark disabled" type="button">${Number(item.qty || 0)}</button>
              <button class="btn btn-outline-dark" type="button" data-cart-action="inc" data-id="${escapeAttr(item.id)}">+</button>
            </div>
            <div class="price">${formatMoney(Number(item.price || 0) * Number(item.qty || 0), item.currency || cart.currency || "EUR")}</div>
          </div>
        `;
        els.cartList.appendChild(row);
      });
    }

    els.cartCurrency.textContent = cart.currency || "EUR";
    els.cartSubtotal.textContent = formatMoney(cartSubtotal(), cart.currency || "EUR");
  }

  function openCartCanvas() {
    const cartCanvas = document.getElementById("cartCanvas");
    if (!cartCanvas || !window.bootstrap || !window.bootstrap.Offcanvas) {
      return;
    }
    const instance = window.bootstrap.Offcanvas.getOrCreateInstance(cartCanvas);
    instance.show();
  }

  function renderProduct(product) {
    els.name.textContent = product.name || "Product";
    els.sku.textContent = `SKU: ${product.sku || "-"}`;
    els.price.textContent = formatMoney(product.price, product.currency);
    els.description.textContent = product.description || "No description.";
    currentProduct = {
      id: product.id,
      name: product.name || "Product",
      price: Number(product.price || 0),
      currency: product.currency || "EUR",
    };
    if (els.addToCartBtn) {
      els.addToCartBtn.disabled = false;
    }

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
      currentProduct = null;
      els.name.textContent = "Product not found";
      els.sku.textContent = "-";
      els.price.textContent = "-";
      els.categories.innerHTML = "";
      els.description.textContent = error.message;
      if (els.addToCartBtn) {
        els.addToCartBtn.disabled = true;
      }
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
    els.addToCartBtn = document.getElementById("pdAddToCartBtn");
    els.cartActionBox = document.getElementById("pdCartActionBox");
    els.continueShoppingBtn = document.getElementById("pdContinueShoppingBtn");
    els.goToCartBtn = document.getElementById("pdGoToCartBtn");
    els.cartCount = document.getElementById("cartCount");
    els.cartList = document.getElementById("cartList");
    els.cartEmpty = document.getElementById("cartEmpty");
    els.cartSubtotal = document.getElementById("cartSubtotal");
    els.cartCurrency = document.getElementById("cartCurrency");
    els.clearCartBtn = document.getElementById("clearCartBtn");
    els.openCheckoutBtn = document.getElementById("openCheckoutBtn");
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

    if (els.addToCartBtn) {
      els.addToCartBtn.addEventListener("click", () => {
        if (!currentProduct || !Number.isFinite(Number(currentProduct.id))) {
          return;
        }
        addToCart(currentProduct);
      });
    }

    if (els.goToCartBtn) {
      els.goToCartBtn.addEventListener("click", openCartCanvas);
    }

    if (els.clearCartBtn) {
      els.clearCartBtn.addEventListener("click", clearCart);
    }

    if (els.openCheckoutBtn) {
      els.openCheckoutBtn.addEventListener("click", () => {
        window.location.assign("/products");
      });
    }

    if (els.cartList) {
      els.cartList.addEventListener("click", (event) => {
        const btn = event.target.closest("[data-cart-action]");
        if (!btn) {
          return;
        }
        const productIdFromBtn = Number(btn.dataset.id);
        if (!Number.isFinite(productIdFromBtn)) {
          return;
        }
        const action = String(btn.dataset.cartAction || "");
        if (action === "remove") {
          removeFromCart(productIdFromBtn);
          return;
        }
        if (action === "dec") {
          changeCartQty(productIdFromBtn, -1);
          return;
        }
        if (action === "inc") {
          changeCartQty(productIdFromBtn, 1);
        }
      });
    }
  }

  async function init() {
    cacheElements();
    productId = parseProductId();
    if (!productId || !els.name || !els.imageGrid) {
      return;
    }
    bindEvents();
    renderCart();
    await loadProduct();
    await loadImages();
    if (window.location.hash === "#cart") {
      openCartCanvas();
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
