/**
 * api-client.ts
 * Exposes typed API client instances on window.ApiClient.
 * Built with esbuild → static/js/api-client.js (IIFE, browser-ready).
 */

import {
  Configuration,
  AuthApi,
  AuthPasswordApi,
  CartApi,
  CategoriesApi,
  CheckoutApi,
  InventoryApi,
  OrdersApi,
  PaymentsApi,
  ProductImagesApi,
  ProductsApi,
  ProfileApi,
  SessionsApi,
  AddressesApi,
} from "../../clients/ts-public/src/index";

import {
  Configuration as AdminConfiguration,
  AdminFulfillmentApi,
  AdminInventoryApi,
  AdminOrdersApi,
  AdminRefundsApi,
  AdminWebhookEventsApi,
} from "../../clients/ts-admin/src/index";

const LS_TOKEN = "auth.token";

function getToken(): string {
  return localStorage.getItem(LS_TOKEN) ?? "";
}

const publicConfig = new Configuration({
  basePath: "",
  accessToken: () => getToken(),
});

const adminConfig = new AdminConfiguration({
  basePath: "",
  accessToken: () => getToken(),
});

export const ApiClient = {
  // Public APIs
  auth: new AuthApi(publicConfig),
  authPassword: new AuthPasswordApi(publicConfig),
  cart: new CartApi(publicConfig),
  categories: new CategoriesApi(publicConfig),
  checkout: new CheckoutApi(publicConfig),
  inventory: new InventoryApi(publicConfig),
  orders: new OrdersApi(publicConfig),
  payments: new PaymentsApi(publicConfig),
  productImages: new ProductImagesApi(publicConfig),
  products: new ProductsApi(publicConfig),
  profile: new ProfileApi(publicConfig),
  sessions: new SessionsApi(publicConfig),
  addresses: new AddressesApi(publicConfig),

  // Admin APIs
  adminFulfillment: new AdminFulfillmentApi(adminConfig),
  adminInventory: new AdminInventoryApi(adminConfig),
  adminOrders: new AdminOrdersApi(adminConfig),
  adminRefunds: new AdminRefundsApi(adminConfig),
  adminWebhookEvents: new AdminWebhookEventsApi(adminConfig),
} as const;

// Attach to window for the vanilla-JS layer (script.js)
(window as any).ApiClient = ApiClient;

// ── Department & tier endpoints not yet in the generated clients ─────────────
// These are thin wrappers over the new /api/v1/dept/* and /api/v1/tier/* paths.

async function authedFetch(path: string): Promise<unknown> {
  const token = getToken();
  const res = await fetch(path, {
    headers: {
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try {
      const p = await res.json();
      msg = p.detail || p.message || p.title || msg;
    } catch (_) {
      /* ignore */
    }
    throw new Error(msg);
  }
  return res.status === 204 ? null : res.json();
}

export const DeptClient = {
  /** Departments the calling user belongs to */
  myDepartments: () => authedFetch("/api/v1/dept/me"),
  /** All departments (admin only) */
  listDepartments: () => authedFetch("/api/v1/dept/list"),
  /** Dashboard for a specific department slug */
  dashboard: (slug: string) => authedFetch(`/api/v1/dept/${slug}/dashboard`),
} as const;

export const TierClient = {
  /** Current user's tier info */
  myTier: () => authedFetch("/api/v1/tier/me"),
  /** Full benefit table */
  benefits: () => authedFetch("/api/v1/tier/benefits"),
} as const;

(window as any).DeptClient = DeptClient;
(window as any).TierClient = TierClient;
