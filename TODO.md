Aşağıdaki örnek **modern Bootstrap 5** ile hazırlanmış tek sayfalık (SPA gibi) bir **e-commerce frontend** iskeleti.

* Ürünleri API’den çeker (`/api/v1/products`)
* Sepeti **offcanvas** olarak yönetir (localStorage)
* Login token’ını localStorage’a alır (Bearer)
* Address seçip “Pay” akışını çağıracak şekilde **hazır yerleri** var

> Endpoint isimlerin sende farklıysa JS içindeki `API.*` URL’lerini değiştirmen yeterli.

## 1) Spring Boot’a koyacağın yer

* **Statik** kullanacaksan: `src/main/resources/static/index.html`
* Thymeleaf kullanacaksan: `src/main/resources/templates/index.html` + `HomeController` ile `/` map edersin.

---

## `index.html` (Bootstrap 5 ile modern frontend)

```html
<!doctype html>
<html lang="tr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Pehlione Shop</title>

  <!-- Bootstrap 5.3 -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <!-- Bootstrap Icons -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">

  <style>
    :root { --glass: rgba(255,255,255,.65); }
    body { background: radial-gradient(1200px 600px at 10% 10%, #e9f2ff, transparent),
                     radial-gradient(900px 500px at 90% 20%, #f3e8ff, transparent),
                     #f7f7fb; }
    .navbar { backdrop-filter: blur(12px); background: var(--glass) !important; }
    .hero {
      background: linear-gradient(135deg, rgba(13,110,253,.12), rgba(111,66,193,.12));
      border: 1px solid rgba(0,0,0,.06);
    }
    .card.product { transition: transform .15s ease, box-shadow .15s ease; border: 1px solid rgba(0,0,0,.06); }
    .card.product:hover { transform: translateY(-2px); box-shadow: 0 18px 48px rgba(0,0,0,.08); }
    .badge-soft { background: rgba(13,110,253,.12); color: #0d6efd; }
    .muted { color: rgba(0,0,0,.6); }
    .price { font-weight: 700; letter-spacing: -0.3px; }
    .skeleton { background: linear-gradient(90deg, #eee, #f7f7f7, #eee); background-size: 200% 100%; animation: shimmer 1.1s infinite; }
    @keyframes shimmer { 0% {background-position: 200% 0;} 100% {background-position: -200% 0;} }
    .skeleton-line { height: 14px; border-radius: 10px; }
    .skeleton-card { height: 220px; border-radius: 16px; }
    .rounded-2xl { border-radius: 1.25rem; }
  </style>
</head>

<body>
  <!-- NAV -->
  <nav class="navbar navbar-expand-lg sticky-top border-bottom">
    <div class="container py-2">
      <a class="navbar-brand fw-bold" href="#">
        <i class="bi bi-bag-heart me-2"></i>Pehlione Shop
      </a>

      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav">
        <span class="navbar-toggler-icon"></span>
      </button>

      <div id="nav" class="collapse navbar-collapse">
        <form class="d-flex ms-lg-4 me-auto mt-3 mt-lg-0" role="search" onsubmit="return false;">
          <div class="input-group">
            <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
            <input id="searchInput" class="form-control" type="search" placeholder="Ürün ara (isim / sku)">
          </div>
        </form>

        <div class="d-flex gap-2 align-items-center mt-3 mt-lg-0">
          <span id="authState" class="small muted d-none d-lg-inline"></span>

          <button class="btn btn-outline-dark" data-bs-toggle="modal" data-bs-target="#loginModal">
            <i class="bi bi-person-circle me-1"></i>Giriş
          </button>

          <button class="btn btn-dark position-relative" data-bs-toggle="offcanvas" data-bs-target="#cartCanvas">
            <i class="bi bi-cart3 me-1"></i>Sepet
            <span id="cartCount" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
              0
            </span>
          </button>
        </div>
      </div>
    </div>
  </nav>

  <!-- HERO -->
  <header class="container my-4">
    <div class="hero rounded-2xl p-4 p-lg-5">
      <div class="row align-items-center g-4">
        <div class="col-lg-7">
          <span class="badge badge-soft rounded-pill px-3 py-2 mb-3">Modern • Bootstrap 5 • API-first</span>
          <h1 class="display-6 fw-bold mb-2">Ürünleri keşfet, sepete ekle, tek akışta ödeme başlat.</h1>
          <p class="muted mb-4">
            Bu UI backend’inle konuşacak şekilde hazır: products → cart → checkout/pay → payment confirm.
          </p>
          <div class="d-flex gap-2 flex-wrap">
            <button class="btn btn-primary" onclick="reloadProducts()"><i class="bi bi-arrow-repeat me-1"></i>Ürünleri Yenile</button>
            <button class="btn btn-outline-primary" onclick="openAddresses()"><i class="bi bi-geo-alt me-1"></i>Adreslerim</button>
            <a class="btn btn-outline-dark" href="/swagger-ui/index.html" target="_blank" rel="noreferrer">
              <i class="bi bi-braces me-1"></i>Swagger
            </a>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="bg-white rounded-2xl p-4 border">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <div class="fw-semibold">Hızlı Durum</div>
                <div class="small muted">Token / cart / API health</div>
              </div>
              <span id="healthPill" class="badge rounded-pill text-bg-secondary">unknown</span>
            </div>
            <hr>
            <div class="small">
              <div class="d-flex justify-content-between"><span class="muted">Auth</span><span id="authPill">—</span></div>
              <div class="d-flex justify-content-between"><span class="muted">Cart Items</span><span id="cartItemsPill">0</span></div>
              <div class="d-flex justify-content-between"><span class="muted">API</span><span id="apiBasePill">/api/v1</span></div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </header>

  <!-- PRODUCTS -->
  <main class="container pb-5">
    <div class="d-flex justify-content-between align-items-end mb-3">
      <div>
        <h2 class="h4 fw-bold mb-1">Ürünler</h2>
        <div class="small muted" id="resultInfo">Yükleniyor…</div>
      </div>
      <div class="d-flex gap-2">
        <select id="sortSelect" class="form-select form-select-sm" style="max-width: 200px;">
          <option value="relevance">Sıralama: Varsayılan</option>
          <option value="priceAsc">Fiyat: Artan</option>
          <option value="priceDesc">Fiyat: Azalan</option>
        </select>
      </div>
    </div>

    <div id="productGrid" class="row g-3">
      <!-- skeleton -->
    </div>
  </main>

  <!-- CART OFFCANVAS -->
  <div class="offcanvas offcanvas-end" tabindex="-1" id="cartCanvas">
    <div class="offcanvas-header">
      <h5 class="offcanvas-title"><i class="bi bi-cart3 me-2"></i>Sepet</h5>
      <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
    </div>
    <div class="offcanvas-body d-flex flex-column gap-3">
      <div id="cartEmpty" class="text-center muted py-5 d-none">
        <i class="bi bi-bag-x fs-1 d-block mb-2"></i>
        Sepet boş
      </div>

      <div id="cartList" class="d-flex flex-column gap-2"></div>

      <div class="mt-auto border-top pt-3">
        <div class="d-flex justify-content-between">
          <span class="muted">Ara Toplam</span>
          <span class="price" id="cartSubtotal">0.00</span>
        </div>
        <div class="d-flex justify-content-between">
          <span class="muted">Para Birimi</span>
          <span id="cartCurrency">EUR</span>
        </div>
        <div class="d-grid gap-2 mt-3">
          <button class="btn btn-outline-dark" onclick="clearCart()"><i class="bi bi-trash3 me-1"></i>Sepeti Temizle</button>
          <button class="btn btn-primary" onclick="openCheckout()"><i class="bi bi-credit-card me-1"></i>Checkout</button>
        </div>
        <div class="small muted mt-2">
          Checkout: draft/reserve + pay endpoints’i sende farklıysa `API.*` URL’lerini güncelle.
        </div>
      </div>
    </div>
  </div>

  <!-- LOGIN MODAL -->
  <div class="modal fade" id="loginModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-2xl">
        <div class="modal-header">
          <h5 class="modal-title"><i class="bi bi-shield-lock me-2"></i>Giriş</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="alert alert-info small mb-3">
            Bu demo, JWT token’ı localStorage’a kaydeder ve API çağrılarına <code>Authorization: Bearer</code> ekler.
          </div>

          <div class="mb-2">
            <label class="form-label">Email</label>
            <input id="loginEmail" class="form-control" placeholder="user@example.com" />
          </div>
          <div class="mb-3">
            <label class="form-label">Password</label>
            <input id="loginPassword" type="password" class="form-control" placeholder="••••••••" />
          </div>

          <div class="d-grid gap-2">
            <button class="btn btn-dark" onclick="login()"><i class="bi bi-box-arrow-in-right me-1"></i>Giriş Yap</button>
            <button class="btn btn-outline-dark" onclick="logout()"><i class="bi bi-box-arrow-right me-1"></i>Çıkış</button>
          </div>

          <div class="small muted mt-3">
            Varsayılan endpoint: <code>POST /api/v1/auth/login</code> → <code>{ accessToken }</code>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- ADDRESSES MODAL -->
  <div class="modal fade" id="addressesModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
      <div class="modal-content rounded-2xl">
        <div class="modal-header">
          <h5 class="modal-title"><i class="bi bi-geo-alt me-2"></i>Adresler</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <div class="muted small">Checkout’ta adres seçmek için kullanılır.</div>
            <button class="btn btn-sm btn-primary" onclick="openNewAddress()"><i class="bi bi-plus-lg me-1"></i>Yeni Adres</button>
          </div>
          <div id="addressesList" class="row g-2"></div>
        </div>
      </div>
    </div>
  </div>

  <!-- CHECKOUT MODAL -->
  <div class="modal fade" id="checkoutModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content rounded-2xl">
        <div class="modal-header">
          <h5 class="modal-title"><i class="bi bi-receipt me-2"></i>Checkout</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="row g-3">
            <div class="col-lg-7">
              <div class="fw-semibold mb-2">Teslimat Adresi</div>
              <div id="checkoutAddresses" class="d-flex flex-column gap-2"></div>
              <div class="small muted mt-2">
                Adres yoksa “Adresler” bölümünden ekle.
              </div>
            </div>
            <div class="col-lg-5">
              <div class="bg-light rounded-2xl p-3">
                <div class="fw-semibold mb-2">Özet</div>
                <div class="d-flex justify-content-between"><span class="muted">Toplam</span><span class="price" id="checkoutTotal">0.00</span></div>
                <div class="d-flex justify-content-between"><span class="muted">Para Birimi</span><span id="checkoutCurrency">EUR</span></div>
                <hr>
                <div class="d-grid gap-2">
                  <button class="btn btn-primary" onclick="startPayment()">
                    <i class="bi bi-credit-card me-1"></i>Ödemeyi Başlat
                  </button>
                  <button class="btn btn-outline-dark" onclick="confirmMockPayment()">
                    <i class="bi bi-check2-circle me-1"></i>Mock Confirm (Dev)
                  </button>
                </div>
                <div class="small muted mt-2">
                  Akış: create draft/reserve → <code>/checkout/.../pay</code> → paymentId.
                </div>
                <div class="alert alert-secondary small mt-3 mb-0">
                  paymentId: <span id="lastPaymentId" class="fw-semibold">—</span><br>
                  orderId: <span id="lastOrderId" class="fw-semibold">—</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- NEW ADDRESS MODAL -->
  <div class="modal fade" id="newAddressModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content rounded-2xl">
        <div class="modal-header">
          <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>Yeni Adres</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="row g-2">
            <div class="col-md-6">
              <label class="form-label">Label</label>
              <input id="addrLabel" class="form-control" placeholder="Home" />
            </div>
            <div class="col-md-6">
              <label class="form-label">Full Name</label>
              <input id="addrFullName" class="form-control" placeholder="Ali Veli" />
            </div>
            <div class="col-md-6">
              <label class="form-label">Phone</label>
              <input id="addrPhone" class="form-control" placeholder="+49..." />
            </div>
            <div class="col-md-6">
              <label class="form-label">Country Code</label>
              <input id="addrCountry" class="form-control" placeholder="DE" />
            </div>
            <div class="col-md-12">
              <label class="form-label">Line 1</label>
              <input id="addrLine1" class="form-control" placeholder="Street 1" />
            </div>
            <div class="col-md-12">
              <label class="form-label">Line 2</label>
              <input id="addrLine2" class="form-control" placeholder="Apt 3" />
            </div>
            <div class="col-md-4">
              <label class="form-label">City</label>
              <input id="addrCity" class="form-control" placeholder="Berlin" />
            </div>
            <div class="col-md-4">
              <label class="form-label">State</label>
              <input id="addrState" class="form-control" placeholder="Berlin" />
            </div>
            <div class="col-md-4">
              <label class="form-label">Postal Code</label>
              <input id="addrPostal" class="form-control" placeholder="10115" />
            </div>
            <div class="col-12 mt-2">
              <div class="form-check">
                <input id="addrDefault" class="form-check-input" type="checkbox" checked />
                <label class="form-check-label">Default yap</label>
              </div>
            </div>
          </div>
          <div class="d-grid mt-3">
            <button class="btn btn-primary" onclick="createAddress()"><i class="bi bi-save me-1"></i>Kaydet</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- TOAST -->
  <div class="position-fixed bottom-0 end-0 p-3" style="z-index: 1080">
    <div id="toast" class="toast align-items-center text-bg-dark border-0" role="alert" aria-live="assertive">
      <div class="d-flex">
        <div id="toastBody" class="toast-body">—</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
      </div>
    </div>
  </div>

  <!-- Bootstrap JS -->
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

  <script>
    // -------------------------
    // CONFIG (endpointleri buradan değiştir)
    // -------------------------
    const API = {
      products: "/api/v1/products",
      login: "/api/v1/auth/login",
      addresses: "/api/v1/addresses",

      // Aşağıdakiler projene göre uyarlanır:
      // - draft create/reserve endpointin farklı olabilir
      draftCreate: "/api/v1/checkout/drafts",             // TODO: uyarlayabilirsin
      payDraft: (draftId) => `/api/v1/checkout/drafts/${draftId}/pay`,
      confirmMock: (paymentId) => `/api/v1/payments/${paymentId}/confirm-mock`,
      health: "/actuator/health"
    };

    // -------------------------
    // STATE
    // -------------------------
    const LS_TOKEN = "auth.token";
    const LS_CART  = "cart.v1";
    let products = [];
    let addresses = [];
    let selectedAddressId = null;

    let lastPaymentId = null;
    let lastOrderId = null;
    let lastDraftId = null;

    // -------------------------
    // HELPERS
    // -------------------------
    function toast(msg) {
      document.getElementById("toastBody").textContent = msg;
      bootstrap.Toast.getOrCreateInstance(document.getElementById("toast"), { delay: 2200 }).show();
    }

    function token() { return localStorage.getItem(LS_TOKEN); }

    async function apiFetch(url, opts = {}) {
      const headers = new Headers(opts.headers || {});
      headers.set("Accept", "application/json");

      if (opts.json) {
        headers.set("Content-Type", "application/json");
        opts.body = JSON.stringify(opts.json);
        delete opts.json;
      }

      const t = token();
      if (t) headers.set("Authorization", `Bearer ${t}`);

      const res = await fetch(url, { ...opts, headers });

      // ProblemDetails support
      if (!res.ok) {
        let msg = `HTTP ${res.status}`;
        try {
          const ct = res.headers.get("content-type") || "";
          if (ct.includes("application/json") || ct.includes("problem+json")) {
            const p = await res.json();
            msg = p.detail || p.message || msg;
          } else {
            msg = await res.text();
          }
        } catch (_) {}
        throw new Error(msg);
      }

      const ct = res.headers.get("content-type") || "";
      if (ct.includes("application/json") || ct.includes("problem+json")) return res.json();
      return res.text();
    }

    function formatMoney(amount, currency = "EUR") {
      try {
        return new Intl.NumberFormat("tr-TR", { style: "currency", currency }).format(amount);
      } catch {
        return `${amount.toFixed(2)} ${currency}`;
      }
    }

    function getCart() {
      try { return JSON.parse(localStorage.getItem(LS_CART)) || { currency: "EUR", items: [] }; }
      catch { return { currency: "EUR", items: [] }; }
    }
    function setCart(c) { localStorage.setItem(LS_CART, JSON.stringify(c)); renderCart(); }
    function cartAdd(p) {
      const c = getCart();
      const existing = c.items.find(i => i.id === p.id);
      if (existing) existing.qty += 1;
      else c.items.push({ id: p.id, name: p.name, price: p.price, currency: p.currency || "EUR", qty: 1 });
      c.currency = p.currency || c.currency || "EUR";
      setCart(c);
      toast("Sepete eklendi");
    }
    function cartRemove(id) {
      const c = getCart();
      c.items = c.items.filter(i => i.id !== id);
      setCart(c);
    }
    function cartQty(id, delta) {
      const c = getCart();
      const it = c.items.find(i => i.id === id);
      if (!it) return;
      it.qty += delta;
      if (it.qty <= 0) c.items = c.items.filter(x => x.id !== id);
      setCart(c);
    }
    function clearCart() { setCart({ currency: "EUR", items: [] }); toast("Sepet temizlendi"); }

    function cartSubtotal() {
      const c = getCart();
      return c.items.reduce((sum, i) => sum + (Number(i.price) * i.qty), 0);
    }

    function normalizeProductsResponse(data) {
      // PageResponse<T> ise { items: [...] } bekler
      if (Array.isArray(data)) return data;
      if (data && Array.isArray(data.items)) return data.items;
      if (data && Array.isArray(data.content)) return data.content; // bazı pageable formatları
      return [];
    }

    // -------------------------
    // UI RENDER
    // -------------------------
    function renderSkeleton() {
      const grid = document.getElementById("productGrid");
      grid.innerHTML = "";
      for (let i = 0; i < 8; i++) {
        const col = document.createElement("div");
        col.className = "col-12 col-sm-6 col-lg-3";
        col.innerHTML = `
          <div class="bg-white border rounded-2xl p-3">
            <div class="skeleton skeleton-card mb-3"></div>
            <div class="skeleton skeleton-line w-75 mb-2"></div>
            <div class="skeleton skeleton-line w-50"></div>
          </div>
        `;
        grid.appendChild(col);
      }
    }

    function renderProducts(list) {
      const grid = document.getElementById("productGrid");
      grid.innerHTML = "";

      if (!list.length) {
        grid.innerHTML = `<div class="col-12"><div class="alert alert-light border">Ürün bulunamadı.</div></div>`;
        return;
      }

      list.forEach(p => {
        const col = document.createElement("div");
        col.className = "col-12 col-sm-6 col-lg-3";
        col.innerHTML = `
          <div class="card product rounded-2xl h-100">
            <div class="card-body d-flex flex-column">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <div class="fw-semibold text-truncate" title="${escapeHtml(p.name)}">${escapeHtml(p.name)}</div>
                <span class="badge text-bg-light border">${escapeHtml(p.sku || "SKU")}</span>
              </div>
              <div class="small muted mb-3">${escapeHtml(p.description || "Modern ürün açıklaması...")}</div>

              <div class="mt-auto d-flex justify-content-between align-items-center">
                <div class="price">${formatMoney(Number(p.price || 0), p.currency || "EUR")}</div>
                <button class="btn btn-sm btn-dark" onclick='cartAdd(${JSON.stringify({
                  id: p.id ?? p.productId ?? p.publicId ?? p.sku,
                  name: p.name,
                  price: p.price ?? 0,
                  currency: p.currency ?? "EUR"
                })})'>
                  <i class="bi bi-plus-lg"></i>
                </button>
              </div>
            </div>
          </div>
        `;
        grid.appendChild(col);
      });

      document.getElementById("resultInfo").textContent = `${list.length} ürün listelendi`;
    }

    function renderCart() {
      const c = getCart();
      const list = document.getElementById("cartList");
      const empty = document.getElementById("cartEmpty");
      list.innerHTML = "";

      const count = c.items.reduce((sum, i) => sum + i.qty, 0);
      document.getElementById("cartCount").textContent = count;
      document.getElementById("cartItemsPill").textContent = count;

      if (!c.items.length) {
        empty.classList.remove("d-none");
      } else {
        empty.classList.add("d-none");
        c.items.forEach(i => {
          const row = document.createElement("div");
          row.className = "border rounded-2xl p-2 bg-white";
          row.innerHTML = `
            <div class="d-flex justify-content-between align-items-start">
              <div class="me-2">
                <div class="fw-semibold">${escapeHtml(i.name)}</div>
                <div class="small muted">${formatMoney(Number(i.price), i.currency)} × ${i.qty}</div>
              </div>
              <button class="btn btn-sm btn-outline-danger" onclick="cartRemove('${escapeJs(i.id)}')">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>

            <div class="d-flex justify-content-between align-items-center mt-2">
              <div class="btn-group btn-group-sm" role="group">
                <button class="btn btn-outline-dark" onclick="cartQty('${escapeJs(i.id)}', -1)">-</button>
                <button class="btn btn-outline-dark disabled">${i.qty}</button>
                <button class="btn btn-outline-dark" onclick="cartQty('${escapeJs(i.id)}', 1)">+</button>
              </div>
              <div class="price">${formatMoney(Number(i.price) * i.qty, i.currency)}</div>
            </div>
          `;
          list.appendChild(row);
        });
      }

      document.getElementById("cartCurrency").textContent = c.currency || "EUR";
      document.getElementById("cartSubtotal").textContent = formatMoney(cartSubtotal(), c.currency || "EUR");
      document.getElementById("checkoutTotal").textContent = formatMoney(cartSubtotal(), c.currency || "EUR");
      document.getElementById("checkoutCurrency").textContent = c.currency || "EUR";
    }

    function escapeHtml(s) {
      return String(s ?? "").replace(/[&<>"']/g, m => ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;" }[m]));
    }
    function escapeJs(s) {
      return String(s ?? "").replace(/\\/g,"\\\\").replace(/'/g,"\\'").replace(/"/g,'\\"');
    }

    // -------------------------
    // LOADERS
    // -------------------------
    async function reloadProducts() {
      renderSkeleton();
      try {
        const data = await apiFetch(API.products);
        products = normalizeProductsResponse(data).map(x => ({
          id: x.id ?? x.productId ?? x.publicId ?? x.sku,
          sku: x.sku,
          name: x.name,
          description: x.description,
          price: x.price ?? x.unitPrice ?? 0,
          currency: x.currency ?? "EUR"
        }));
        applySearchSort();
        toast("Ürünler yüklendi");
      } catch (e) {
        document.getElementById("productGrid").innerHTML = `<div class="col-12"><div class="alert alert-danger">Ürünler yüklenemedi: ${escapeHtml(e.message)}</div></div>`;
      }
    }

    function applySearchSort() {
      const q = (document.getElementById("searchInput").value || "").trim().toLowerCase();
      let list = products.filter(p =>
        !q || (p.name || "").toLowerCase().includes(q) || (p.sku || "").toLowerCase().includes(q)
      );

      const sort = document.getElementById("sortSelect").value;
      if (sort === "priceAsc") list.sort((a,b)=> Number(a.price)-Number(b.price));
      if (sort === "priceDesc") list.sort((a,b)=> Number(b.price)-Number(a.price));

      renderProducts(list);
    }

    async function checkHealth() {
      try {
        const data = await apiFetch(API.health);
        const ok = data && (data.status === "UP" || data.status === "up");
        const pill = document.getElementById("healthPill");
        pill.className = "badge rounded-pill " + (ok ? "text-bg-success" : "text-bg-danger");
        pill.textContent = ok ? "UP" : (data.status || "DOWN");
      } catch {
        const pill = document.getElementById("healthPill");
        pill.className = "badge rounded-pill text-bg-danger";
        pill.textContent = "DOWN";
      }
    }

    // -------------------------
    // AUTH
    // -------------------------
    async function login() {
      const email = document.getElementById("loginEmail").value.trim();
      const password = document.getElementById("loginPassword").value;

      if (!email || !password) { toast("Email ve şifre gerekli"); return; }

      try {
        // Beklenen: { accessToken: "..." }
        const data = await apiFetch(API.login, { method: "POST", json: { email, password } });
        const accessToken = data.accessToken || data.token || data.jwt;
        if (!accessToken) throw new Error("Token yok (login response'u uyarlayın)");
        localStorage.setItem(LS_TOKEN, accessToken);
        updateAuthUI();
        toast("Giriş başarılı");
        bootstrap.Modal.getOrCreateInstance(document.getElementById("loginModal")).hide();
      } catch (e) {
        toast("Giriş başarısız: " + e.message);
      }
    }

    function logout() {
      localStorage.removeItem(LS_TOKEN);
      updateAuthUI();
      toast("Çıkış yapıldı");
    }

    function updateAuthUI() {
      const t = token();
      document.getElementById("authPill").textContent = t ? "Bearer ✓" : "—";
      const a = document.getElementById("authState");
      if (t) { a.textContent = "Authenticated"; a.classList.remove("d-none"); }
      else { a.textContent = ""; a.classList.add("d-none"); }
    }

    // -------------------------
    // ADDRESSES
    // -------------------------
    async function openAddresses() {
      await loadAddresses();
      bootstrap.Modal.getOrCreateInstance(document.getElementById("addressesModal")).show();
    }

    async function loadAddresses() {
      try {
        addresses = await apiFetch(API.addresses);
        renderAddresses();
      } catch (e) {
        addresses = [];
        document.getElementById("addressesList").innerHTML =
          `<div class="col-12"><div class="alert alert-warning">Adresler yüklenemedi: ${escapeHtml(e.message)} (Login gerekli olabilir)</div></div>`;
      }
    }

    function renderAddresses() {
      const wrap = document.getElementById("addressesList");
      wrap.innerHTML = "";
      if (!addresses.length) {
        wrap.innerHTML = `<div class="col-12"><div class="alert alert-light border">Adres yok.</div></div>`;
        return;
      }
      addresses.forEach(a => {
        const col = document.createElement("div");
        col.className = "col-12 col-md-6";
        col.innerHTML = `
          <div class="border bg-white rounded-2xl p-3 h-100">
            <div class="d-flex justify-content-between align-items-start">
              <div>
                <div class="fw-semibold">${escapeHtml(a.label || "Address")}${a.isDefault ? ' <span class="badge text-bg-success ms-1">Default</span>' : ""}</div>
                <div class="small muted">${escapeHtml(a.fullName)} • ${escapeHtml(a.phone || "")}</div>
              </div>
              <button class="btn btn-sm btn-outline-dark" onclick="setDefaultAddress(${a.id})">
                Default
              </button>
            </div>
            <div class="small mt-2">
              ${escapeHtml(a.line1)} ${escapeHtml(a.line2 || "")}<br>
              ${escapeHtml(a.postalCode)} ${escapeHtml(a.city)} ${escapeHtml(a.state || "")}<br>
              ${escapeHtml(a.countryCode)}
            </div>
          </div>
        `;
        wrap.appendChild(col);
      });
    }

    function openNewAddress() {
      bootstrap.Modal.getOrCreateInstance(document.getElementById("newAddressModal")).show();
    }

    async function createAddress() {
      try {
        const payload = {
          label: document.getElementById("addrLabel").value || null,
          fullName: document.getElementById("addrFullName").value,
          phone: document.getElementById("addrPhone").value || null,
          line1: document.getElementById("addrLine1").value,
          line2: document.getElementById("addrLine2").value || null,
          city: document.getElementById("addrCity").value,
          state: document.getElementById("addrState").value || null,
          postalCode: document.getElementById("addrPostal").value,
          countryCode: (document.getElementById("addrCountry").value || "DE").toUpperCase(),
          makeDefault: document.getElementById("addrDefault").checked
        };
        await apiFetch(API.addresses, { method: "POST", json: payload });
        toast("Adres kaydedildi");
        bootstrap.Modal.getOrCreateInstance(document.getElementById("newAddressModal")).hide();
        await loadAddresses();
      } catch (e) {
        toast("Adres eklenemedi: " + e.message);
      }
    }

    async function setDefaultAddress(id) {
      try {
        await apiFetch(`${API.addresses}/${id}/default`, { method: "POST" });
        toast("Default adres güncellendi");
        await loadAddresses();
      } catch (e) {
        toast("Default set edilemedi: " + e.message);
      }
    }

    // -------------------------
    // CHECKOUT / PAY (uyarlanabilir)
    // -------------------------
    async function openCheckout() {
      await loadAddresses();
      renderCheckoutAddresses();
      bootstrap.Modal.getOrCreateInstance(document.getElementById("checkoutModal")).show();
    }

    function renderCheckoutAddresses() {
      const wrap = document.getElementById("checkoutAddresses");
      wrap.innerHTML = "";

      if (!addresses.length) {
        wrap.innerHTML = `<div class="alert alert-light border">Adres yok. Önce adres ekle.</div>`;
        return;
      }

      // default seç
      const def = addresses.find(a => a.isDefault) || addresses[0];
      selectedAddressId = selectedAddressId || def.id;

      addresses.forEach(a => {
        const id = `addr_${a.id}`;
        const div = document.createElement("div");
        div.className = "border rounded-2xl p-3 bg-white";
        div.innerHTML = `
          <div class="form-check">
            <input class="form-check-input" type="radio" name="addrRadio" id="${id}" ${a.id === selectedAddressId ? "checked" : ""} onchange="selectedAddressId=${a.id}">
            <label class="form-check-label" for="${id}">
              <div class="fw-semibold">${escapeHtml(a.label || "Address")} ${a.isDefault ? '<span class="badge text-bg-success ms-1">Default</span>' : ""}</div>
              <div class="small muted">${escapeHtml(a.fullName)} • ${escapeHtml(a.phone || "")}</div>
              <div class="small">${escapeHtml(a.line1)} ${escapeHtml(a.line2 || "")} — ${escapeHtml(a.postalCode)} ${escapeHtml(a.city)} (${escapeHtml(a.countryCode)})</div>
            </label>
          </div>
        `;
        wrap.appendChild(div);
      });
    }

    async function startPayment() {
      const c = getCart();
      if (!c.items.length) { toast("Sepet boş"); return; }
      if (!selectedAddressId) { toast("Adres seç"); return; }

      try {
        // 1) Draft create/reserve (endpoint sende farklı olabilir)
        // Beklenen response: { draftId: "..." } veya benzeri
        const draftPayload = {
          currency: c.currency,
          items: c.items.map(i => ({ productId: i.id, quantity: i.qty }))
        };

        const draftResp = await apiFetch(API.draftCreate, { method: "POST", json: draftPayload });
        lastDraftId = draftResp.draftId || draftResp.id || draftResp.publicId;
        if (!lastDraftId) throw new Error("Draft id yok (draft endpoint response'u uyarlayın)");

        // 2) Pay
        const payResp = await apiFetch(API.payDraft(lastDraftId), {
          method: "POST",
          headers: { "Idempotency-Key": `pay-${lastDraftId}-1` },
          json: { addressId: selectedAddressId }
        });

        lastPaymentId = payResp.paymentId;
        lastOrderId = payResp.orderId;

        document.getElementById("lastPaymentId").textContent = lastPaymentId || "—";
        document.getElementById("lastOrderId").textContent = lastOrderId || "—";
        toast("Ödeme başlatıldı");
      } catch (e) {
        toast("Checkout başarısız: " + e.message);
      }
    }

    async function confirmMockPayment() {
      if (!lastPaymentId) { toast("Önce ödeme başlat"); return; }
      try {
        await apiFetch(API.confirmMock(lastPaymentId), { method: "POST" });
        toast("Mock payment confirmed");
        // başarılı ödeme sonrası sepeti temizlemek iyi UX
        clearCart();
      } catch (e) {
        toast("Mock confirm başarısız: " + e.message);
      }
    }

    // -------------------------
    // EVENTS
    // -------------------------
    document.getElementById("searchInput").addEventListener("input", applySearchSort);
    document.getElementById("sortSelect").addEventListener("change", applySearchSort);

    // INIT
    updateAuthUI();
    renderCart();
    checkHealth();
    reloadProducts();
  </script>
</body>
</html>
```

---

### İstersen bir sonraki adım

Bunu bir tık daha “gerçek ürün” gibi yapalım:

* **Header’da kategori filtresi**,
* Ürün detay sayfası (modal yerine route),
* Admin panel (orders/refunds/webhook-events) için ayrı `/admin.html`,
* Bootstrap + Thymeleaf layout (fragment) şeklinde bölmek.

Sende şu an ürün endpoint’i tam olarak ne dönüyor? (`/api/v1/products` response örneğini yapıştırırsan) bu UI’daki `normalizeProductsResponse()` ve alan eşlemesini %100 senin modele uyarlayıp “tak-çalıştır” hale getiririm.
