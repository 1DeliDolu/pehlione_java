(() => {
    const API = {
        products: "/api/v1/products",
        productById: (id) => `/api/v1/products/${id}`,
        productImages: (productId) => `/api/v1/products/${productId}/images`,
        productImageById: (productId, imageId) =>
            `/api/v1/products/${productId}/images/${imageId}`,
        categories: "/api/v1/categories",
        categoryById: (id) => `/api/v1/categories/${id}`,
        orders: "/api/v1/orders",
        orderById: (orderId) => `/api/v1/orders/${orderId}`,
        login: "/api/v1/auth/login",
        logout: "/api/v1/auth/logout",
        me: "/api/v1/me",
        changeMyPassword: "/api/v1/me/password",
        addresses: "/api/v1/addresses",
        addressById: (id) => `/api/v1/addresses/${id}`,
        cart: "/api/v1/cart",
        cartItems: "/api/v1/cart/items",
        reserveDraft: "/api/v1/checkout/reserve",
        payDraft: (draftId) => `/api/v1/checkout/drafts/${draftId}/pay`,
        confirmMock: (paymentId) => `/api/v1/payments/${paymentId}/confirm-mock`,
        adminOrders: "/api/v1/admin/orders",
        adminOrderById: (orderId) => `/api/v1/admin/orders/${orderId}`,
        adminOrderShip: (orderId) => `/api/v1/admin/orders/${orderId}/ship`,
        adminOrderDeliver: (orderId) => `/api/v1/admin/orders/${orderId}/deliver`,
        adminOrderCancel: (orderId) => `/api/v1/admin/orders/${orderId}/cancel`,
        adminRestock: (productId) =>
            `/api/v1/admin/inventory/products/${productId}/restock`,
        adminAdjust: (productId) =>
            `/api/v1/admin/inventory/products/${productId}/adjust`,
        adminRefunds: "/api/v1/admin/refunds",
        adminWebhooks: "/api/v1/admin/webhook-events",
        health: "/api/v1/products?page=0&size=1",
    };

    const LS_TOKEN = "auth.token";
    const LS_CART = "cart.v1";
    const LS_PROFILE_CARD_PREFIX = "profile.card.v1";

    let products = [];
    let categoryFilters = [];
    let addresses = [];
    let selectedAddressId = null;
    let lastDraftId = null;
    let lastPaymentId = null;
    let lastOrderId = null;
    let currentIdentity = null;
    let profileInfo = null;
    let adminBootstrapped = false;
    let checkoutStep = 1;
    let currentView = "home";

    const CHECKOUT_TOTAL_STEPS = 5;

    const els = {
        homeSection: null,
        catalogSection: null,
        catalogSearchWrap: null,
        navHomeLink: null,
        navProductsLink: null,
        openProductsViewBtn: null,
        productGrid: null,
        resultInfo: null,
        searchInput: null,
        sortSelect: null,
        categoryFilterList: null,
        filterPriceMin: null,
        filterPriceMax: null,
        clearFiltersBtn: null,
        cartCount: null,
        cartItemsPill: null,
        cartList: null,
        cartEmpty: null,
        cartSubtotal: null,
        cartCurrency: null,
        checkoutTotal: null,
        checkoutCurrency: null,
        checkoutStepIndicator: null,
        checkoutProgressBar: null,
        checkoutPrevBtn: null,
        checkoutNextBtn: null,
        checkoutLoginStatus: null,
        checkoutReviewBox: null,
        checkoutTermsCheck: null,
        invoiceFullName: null,
        invoiceEmail: null,
        invoiceType: null,
        invoiceTaxNumber: null,
        invoiceNote: null,
        paymentMethodCredit: null,
        paymentMethodDebit: null,
        paymentMethodPaypal: null,
        paymentCardFields: null,
        loginEmail: null,
        loginPassword: null,
        authPill: null,
        authState: null,
        healthPill: null,
        addressesList: null,
        checkoutAddresses: null,
        lastPaymentId: null,
        lastOrderId: null,
        openLoginBtn: null,
        profileMenuWrap: null,
        profileName: null,
        profileRolePill: null,
        openOrdersBtn: null,
        openDashboardBtn: null,
        logoutMenuBtn: null,
        reloadProductsBtn: null,
        openAddressesBtn: null,
        clearCartBtn: null,
        openCheckoutBtn: null,
        loginSubmitBtn: null,
        openNewAddressBtn: null,
        createAddressBtn: null,
        startPaymentBtn: null,
        confirmPaymentBtn: null,
        loginModal: null,
        dashboardModal: null,
        ordersModal: null,
        addressesModal: null,
        checkoutModal: null,
        newAddressModal: null,
        dashboardUserEmail: null,
        dashboardRoleList: null,
        dashboardUserSection: null,
        dashboardAdminSection: null,
        dashboardWorkerSection: null,
        dashboardAddressesBtn: null,
        dashboardCheckoutBtn: null,
        profileEmailValue: null,
        profileRolesValue: null,
        profileStatusValue: null,
        profileCreatedAtValue: null,
        profileCurrentPassword: null,
        profileNewPassword: null,
        profileConfirmPassword: null,
        profilePasswordChangeBtn: null,
        profileAddressSelect: null,
        profileAddressLoadBtn: null,
        profileAddrLabel: null,
        profileAddrFullName: null,
        profileAddrPhone: null,
        profileAddrCountry: null,
        profileAddrStreet: null,
        profileAddrHouseNumber: null,
        profileAddrLine2: null,
        profileAddrCity: null,
        profileAddrState: null,
        profileAddrPostal: null,
        profileAddrDefault: null,
        profileAddressUpdateBtn: null,
        profileAddressResult: null,
        profileCardHolderName: null,
        profileCardNumber: null,
        profileCardExpMonth: null,
        profileCardExpYear: null,
        profileCardSaveBtn: null,
        profileCardClearBtn: null,
        profileCardResult: null,
        ordersLoadBtn: null,
        ordersBody: null,
        ordersMeta: null,
        ordersDetailBox: null,
        adminOrderStatus: null,
        adminOrderEmail: null,
        adminOrderQuery: null,
        adminOrdersLoadBtn: null,
        adminOrdersBody: null,
        adminOrdersMeta: null,
        adminOrderIdInput: null,
        adminShipCarrier: null,
        adminShipTracking: null,
        adminCancelReason: null,
        adminOrderDetailBtn: null,
        adminShipBtn: null,
        adminDeliverBtn: null,
        adminCancelBtn: null,
        adminOrderDetailBox: null,
        adminInventoryProductId: null,
        adminRestockQty: null,
        adminAdjustDelta: null,
        adminInventoryReason: null,
        adminRestockBtn: null,
        adminAdjustBtn: null,
        adminInventoryResult: null,
        adminRefundStatus: null,
        adminRefundEmail: null,
        adminRefundOrderId: null,
        adminRefundsLoadBtn: null,
        adminRefundsBody: null,
        adminRefundsMeta: null,
        adminWebhookProvider: null,
        adminWebhookEventId: null,
        adminWebhookLoadBtn: null,
        adminWebhookBody: null,
        adminWebhookMeta: null,
        adminCategoryId: null,
        adminCategorySlug: null,
        adminCategoryName: null,
        adminCategoryCreateBtn: null,
        adminCategoryUpdateBtn: null,
        adminCategoryDeleteBtn: null,
        adminCategoryResult: null,
        adminProductSearch: null,
        adminProductsLoadBtn: null,
        adminProductsBody: null,
        adminProductsMeta: null,
        adminProductId: null,
        adminProductSku: null,
        adminProductName: null,
        adminProductPrice: null,
        adminProductCurrency: null,
        adminProductStock: null,
        adminProductStatus: null,
        adminProductCategoryIds: null,
        adminProductDescription: null,
        adminProductCreateBtn: null,
        adminProductUpdateBtn: null,
        adminProductDeleteBtn: null,
        adminProductResult: null,
        adminImageProductId: null,
        adminImageUrl: null,
        adminImageAlt: null,
        adminImageId: null,
        adminImageAddBtn: null,
        adminImageDeleteBtn: null,
        adminImageResult: null,
        cardHolderName: null,
        cardNumber: null,
        cardExpMonth: null,
        cardExpYear: null,
        cardCvc: null,
        addrLabel: null,
        addrFullName: null,
        addrPhone: null,
        addrCountry: null,
        addrStreet: null,
        addrHouseNumber: null,
        addrLine2: null,
        addrCity: null,
        addrState: null,
        addrPostal: null,
        addrDefault: null,
    };

    function cacheElements() {
        els.homeSection = document.getElementById("homeSection");
        els.catalogSection = document.getElementById("catalogSection");
        els.catalogSearchWrap = document.getElementById("catalogSearchWrap");
        els.navHomeLink = document.getElementById("navHomeLink");
        els.navProductsLink = document.getElementById("navProductsLink");
        els.openProductsViewBtn = document.getElementById("openProductsViewBtn");
        els.productGrid = document.getElementById("productGrid");
        els.resultInfo = document.getElementById("resultInfo");
        els.searchInput = document.getElementById("searchInput");
        els.sortSelect = document.getElementById("sortSelect");
        els.categoryFilterList = document.getElementById("categoryFilterList");
        els.filterPriceMin = document.getElementById("filterPriceMin");
        els.filterPriceMax = document.getElementById("filterPriceMax");
        els.clearFiltersBtn = document.getElementById("clearFiltersBtn");
        els.cartCount = document.getElementById("cartCount");
        els.cartItemsPill = document.getElementById("cartItemsPill");
        els.cartList = document.getElementById("cartList");
        els.cartEmpty = document.getElementById("cartEmpty");
        els.cartSubtotal = document.getElementById("cartSubtotal");
        els.cartCurrency = document.getElementById("cartCurrency");
        els.checkoutTotal = document.getElementById("checkoutTotal");
        els.checkoutCurrency = document.getElementById("checkoutCurrency");
        els.checkoutStepIndicator = document.getElementById(
            "checkoutStepIndicator",
        );
        els.checkoutProgressBar = document.getElementById("checkoutProgressBar");
        els.checkoutPrevBtn = document.getElementById("checkoutPrevBtn");
        els.checkoutNextBtn = document.getElementById("checkoutNextBtn");
        els.checkoutLoginStatus = document.getElementById("checkoutLoginStatus");
        els.checkoutReviewBox = document.getElementById("checkoutReviewBox");
        els.checkoutTermsCheck = document.getElementById("checkoutTermsCheck");
        els.invoiceFullName = document.getElementById("invoiceFullName");
        els.invoiceEmail = document.getElementById("invoiceEmail");
        els.invoiceType = document.getElementById("invoiceType");
        els.invoiceTaxNumber = document.getElementById("invoiceTaxNumber");
        els.invoiceNote = document.getElementById("invoiceNote");
        els.paymentMethodCredit = document.getElementById("paymentMethodCredit");
        els.paymentMethodDebit = document.getElementById("paymentMethodDebit");
        els.paymentMethodPaypal = document.getElementById("paymentMethodPaypal");
        els.paymentCardFields = document.getElementById("paymentCardFields");
        els.loginEmail = document.getElementById("loginEmail");
        els.loginPassword = document.getElementById("loginPassword");
        els.authPill = document.getElementById("authPill");
        els.authState = document.getElementById("authState");
        els.healthPill = document.getElementById("healthPill");
        els.addressesList = document.getElementById("addressesList");
        els.checkoutAddresses = document.getElementById("checkoutAddresses");
        els.lastPaymentId = document.getElementById("lastPaymentId");
        els.lastOrderId = document.getElementById("lastOrderId");
        els.openLoginBtn = document.getElementById("openLoginBtn");
        els.profileMenuWrap = document.getElementById("profileMenuWrap");
        els.profileName = document.getElementById("profileName");
        els.profileRolePill = document.getElementById("profileRolePill");
        els.openOrdersBtn = document.getElementById("openOrdersBtn");
        els.openDashboardBtn = document.getElementById("openDashboardBtn");
        els.logoutMenuBtn = document.getElementById("logoutMenuBtn");
        els.reloadProductsBtn = document.getElementById("reloadProductsBtn");
        els.openAddressesBtn = document.getElementById("openAddressesBtn");
        els.clearCartBtn = document.getElementById("clearCartBtn");
        els.openCheckoutBtn = document.getElementById("openCheckoutBtn");
        els.loginSubmitBtn = document.getElementById("loginSubmitBtn");
        els.openNewAddressBtn = document.getElementById("openNewAddressBtn");
        els.createAddressBtn = document.getElementById("createAddressBtn");
        els.startPaymentBtn = document.getElementById("startPaymentBtn");
        els.confirmPaymentBtn = document.getElementById("confirmPaymentBtn");
        els.loginModal = document.getElementById("loginModal");
        els.dashboardModal = document.getElementById("dashboardModal");
        els.ordersModal = document.getElementById("ordersModal");
        els.addressesModal = document.getElementById("addressesModal");
        els.checkoutModal = document.getElementById("checkoutModal");
        els.newAddressModal = document.getElementById("newAddressModal");
        els.dashboardUserEmail = document.getElementById("dashboardUserEmail");
        els.dashboardRoleList = document.getElementById("dashboardRoleList");
        els.dashboardUserSection = document.getElementById("dashboardUserSection");
        els.dashboardAdminSection = document.getElementById(
            "dashboardAdminSection",
        );
        els.dashboardWorkerSection = document.getElementById(
            "dashboardWorkerSection",
        );
        els.dashboardAddressesBtn = document.getElementById(
            "dashboardAddressesBtn",
        );
        els.dashboardCheckoutBtn = document.getElementById("dashboardCheckoutBtn");
        els.profileEmailValue = document.getElementById("profileEmailValue");
        els.profileRolesValue = document.getElementById("profileRolesValue");
        els.profileStatusValue = document.getElementById("profileStatusValue");
        els.profileCreatedAtValue = document.getElementById(
            "profileCreatedAtValue",
        );
        els.profileCurrentPassword = document.getElementById(
            "profileCurrentPassword",
        );
        els.profileNewPassword = document.getElementById("profileNewPassword");
        els.profileConfirmPassword = document.getElementById(
            "profileConfirmPassword",
        );
        els.profilePasswordChangeBtn = document.getElementById(
            "profilePasswordChangeBtn",
        );
        els.profileAddressSelect = document.getElementById("profileAddressSelect");
        els.profileAddressLoadBtn = document.getElementById(
            "profileAddressLoadBtn",
        );
        els.profileAddrLabel = document.getElementById("profileAddrLabel");
        els.profileAddrFullName = document.getElementById("profileAddrFullName");
        els.profileAddrPhone = document.getElementById("profileAddrPhone");
        els.profileAddrCountry = document.getElementById("profileAddrCountry");
        els.profileAddrStreet = document.getElementById("profileAddrStreet");
        els.profileAddrHouseNumber = document.getElementById(
            "profileAddrHouseNumber",
        );
        els.profileAddrLine2 = document.getElementById("profileAddrLine2");
        els.profileAddrCity = document.getElementById("profileAddrCity");
        els.profileAddrState = document.getElementById("profileAddrState");
        els.profileAddrPostal = document.getElementById("profileAddrPostal");
        els.profileAddrDefault = document.getElementById("profileAddrDefault");
        els.profileAddressUpdateBtn = document.getElementById(
            "profileAddressUpdateBtn",
        );
        els.profileAddressResult = document.getElementById("profileAddressResult");
        els.profileCardHolderName = document.getElementById(
            "profileCardHolderName",
        );
        els.profileCardNumber = document.getElementById("profileCardNumber");
        els.profileCardExpMonth = document.getElementById("profileCardExpMonth");
        els.profileCardExpYear = document.getElementById("profileCardExpYear");
        els.profileCardSaveBtn = document.getElementById("profileCardSaveBtn");
        els.profileCardClearBtn = document.getElementById("profileCardClearBtn");
        els.profileCardResult = document.getElementById("profileCardResult");
        els.ordersLoadBtn = document.getElementById("ordersLoadBtn");
        els.ordersBody = document.getElementById("ordersBody");
        els.ordersMeta = document.getElementById("ordersMeta");
        els.ordersDetailBox = document.getElementById("ordersDetailBox");
        els.adminOrderStatus = document.getElementById("adminOrderStatus");
        els.adminOrderEmail = document.getElementById("adminOrderEmail");
        els.adminOrderQuery = document.getElementById("adminOrderQuery");
        els.adminOrdersLoadBtn = document.getElementById("adminOrdersLoadBtn");
        els.adminOrdersBody = document.getElementById("adminOrdersBody");
        els.adminOrdersMeta = document.getElementById("adminOrdersMeta");
        els.adminOrderIdInput = document.getElementById("adminOrderIdInput");
        els.adminShipCarrier = document.getElementById("adminShipCarrier");
        els.adminShipTracking = document.getElementById("adminShipTracking");
        els.adminCancelReason = document.getElementById("adminCancelReason");
        els.adminOrderDetailBtn = document.getElementById("adminOrderDetailBtn");
        els.adminShipBtn = document.getElementById("adminShipBtn");
        els.adminDeliverBtn = document.getElementById("adminDeliverBtn");
        els.adminCancelBtn = document.getElementById("adminCancelBtn");
        els.adminOrderDetailBox = document.getElementById("adminOrderDetailBox");
        els.adminInventoryProductId = document.getElementById(
            "adminInventoryProductId",
        );
        els.adminRestockQty = document.getElementById("adminRestockQty");
        els.adminAdjustDelta = document.getElementById("adminAdjustDelta");
        els.adminInventoryReason = document.getElementById("adminInventoryReason");
        els.adminRestockBtn = document.getElementById("adminRestockBtn");
        els.adminAdjustBtn = document.getElementById("adminAdjustBtn");
        els.adminInventoryResult = document.getElementById("adminInventoryResult");
        els.adminRefundStatus = document.getElementById("adminRefundStatus");
        els.adminRefundEmail = document.getElementById("adminRefundEmail");
        els.adminRefundOrderId = document.getElementById("adminRefundOrderId");
        els.adminRefundsLoadBtn = document.getElementById("adminRefundsLoadBtn");
        els.adminRefundsBody = document.getElementById("adminRefundsBody");
        els.adminRefundsMeta = document.getElementById("adminRefundsMeta");
        els.adminWebhookProvider = document.getElementById("adminWebhookProvider");
        els.adminWebhookEventId = document.getElementById("adminWebhookEventId");
        els.adminWebhookLoadBtn = document.getElementById("adminWebhookLoadBtn");
        els.adminWebhookBody = document.getElementById("adminWebhookBody");
        els.adminWebhookMeta = document.getElementById("adminWebhookMeta");
        els.adminCategoryId = document.getElementById("adminCategoryId");
        els.adminCategorySlug = document.getElementById("adminCategorySlug");
        els.adminCategoryName = document.getElementById("adminCategoryName");
        els.adminCategoryCreateBtn = document.getElementById(
            "adminCategoryCreateBtn",
        );
        els.adminCategoryUpdateBtn = document.getElementById(
            "adminCategoryUpdateBtn",
        );
        els.adminCategoryDeleteBtn = document.getElementById(
            "adminCategoryDeleteBtn",
        );
        els.adminCategoryResult = document.getElementById("adminCategoryResult");
        els.adminProductSearch = document.getElementById("adminProductSearch");
        els.adminProductsLoadBtn = document.getElementById("adminProductsLoadBtn");
        els.adminProductsBody = document.getElementById("adminProductsBody");
        els.adminProductsMeta = document.getElementById("adminProductsMeta");
        els.adminProductId = document.getElementById("adminProductId");
        els.adminProductSku = document.getElementById("adminProductSku");
        els.adminProductName = document.getElementById("adminProductName");
        els.adminProductPrice = document.getElementById("adminProductPrice");
        els.adminProductCurrency = document.getElementById("adminProductCurrency");
        els.adminProductStock = document.getElementById("adminProductStock");
        els.adminProductStatus = document.getElementById("adminProductStatus");
        els.adminProductCategoryIds = document.getElementById(
            "adminProductCategoryIds",
        );
        els.adminProductDescription = document.getElementById(
            "adminProductDescription",
        );
        els.adminProductCreateBtn = document.getElementById(
            "adminProductCreateBtn",
        );
        els.adminProductUpdateBtn = document.getElementById(
            "adminProductUpdateBtn",
        );
        els.adminProductDeleteBtn = document.getElementById(
            "adminProductDeleteBtn",
        );
        els.adminProductResult = document.getElementById("adminProductResult");
        els.adminImageProductId = document.getElementById("adminImageProductId");
        els.adminImageUrl = document.getElementById("adminImageUrl");
        els.adminImageAlt = document.getElementById("adminImageAlt");
        els.adminImageId = document.getElementById("adminImageId");
        els.adminImageAddBtn = document.getElementById("adminImageAddBtn");
        els.adminImageDeleteBtn = document.getElementById("adminImageDeleteBtn");
        els.adminImageResult = document.getElementById("adminImageResult");
        els.cardHolderName = document.getElementById("cardHolderName");
        els.cardNumber = document.getElementById("cardNumber");
        els.cardExpMonth = document.getElementById("cardExpMonth");
        els.cardExpYear = document.getElementById("cardExpYear");
        els.cardCvc = document.getElementById("cardCvc");
        els.addrLabel = document.getElementById("addrLabel");
        els.addrFullName = document.getElementById("addrFullName");
        els.addrPhone = document.getElementById("addrPhone");
        els.addrCountry = document.getElementById("addrCountry");
        els.addrStreet = document.getElementById("addrStreet");
        els.addrHouseNumber = document.getElementById("addrHouseNumber");
        els.addrLine2 = document.getElementById("addrLine2");
        els.addrCity = document.getElementById("addrCity");
        els.addrState = document.getElementById("addrState");
        els.addrPostal = document.getElementById("addrPostal");
        els.addrDefault = document.getElementById("addrDefault");
    }

    function toast(message) {
        document.getElementById("toastBody").textContent = message;
        bootstrap.Toast.getOrCreateInstance(document.getElementById("toast"), {
            delay: 2300,
        }).show();
    }

    function showModal(el) {
        bootstrap.Modal.getOrCreateInstance(el).show();
    }

    function hideModal(el) {
        bootstrap.Modal.getOrCreateInstance(el).hide();
    }

    function normalizedHash(hashValue) {
        return String(hashValue || "")
            .replace(/^#/, "")
            .toLowerCase();
    }

    function normalizedPath() {
        const raw = String(window.location.pathname || "").trim();
        if (!raw || raw === "/") {
            return "/";
        }
        return raw.replace(/\/+$/, "") || "/";
    }

    function viewFromHash(hashValue) {
        const normalized = normalizedHash(hashValue);
        if (normalized === "productssection" || normalized === "products") {
            return "products";
        }
        if (!normalized && normalizedPath() === "/products") {
            return "products";
        }
        return "home";
    }

    function renderViewState() {
        const isProductsView = currentView === "products";
        if (els.homeSection) {
            els.homeSection.classList.toggle("d-none", isProductsView);
        }
        if (els.catalogSection) {
            els.catalogSection.classList.toggle("d-none", !isProductsView);
        }
        if (els.navHomeLink) {
            els.navHomeLink.classList.toggle("active", !isProductsView);
        }
        if (els.navProductsLink) {
            els.navProductsLink.classList.toggle("active", isProductsView);
        }
    }

    function switchView(view, syncHash = true) {
        currentView = view === "products" ? "products" : "home";
        renderViewState();
        if (!syncHash) {
            return;
        }
        const nextHash =
            currentView === "products" ? "#productsSection" : "#homeSection";
        if (window.location.hash !== nextHash) {
            history.replaceState(null, "", nextHash);
        }
    }

    function getToken() {
        return localStorage.getItem(LS_TOKEN);
    }

    function setToken(value) {
        if (!value) {
            localStorage.removeItem(LS_TOKEN);
            return;
        }
        localStorage.setItem(LS_TOKEN, value);
    }

    function parseJwtPayload(token) {
        if (!token) {
            return null;
        }
        const parts = token.split(".");
        if (parts.length < 2) {
            return null;
        }

        try {
            const normalized = parts[1].replace(/-/g, "+").replace(/_/g, "/");
            const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
            return JSON.parse(atob(padded));
        } catch (_) {
            return null;
        }
    }

    function extractRolesFromClaims(claims) {
        if (!claims) {
            return [];
        }

        const raw = [];
        if (typeof claims.scope === "string") {
            raw.push(...claims.scope.split(/\s+/));
        }
        if (Array.isArray(claims.scope)) {
            raw.push(...claims.scope);
        }
        if (Array.isArray(claims.scp)) {
            raw.push(...claims.scp);
        }
        if (typeof claims.authorities === "string") {
            raw.push(...claims.authorities.split(/\s+/));
        }
        if (Array.isArray(claims.authorities)) {
            raw.push(...claims.authorities);
        }

        return [
            ...new Set(raw.map((it) => String(it || "").trim()).filter(Boolean)),
        ];
    }

    function resolveIdentity() {
        const token = getToken();
        if (!token) {
            return null;
        }

        const claims = parseJwtPayload(token);
        const roles = extractRolesFromClaims(claims);

        return {
            email: claims && claims.sub ? String(claims.sub) : "authenticated",
            roles,
        };
    }

    function hasRole(identity, roleName) {
        if (!identity) {
            return false;
        }
        return identity.roles.includes(roleName);
    }

    function isWorker(identity) {
        if (!identity) {
            return false;
        }
        return identity.roles.some(
            (role) =>
                role === "ROLE_WORKER" ||
                role === "ROLE_EMPLOYEE" ||
                role === "ROLE_STAFF",
        );
    }

    function roleLabel(identity) {
        if (hasRole(identity, "ROLE_ADMIN")) {
            return "ADMIN";
        }
        if (isWorker(identity)) {
            return "CALISAN";
        }
        if (identity && identity.roles.length) {
            return identity.roles[0];
        }
        return "USER";
    }

    async function loadProfileInfo() {
        if (!currentIdentity) {
            profileInfo = null;
            return;
        }
        try {
            const data = await apiFetch(API.me);
            profileInfo = data && typeof data === "object" ? data : null;
        } catch (_) {
            profileInfo = null;
        }
    }

    function resolvedRoles(identity) {
        if (
            profileInfo &&
            Array.isArray(profileInfo.roles) &&
            profileInfo.roles.length
        ) {
            return profileInfo.roles;
        }
        return identity ? identity.roles : [];
    }

    function profileCardStorageKey() {
        if (!currentIdentity || !currentIdentity.email) {
            return null;
        }
        return `${LS_PROFILE_CARD_PREFIX}:${String(currentIdentity.email).toLowerCase()}`;
    }

    function getSavedProfileCard() {
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

    function saveProfileCard(card) {
        const key = profileCardStorageKey();
        if (!key) {
            return;
        }
        localStorage.setItem(key, JSON.stringify(card));
    }

    function clearProfileCard() {
        const key = profileCardStorageKey();
        if (!key) {
            return;
        }
        localStorage.removeItem(key);
    }

    function maskCardNumber(cardNumber) {
        const digits = normalizeDigits(cardNumber);
        if (digits.length < 4) {
            return "****";
        }
        return `**** **** **** ${digits.slice(-4)}`;
    }

    function renderSavedCardResult() {
        if (!els.profileCardResult) {
            return;
        }
        const saved = getSavedProfileCard();
        if (!saved || !saved.cardNumber) {
            els.profileCardResult.textContent = "No saved card";
            return;
        }
        els.profileCardResult.textContent = `${saved.cardHolderName || "Card"} | ${maskCardNumber(saved.cardNumber)} | ${String(saved.expiryMonth).padStart(2, "0")}/${saved.expiryYear}`;
    }

    function hydrateProfileCardForm() {
        const saved = getSavedProfileCard();
        if (!saved) {
            if (els.profileCardHolderName) {
                els.profileCardHolderName.value = "";
            }
            if (els.profileCardNumber) {
                els.profileCardNumber.value = "";
            }
            if (els.profileCardExpMonth) {
                els.profileCardExpMonth.value = "";
            }
            if (els.profileCardExpYear) {
                els.profileCardExpYear.value = "";
            }
            renderSavedCardResult();
            return;
        }
        els.profileCardHolderName.value = saved.cardHolderName || "";
        els.profileCardNumber.value = saved.cardNumber || "";
        els.profileCardExpMonth.value = Number.isFinite(saved.expiryMonth)
            ? String(saved.expiryMonth)
            : "";
        els.profileCardExpYear.value = Number.isFinite(saved.expiryYear)
            ? String(saved.expiryYear)
            : "";
        renderSavedCardResult();
    }

    function renderDashboard(identity) {
        const roles = resolvedRoles(identity);
        const isAdmin = roles.includes("ROLE_ADMIN");
        const worker = roles.some(
            (role) =>
                role === "ROLE_WORKER" ||
                role === "ROLE_EMPLOYEE" ||
                role === "ROLE_STAFF",
        );
        const email =
            profileInfo && profileInfo.email
                ? profileInfo.email
                : identity
                    ? identity.email
                    : "-";

        els.dashboardUserEmail.textContent = email;
        els.dashboardRoleList.textContent = roles.length ? roles.join(", ") : "-";
        if (els.profileEmailValue) {
            els.profileEmailValue.textContent = email;
        }
        if (els.profileRolesValue) {
            els.profileRolesValue.textContent = roles.length ? roles.join(", ") : "-";
        }
        if (els.profileStatusValue) {
            const enabledText =
                profileInfo && profileInfo.enabled === false ? "Disabled" : "Enabled";
            const lockedText =
                profileInfo && profileInfo.locked ? "Locked" : "Unlocked";
            els.profileStatusValue.textContent = `${enabledText} / ${lockedText}`;
        }
        if (els.profileCreatedAtValue) {
            els.profileCreatedAtValue.textContent = profileInfo
                ? formatDateTime(profileInfo.createdAt)
                : "-";
        }

        els.dashboardUserSection.classList.toggle("d-none", !identity);
        els.dashboardAdminSection.classList.toggle("d-none", !isAdmin);
        els.dashboardWorkerSection.classList.toggle("d-none", !worker);
    }

    async function openDashboard() {
        if (!currentIdentity) {
            toast("Dashboard icin once giris yapin");
            showModal(els.loginModal);
            return;
        }

        await loadProfileInfo();
        await loadAddresses();
        hydrateProfileAddressEditor();
        hydrateProfileCardForm();
        renderDashboard(currentIdentity);
        showModal(els.dashboardModal);
        if (resolvedRoles(currentIdentity).includes("ROLE_ADMIN")) {
            bootstrapAdminDashboard();
        }
    }

    function ensureLoggedIn(message) {
        if (getToken()) {
            return true;
        }
        toast(message || "Bu islem icin giris gerekli");
        showModal(els.loginModal);
        return false;
    }

    function ensureAdmin() {
        if (currentIdentity && hasRole(currentIdentity, "ROLE_ADMIN")) {
            return true;
        }
        toast("Bu alan sadece admin kullanicilar icin");
        return false;
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }
        return date.toLocaleString("tr-TR");
    }

    function pageMetaText(page) {
        if (!page) {
            return "-";
        }
        return `page ${page.number + 1}/${Math.max(1, page.totalPages)} | total ${page.totalElements}`;
    }

    function setResultBox(el, value) {
        if (!el) {
            return;
        }
        if (typeof value === "string") {
            el.textContent = value;
            return;
        }
        el.textContent = JSON.stringify(value, null, 2);
    }

    function parseIdCsv(value) {
        const raw = String(value || "").split(",");
        const ids = [];
        for (const token of raw) {
            const trimmed = token.trim();
            if (!trimmed) {
                continue;
            }
            const n = Number(trimmed);
            if (Number.isInteger(n) && n > 0) {
                ids.push(n);
            }
        }
        return [...new Set(ids)];
    }

    function toPositiveInt(value) {
        const n = Number(value);
        return Number.isInteger(n) && n > 0 ? n : null;
    }

    function toInt(value) {
        const n = Number(value);
        return Number.isInteger(n) ? n : null;
    }

    function buildQuery(params) {
        const qs = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value == null) {
                return;
            }
            const asString = String(value).trim();
            if (!asString.length) {
                return;
            }
            qs.set(key, asString);
        });
        return qs.toString();
    }

    async function openOrdersModal() {
        if (!ensureLoggedIn("Siparisler icin once giris yapin")) {
            return;
        }
        await loadMyOrders();
        showModal(els.ordersModal);
    }

    async function loadMyOrders() {
        if (!ensureLoggedIn("Siparisler icin once giris yapin")) {
            return;
        }

        try {
            const query = buildQuery({
                page: 0,
                size: 20,
                sort: "createdAt,desc",
            });
            const data = await apiFetch(`${API.orders}?${query}`);
            const items = Array.isArray(data && data.items) ? data.items : [];

            if (!items.length) {
                els.ordersBody.innerHTML =
                    '<tr><td colspan="5" class="small muted">Siparis bulunamadi</td></tr>';
            } else {
                els.ordersBody.innerHTML = items
                    .map(
                        (order) => `
              <tr>
                <td>${escapeHtml(order.orderId)}</td>
                <td>${escapeHtml(order.status)}</td>
                <td>${formatMoney(order.totalAmount, order.currency)}</td>
                <td>${escapeHtml(formatDateTime(order.createdAt))}</td>
                <td>
                  <button class="btn btn-sm btn-outline-dark" type="button" data-my-order-detail="${escapeAttr(order.orderId)}">Detay</button>
                </td>
              </tr>
            `,
                    )
                    .join("");
            }
            els.ordersMeta.textContent = pageMetaText(data && data.page);
        } catch (error) {
            els.ordersBody.innerHTML = `<tr><td colspan="5" class="small text-danger">${escapeHtml(error.message)}</td></tr>`;
            els.ordersMeta.textContent = "Yukleme hatasi";
        }
    }

    function statusBadgeHtml(status) {
        const normalized = String(status || "UNKNOWN").trim() || "UNKNOWN";
        const variant = ({
            PENDING_PAYMENT: "text-bg-warning",
            PAYMENT_FAILED: "text-bg-danger",
            PAID: "text-bg-success",
            PLACED: "text-bg-primary",
            SHIPPED: "text-bg-info",
            FULFILLED: "text-bg-success",
            REFUND_PENDING: "text-bg-warning",
            REFUNDED: "text-bg-secondary",
            CANCELLED: "text-bg-dark",
        })[normalized] || "text-bg-secondary";
        return `<span class="badge ${variant}">${escapeHtml(normalized)}</span>`;
    }

    function tableRowHtml(label, valueHtml) {
        return `
          <tr>
            <th class="w-25 bg-light">${escapeHtml(label)}</th>
            <td>${valueHtml}</td>
          </tr>
        `;
    }

    function renderOrderSummaryHtml(detail) {
        const currency = detail && detail.currency ? detail.currency : "EUR";
        return `
          <div class="table-responsive border rounded mb-3">
            <table class="table table-sm align-middle mb-0">
              <tbody>
                ${tableRowHtml("Order ID", escapeHtml(detail.orderId || "-"))}
                ${tableRowHtml("Status", statusBadgeHtml(detail.status))}
                ${tableRowHtml("Currency", escapeHtml(currency))}
                ${tableRowHtml(
            "Total",
            escapeHtml(formatMoney(detail.totalAmount, currency)),
        )}
                ${tableRowHtml(
            "Created",
            escapeHtml(formatDateTime(detail.createdAt)),
        )}
              </tbody>
            </table>
          </div>
        `;
    }

    function renderOrderItemsHtml(detail) {
        const currency = detail && detail.currency ? detail.currency : "EUR";
        const items = Array.isArray(detail && detail.items) ? detail.items : [];
        if (!items.length) {
            return '<div class="small muted mb-3">Bu sipariste urun yok.</div>';
        }

        const rows = items
            .map((item) => {
                const qty = Number(item.quantity || 0);
                const unit = Number(item.unitPrice ?? 0);
                const lineTotal =
                    item.lineTotal != null
                        ? Number(item.lineTotal)
                        : Number.isFinite(unit) && Number.isFinite(qty)
                            ? unit * qty
                            : 0;
                const rowCurrency = item.currency || currency;

                return `
                  <tr>
                    <td>${escapeHtml(item.name || "-")}</td>
                    <td>${escapeHtml(item.sku || "-")}</td>
                    <td>${escapeHtml(item.productId ?? "-")}</td>
                    <td>${escapeHtml(String(qty))}</td>
                    <td>${escapeHtml(formatMoney(unit, rowCurrency))}</td>
                    <td>${escapeHtml(formatMoney(lineTotal, rowCurrency))}</td>
                  </tr>
                `;
            })
            .join("");

        return `
          <div class="table-responsive border rounded mb-3">
            <table class="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Product ID</th>
                  <th>Qty</th>
                  <th>Unit Price</th>
                  <th>Line Total</th>
                </tr>
              </thead>
              <tbody>${rows}</tbody>
            </table>
          </div>
        `;
    }

    function renderOrderShipmentsHtml(detail) {
        const shipments = Array.isArray(detail && detail.shipments)
            ? detail.shipments
            : [];
        if (!shipments.length) {
            return '<div class="small muted mb-3">Henuz sevkiyat kaydi yok.</div>';
        }

        const rows = shipments
            .map((shipment) => {
                const shipmentId =
                    shipment.shipmentId || shipment.publicId || shipment.id || "-";
                const status = shipment.status || "-";
                const carrier = shipment.carrier || "-";
                const tracking = shipment.trackingNumber || shipment.tracking || "-";
                const updatedAt =
                    shipment.deliveredAt ||
                    shipment.shippedAt ||
                    shipment.createdAt ||
                    shipment.updatedAt ||
                    null;

                return `
                  <tr>
                    <td>${escapeHtml(String(shipmentId))}</td>
                    <td>${escapeHtml(String(status))}</td>
                    <td>${escapeHtml(String(carrier))}</td>
                    <td>${escapeHtml(String(tracking))}</td>
                    <td>${escapeHtml(formatDateTime(updatedAt))}</td>
                  </tr>
                `;
            })
            .join("");

        return `
          <div class="table-responsive border rounded mb-3">
            <table class="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Shipment</th>
                  <th>Status</th>
                  <th>Carrier</th>
                  <th>Tracking</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>${rows}</tbody>
            </table>
          </div>
        `;
    }

    function renderOrderAddressHtml(detail) {
        const address = detail && detail.shippingAddress ? detail.shippingAddress : null;
        if (!address || typeof address !== "object") {
            return '<div class="small muted">Teslimat adresi bulunamadi.</div>';
        }

        const street = [address.line1, address.line2]
            .filter((part) => part != null && String(part).trim().length > 0)
            .join(" / ");
        const cityState = [address.city, address.state]
            .filter((part) => part != null && String(part).trim().length > 0)
            .join(" / ");

        return `
          <div class="table-responsive border rounded">
            <table class="table table-sm align-middle mb-0">
              <tbody>
                ${tableRowHtml("Full Name", escapeHtml(address.fullName || "-"))}
                ${tableRowHtml("Phone", escapeHtml(address.phone || "-"))}
                ${tableRowHtml("Street", escapeHtml(street || "-"))}
                ${tableRowHtml("City / State", escapeHtml(cityState || "-"))}
                ${tableRowHtml(
            "Postal Code",
            escapeHtml(address.postalCode || "-"),
        )}
                ${tableRowHtml(
            "Country",
            escapeHtml(address.countryCode || "-"),
        )}
              </tbody>
            </table>
          </div>
        `;
    }

    function renderMyOrderDetail(detail) {
        if (!els.ordersDetailBox) {
            return;
        }
        if (!detail || typeof detail !== "object") {
            els.ordersDetailBox.innerHTML =
                '<div class="small text-danger">Siparis detayi bulunamadi.</div>';
            return;
        }

        els.ordersDetailBox.innerHTML = `
          <div class="mb-2 fw-semibold">Siparis Ozeti</div>
          ${renderOrderSummaryHtml(detail)}
          <div class="mb-2 fw-semibold">Siparis Urunleri</div>
          ${renderOrderItemsHtml(detail)}
          <div class="mb-2 fw-semibold">Sevkiyat</div>
          ${renderOrderShipmentsHtml(detail)}
          <div class="mb-2 fw-semibold">Teslimat Adresi</div>
          ${renderOrderAddressHtml(detail)}
        `;
    }

    async function loadMyOrderDetail(orderId) {
        if (!orderId) {
            return;
        }
        try {
            const detail = await apiFetch(API.orderById(orderId));
            renderMyOrderDetail(detail);
        } catch (error) {
            if (els.ordersDetailBox) {
                els.ordersDetailBox.innerHTML = `<div class="small text-danger">Detay yuklenemedi: ${escapeHtml(error.message)}</div>`;
            }
        }
    }

    async function bootstrapAdminDashboard() {
        if (adminBootstrapped) {
            return;
        }
        if (!ensureAdmin()) {
            return;
        }
        await Promise.allSettled([
            loadAdminOrders(),
            loadAdminProducts(),
            loadAdminRefunds(),
            loadAdminWebhooks(),
        ]);
        adminBootstrapped = true;
    }

    async function loadAdminOrders() {
        if (!ensureAdmin()) {
            return;
        }
        try {
            const query = buildQuery({
                page: 0,
                size: 20,
                sort: "createdAt,desc",
                status: els.adminOrderStatus.value,
                email: normalizeText(els.adminOrderEmail.value),
                q: normalizeText(els.adminOrderQuery.value),
            });
            const data = await apiFetch(`${API.adminOrders}?${query}`);
            const items = Array.isArray(data && data.items) ? data.items : [];

            if (!items.length) {
                els.adminOrdersBody.innerHTML =
                    '<tr><td colspan="5" class="small muted">Kayit bulunamadi</td></tr>';
            } else {
                els.adminOrdersBody.innerHTML = items
                    .map(
                        (order) => `
              <tr>
                <td>${escapeHtml(order.orderId)}</td>
                <td>${escapeHtml(order.status)}</td>
                <td>${escapeHtml(order.userEmail)}</td>
                <td>${formatMoney(order.totalAmount, order.currency)}</td>
                <td class="text-nowrap">
                  <button class="btn btn-sm btn-outline-dark" type="button" data-admin-order-detail="${escapeAttr(order.orderId)}">Detay</button>
                  <button class="btn btn-sm btn-outline-secondary" type="button" data-admin-order-use="${escapeAttr(order.orderId)}">Sec</button>
                </td>
              </tr>
            `,
                    )
                    .join("");
            }
            els.adminOrdersMeta.textContent = pageMetaText(data && data.page);
        } catch (error) {
            els.adminOrdersBody.innerHTML = `<tr><td colspan="5" class="small text-danger">${escapeHtml(error.message)}</td></tr>`;
            els.adminOrdersMeta.textContent = "Yukleme hatasi";
        }
    }

    async function loadAdminOrderDetail(orderId) {
        if (!ensureAdmin()) {
            return;
        }
        if (!orderId) {
            toast("Order id gerekli");
            return;
        }
        try {
            const detail = await apiFetch(API.adminOrderById(orderId));
            els.adminOrderIdInput.value = orderId;
            setResultBox(els.adminOrderDetailBox, detail);
        } catch (error) {
            setResultBox(
                els.adminOrderDetailBox,
                `Detay yuklenemedi: ${error.message}`,
            );
        }
    }

    async function adminShip() {
        if (!ensureAdmin()) {
            return;
        }
        const orderId = normalizeText(els.adminOrderIdInput.value);
        if (!orderId) {
            toast("Ship icin order id gerekli");
            return;
        }
        try {
            const payload = {
                carrier: normalizeText(els.adminShipCarrier.value),
                trackingNumber: normalizeText(els.adminShipTracking.value),
            };
            const res = await apiFetch(API.adminOrderShip(orderId), {
                method: "POST",
                json: payload,
            });
            setResultBox(els.adminOrderDetailBox, res || { ok: true });
            toast("Ship islemi tamamlandi");
            await loadAdminOrders();
            await loadAdminOrderDetail(orderId);
        } catch (error) {
            toast(`Ship hatasi: ${error.message}`);
        }
    }

    async function adminDeliver() {
        if (!ensureAdmin()) {
            return;
        }
        const orderId = normalizeText(els.adminOrderIdInput.value);
        if (!orderId) {
            toast("Deliver icin order id gerekli");
            return;
        }
        try {
            await apiFetch(API.adminOrderDeliver(orderId), { method: "POST" });
            toast("Deliver islemi tamamlandi");
            await loadAdminOrders();
            await loadAdminOrderDetail(orderId);
        } catch (error) {
            toast(`Deliver hatasi: ${error.message}`);
        }
    }

    async function adminCancel() {
        if (!ensureAdmin()) {
            return;
        }
        const orderId = normalizeText(els.adminOrderIdInput.value);
        if (!orderId) {
            toast("Cancel icin order id gerekli");
            return;
        }
        try {
            const reason = normalizeText(els.adminCancelReason.value);
            const opts = { method: "POST" };
            if (reason) {
                opts.json = { reason };
            }
            await apiFetch(API.adminOrderCancel(orderId), opts);
            toast("Cancel islemi tamamlandi");
            await loadAdminOrders();
            await loadAdminOrderDetail(orderId);
        } catch (error) {
            toast(`Cancel hatasi: ${error.message}`);
        }
    }

    async function adminRestock() {
        if (!ensureAdmin()) {
            return;
        }
        const productId = toPositiveInt(els.adminInventoryProductId.value);
        const quantity = toPositiveInt(els.adminRestockQty.value);
        const reason = normalizeText(els.adminInventoryReason.value);
        if (!productId || !quantity || !reason) {
            toast("Restock icin productId, quantity ve reason gerekli");
            return;
        }
        try {
            const result = await apiFetch(API.adminRestock(productId), {
                method: "POST",
                json: { quantity, reason },
            });
            setResultBox(els.adminInventoryResult, result);
            toast("Restock tamamlandi");
        } catch (error) {
            setResultBox(
                els.adminInventoryResult,
                `Restock hatasi: ${error.message}`,
            );
        }
    }

    async function adminAdjust() {
        if (!ensureAdmin()) {
            return;
        }
        const productId = toPositiveInt(els.adminInventoryProductId.value);
        const delta = toInt(els.adminAdjustDelta.value);
        const reason = normalizeText(els.adminInventoryReason.value);
        if (!productId || delta == null || !reason) {
            toast("Adjust icin productId, delta ve reason gerekli");
            return;
        }
        try {
            const result = await apiFetch(API.adminAdjust(productId), {
                method: "POST",
                json: { delta, reason },
            });
            setResultBox(els.adminInventoryResult, result);
            toast("Adjust tamamlandi");
        } catch (error) {
            setResultBox(els.adminInventoryResult, `Adjust hatasi: ${error.message}`);
        }
    }

    async function loadAdminRefunds() {
        if (!ensureAdmin()) {
            return;
        }
        try {
            const query = buildQuery({
                page: 0,
                size: 20,
                sort: "createdAt,desc",
                status: els.adminRefundStatus.value,
                email: normalizeText(els.adminRefundEmail.value),
                orderId: normalizeText(els.adminRefundOrderId.value),
            });
            const data = await apiFetch(`${API.adminRefunds}?${query}`);
            const items = Array.isArray(data && data.items) ? data.items : [];
            if (!items.length) {
                els.adminRefundsBody.innerHTML =
                    '<tr><td colspan="5" class="small muted">Kayit bulunamadi</td></tr>';
            } else {
                els.adminRefundsBody.innerHTML = items
                    .map(
                        (item) => `
              <tr>
                <td>${escapeHtml(item.refundId)}</td>
                <td>${escapeHtml(item.status)}</td>
                <td>${escapeHtml(item.userEmail)}</td>
                <td>${escapeHtml(item.orderId)}</td>
                <td>${formatMoney(item.amount, item.currency)}</td>
              </tr>
            `,
                    )
                    .join("");
            }
            els.adminRefundsMeta.textContent = pageMetaText(data && data.page);
        } catch (error) {
            els.adminRefundsBody.innerHTML = `<tr><td colspan="5" class="small text-danger">${escapeHtml(error.message)}</td></tr>`;
            els.adminRefundsMeta.textContent = "Yukleme hatasi";
        }
    }

    async function loadAdminWebhooks() {
        if (!ensureAdmin()) {
            return;
        }
        try {
            const query = buildQuery({
                page: 0,
                size: 20,
                sort: "receivedAt,desc",
                provider: normalizeText(els.adminWebhookProvider.value),
                eventId: normalizeText(els.adminWebhookEventId.value),
            });
            const data = await apiFetch(`${API.adminWebhooks}?${query}`);
            const items = Array.isArray(data && data.items) ? data.items : [];
            if (!items.length) {
                els.adminWebhookBody.innerHTML =
                    '<tr><td colspan="4" class="small muted">Kayit bulunamadi</td></tr>';
            } else {
                els.adminWebhookBody.innerHTML = items
                    .map(
                        (event) => `
              <tr>
                <td>${escapeHtml(event.provider)}</td>
                <td>${escapeHtml(event.eventId)}</td>
                <td>${escapeHtml(event.payloadHash)}</td>
                <td>${escapeHtml(formatDateTime(event.receivedAt))}</td>
              </tr>
            `,
                    )
                    .join("");
            }
            els.adminWebhookMeta.textContent = pageMetaText(data && data.page);
        } catch (error) {
            els.adminWebhookBody.innerHTML = `<tr><td colspan="4" class="small text-danger">${escapeHtml(error.message)}</td></tr>`;
            els.adminWebhookMeta.textContent = "Yukleme hatasi";
        }
    }

    function applyProductForm(product) {
        if (!product) {
            return;
        }
        els.adminProductId.value = product.id ?? "";
        els.adminProductSku.value = product.sku ?? "";
        els.adminProductName.value = product.name ?? "";
        els.adminProductDescription.value = product.description ?? "";
        els.adminProductPrice.value = product.price ?? "";
        els.adminProductCurrency.value = product.currency ?? "EUR";
        els.adminProductStock.value = product.stockQuantity ?? 0;
        els.adminProductStatus.value = product.status ?? "ACTIVE";

        const categoryIds = Array.isArray(product.categories)
            ? product.categories
                .map((c) => c && c.id)
                .filter((id) => Number.isFinite(Number(id)))
            : [];
        els.adminProductCategoryIds.value = categoryIds.join(",");
        els.adminImageProductId.value = product.id ?? "";
    }

    async function loadAdminProducts() {
        if (!ensureAdmin()) {
            return;
        }
        try {
            const query = buildQuery({
                page: 0,
                size: 50,
                sort: "id,desc",
                q: normalizeText(els.adminProductSearch.value),
            });
            const data = await apiFetch(`${API.products}?${query}`);
            const items = normalizeProductsResponse(data);

            if (!items.length) {
                els.adminProductsBody.innerHTML =
                    '<tr><td colspan="6" class="small muted">Urun bulunamadi</td></tr>';
            } else {
                els.adminProductsBody.innerHTML = items
                    .map(
                        (product) => `
              <tr>
                <td>${escapeHtml(product.id)}</td>
                <td>${escapeHtml(product.sku)}</td>
                <td>${escapeHtml(product.name)}</td>
                <td>${escapeHtml(product.status)}</td>
                <td>${formatMoney(product.price, product.currency)}</td>
                <td>
                  <button class="btn btn-sm btn-outline-dark" type="button" data-admin-product-use="${escapeAttr(product.id)}">Forma Al</button>
                </td>
              </tr>
            `,
                    )
                    .join("");
            }

            if (data && data.page) {
                els.adminProductsMeta.textContent = pageMetaText(data.page);
            } else {
                els.adminProductsMeta.textContent = `${items.length} urun`;
            }
        } catch (error) {
            els.adminProductsBody.innerHTML = `<tr><td colspan="6" class="small text-danger">${escapeHtml(error.message)}</td></tr>`;
            els.adminProductsMeta.textContent = "Yukleme hatasi";
        }
    }

    async function loadAdminProductIntoForm(productId) {
        if (!ensureAdmin()) {
            return;
        }
        const id = toPositiveInt(productId);
        if (!id) {
            toast("Gecerli product id gerekli");
            return;
        }
        try {
            const product = await apiFetch(API.productById(id));
            applyProductForm(product);
            setResultBox(els.adminProductResult, product);
            toast("Product forma alindi");
        } catch (error) {
            setResultBox(
                els.adminProductResult,
                `Product yuklenemedi: ${error.message}`,
            );
        }
    }

    function buildProductPayload(includeSku) {
        const name = normalizeText(els.adminProductName.value);
        const description = normalizeText(els.adminProductDescription.value);
        const price = Number(els.adminProductPrice.value);
        const currency = String(els.adminProductCurrency.value || "")
            .trim()
            .toUpperCase();
        const stockQuantity = toInt(els.adminProductStock.value);
        const status = els.adminProductStatus.value;
        const categoryIds = parseIdCsv(els.adminProductCategoryIds.value);

        if (
            !name ||
            !Number.isFinite(price) ||
            price <= 0 ||
            !currency ||
            stockQuantity == null ||
            stockQuantity < 0 ||
            !status
        ) {
            throw new Error("Product alanlari gecersiz");
        }

        const payload = {
            name,
            description,
            price,
            currency,
            stockQuantity,
            status,
            categoryIds,
        };

        if (includeSku) {
            const sku = normalizeText(els.adminProductSku.value);
            if (!sku) {
                throw new Error("Create icin SKU gerekli");
            }
            payload.sku = sku;
        }

        return payload;
    }

    async function adminCreateCategory() {
        if (!ensureAdmin()) {
            return;
        }
        const slug = normalizeText(els.adminCategorySlug.value);
        const name = normalizeText(els.adminCategoryName.value);
        if (!slug || !name) {
            toast("Category create icin slug ve name gerekli");
            return;
        }
        try {
            const result = await apiFetch(API.categories, {
                method: "POST",
                json: { slug, name },
            });
            setResultBox(els.adminCategoryResult, result);
            toast("Category create tamamlandi");
            els.adminCategoryId.value =
                result && result.id ? result.id : els.adminCategoryId.value;
        } catch (error) {
            setResultBox(
                els.adminCategoryResult,
                `Category create hatasi: ${error.message}`,
            );
        }
    }

    async function adminUpdateCategory() {
        if (!ensureAdmin()) {
            return;
        }
        const categoryId = toPositiveInt(els.adminCategoryId.value);
        const name = normalizeText(els.adminCategoryName.value);
        if (!categoryId || !name) {
            toast("Category update icin id ve name gerekli");
            return;
        }
        try {
            const result = await apiFetch(API.categoryById(categoryId), {
                method: "PUT",
                json: { name },
            });
            setResultBox(els.adminCategoryResult, result);
            toast("Category update tamamlandi");
        } catch (error) {
            setResultBox(
                els.adminCategoryResult,
                `Category update hatasi: ${error.message}`,
            );
        }
    }

    async function adminDeleteCategory() {
        if (!ensureAdmin()) {
            return;
        }
        const categoryId = toPositiveInt(els.adminCategoryId.value);
        if (!categoryId) {
            toast("Category delete icin id gerekli");
            return;
        }
        try {
            await apiFetch(API.categoryById(categoryId), { method: "DELETE" });
            setResultBox(els.adminCategoryResult, {
                deletedCategoryId: categoryId,
                ok: true,
            });
            toast("Category delete tamamlandi");
        } catch (error) {
            setResultBox(
                els.adminCategoryResult,
                `Category delete hatasi: ${error.message}`,
            );
        }
    }

    async function adminCreateProduct() {
        if (!ensureAdmin()) {
            return;
        }
        try {
            const payload = buildProductPayload(true);
            const result = await apiFetch(API.products, {
                method: "POST",
                json: payload,
            });
            applyProductForm(result);
            setResultBox(els.adminProductResult, result);
            toast("Product create tamamlandi");
            await loadAdminProducts();
        } catch (error) {
            setResultBox(
                els.adminProductResult,
                `Product create hatasi: ${error.message}`,
            );
        }
    }

    async function adminUpdateProduct() {
        if (!ensureAdmin()) {
            return;
        }
        const productId = toPositiveInt(els.adminProductId.value);
        if (!productId) {
            toast("Product update icin product id gerekli");
            return;
        }
        try {
            const payload = buildProductPayload(false);
            const result = await apiFetch(API.productById(productId), {
                method: "PUT",
                json: payload,
            });
            applyProductForm(result);
            setResultBox(els.adminProductResult, result);
            toast("Product update tamamlandi");
            await loadAdminProducts();
        } catch (error) {
            setResultBox(
                els.adminProductResult,
                `Product update hatasi: ${error.message}`,
            );
        }
    }

    async function adminDeleteProduct() {
        if (!ensureAdmin()) {
            return;
        }
        const productId = toPositiveInt(els.adminProductId.value);
        if (!productId) {
            toast("Product delete icin product id gerekli");
            return;
        }
        try {
            await apiFetch(API.productById(productId), { method: "DELETE" });
            setResultBox(els.adminProductResult, {
                deletedProductId: productId,
                ok: true,
            });
            els.adminProductId.value = "";
            els.adminProductSku.value = "";
            els.adminProductName.value = "";
            els.adminProductDescription.value = "";
            els.adminProductPrice.value = "10.00";
            els.adminProductCurrency.value = "EUR";
            els.adminProductStock.value = "1";
            els.adminProductStatus.value = "ACTIVE";
            els.adminProductCategoryIds.value = "";
            toast("Product delete tamamlandi");
            await loadAdminProducts();
        } catch (error) {
            setResultBox(
                els.adminProductResult,
                `Product delete hatasi: ${error.message}`,
            );
        }
    }

    async function adminAddImage() {
        if (!ensureAdmin()) {
            return;
        }
        const productId = toPositiveInt(els.adminImageProductId.value);
        const url = normalizeText(els.adminImageUrl.value);
        const altText = normalizeText(els.adminImageAlt.value);
        if (!productId || !url) {
            toast("Image add icin product id ve url gerekli");
            return;
        }
        try {
            const result = await apiFetch(API.productImages(productId), {
                method: "POST",
                json: { url, altText },
            });
            setResultBox(els.adminImageResult, result);
            toast("Image add tamamlandi");
        } catch (error) {
            setResultBox(els.adminImageResult, `Image add hatasi: ${error.message}`);
        }
    }

    async function adminDeleteImage() {
        if (!ensureAdmin()) {
            return;
        }
        const productId = toPositiveInt(els.adminImageProductId.value);
        const imageId = toPositiveInt(els.adminImageId.value);
        if (!productId || !imageId) {
            toast("Image delete icin product id ve image id gerekli");
            return;
        }
        try {
            await apiFetch(API.productImageById(productId, imageId), {
                method: "DELETE",
            });
            setResultBox(els.adminImageResult, {
                deletedImageId: imageId,
                productId,
                ok: true,
            });
            toast("Image delete tamamlandi");
        } catch (error) {
            setResultBox(
                els.adminImageResult,
                `Image delete hatasi: ${error.message}`,
            );
        }
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

        const authToken = getToken();
        if (authToken && !isPublicReadonlyEndpoint(url, opts.method)) {
            headers.set("Authorization", `Bearer ${authToken}`);
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
                // keep fallback status message
            }
            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        if (
            contentType.includes("application/json") ||
            contentType.includes("problem+json")
        ) {
            return response.json();
        }
        return response.text();
    }

    function isPublicReadonlyEndpoint(url, method) {
        const normalizedMethod = String(method || "GET").toUpperCase();
        if (
            normalizedMethod !== "GET" &&
            normalizedMethod !== "HEAD" &&
            normalizedMethod !== "OPTIONS"
        ) {
            return false;
        }
        try {
            const parsed = new URL(url, window.location.origin);
            return (
                parsed.pathname.startsWith("/api/v1/products") ||
                parsed.pathname.startsWith("/api/v1/categories")
            );
        } catch (_) {
            return false;
        }
    }

    function formatMoney(amount, currency = "EUR") {
        const value = Number(amount || 0);
        try {
            return new Intl.NumberFormat("tr-TR", {
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
            // ignore parse issue
        }
        return { currency: "EUR", items: [] };
    }

    function setCart(cart) {
        localStorage.setItem(LS_CART, JSON.stringify(cart));
        renderCart();
    }

    function cartItemCount() {
        return getCart().items.reduce((sum, item) => sum + item.qty, 0);
    }

    function cartSubtotal() {
        return getCart().items.reduce(
            (sum, item) => sum + Number(item.price) * item.qty,
            0,
        );
    }

    function addToCart(product) {
        const cart = getCart();
        const existing = cart.items.find((item) => item.id === product.id);
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
        toast("Sepete eklendi");
    }

    function removeFromCart(productId) {
        const cart = getCart();
        cart.items = cart.items.filter((item) => item.id !== productId);
        setCart(cart);
    }

    function changeCartQty(productId, delta) {
        const cart = getCart();
        const item = cart.items.find((it) => it.id === productId);
        if (!item) {
            return;
        }
        item.qty += delta;
        if (item.qty <= 0) {
            cart.items = cart.items.filter((it) => it.id !== productId);
        }
        setCart(cart);
    }

    function clearCart() {
        setCart({ currency: "EUR", items: [] });
        toast("Sepet temizlendi");
    }

    function normalizeProductsResponse(data) {
        if (Array.isArray(data)) {
            return data;
        }
        if (data && Array.isArray(data.items)) {
            return data.items;
        }
        if (data && Array.isArray(data.content)) {
            return data.content;
        }
        return [];
    }

    function normalizeCategoryResponse(data) {
        if (Array.isArray(data)) {
            return data;
        }
        if (data && Array.isArray(data.items)) {
            return data.items;
        }
        if (data && Array.isArray(data.content)) {
            return data.content;
        }
        return [];
    }

    function normalizeProductCategories(row) {
        if (!row || !Array.isArray(row.categories)) {
            return [];
        }
        return row.categories
            .map((category) => ({
                slug: String(category && category.slug ? category.slug : "")
                    .trim()
                    .toLowerCase(),
                name: String(
                    category && (category.name || category.slug)
                        ? category.name || category.slug
                        : "",
                ).trim(),
            }))
            .filter((category) => category.slug && category.name);
    }

    async function loadCategoryFilters() {
        try {
            const data = await apiFetch(API.categories);
            categoryFilters = normalizeCategoryResponse(data)
                .map((category) => ({
                    slug: String(category && category.slug ? category.slug : "")
                        .trim()
                        .toLowerCase(),
                    name: String(
                        category && (category.name || category.slug)
                            ? category.name || category.slug
                            : "",
                    ).trim(),
                }))
                .filter((category) => category.slug && category.name);
        } catch (_) {
            categoryFilters = [];
        }
        renderCategoryFilters();
    }

    function renderCategoryFilters() {
        if (!els.categoryFilterList) {
            return;
        }
        els.categoryFilterList.innerHTML = "";

        if (!categoryFilters.length) {
            const empty = document.createElement("div");
            empty.className = "muted";
            empty.textContent = "No categories";
            els.categoryFilterList.appendChild(empty);
            return;
        }

        categoryFilters.forEach((category) => {
            const label = document.createElement("label");
            label.className = "form-check";
            label.innerHTML = `
            <input class="form-check-input" type="checkbox" name="categoryFilter" value="${escapeAttr(category.slug)}">
            <span class="form-check-label">${escapeHtml(category.name)}</span>
          `;
            els.categoryFilterList.appendChild(label);
        });
    }

    function selectedCategorySlugs() {
        if (!els.categoryFilterList) {
            return [];
        }
        return Array.from(
            els.categoryFilterList.querySelectorAll(
                'input[name="categoryFilter"]:checked',
            ),
        )
            .map((input) =>
                String(input.value || "")
                    .trim()
                    .toLowerCase(),
            )
            .filter(Boolean);
    }

    function clearCatalogFilters() {
        if (els.filterPriceMin) {
            els.filterPriceMin.value = "";
        }
        if (els.filterPriceMax) {
            els.filterPriceMax.value = "";
        }
        if (els.categoryFilterList) {
            els.categoryFilterList
                .querySelectorAll('input[name="categoryFilter"]')
                .forEach((input) => {
                    input.checked = false;
                });
        }
        applySearchSort();
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

    function escapeAttr(value) {
        return escapeHtml(value).replace(/`/g, "&#96;");
    }

    function renderSkeleton() {
        els.productGrid.innerHTML = "";
        for (let i = 0; i < 8; i += 1) {
            const col = document.createElement("div");
            col.className = "col-12 col-sm-6 col-lg-4";
            col.innerHTML = `
            <div class="bg-white border rounded-xl p-3">
              <div class="skeleton skeleton-card mb-3"></div>
              <div class="skeleton skeleton-line w-75 mb-2"></div>
              <div class="skeleton skeleton-line w-50"></div>
            </div>
          `;
            els.productGrid.appendChild(col);
        }
    }

    function renderProducts(list) {
        els.productGrid.innerHTML = "";

        if (!list.length) {
            els.productGrid.innerHTML =
                '<div class="col-12"><div class="alert alert-light border">No product found for selected filters.</div></div>';
            els.resultInfo.textContent = "0 products";
            return;
        }

        list.forEach((product) => {
            const col = document.createElement("div");
            col.className = "col-12 col-sm-6 col-lg-4";
            const categoryBadges = Array.isArray(product.categoryNames)
                ? product.categoryNames
                    .slice(0, 3)
                    .map(
                        (name) =>
                            `<span class="badge text-bg-light border">${escapeHtml(name)}</span>`,
                    )
                    .join(" ")
                : "";
            const imageBlock = product.primaryImageUrl
                ? `<img src="${escapeAttr(product.primaryImageUrl)}" alt="${escapeAttr(product.name || "Product")}" class="w-100 rounded mb-3" style="height: 180px; object-fit: cover;">`
                : `<div class="rounded mb-3 d-flex align-items-center justify-content-center bg-light border" style="height: 180px;"><span class="muted small">No image</span></div>`;

            col.innerHTML = `
            <div class="card product h-100">
              <div class="card-body d-flex flex-column">
                ${imageBlock}
                <div class="d-flex justify-content-between align-items-start mb-2">
                  <div class="fw-semibold text-truncate" title="${escapeHtml(product.name)}">${escapeHtml(product.name)}</div>
                  <span class="badge text-bg-light border">${escapeHtml(product.sku || "SKU")}</span>
                </div>
                <div class="small muted mb-3">${escapeHtml(product.description || "Aciklama eklenmemis")}</div>
                <div class="d-flex flex-wrap gap-1 mb-3">${categoryBadges}</div>
                <div class="mt-auto d-flex justify-content-between align-items-center">
                  <div class="price">${formatMoney(product.price, product.currency)}</div>
                  <button
                    class="btn btn-sm btn-dark"
                    type="button"
                    data-add-to-cart="1"
                    data-id="${escapeAttr(product.id)}"
                    data-name="${escapeAttr(product.name)}"
                    data-price="${Number(product.price || 0)}"
                    data-currency="${escapeAttr(product.currency || "EUR")}">
                    <i class="bi bi-plus-lg"></i>
                  </button>
                </div>
              </div>
            </div>
          `;

            els.productGrid.appendChild(col);
        });

        els.resultInfo.textContent = `${list.length} products listed`;
    }

    function renderCart() {
        const cart = getCart();
        els.cartList.innerHTML = "";

        const count = cartItemCount();
        els.cartCount.textContent = String(count);
        els.cartItemsPill.textContent = String(count);

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
                  <div class="fw-semibold">${escapeHtml(item.name)}</div>
                  <div class="small muted">${formatMoney(item.price, item.currency)} x ${item.qty}</div>
                </div>
                <button class="btn btn-sm btn-outline-danger" type="button" data-cart-action="remove" data-id="${escapeAttr(item.id)}">
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
              <div class="d-flex justify-content-between align-items-center mt-2">
                <div class="btn-group btn-group-sm" role="group">
                  <button class="btn btn-outline-dark" type="button" data-cart-action="dec" data-id="${escapeAttr(item.id)}">-</button>
                  <button class="btn btn-outline-dark disabled" type="button">${item.qty}</button>
                  <button class="btn btn-outline-dark" type="button" data-cart-action="inc" data-id="${escapeAttr(item.id)}">+</button>
                </div>
                <div class="price">${formatMoney(Number(item.price) * item.qty, item.currency)}</div>
              </div>
            `;
                els.cartList.appendChild(row);
            });
        }

        els.cartCurrency.textContent = cart.currency || "EUR";
        els.cartSubtotal.textContent = formatMoney(
            cartSubtotal(),
            cart.currency || "EUR",
        );
        els.checkoutTotal.textContent = formatMoney(
            cartSubtotal(),
            cart.currency || "EUR",
        );
        els.checkoutCurrency.textContent = cart.currency || "EUR";
    }

    function applySearchSort() {
        const query = (els.searchInput.value || "").trim().toLowerCase();
        const selectedCategories = selectedCategorySlugs();
        const minPrice =
            els.filterPriceMin.value === "" ? null : Number(els.filterPriceMin.value);
        const maxPrice =
            els.filterPriceMax.value === "" ? null : Number(els.filterPriceMax.value);
        let list = products.filter((product) => {
            const name = String(product.name || "").toLowerCase();
            const sku = String(product.sku || "").toLowerCase();
            if (query && !name.includes(query) && !sku.includes(query)) {
                return false;
            }

            const price = Number(product.price || 0);
            if (Number.isFinite(minPrice) && price < minPrice) {
                return false;
            }
            if (Number.isFinite(maxPrice) && price > maxPrice) {
                return false;
            }

            if (selectedCategories.length) {
                const productCategorySlugs = Array.isArray(product.categorySlugs)
                    ? product.categorySlugs
                    : [];
                const hasMatch = selectedCategories.some((categorySlug) =>
                    productCategorySlugs.includes(categorySlug),
                );
                if (!hasMatch) {
                    return false;
                }
            }

            return true;
        });

        const sort = els.sortSelect.value;
        if (sort === "priceAsc") {
            list.sort((a, b) => Number(a.price) - Number(b.price));
        }
        if (sort === "priceDesc") {
            list.sort((a, b) => Number(b.price) - Number(a.price));
        }

        renderProducts(list);
    }

    async function reloadProducts(showToast = true) {
        renderSkeleton();
        try {
            const data = await apiFetch(API.products);
            products = normalizeProductsResponse(data)
                .map((row) => {
                    const normalizedCategories = normalizeProductCategories(row);
                    return {
                        id: row.id ?? row.productId ?? row.publicId,
                        sku: row.sku,
                        name: row.name,
                        description: row.description,
                        price: Number(row.price ?? row.unitPrice ?? 0),
                        currency: row.currency || "EUR",
                        categorySlugs: normalizedCategories.map(
                            (category) => category.slug,
                        ),
                        categoryNames: normalizedCategories.map(
                            (category) => category.name,
                        ),
                        primaryImageUrl:
                            row.primaryImage && row.primaryImage.url
                                ? row.primaryImage.url
                                : null,
                    };
                })
                .filter((row) => Number.isFinite(Number(row.id)));

            applySearchSort();
            if (showToast) {
                toast("Urunler yuklendi");
            }
        } catch (error) {
            els.productGrid.innerHTML = `<div class="col-12"><div class="alert alert-danger">Urunler yuklenemedi: ${escapeHtml(error.message)}</div></div>`;
            els.resultInfo.textContent = "Yukleme hatasi";
        }
    }

    function updateAuthUi() {
        const authToken = getToken();
        currentIdentity = resolveIdentity();
        if (!currentIdentity) {
            profileInfo = null;
        }

        els.authPill.textContent = authToken ? "Bearer OK" : "-";

        if (currentIdentity) {
            const shortName = currentIdentity.email.includes("@")
                ? currentIdentity.email.split("@")[0]
                : currentIdentity.email;

            els.authState.textContent = "";
            els.authState.classList.add("d-none");

            els.openLoginBtn.classList.add("d-none");
            els.profileMenuWrap.classList.remove("d-none");
            els.profileName.textContent = shortName;
            els.profileRolePill.textContent = roleLabel(currentIdentity);
        } else {
            els.authState.textContent = "";
            els.authState.classList.add("d-none");

            els.openLoginBtn.classList.remove("d-none");
            els.profileMenuWrap.classList.add("d-none");
            els.profileName.textContent = "Profil";
            els.profileRolePill.textContent = "ROLE_USER";
        }

        renderDashboard(currentIdentity);
        renderSavedCardResult();
    }

    async function login() {
        const email = els.loginEmail.value.trim();
        const password = els.loginPassword.value;

        if (!email || !password) {
            toast("Email ve password gerekli");
            return;
        }

        try {
            const data = await apiFetch(API.login, {
                method: "POST",
                json: { email, password },
            });

            const accessToken = data && (data.accessToken || data.token || data.jwt);
            if (!accessToken) {
                throw new Error("Login response icinde accessToken bulunamadi");
            }

            setToken(accessToken);
            profileInfo = null;
            adminBootstrapped = false;
            updateAuthUi();
            hideModal(els.loginModal);
            toast("Giris basarili");
        } catch (error) {
            toast(`Giris basarisiz: ${error.message}`);
        }
    }

    async function logout() {
        try {
            await apiFetch(API.logout, { method: "POST" });
        } catch (_) {
            // ignore logout API errors and still clear local token
        }
        setToken(null);
        profileInfo = null;
        adminBootstrapped = false;
        hideModal(els.dashboardModal);
        hideModal(els.ordersModal);
        updateAuthUi();
        toast("Cikis yapildi");
    }

    async function loadAddresses() {
        try {
            const data = await apiFetch(API.addresses);
            addresses = Array.isArray(data) ? data : [];
            renderAddresses();
            hydrateProfileAddressEditor();
        } catch (error) {
            addresses = [];
            els.addressesList.innerHTML = `<div class="col-12"><div class="alert alert-warning">Addresses could not be loaded: ${escapeHtml(error.message)}</div></div>`;
            hydrateProfileAddressEditor();
        }
    }

    function renderAddresses() {
        els.addressesList.innerHTML = "";

        if (!addresses.length) {
            els.addressesList.innerHTML =
                '<div class="col-12"><div class="alert alert-light border">No saved addresses.</div></div>';
            return;
        }

        addresses.forEach((address) => {
            const col = document.createElement("div");
            col.className = "col-12 col-md-6";
            col.innerHTML = `
            <div class="border bg-white rounded-xl p-3 h-100">
              <div class="d-flex justify-content-between align-items-start">
                <div>
                  <div class="fw-semibold">
                    ${escapeHtml(address.label || "Address")}
                    ${address.isDefault ? '<span class="badge text-bg-success ms-1">Default</span>' : ""}
                  </div>
                  <div class="small muted">${escapeHtml(address.fullName || "")} - ${escapeHtml(address.phone || "")}</div>
                </div>
                <button class="btn btn-sm btn-outline-dark" type="button" data-set-default="${escapeAttr(address.id)}">Default</button>
              </div>
              <div class="small mt-2">
                ${escapeHtml(address.line1 || "")} ${escapeHtml(address.line2 || "")}<br>
                ${escapeHtml(address.postalCode || "")} ${escapeHtml(address.city || "")} ${escapeHtml(address.state || "")}<br>
                ${escapeHtml(address.countryCode || "")}
              </div>
            </div>
          `;
            els.addressesList.appendChild(col);
        });
    }

    function renderCheckoutAddresses() {
        els.checkoutAddresses.innerHTML = "";

        if (!addresses.length) {
            els.checkoutAddresses.innerHTML =
                '<div class="alert alert-light border">No address found. Add one first.</div>';
            selectedAddressId = null;
            renderCheckoutReview();
            return;
        }

        const defaultAddress =
            addresses.find((address) => address.isDefault) || addresses[0];
        const hasSelectedAddress = addresses.some(
            (address) => Number(address.id) === Number(selectedAddressId),
        );
        if (!hasSelectedAddress) {
            selectedAddressId = Number(defaultAddress.id);
        }

        addresses.forEach((address) => {
            const id = Number(address.id);
            const checked = id === Number(selectedAddressId) ? "checked" : "";

            const row = document.createElement("div");
            row.className = "border rounded-xl p-3 bg-white";
            row.innerHTML = `
            <div class="form-check">
              <input class="form-check-input" type="radio" name="checkoutAddress" value="${id}" id="addr_${id}" ${checked}>
              <label class="form-check-label" for="addr_${id}">
                <div class="fw-semibold">
                  ${escapeHtml(address.label || "Address")}
                  ${address.isDefault ? '<span class="badge text-bg-success ms-1">Default</span>' : ""}
                </div>
                <div class="small muted">${escapeHtml(address.fullName || "")} - ${escapeHtml(address.phone || "")}</div>
                <div class="small">${escapeHtml(address.line1 || "")} ${escapeHtml(address.line2 || "")} - ${escapeHtml(address.postalCode || "")} ${escapeHtml(address.city || "")} (${escapeHtml(address.countryCode || "")})</div>
              </label>
            </div>
          `;

            els.checkoutAddresses.appendChild(row);
        });
        renderCheckoutReview();
    }

    async function setDefaultAddress(id) {
        try {
            await apiFetch(`${API.addresses}/${id}/default`, {
                method: "POST",
            });
            toast("Default address updated");
            await loadAddresses();
            renderCheckoutAddresses();
        } catch (error) {
            toast(`Default address could not be updated: ${error.message}`);
        }
    }

    function parseGermanLine1(line1) {
        const raw = normalizeText(line1) || "";
        const match = raw.match(/^(.*?)(?:\s+(\d+[a-zA-Z0-9\-\/]*))$/);
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
        const countryCode = (
            normalizeText(values.countryCode) || "DE"
        ).toUpperCase();
        const postalCode = normalizeText(values.postalCode);

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

        if (
            !payload.fullName ||
            !payload.line1 ||
            !payload.city ||
            !payload.postalCode ||
            !payload.countryCode
        ) {
            throw new Error("Please fill all required address fields");
        }
        if (payload.countryCode !== "DE") {
            throw new Error("Country code must be DE for German address format");
        }
        if (!/^[0-9]{5}$/.test(payload.postalCode)) {
            throw new Error("German postal code must be 5 digits");
        }

        return payload;
    }

    async function createAddress() {
        let payload;
        try {
            payload = buildGermanAddressPayload({
                label: els.addrLabel.value,
                fullName: els.addrFullName.value,
                phone: els.addrPhone.value,
                street: els.addrStreet.value,
                houseNumber: els.addrHouseNumber.value,
                line2: els.addrLine2.value,
                city: els.addrCity.value,
                state: els.addrState.value,
                postalCode: els.addrPostal.value,
                countryCode: els.addrCountry.value,
                makeDefault: els.addrDefault.checked,
            });
        } catch (error) {
            toast(error.message);
            return;
        }

        try {
            await apiFetch(API.addresses, {
                method: "POST",
                json: payload,
            });
            toast("Address saved");
            hideModal(els.newAddressModal);
            await loadAddresses();
            renderCheckoutAddresses();
        } catch (error) {
            toast(`Address could not be saved: ${error.message}`);
        }
    }

    function clearProfileAddressEditor() {
        if (!els.profileAddrLabel) {
            return;
        }
        els.profileAddrLabel.value = "";
        els.profileAddrFullName.value = "";
        els.profileAddrPhone.value = "";
        els.profileAddrCountry.value = "DE";
        els.profileAddrStreet.value = "";
        els.profileAddrHouseNumber.value = "";
        els.profileAddrLine2.value = "";
        els.profileAddrCity.value = "";
        els.profileAddrState.value = "";
        els.profileAddrPostal.value = "";
        els.profileAddrDefault.checked = false;
    }

    function fillProfileAddressFormById(addressId) {
        const numericId = Number(addressId);
        const address = addresses.find((item) => Number(item.id) === numericId);
        if (!address) {
            clearProfileAddressEditor();
            if (els.profileAddressResult) {
                els.profileAddressResult.textContent = "Address not found";
            }
            return;
        }
        const parsedLine1 = parseGermanLine1(address.line1);
        els.profileAddrLabel.value = address.label || "";
        els.profileAddrFullName.value = address.fullName || "";
        els.profileAddrPhone.value = address.phone || "";
        els.profileAddrCountry.value = address.countryCode || "DE";
        els.profileAddrStreet.value = parsedLine1.street || "";
        els.profileAddrHouseNumber.value = parsedLine1.houseNumber || "";
        els.profileAddrLine2.value = address.line2 || "";
        els.profileAddrCity.value = address.city || "";
        els.profileAddrState.value = address.state || "";
        els.profileAddrPostal.value = address.postalCode || "";
        els.profileAddrDefault.checked = !!address.isDefault;
        if (els.profileAddressResult) {
            els.profileAddressResult.textContent = `Loaded address: ${address.label || address.id}`;
        }
    }

    function hydrateProfileAddressEditor() {
        if (!els.profileAddressSelect) {
            return;
        }
        const previousId = Number(els.profileAddressSelect.value);
        els.profileAddressSelect.innerHTML = "";

        if (!addresses.length) {
            els.profileAddressSelect.innerHTML =
                '<option value="">No address found</option>';
            clearProfileAddressEditor();
            if (els.profileAddressResult) {
                els.profileAddressResult.textContent = "No saved address";
            }
            return;
        }

        addresses.forEach((address) => {
            const option = document.createElement("option");
            option.value = String(address.id);
            option.textContent = `${address.label || "Address"} - ${address.city || "-"}${address.isDefault ? " (Default)" : ""}`;
            els.profileAddressSelect.appendChild(option);
        });

        const hasPrevious =
            Number.isFinite(previousId) &&
            addresses.some((address) => Number(address.id) === previousId);
        const defaultAddress =
            addresses.find((address) => address.isDefault) || addresses[0];
        const selectedId = hasPrevious ? previousId : Number(defaultAddress.id);
        els.profileAddressSelect.value = String(selectedId);
        fillProfileAddressFormById(selectedId);
    }

    function loadSelectedProfileAddress() {
        const selectedId = Number(els.profileAddressSelect.value);
        if (!Number.isFinite(selectedId)) {
            toast("Select an address first");
            return;
        }
        fillProfileAddressFormById(selectedId);
    }

    async function updateProfileAddress() {
        if (!ensureLoggedIn("Please sign in to update address")) {
            return;
        }
        const selectedId = Number(els.profileAddressSelect.value);
        if (!Number.isFinite(selectedId)) {
            toast("Select an address first");
            return;
        }

        let payload;
        try {
            payload = buildGermanAddressPayload({
                label: els.profileAddrLabel.value,
                fullName: els.profileAddrFullName.value,
                phone: els.profileAddrPhone.value,
                street: els.profileAddrStreet.value,
                houseNumber: els.profileAddrHouseNumber.value,
                line2: els.profileAddrLine2.value,
                city: els.profileAddrCity.value,
                state: els.profileAddrState.value,
                postalCode: els.profileAddrPostal.value,
                countryCode: els.profileAddrCountry.value,
                makeDefault: els.profileAddrDefault.checked,
            });
        } catch (error) {
            toast(error.message);
            return;
        }

        try {
            const addressPayload = {
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
            await apiFetch(API.addressById(selectedId), {
                method: "PUT",
                json: addressPayload,
            });

            if (payload.makeDefault) {
                await apiFetch(`${API.addresses}/${selectedId}/default`, {
                    method: "POST",
                });
            }

            await loadAddresses();
            renderCheckoutAddresses();
            hydrateProfileAddressEditor();
            if (els.profileAddressResult) {
                els.profileAddressResult.textContent = `Address ${selectedId} updated`;
            }
            toast("Address updated");
        } catch (error) {
            if (els.profileAddressResult) {
                els.profileAddressResult.textContent = `Update failed: ${error.message}`;
            }
            toast(`Address update failed: ${error.message}`);
        }
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

    function collectCardPayload() {
        const cardHolderName = normalizeText(els.cardHolderName.value);
        const cardNumber = normalizeDigits(els.cardNumber.value);
        const cvc = normalizeDigits(els.cardCvc.value);
        const expiryMonthRaw = normalizeDigits(els.cardExpMonth.value);
        const expiryYearRaw = normalizeDigits(els.cardExpYear.value);
        const expiryMonth = Number(expiryMonthRaw);
        const expiryYear = Number(expiryYearRaw);

        if (
            !cardHolderName ||
            !cardNumber ||
            !expiryMonthRaw ||
            !expiryYearRaw ||
            !cvc
        ) {
            throw new Error("Please fill all card fields");
        }
        if (!/^\d{13,19}$/.test(cardNumber)) {
            throw new Error("Card number must be 13-19 digits");
        }
        if (!/^\d{3,4}$/.test(cvc)) {
            throw new Error("CVC must be 3-4 digits");
        }
        if (!Number.isInteger(expiryMonth) || expiryMonth < 1 || expiryMonth > 12) {
            throw new Error("Expiration month must be between 1 and 12");
        }
        if (!Number.isInteger(expiryYear) || expiryYearRaw.length !== 4) {
            throw new Error("Expiration year must be 4 digits");
        }

        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth() + 1;
        if (expiryYear < currentYear || expiryYear > currentYear + 30) {
            throw new Error("Expiration year is not valid");
        }
        if (expiryYear === currentYear && expiryMonth < currentMonth) {
            throw new Error("Card is expired");
        }

        return {
            cardHolderName,
            cardNumber,
            expiryMonth,
            expiryYear,
            cvc,
        };
    }

    function collectProfileCardPayload() {
        const cardHolderName = normalizeText(els.profileCardHolderName.value);
        const cardNumber = normalizeDigits(els.profileCardNumber.value);
        const expiryMonthRaw = normalizeDigits(els.profileCardExpMonth.value);
        const expiryYearRaw = normalizeDigits(els.profileCardExpYear.value);
        const expiryMonth = Number(expiryMonthRaw);
        const expiryYear = Number(expiryYearRaw);

        if (!cardHolderName || !cardNumber || !expiryMonthRaw || !expiryYearRaw) {
            throw new Error("Please fill all saved card fields");
        }
        if (!/^\d{13,19}$/.test(cardNumber)) {
            throw new Error("Card number must be 13-19 digits");
        }
        if (!Number.isInteger(expiryMonth) || expiryMonth < 1 || expiryMonth > 12) {
            throw new Error("Expiration month must be between 1 and 12");
        }
        if (!Number.isInteger(expiryYear) || expiryYearRaw.length !== 4) {
            throw new Error("Expiration year must be 4 digits");
        }

        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth() + 1;
        if (expiryYear < currentYear || expiryYear > currentYear + 30) {
            throw new Error("Expiration year is not valid");
        }
        if (expiryYear === currentYear && expiryMonth < currentMonth) {
            throw new Error("Card is expired");
        }

        return {
            cardHolderName,
            cardNumber,
            expiryMonth,
            expiryYear,
        };
    }

    function applySavedProfileCardToCheckout() {
        const saved = getSavedProfileCard();
        if (!saved) {
            return;
        }
        els.cardHolderName.value = saved.cardHolderName || "";
        els.cardNumber.value = saved.cardNumber || "";
        els.cardExpMonth.value = Number.isFinite(saved.expiryMonth)
            ? String(saved.expiryMonth)
            : "";
        els.cardExpYear.value = Number.isFinite(saved.expiryYear)
            ? String(saved.expiryYear)
            : "";
    }

    async function saveProfileCardFromDashboard() {
        if (!ensureLoggedIn("Please sign in to save card")) {
            return;
        }
        let payload;
        try {
            payload = collectProfileCardPayload();
        } catch (error) {
            toast(error.message);
            return;
        }
        saveProfileCard(payload);
        renderSavedCardResult();
        toast("Card saved for checkout");
    }

    function clearProfileCardFromDashboard() {
        clearProfileCard();
        if (els.profileCardHolderName) {
            els.profileCardHolderName.value = "";
        }
        if (els.profileCardNumber) {
            els.profileCardNumber.value = "";
        }
        if (els.profileCardExpMonth) {
            els.profileCardExpMonth.value = "";
        }
        if (els.profileCardExpYear) {
            els.profileCardExpYear.value = "";
        }
        renderSavedCardResult();
        toast("Saved card cleared");
    }

    async function changeMyPassword() {
        if (!ensureLoggedIn("Please sign in to change password")) {
            return;
        }
        const currentPassword = els.profileCurrentPassword.value || "";
        const newPassword = els.profileNewPassword.value || "";
        const confirmNewPassword = els.profileConfirmPassword.value || "";

        if (!currentPassword || !newPassword || !confirmNewPassword) {
            toast("Please fill all password fields");
            return;
        }
        if (newPassword.length < 8) {
            toast("New password must be at least 8 characters");
            return;
        }
        if (newPassword !== confirmNewPassword) {
            toast("New password confirmation does not match");
            return;
        }

        try {
            await apiFetch(API.changeMyPassword, {
                method: "POST",
                json: {
                    currentPassword,
                    newPassword,
                    confirmNewPassword,
                },
            });
            els.profileCurrentPassword.value = "";
            els.profileNewPassword.value = "";
            els.profileConfirmPassword.value = "";
            toast("Password updated");
        } catch (error) {
            toast(`Password update failed: ${error.message}`);
        }
    }

    function selectedPaymentMethod() {
        const selected = document.querySelector(
            'input[name="paymentMethod"]:checked',
        );
        return selected ? selected.value : "CREDIT_CARD";
    }

    function paymentMethodNeedsCard(method) {
        return method === "CREDIT_CARD" || method === "DEBIT_CARD";
    }

    function paymentMethodLabel(method) {
        if (method === "PAYPAL") {
            return "PayPal";
        }
        if (method === "DEBIT_CARD") {
            return "Debit Card";
        }
        return "Credit Card";
    }

    function updatePaymentMethodUi() {
        if (!els.paymentCardFields) {
            return;
        }
        const needsCard = paymentMethodNeedsCard(selectedPaymentMethod());
        els.paymentCardFields.classList.toggle("d-none", !needsCard);
    }

    function collectInvoiceInfo(validate = true) {
        const fullName = normalizeText(els.invoiceFullName.value);
        const email = normalizeText(els.invoiceEmail.value);
        const invoiceType = normalizeText(els.invoiceType.value) || "PERSONAL";
        const taxNumber = normalizeText(els.invoiceTaxNumber.value);
        const note = normalizeText(els.invoiceNote.value);

        if (validate) {
            if (!fullName || !email) {
                throw new Error("Please fill billing full name and billing email");
            }
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                throw new Error("Billing email format is invalid");
            }
            if (invoiceType === "COMPANY" && !taxNumber) {
                throw new Error("Tax number is required for company invoices");
            }
        }

        return {
            fullName,
            email,
            invoiceType,
            taxNumber,
            note,
        };
    }

    function checkoutAddressSummary() {
        const address = addresses.find(
            (item) => Number(item.id) === Number(selectedAddressId),
        );
        if (!address) {
            return "No address selected";
        }
        return `${address.fullName || "-"}, ${address.line1 || "-"} ${address.line2 || ""}, ${address.postalCode || "-"} ${address.city || "-"}, ${address.countryCode || "-"}`.trim();
    }

    function renderCheckoutReview() {
        if (!els.checkoutReviewBox) {
            return;
        }
        let invoice;
        try {
            invoice = collectInvoiceInfo(false);
        } catch (_) {
            invoice = null;
        }
        const reviewLines = [
            `Account: ${currentIdentity ? currentIdentity.email : "-"}`,
            `Address: ${checkoutAddressSummary()}`,
            `Invoice Name: ${invoice && invoice.fullName ? invoice.fullName : "-"}`,
            `Invoice Email: ${invoice && invoice.email ? invoice.email : "-"}`,
            `Invoice Type: ${invoice && invoice.invoiceType ? invoice.invoiceType : "-"}`,
            `Payment Method: ${paymentMethodLabel(selectedPaymentMethod())}`,
            `Total: ${els.checkoutTotal.textContent || "0.00"}`,
        ];
        els.checkoutReviewBox.textContent = reviewLines.join("\n");
    }

    function validateCheckoutStep(step) {
        if (step === 1) {
            if (!getToken()) {
                throw new Error("Please sign in before checkout");
            }
            return;
        }
        if (step === 2) {
            const hasSelectedAddress = addresses.some(
                (item) => Number(item.id) === Number(selectedAddressId),
            );
            if (!selectedAddressId || !hasSelectedAddress) {
                throw new Error("Select a delivery address");
            }
            return;
        }
        if (step === 3) {
            collectInvoiceInfo(true);
            return;
        }
        if (step === 4) {
            if (paymentMethodNeedsCard(selectedPaymentMethod())) {
                collectCardPayload();
            }
            return;
        }
        if (step === 5) {
            if (!els.checkoutTermsCheck.checked) {
                throw new Error("Accept the terms to continue");
            }
        }
    }

    function renderCheckoutStep() {
        const stepPanels = document.querySelectorAll(".checkout-step");
        stepPanels.forEach((panel) => {
            const stepNumber = Number(panel.getAttribute("data-checkout-step"));
            panel.classList.toggle("d-none", stepNumber !== checkoutStep);
        });
        if (els.checkoutStepIndicator) {
            els.checkoutStepIndicator.textContent = String(checkoutStep);
        }
        if (els.checkoutProgressBar) {
            const progressPercent = Math.max(
                0,
                Math.min(
                    100,
                    Math.round((checkoutStep / CHECKOUT_TOTAL_STEPS) * 100),
                ),
            );
            els.checkoutProgressBar.style.width = `${progressPercent}%`;
            els.checkoutProgressBar.setAttribute(
                "aria-valuenow",
                String(progressPercent),
            );
            els.checkoutProgressBar.textContent = `${progressPercent}%`;
        }
        if (els.checkoutPrevBtn) {
            els.checkoutPrevBtn.disabled = checkoutStep <= 1;
        }
        if (els.checkoutNextBtn) {
            els.checkoutNextBtn.classList.toggle(
                "d-none",
                checkoutStep >= CHECKOUT_TOTAL_STEPS,
            );
        }
        updatePaymentMethodUi();
        if (checkoutStep >= CHECKOUT_TOTAL_STEPS) {
            renderCheckoutReview();
        }
    }

    function nextCheckoutStep() {
        try {
            validateCheckoutStep(checkoutStep);
            checkoutStep = Math.min(CHECKOUT_TOTAL_STEPS, checkoutStep + 1);
            renderCheckoutStep();
        } catch (error) {
            toast(error.message);
        }
    }

    function prevCheckoutStep() {
        checkoutStep = Math.max(1, checkoutStep - 1);
        renderCheckoutStep();
    }

    function resetCheckoutWizard() {
        checkoutStep = 1;
        if (els.checkoutTermsCheck) {
            els.checkoutTermsCheck.checked = false;
        }
        if (els.checkoutLoginStatus) {
            els.checkoutLoginStatus.textContent = currentIdentity
                ? `Signed in as ${currentIdentity.email}`
                : "Not signed in";
        }
        if (els.invoiceFullName) {
            els.invoiceFullName.value =
                profileInfo && profileInfo.email
                    ? String(profileInfo.email).split("@")[0]
                    : "";
        }
        if (els.invoiceEmail) {
            els.invoiceEmail.value = currentIdentity ? currentIdentity.email : "";
        }
        if (els.invoiceType) {
            els.invoiceType.value = "PERSONAL";
        }
        if (els.invoiceTaxNumber) {
            els.invoiceTaxNumber.value = "";
        }
        if (els.invoiceNote) {
            els.invoiceNote.value = "";
        }
        if (els.paymentMethodCredit) {
            els.paymentMethodCredit.checked = true;
        }
        renderCheckoutStep();
    }

    async function syncCartToServer() {
        const authToken = getToken();
        if (!authToken) {
            throw new Error("Checkout icin giris gerekli");
        }

        const cart = getCart();
        if (!cart.items.length) {
            throw new Error("Sepet bos");
        }

        await apiFetch(API.cart, { method: "DELETE" });

        for (const item of cart.items) {
            const productId = Number(item.id);
            if (!Number.isFinite(productId)) {
                throw new Error("Sepette gecersiz urun id var");
            }

            await apiFetch(API.cartItems, {
                method: "POST",
                json: {
                    productId,
                    quantity: item.qty,
                },
            });
        }
    }

    async function startPayment() {
        if (!getToken()) {
            toast("Once giris yapin");
            showModal(els.loginModal);
            return;
        }

        try {
            for (let step = 1; step <= CHECKOUT_TOTAL_STEPS; step += 1) {
                validateCheckoutStep(step);
            }
            if (checkoutStep !== CHECKOUT_TOTAL_STEPS) {
                checkoutStep = CHECKOUT_TOTAL_STEPS;
                renderCheckoutStep();
            }

            const method = selectedPaymentMethod();
            const cardPayload = paymentMethodNeedsCard(method)
                ? collectCardPayload()
                : null;
            collectInvoiceInfo(true);
            await syncCartToServer();

            const reserveResponse = await apiFetch(API.reserveDraft, {
                method: "POST",
                json: {},
            });

            lastDraftId =
                reserveResponse &&
                (reserveResponse.draftId ||
                    reserveResponse.id ||
                    reserveResponse.publicId);
            if (!lastDraftId) {
                throw new Error("Reserve response icinde draft id bulunamadi");
            }

            const payResponse = await apiFetch(API.payDraft(lastDraftId), {
                method: "POST",
                headers: {
                    "Idempotency-Key": `pay-${lastDraftId}-1`,
                },
                json: {
                    addressId: Number(selectedAddressId),
                    cardHolderName: cardPayload ? cardPayload.cardHolderName : null,
                    cardNumber: cardPayload ? cardPayload.cardNumber : null,
                    expiryMonth: cardPayload ? cardPayload.expiryMonth : null,
                    expiryYear: cardPayload ? cardPayload.expiryYear : null,
                    cvc: cardPayload ? cardPayload.cvc : null,
                },
            });

            lastPaymentId = payResponse && payResponse.paymentId;
            lastOrderId = payResponse && payResponse.orderId;

            els.lastPaymentId.textContent = lastPaymentId || "-";
            els.lastOrderId.textContent = lastOrderId || "-";

            toast("Odeme baslatildi");
        } catch (error) {
            toast(`Checkout basarisiz: ${error.message}`);
        }
    }

    async function confirmMockPayment() {
        if (!lastPaymentId) {
            toast("Once odeme baslatin");
            return;
        }

        try {
            await apiFetch(API.confirmMock(lastPaymentId), {
                method: "POST",
            });
            try {
                await apiFetch(API.cart, { method: "DELETE" });
            } catch (_) {
                // local clear still enough for UI
            }
            clearCart();
            toast("Mock payment confirmed");
        } catch (error) {
            toast(`Mock confirm basarisiz: ${error.message}`);
        }
    }

    async function openAddresses() {
        if (!getToken()) {
            toast("Please sign in to manage addresses");
            showModal(els.loginModal);
            return;
        }
        window.location.assign("/adress");
    }

    async function openCheckout() {
        if (!getToken()) {
            toast("Checkout icin once giris yapin");
            showModal(els.loginModal);
            return;
        }

        await loadAddresses();
        renderCheckoutAddresses();
        if (els.cardHolderName) {
            els.cardHolderName.value = "";
        }
        if (els.cardNumber) {
            els.cardNumber.value = "";
        }
        if (els.cardExpMonth) {
            els.cardExpMonth.value = "";
        }
        if (els.cardExpYear) {
            els.cardExpYear.value = "";
        }
        if (els.cardCvc) {
            els.cardCvc.value = "";
        }
        applySavedProfileCardToCheckout();
        resetCheckoutWizard();
        renderCheckoutReview();
        showModal(els.checkoutModal);
    }

    async function checkApiHealth() {
        try {
            await apiFetch(API.health);
            els.healthPill.className = "badge rounded-pill text-bg-success";
            els.healthPill.textContent = "UP";
        } catch (_) {
            els.healthPill.className = "badge rounded-pill text-bg-danger";
            els.healthPill.textContent = "DOWN";
        }
    }

    function handleProductGridClick(event) {
        const btn = event.target.closest("[data-add-to-cart]");
        if (!btn) {
            return;
        }

        const productId = Number(btn.dataset.id);
        if (!Number.isFinite(productId)) {
            toast("Gecersiz urun id");
            return;
        }

        addToCart({
            id: productId,
            name: btn.dataset.name || "Urun",
            price: Number(btn.dataset.price || 0),
            currency: btn.dataset.currency || "EUR",
        });
    }

    function handleCartListClick(event) {
        const btn = event.target.closest("[data-cart-action]");
        if (!btn) {
            return;
        }

        const action = btn.dataset.cartAction;
        const productId = Number(btn.dataset.id);
        if (!Number.isFinite(productId)) {
            return;
        }

        if (action === "remove") {
            removeFromCart(productId);
        }
        if (action === "dec") {
            changeCartQty(productId, -1);
        }
        if (action === "inc") {
            changeCartQty(productId, 1);
        }
    }

    function wireEvents() {
        els.navHomeLink.addEventListener("click", (event) => {
            const href = String(els.navHomeLink.getAttribute("href") || "");
            if (!href.startsWith("#")) {
                return;
            }
            event.preventDefault();
            switchView("home");
        });
        els.navProductsLink.addEventListener("click", (event) => {
            const href = String(els.navProductsLink.getAttribute("href") || "");
            if (!href.startsWith("#")) {
                return;
            }
            event.preventDefault();
            switchView("products");
        });
        els.openProductsViewBtn.addEventListener("click", () => {
            if (normalizedPath() !== "/products") {
                window.location.assign("/products");
                return;
            }
            switchView("products");
        });
        window.addEventListener("hashchange", () => {
            switchView(viewFromHash(window.location.hash), false);
        });

        els.searchInput.addEventListener("focus", () => {
            if (normalizedPath() !== "/products") {
                window.location.assign("/products");
                return;
            }
            if (currentView !== "products") {
                switchView("products");
            }
        });
        els.searchInput.addEventListener("input", () => {
            if (normalizedPath() !== "/products") {
                window.location.assign("/products");
                return;
            }
            if (currentView !== "products") {
                switchView("products");
            }
            applySearchSort();
        });
        els.sortSelect.addEventListener("change", applySearchSort);
        els.filterPriceMin.addEventListener("input", applySearchSort);
        els.filterPriceMax.addEventListener("input", applySearchSort);
        els.clearFiltersBtn.addEventListener("click", clearCatalogFilters);
        els.categoryFilterList.addEventListener("change", applySearchSort);

        els.openLoginBtn.addEventListener("click", () => showModal(els.loginModal));
        els.openOrdersBtn.addEventListener("click", openOrdersModal);
        els.openDashboardBtn.addEventListener("click", openDashboard);
        els.logoutMenuBtn.addEventListener("click", logout);
        els.reloadProductsBtn.addEventListener("click", reloadProducts);
        els.openAddressesBtn.addEventListener("click", openAddresses);
        els.clearCartBtn.addEventListener("click", clearCart);
        els.openCheckoutBtn.addEventListener("click", openCheckout);
        els.ordersLoadBtn.addEventListener("click", loadMyOrders);

        els.loginSubmitBtn.addEventListener("click", login);

        els.openNewAddressBtn.addEventListener("click", () =>
            showModal(els.newAddressModal),
        );
        els.createAddressBtn.addEventListener("click", createAddress);
        els.dashboardAddressesBtn.addEventListener("click", async () => {
            hideModal(els.dashboardModal);
            await openAddresses();
        });
        els.dashboardCheckoutBtn.addEventListener("click", async () => {
            hideModal(els.dashboardModal);
            await openCheckout();
        });
        els.profilePasswordChangeBtn.addEventListener("click", changeMyPassword);
        els.profileAddressLoadBtn.addEventListener(
            "click",
            loadSelectedProfileAddress,
        );
        els.profileAddressUpdateBtn.addEventListener("click", updateProfileAddress);
        els.profileCardSaveBtn.addEventListener(
            "click",
            saveProfileCardFromDashboard,
        );
        els.profileCardClearBtn.addEventListener(
            "click",
            clearProfileCardFromDashboard,
        );
        els.profileAddressSelect.addEventListener("change", () => {
            const selectedId = Number(els.profileAddressSelect.value);
            if (Number.isFinite(selectedId)) {
                fillProfileAddressFormById(selectedId);
            }
        });

        els.adminOrdersLoadBtn.addEventListener("click", loadAdminOrders);
        els.adminOrderDetailBtn.addEventListener("click", () =>
            loadAdminOrderDetail(normalizeText(els.adminOrderIdInput.value)),
        );
        els.adminShipBtn.addEventListener("click", adminShip);
        els.adminDeliverBtn.addEventListener("click", adminDeliver);
        els.adminCancelBtn.addEventListener("click", adminCancel);
        els.adminRestockBtn.addEventListener("click", adminRestock);
        els.adminAdjustBtn.addEventListener("click", adminAdjust);
        els.adminRefundsLoadBtn.addEventListener("click", loadAdminRefunds);
        els.adminWebhookLoadBtn.addEventListener("click", loadAdminWebhooks);
        els.adminCategoryCreateBtn.addEventListener("click", adminCreateCategory);
        els.adminCategoryUpdateBtn.addEventListener("click", adminUpdateCategory);
        els.adminCategoryDeleteBtn.addEventListener("click", adminDeleteCategory);
        els.adminProductsLoadBtn.addEventListener("click", loadAdminProducts);
        els.adminProductCreateBtn.addEventListener("click", adminCreateProduct);
        els.adminProductUpdateBtn.addEventListener("click", adminUpdateProduct);
        els.adminProductDeleteBtn.addEventListener("click", adminDeleteProduct);
        els.adminImageAddBtn.addEventListener("click", adminAddImage);
        els.adminImageDeleteBtn.addEventListener("click", adminDeleteImage);

        els.startPaymentBtn.addEventListener("click", startPayment);
        els.confirmPaymentBtn.addEventListener("click", confirmMockPayment);
        els.checkoutNextBtn.addEventListener("click", nextCheckoutStep);
        els.checkoutPrevBtn.addEventListener("click", prevCheckoutStep);

        els.productGrid.addEventListener("click", handleProductGridClick);
        els.cartList.addEventListener("click", handleCartListClick);

        [
            els.paymentMethodCredit,
            els.paymentMethodDebit,
            els.paymentMethodPaypal,
        ].forEach((field) => {
            field.addEventListener("change", () => {
                updatePaymentMethodUi();
                renderCheckoutReview();
            });
        });

        [
            els.invoiceFullName,
            els.invoiceEmail,
            els.invoiceType,
            els.invoiceTaxNumber,
            els.invoiceNote,
        ].forEach((field) => {
            field.addEventListener("input", renderCheckoutReview);
            field.addEventListener("change", renderCheckoutReview);
        });

        els.checkoutTermsCheck.addEventListener("change", renderCheckoutReview);

        els.ordersBody.addEventListener("click", (event) => {
            const btn = event.target.closest("[data-my-order-detail]");
            if (!btn) {
                return;
            }
            loadMyOrderDetail(btn.dataset.myOrderDetail);
        });

        els.adminOrdersBody.addEventListener("click", (event) => {
            const detailBtn = event.target.closest("[data-admin-order-detail]");
            if (detailBtn) {
                loadAdminOrderDetail(detailBtn.dataset.adminOrderDetail);
                return;
            }

            const useBtn = event.target.closest("[data-admin-order-use]");
            if (useBtn) {
                const orderId = useBtn.dataset.adminOrderUse;
                els.adminOrderIdInput.value = orderId;
                loadAdminOrderDetail(orderId);
            }
        });

        els.adminProductsBody.addEventListener("click", (event) => {
            const useBtn = event.target.closest("[data-admin-product-use]");
            if (!useBtn) {
                return;
            }
            loadAdminProductIntoForm(useBtn.dataset.adminProductUse);
        });

        els.adminProductSearch.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                loadAdminProducts();
            }
        });

        els.addressesList.addEventListener("click", (event) => {
            const btn = event.target.closest("[data-set-default]");
            if (!btn) {
                return;
            }
            const id = Number(btn.dataset.setDefault);
            if (Number.isFinite(id)) {
                setDefaultAddress(id);
            }
        });

        els.checkoutAddresses.addEventListener("change", (event) => {
            if (event.target && event.target.name === "checkoutAddress") {
                selectedAddressId = Number(event.target.value);
                renderCheckoutReview();
            }
        });

        [els.loginEmail, els.loginPassword].forEach((field) => {
            field.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    login();
                }
            });
        });

        [
            els.profileCurrentPassword,
            els.profileNewPassword,
            els.profileConfirmPassword,
        ].forEach((field) => {
            field.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    changeMyPassword();
                }
            });
        });
    }

    function init() {
        cacheElements();
        if (!document.body || document.body.dataset.page !== "storefront") {
            return;
        }

        const criticalElements = [
            els.homeSection,
            els.catalogSection,
            els.navHomeLink,
            els.navProductsLink,
            els.productGrid,
            els.cartList,
            els.openLoginBtn,
            els.profileMenuWrap,
            els.loginModal,
            els.dashboardModal,
            els.ordersModal,
            els.addressesModal,
            els.checkoutModal,
            els.newAddressModal,
        ];
        if (criticalElements.some((el) => !el)) {
            console.warn("Storefront template is missing required DOM elements.");
            return;
        }

        switchView(viewFromHash(window.location.hash), false);
        wireEvents();
        updateAuthUi();
        if (normalizedHash(window.location.hash) === "admindashboard") {
            openDashboard();
        }
        renderCart();
        checkApiHealth();
        loadCategoryFilters();
        reloadProducts(false);
    }

    document.addEventListener("DOMContentLoaded", init);
})();
