var __unused = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // src/js/api-client.ts
  var api_client_exports = {};
  __export(api_client_exports, {
    ApiClient: () => ApiClient,
    DeptClient: () => DeptClient,
    TierClient: () => TierClient
  });

  // clients/ts-public/src/runtime.ts
  var BASE_PATH = "http://localhost".replace(/\/+$/, "");
  var Configuration = class {
    constructor(configuration = {}) {
      this.configuration = configuration;
    }
    set config(configuration) {
      this.configuration = configuration;
    }
    get basePath() {
      return this.configuration.basePath != null ? this.configuration.basePath : BASE_PATH;
    }
    get fetchApi() {
      return this.configuration.fetchApi;
    }
    get middleware() {
      return this.configuration.middleware || [];
    }
    get queryParamsStringify() {
      return this.configuration.queryParamsStringify || querystring;
    }
    get username() {
      return this.configuration.username;
    }
    get password() {
      return this.configuration.password;
    }
    get apiKey() {
      const apiKey = this.configuration.apiKey;
      if (apiKey) {
        return typeof apiKey === "function" ? apiKey : () => apiKey;
      }
      return void 0;
    }
    get accessToken() {
      const accessToken = this.configuration.accessToken;
      if (accessToken) {
        return typeof accessToken === "function" ? accessToken : async () => accessToken;
      }
      return void 0;
    }
    get headers() {
      return this.configuration.headers;
    }
    get credentials() {
      return this.configuration.credentials;
    }
  };
  var DefaultConfig = new Configuration();
  var _BaseAPI = class _BaseAPI {
    constructor(configuration = DefaultConfig) {
      this.configuration = configuration;
      this.fetchApi = async (url, init) => {
        let fetchParams = { url, init };
        for (const middleware of this.middleware) {
          if (middleware.pre) {
            fetchParams = await middleware.pre({
              fetch: this.fetchApi,
              ...fetchParams
            }) || fetchParams;
          }
        }
        let response = void 0;
        try {
          response = await (this.configuration.fetchApi || fetch)(fetchParams.url, fetchParams.init);
        } catch (e) {
          for (const middleware of this.middleware) {
            if (middleware.onError) {
              response = await middleware.onError({
                fetch: this.fetchApi,
                url: fetchParams.url,
                init: fetchParams.init,
                error: e,
                response: response ? response.clone() : void 0
              }) || response;
            }
          }
          if (response === void 0) {
            if (e instanceof Error) {
              throw new FetchError(e, "The request failed and the interceptors did not return an alternative response");
            } else {
              throw e;
            }
          }
        }
        for (const middleware of this.middleware) {
          if (middleware.post) {
            response = await middleware.post({
              fetch: this.fetchApi,
              url: fetchParams.url,
              init: fetchParams.init,
              response: response.clone()
            }) || response;
          }
        }
        return response;
      };
      this.middleware = configuration.middleware;
    }
    withMiddleware(...middlewares) {
      const next = this.clone();
      next.middleware = next.middleware.concat(...middlewares);
      return next;
    }
    withPreMiddleware(...preMiddlewares) {
      const middlewares = preMiddlewares.map((pre) => ({ pre }));
      return this.withMiddleware(...middlewares);
    }
    withPostMiddleware(...postMiddlewares) {
      const middlewares = postMiddlewares.map((post) => ({ post }));
      return this.withMiddleware(...middlewares);
    }
    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     *   application/json
     *   application/json; charset=UTF8
     *   APPLICATION/JSON
     *   application/vnd.company+json
     * @param mime - MIME (Multipurpose Internet Mail Extensions)
     * @return True if the given MIME is JSON, false otherwise.
     */
    isJsonMime(mime) {
      if (!mime) {
        return false;
      }
      return _BaseAPI.jsonRegex.test(mime);
    }
    async request(context, initOverrides) {
      const { url, init } = await this.createFetchParams(context, initOverrides);
      const response = await this.fetchApi(url, init);
      if (response && (response.status >= 200 && response.status < 300)) {
        return response;
      }
      throw new ResponseError(response, "Response returned an error code");
    }
    async createFetchParams(context, initOverrides) {
      let url = this.configuration.basePath + context.path;
      if (context.query !== void 0 && Object.keys(context.query).length !== 0) {
        url += "?" + this.configuration.queryParamsStringify(context.query);
      }
      const headers = Object.assign({}, this.configuration.headers, context.headers);
      Object.keys(headers).forEach((key) => headers[key] === void 0 ? delete headers[key] : {});
      const initOverrideFn = typeof initOverrides === "function" ? initOverrides : async () => initOverrides;
      const initParams = {
        method: context.method,
        headers,
        body: context.body,
        credentials: this.configuration.credentials
      };
      const overriddenInit = {
        ...initParams,
        ...await initOverrideFn({
          init: initParams,
          context
        })
      };
      let body;
      if (isFormData(overriddenInit.body) || overriddenInit.body instanceof URLSearchParams || isBlob(overriddenInit.body)) {
        body = overriddenInit.body;
      } else if (this.isJsonMime(headers["Content-Type"])) {
        body = JSON.stringify(overriddenInit.body);
      } else {
        body = overriddenInit.body;
      }
      const init = {
        ...overriddenInit,
        body
      };
      return { url, init };
    }
    /**
     * Create a shallow clone of `this` by constructing a new instance
     * and then shallow cloning data members.
     */
    clone() {
      const constructor = this.constructor;
      const next = new constructor(this.configuration);
      next.middleware = this.middleware.slice();
      return next;
    }
  };
  _BaseAPI.jsonRegex = new RegExp("^(:?application/json|[^;/ 	]+/[^;/ 	]+[+]json)[ 	]*(:?;.*)?$", "i");
  var BaseAPI = _BaseAPI;
  function isBlob(value) {
    return typeof Blob !== "undefined" && value instanceof Blob;
  }
  function isFormData(value) {
    return typeof FormData !== "undefined" && value instanceof FormData;
  }
  var ResponseError = class extends Error {
    constructor(response, msg) {
      super(msg);
      this.response = response;
      this.name = "ResponseError";
    }
  };
  var FetchError = class extends Error {
    constructor(cause, msg) {
      super(msg);
      this.cause = cause;
      this.name = "FetchError";
    }
  };
  var RequiredError = class extends Error {
    constructor(field, msg) {
      super(msg);
      this.field = field;
      this.name = "RequiredError";
    }
  };
  function querystring(params, prefix = "") {
    return Object.keys(params).map((key) => querystringSingleKey(key, params[key], prefix)).filter((part) => part.length > 0).join("&");
  }
  function querystringSingleKey(key, value, keyPrefix = "") {
    const fullKey = keyPrefix + (keyPrefix.length ? `[${key}]` : key);
    if (value instanceof Array) {
      const multiValue = value.map((singleValue) => encodeURIComponent(String(singleValue))).join(`&${encodeURIComponent(fullKey)}=`);
      return `${encodeURIComponent(fullKey)}=${multiValue}`;
    }
    if (value instanceof Set) {
      const valueAsArray = Array.from(value);
      return querystringSingleKey(key, valueAsArray, keyPrefix);
    }
    if (value instanceof Date) {
      return `${encodeURIComponent(fullKey)}=${encodeURIComponent(value.toISOString())}`;
    }
    if (value instanceof Object) {
      return querystring(value, fullKey);
    }
    return `${encodeURIComponent(fullKey)}=${encodeURIComponent(String(value))}`;
  }
  function canConsumeForm(consumes) {
    for (const consume of consumes) {
      if ("multipart/form-data" === consume.contentType) {
        return true;
      }
    }
    return false;
  }
  var JSONApiResponse = class {
    constructor(raw, transformer = (jsonValue) => jsonValue) {
      this.raw = raw;
      this.transformer = transformer;
    }
    async value() {
      return this.transformer(await this.raw.json());
    }
  };
  var VoidApiResponse = class {
    constructor(raw) {
      this.raw = raw;
    }
    async value() {
      return void 0;
    }
  };

  // clients/ts-public/src/models/AddImageRequest.ts
  function AddImageRequestToJSON(json) {
    return AddImageRequestToJSONTyped(json, false);
  }
  function AddImageRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "altText": value["altText"],
      "url": value["url"]
    };
  }

  // clients/ts-public/src/models/AddressResponse.ts
  function AddressResponseFromJSON(json) {
    return AddressResponseFromJSONTyped(json, false);
  }
  function AddressResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "city": json["city"] == null ? void 0 : json["city"],
      "countryCode": json["countryCode"] == null ? void 0 : json["countryCode"],
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "fullName": json["fullName"] == null ? void 0 : json["fullName"],
      "id": json["id"],
      "isDefault": json["isDefault"] == null ? void 0 : json["isDefault"],
      "label": json["label"] == null ? void 0 : json["label"],
      "line1": json["line1"] == null ? void 0 : json["line1"],
      "line2": json["line2"] == null ? void 0 : json["line2"],
      "phone": json["phone"] == null ? void 0 : json["phone"],
      "postalCode": json["postalCode"] == null ? void 0 : json["postalCode"],
      "state": json["state"] == null ? void 0 : json["state"],
      "updatedAt": json["updatedAt"] == null ? void 0 : new Date(json["updatedAt"])
    };
  }

  // clients/ts-public/src/models/CartItemResponse.ts
  function CartItemResponseFromJSON(json) {
    return CartItemResponseFromJSONTyped(json, false);
  }
  function CartItemResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "lineTotal": json["lineTotal"] == null ? void 0 : json["lineTotal"],
      "name": json["name"] == null ? void 0 : json["name"],
      "primaryImageUrl": json["primaryImageUrl"] == null ? void 0 : json["primaryImageUrl"],
      "productId": json["productId"] == null ? void 0 : json["productId"],
      "quantity": json["quantity"] == null ? void 0 : json["quantity"],
      "sku": json["sku"] == null ? void 0 : json["sku"],
      "unitPrice": json["unitPrice"] == null ? void 0 : json["unitPrice"]
    };
  }

  // clients/ts-public/src/models/CartResponse.ts
  function CartResponseFromJSON(json) {
    return CartResponseFromJSONTyped(json, false);
  }
  function CartResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "items": json["items"] == null ? void 0 : json["items"].map(CartItemResponseFromJSON)
    };
  }

  // clients/ts-public/src/models/CategoryRef.ts
  function CategoryRefFromJSON(json) {
    return CategoryRefFromJSONTyped(json, false);
  }
  function CategoryRefFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "id": json["id"] == null ? void 0 : json["id"],
      "name": json["name"] == null ? void 0 : json["name"],
      "slug": json["slug"] == null ? void 0 : json["slug"]
    };
  }

  // clients/ts-public/src/models/CategoryResponse.ts
  function CategoryResponseFromJSON(json) {
    return CategoryResponseFromJSONTyped(json, false);
  }
  function CategoryResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "id": json["id"] == null ? void 0 : json["id"],
      "name": json["name"] == null ? void 0 : json["name"],
      "slug": json["slug"] == null ? void 0 : json["slug"],
      "updatedAt": json["updatedAt"] == null ? void 0 : new Date(json["updatedAt"])
    };
  }

  // clients/ts-public/src/models/ChangePasswordRequest.ts
  function ChangePasswordRequestToJSON(json) {
    return ChangePasswordRequestToJSONTyped(json, false);
  }
  function ChangePasswordRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "confirmNewPassword": value["confirmNewPassword"],
      "currentPassword": value["currentPassword"],
      "newPassword": value["newPassword"]
    };
  }

  // clients/ts-public/src/models/CreateAddressRequest.ts
  function CreateAddressRequestToJSON(json) {
    return CreateAddressRequestToJSONTyped(json, false);
  }
  function CreateAddressRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "city": value["city"],
      "countryCode": value["countryCode"],
      "fullName": value["fullName"],
      "label": value["label"],
      "line1": value["line1"],
      "line2": value["line2"],
      "makeDefault": value["makeDefault"],
      "phone": value["phone"],
      "postalCode": value["postalCode"],
      "state": value["state"]
    };
  }

  // clients/ts-public/src/models/CreateCategoryRequest.ts
  function CreateCategoryRequestToJSON(json) {
    return CreateCategoryRequestToJSONTyped(json, false);
  }
  function CreateCategoryRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "name": value["name"],
      "slug": value["slug"]
    };
  }

  // clients/ts-public/src/models/CreateProductRequest.ts
  function CreateProductRequestToJSON(json) {
    return CreateProductRequestToJSONTyped(json, false);
  }
  function CreateProductRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "categoryIds": value["categoryIds"] == null ? void 0 : Array.from(value["categoryIds"]),
      "currency": value["currency"],
      "description": value["description"],
      "name": value["name"],
      "price": value["price"],
      "sku": value["sku"],
      "status": value["status"],
      "stockQuantity": value["stockQuantity"]
    };
  }

  // clients/ts-public/src/models/DraftItemResponse.ts
  function DraftItemResponseFromJSON(json) {
    return DraftItemResponseFromJSONTyped(json, false);
  }
  function DraftItemResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "lineTotal": json["lineTotal"] == null ? void 0 : json["lineTotal"],
      "name": json["name"] == null ? void 0 : json["name"],
      "productId": json["productId"] == null ? void 0 : json["productId"],
      "quantity": json["quantity"] == null ? void 0 : json["quantity"],
      "reservationExpiresAt": json["reservationExpiresAt"] == null ? void 0 : new Date(json["reservationExpiresAt"]),
      "reservationId": json["reservationId"] == null ? void 0 : json["reservationId"],
      "sku": json["sku"] == null ? void 0 : json["sku"],
      "unitPrice": json["unitPrice"] == null ? void 0 : json["unitPrice"]
    };
  }

  // clients/ts-public/src/models/DraftResponse.ts
  function DraftResponseFromJSON(json) {
    return DraftResponseFromJSONTyped(json, false);
  }
  function DraftResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "draftId": json["draftId"] == null ? void 0 : json["draftId"],
      "expiresAt": json["expiresAt"] == null ? void 0 : new Date(json["expiresAt"]),
      "items": json["items"] == null ? void 0 : json["items"].map(DraftItemResponseFromJSON),
      "status": json["status"] == null ? void 0 : json["status"],
      "totalAmount": json["totalAmount"] == null ? void 0 : json["totalAmount"]
    };
  }

  // clients/ts-public/src/models/FailRequest.ts
  function FailRequestToJSON(json) {
    return FailRequestToJSONTyped(json, false);
  }
  function FailRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "error": value["error"]
    };
  }

  // clients/ts-public/src/models/ForgotRequest.ts
  function ForgotRequestToJSON(json) {
    return ForgotRequestToJSONTyped(json, false);
  }
  function ForgotRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "email": value["email"]
    };
  }

  // clients/ts-public/src/models/ImagePageItem.ts
  function ImagePageItemFromJSON(json) {
    return ImagePageItemFromJSONTyped(json, false);
  }
  function ImagePageItemFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "altText": json["altText"] == null ? void 0 : json["altText"],
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "id": json["id"] == null ? void 0 : json["id"],
      "primary": json["primary"] == null ? void 0 : json["primary"],
      "sortOrder": json["sortOrder"] == null ? void 0 : json["sortOrder"],
      "url": json["url"] == null ? void 0 : json["url"]
    };
  }

  // clients/ts-public/src/models/ImageRef.ts
  function ImageRefFromJSON(json) {
    return ImageRefFromJSONTyped(json, false);
  }
  function ImageRefFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "altText": json["altText"] == null ? void 0 : json["altText"],
      "id": json["id"] == null ? void 0 : json["id"],
      "primary": json["primary"] == null ? void 0 : json["primary"],
      "sortOrder": json["sortOrder"] == null ? void 0 : json["sortOrder"],
      "url": json["url"] == null ? void 0 : json["url"]
    };
  }

  // clients/ts-public/src/models/LoginRequest.ts
  function LoginRequestToJSON(json) {
    return LoginRequestToJSONTyped(json, false);
  }
  function LoginRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "email": value["email"],
      "password": value["password"]
    };
  }

  // clients/ts-public/src/models/MeResponse.ts
  function MeResponseFromJSON(json) {
    return MeResponseFromJSONTyped(json, false);
  }
  function MeResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "email": json["email"] == null ? void 0 : json["email"],
      "enabled": json["enabled"] == null ? void 0 : json["enabled"],
      "locked": json["locked"] == null ? void 0 : json["locked"],
      "roles": json["roles"] == null ? void 0 : json["roles"],
      "updatedAt": json["updatedAt"] == null ? void 0 : new Date(json["updatedAt"])
    };
  }

  // clients/ts-public/src/models/OrderItemResponse.ts
  function OrderItemResponseFromJSON(json) {
    return OrderItemResponseFromJSONTyped(json, false);
  }
  function OrderItemResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "lineTotal": json["lineTotal"] == null ? void 0 : json["lineTotal"],
      "name": json["name"] == null ? void 0 : json["name"],
      "productId": json["productId"] == null ? void 0 : json["productId"],
      "quantity": json["quantity"] == null ? void 0 : json["quantity"],
      "sku": json["sku"] == null ? void 0 : json["sku"],
      "unitPrice": json["unitPrice"] == null ? void 0 : json["unitPrice"]
    };
  }

  // clients/ts-public/src/models/ShipmentInfo.ts
  function ShipmentInfoFromJSON(json) {
    return ShipmentInfoFromJSONTyped(json, false);
  }
  function ShipmentInfoFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "carrier": json["carrier"] == null ? void 0 : json["carrier"],
      "deliveredAt": json["deliveredAt"] == null ? void 0 : new Date(json["deliveredAt"]),
      "shipmentId": json["shipmentId"] == null ? void 0 : json["shipmentId"],
      "shippedAt": json["shippedAt"] == null ? void 0 : new Date(json["shippedAt"]),
      "status": json["status"] == null ? void 0 : json["status"],
      "trackingNumber": json["trackingNumber"] == null ? void 0 : json["trackingNumber"]
    };
  }

  // clients/ts-public/src/models/ShippingAddressInfo.ts
  function ShippingAddressInfoFromJSON(json) {
    return ShippingAddressInfoFromJSONTyped(json, false);
  }
  function ShippingAddressInfoFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "city": json["city"] == null ? void 0 : json["city"],
      "countryCode": json["countryCode"] == null ? void 0 : json["countryCode"],
      "fullName": json["fullName"] == null ? void 0 : json["fullName"],
      "line1": json["line1"] == null ? void 0 : json["line1"],
      "line2": json["line2"] == null ? void 0 : json["line2"],
      "phone": json["phone"] == null ? void 0 : json["phone"],
      "postalCode": json["postalCode"] == null ? void 0 : json["postalCode"],
      "state": json["state"] == null ? void 0 : json["state"]
    };
  }

  // clients/ts-public/src/models/OrderDetailResponse.ts
  function OrderDetailResponseFromJSON(json) {
    return OrderDetailResponseFromJSONTyped(json, false);
  }
  function OrderDetailResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "items": json["items"] == null ? void 0 : json["items"].map(OrderItemResponseFromJSON),
      "orderId": json["orderId"] == null ? void 0 : json["orderId"],
      "shipments": json["shipments"] == null ? void 0 : json["shipments"].map(ShipmentInfoFromJSON),
      "shippingAddress": json["shippingAddress"] == null ? void 0 : ShippingAddressInfoFromJSON(json["shippingAddress"]),
      "status": json["status"] == null ? void 0 : json["status"],
      "totalAmount": json["totalAmount"] == null ? void 0 : json["totalAmount"]
    };
  }

  // clients/ts-public/src/models/PageMeta.ts
  function PageMetaFromJSON(json) {
    return PageMetaFromJSONTyped(json, false);
  }
  function PageMetaFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "first": json["first"] == null ? void 0 : json["first"],
      "last": json["last"] == null ? void 0 : json["last"],
      "number": json["number"] == null ? void 0 : json["number"],
      "size": json["size"] == null ? void 0 : json["size"],
      "totalElements": json["totalElements"] == null ? void 0 : json["totalElements"],
      "totalPages": json["totalPages"] == null ? void 0 : json["totalPages"]
    };
  }

  // clients/ts-public/src/models/PageResponse.ts
  function PageResponseFromJSON(json) {
    return PageResponseFromJSONTyped(json, false);
  }
  function PageResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "items": json["items"] == null ? void 0 : json["items"].map(ImagePageItemFromJSON),
      "page": json["page"] == null ? void 0 : PageMetaFromJSON(json["page"])
    };
  }

  // clients/ts-public/src/models/PayRequest.ts
  function PayRequestToJSON(json) {
    return PayRequestToJSONTyped(json, false);
  }
  function PayRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "addressId": value["addressId"],
      "cardHolderName": value["cardHolderName"],
      "cardNumber": value["cardNumber"],
      "cvc": value["cvc"],
      "expiryMonth": value["expiryMonth"],
      "expiryYear": value["expiryYear"]
    };
  }

  // clients/ts-public/src/models/PaymentResponse.ts
  function PaymentResponseFromJSON(json) {
    return PaymentResponseFromJSONTyped(json, false);
  }
  function PaymentResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "amount": json["amount"] == null ? void 0 : json["amount"],
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "orderId": json["orderId"] == null ? void 0 : json["orderId"],
      "paymentId": json["paymentId"] == null ? void 0 : json["paymentId"],
      "provider": json["provider"] == null ? void 0 : json["provider"],
      "status": json["status"] == null ? void 0 : json["status"],
      "updatedAt": json["updatedAt"] == null ? void 0 : new Date(json["updatedAt"])
    };
  }

  // clients/ts-public/src/models/ProductResponse.ts
  function ProductResponseFromJSON(json) {
    return ProductResponseFromJSONTyped(json, false);
  }
  function ProductResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "categories": json["categories"] == null ? void 0 : json["categories"].map(CategoryRefFromJSON),
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "description": json["description"] == null ? void 0 : json["description"],
      "id": json["id"] == null ? void 0 : json["id"],
      "images": json["images"] == null ? void 0 : json["images"].map(ImageRefFromJSON),
      "name": json["name"] == null ? void 0 : json["name"],
      "price": json["price"] == null ? void 0 : json["price"],
      "primaryImage": json["primaryImage"] == null ? void 0 : ImageRefFromJSON(json["primaryImage"]),
      "sku": json["sku"] == null ? void 0 : json["sku"],
      "status": json["status"] == null ? void 0 : json["status"],
      "stockQuantity": json["stockQuantity"] == null ? void 0 : json["stockQuantity"],
      "updatedAt": json["updatedAt"] == null ? void 0 : new Date(json["updatedAt"])
    };
  }

  // clients/ts-public/src/models/RefundRequest.ts
  function RefundRequestToJSON(json) {
    return RefundRequestToJSONTyped(json, false);
  }
  function RefundRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "reason": value["reason"]
    };
  }

  // clients/ts-public/src/models/RefundResponse.ts
  function RefundResponseFromJSON(json) {
    return RefundResponseFromJSONTyped(json, false);
  }
  function RefundResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "refundId": json["refundId"] == null ? void 0 : json["refundId"]
    };
  }

  // clients/ts-public/src/models/RenameRequest.ts
  function RenameRequestToJSON(json) {
    return RenameRequestToJSONTyped(json, false);
  }
  function RenameRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "deviceName": value["deviceName"]
    };
  }

  // clients/ts-public/src/models/ReorderItem.ts
  function ReorderItemToJSON(json) {
    return ReorderItemToJSONTyped(json, false);
  }
  function ReorderItemToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "id": value["id"],
      "primary": value["primary"],
      "sortOrder": value["sortOrder"]
    };
  }

  // clients/ts-public/src/models/ReorderRequest.ts
  function ReorderRequestToJSON(json) {
    return ReorderRequestToJSONTyped(json, false);
  }
  function ReorderRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "items": value["items"].map(ReorderItemToJSON)
    };
  }

  // clients/ts-public/src/models/ReservationResponse.ts
  function ReservationResponseFromJSON(json) {
    return ReservationResponseFromJSONTyped(json, false);
  }
  function ReservationResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "expiresAt": json["expiresAt"] == null ? void 0 : json["expiresAt"],
      "remainingStock": json["remainingStock"] == null ? void 0 : json["remainingStock"],
      "reservationId": json["reservationId"] == null ? void 0 : json["reservationId"]
    };
  }

  // clients/ts-public/src/models/ReserveRequest.ts
  function ReserveRequestToJSON(json) {
    return ReserveRequestToJSONTyped(json, false);
  }
  function ReserveRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "productId": value["productId"],
      "quantity": value["quantity"],
      "ttlMinutes": value["ttlMinutes"]
    };
  }

  // clients/ts-public/src/models/ResetRequest.ts
  function ResetRequestToJSON(json) {
    return ResetRequestToJSONTyped(json, false);
  }
  function ResetRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "newPassword": value["newPassword"],
      "token": value["token"]
    };
  }

  // clients/ts-public/src/models/SessionDto.ts
  function SessionDtoFromJSON(json) {
    return SessionDtoFromJSONTyped(json, false);
  }
  function SessionDtoFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "current": json["current"] == null ? void 0 : json["current"],
      "deviceName": json["deviceName"] == null ? void 0 : json["deviceName"],
      "ip": json["ip"] == null ? void 0 : json["ip"],
      "lastSeenAt": json["lastSeenAt"] == null ? void 0 : new Date(json["lastSeenAt"]),
      "revoked": json["revoked"] == null ? void 0 : json["revoked"],
      "revokedAt": json["revokedAt"] == null ? void 0 : new Date(json["revokedAt"]),
      "sessionId": json["sessionId"] == null ? void 0 : json["sessionId"],
      "userAgent": json["userAgent"] == null ? void 0 : json["userAgent"]
    };
  }

  // clients/ts-public/src/models/StartPaymentResponse.ts
  function StartPaymentResponseFromJSON(json) {
    return StartPaymentResponseFromJSONTyped(json, false);
  }
  function StartPaymentResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "orderId": json["orderId"] == null ? void 0 : json["orderId"],
      "paymentId": json["paymentId"] == null ? void 0 : json["paymentId"]
    };
  }

  // clients/ts-public/src/models/SubmitResponse.ts
  function SubmitResponseFromJSON(json) {
    return SubmitResponseFromJSONTyped(json, false);
  }
  function SubmitResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "orderId": json["orderId"] == null ? void 0 : json["orderId"]
    };
  }

  // clients/ts-public/src/models/TokenResponse.ts
  function TokenResponseFromJSON(json) {
    return TokenResponseFromJSONTyped(json, false);
  }
  function TokenResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "accessToken": json["accessToken"] == null ? void 0 : json["accessToken"],
      "expiresInSeconds": json["expiresInSeconds"] == null ? void 0 : json["expiresInSeconds"],
      "sessionId": json["sessionId"] == null ? void 0 : json["sessionId"],
      "tokenType": json["tokenType"] == null ? void 0 : json["tokenType"]
    };
  }

  // clients/ts-public/src/models/UpdateAddressRequest.ts
  function UpdateAddressRequestToJSON(json) {
    return UpdateAddressRequestToJSONTyped(json, false);
  }
  function UpdateAddressRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "city": value["city"],
      "countryCode": value["countryCode"],
      "fullName": value["fullName"],
      "label": value["label"],
      "line1": value["line1"],
      "line2": value["line2"],
      "phone": value["phone"],
      "postalCode": value["postalCode"],
      "state": value["state"]
    };
  }

  // clients/ts-public/src/models/UpdateCategoryRequest.ts
  function UpdateCategoryRequestToJSON(json) {
    return UpdateCategoryRequestToJSONTyped(json, false);
  }
  function UpdateCategoryRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "name": value["name"]
    };
  }

  // clients/ts-public/src/models/UpdateProductRequest.ts
  function UpdateProductRequestToJSON(json) {
    return UpdateProductRequestToJSONTyped(json, false);
  }
  function UpdateProductRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "categoryIds": value["categoryIds"] == null ? void 0 : Array.from(value["categoryIds"]),
      "currency": value["currency"],
      "description": value["description"],
      "name": value["name"],
      "price": value["price"],
      "status": value["status"],
      "stockQuantity": value["stockQuantity"]
    };
  }

  // clients/ts-public/src/models/UpsertCartItemRequest.ts
  function UpsertCartItemRequestToJSON(json) {
    return UpsertCartItemRequestToJSONTyped(json, false);
  }
  function UpsertCartItemRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "productId": value["productId"],
      "quantity": value["quantity"]
    };
  }

  // clients/ts-public/src/apis/AddressesApi.ts
  var AddressesApi = class extends BaseAPI {
    /**
     * Creates request options for create2 without sending the request
     */
    async create2RequestOpts(requestParameters) {
      if (requestParameters["createAddressRequest"] == null) {
        throw new RequiredError(
          "createAddressRequest",
          'Required parameter "createAddressRequest" was null or undefined when calling create2().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/addresses`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: CreateAddressRequestToJSON(requestParameters["createAddressRequest"])
      };
    }
    /**
     */
    async create2Raw(requestParameters, initOverrides) {
      const requestOptions = await this.create2RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => AddressResponseFromJSON(jsonValue));
    }
    /**
     */
    async create2(requestParameters, initOverrides) {
      const response = await this.create2Raw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for delete2 without sending the request
     */
    async delete2RequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling delete2().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/addresses/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "DELETE",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async delete2Raw(requestParameters, initOverrides) {
      const requestOptions = await this.delete2RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async delete2(requestParameters, initOverrides) {
      await this.delete2Raw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for list3 without sending the request
     */
    async list3RequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/addresses`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async list3Raw(initOverrides) {
      const requestOptions = await this.list3RequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => jsonValue.map(AddressResponseFromJSON));
    }
    /**
     */
    async list3(initOverrides) {
      const response = await this.list3Raw(initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for setDefault without sending the request
     */
    async setDefaultRequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling setDefault().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/addresses/{id}/default`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async setDefaultRaw(requestParameters, initOverrides) {
      const requestOptions = await this.setDefaultRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async setDefault(requestParameters, initOverrides) {
      await this.setDefaultRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for update2 without sending the request
     */
    async update2RequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling update2().'
        );
      }
      if (requestParameters["updateAddressRequest"] == null) {
        throw new RequiredError(
          "updateAddressRequest",
          'Required parameter "updateAddressRequest" was null or undefined when calling update2().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/addresses/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "PUT",
        headers: headerParameters,
        query: queryParameters,
        body: UpdateAddressRequestToJSON(requestParameters["updateAddressRequest"])
      };
    }
    /**
     */
    async update2Raw(requestParameters, initOverrides) {
      const requestOptions = await this.update2RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => AddressResponseFromJSON(jsonValue));
    }
    /**
     */
    async update2(requestParameters, initOverrides) {
      const response = await this.update2Raw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/AuthApi.ts
  var AuthApi = class extends BaseAPI {
    /**
     * Creates request options for login without sending the request
     */
    async loginRequestOpts(requestParameters) {
      if (requestParameters["loginRequest"] == null) {
        throw new RequiredError(
          "loginRequest",
          'Required parameter "loginRequest" was null or undefined when calling login().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/auth/login`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: LoginRequestToJSON(requestParameters["loginRequest"])
      };
    }
    /**
     */
    async loginRaw(requestParameters, initOverrides) {
      const requestOptions = await this.loginRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }
    /**
     */
    async login(requestParameters, initOverrides) {
      const response = await this.loginRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for logout without sending the request
     */
    async logoutRequestOpts(requestParameters) {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/auth/logout`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async logoutRaw(requestParameters, initOverrides) {
      const requestOptions = await this.logoutRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async logout(requestParameters = {}, initOverrides) {
      await this.logoutRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for refresh without sending the request
     */
    async refreshRequestOpts(requestParameters) {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/auth/refresh`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async refreshRaw(requestParameters, initOverrides) {
      const requestOptions = await this.refreshRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }
    /**
     */
    async refresh(requestParameters = {}, initOverrides) {
      const response = await this.refreshRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/AuthPasswordApi.ts
  var AuthPasswordApi = class extends BaseAPI {
    /**
     * Creates request options for forgot without sending the request
     */
    async forgotRequestOpts(requestParameters) {
      if (requestParameters["forgotRequest"] == null) {
        throw new RequiredError(
          "forgotRequest",
          'Required parameter "forgotRequest" was null or undefined when calling forgot().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/auth/password/forgot`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: ForgotRequestToJSON(requestParameters["forgotRequest"])
      };
    }
    /**
     */
    async forgotRaw(requestParameters, initOverrides) {
      const requestOptions = await this.forgotRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async forgot(requestParameters, initOverrides) {
      await this.forgotRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for reset without sending the request
     */
    async resetRequestOpts(requestParameters) {
      if (requestParameters["resetRequest"] == null) {
        throw new RequiredError(
          "resetRequest",
          'Required parameter "resetRequest" was null or undefined when calling reset().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/auth/password/reset`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: ResetRequestToJSON(requestParameters["resetRequest"])
      };
    }
    /**
     */
    async resetRaw(requestParameters, initOverrides) {
      const requestOptions = await this.resetRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async reset(requestParameters, initOverrides) {
      await this.resetRaw(requestParameters, initOverrides);
    }
  };

  // clients/ts-public/src/apis/CartApi.ts
  var CartApi = class extends BaseAPI {
    /**
     * Creates request options for clear without sending the request
     */
    async clearRequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/cart`;
      return {
        path: urlPath,
        method: "DELETE",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async clearRaw(initOverrides) {
      const requestOptions = await this.clearRequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async clear(initOverrides) {
      await this.clearRaw(initOverrides);
    }
    /**
     * Creates request options for get3 without sending the request
     */
    async get3RequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/cart`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async get3Raw(initOverrides) {
      const requestOptions = await this.get3RequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => CartResponseFromJSON(jsonValue));
    }
    /**
     */
    async get3(initOverrides) {
      const response = await this.get3Raw(initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for remove without sending the request
     */
    async removeRequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError(
          "productId",
          'Required parameter "productId" was null or undefined when calling remove().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/cart/items/{productId}`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "DELETE",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async removeRaw(requestParameters, initOverrides) {
      const requestOptions = await this.removeRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async remove(requestParameters, initOverrides) {
      await this.removeRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for upsert without sending the request
     */
    async upsertRequestOpts(requestParameters) {
      if (requestParameters["upsertCartItemRequest"] == null) {
        throw new RequiredError(
          "upsertCartItemRequest",
          'Required parameter "upsertCartItemRequest" was null or undefined when calling upsert().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/cart/items`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: UpsertCartItemRequestToJSON(requestParameters["upsertCartItemRequest"])
      };
    }
    /**
     */
    async upsertRaw(requestParameters, initOverrides) {
      const requestOptions = await this.upsertRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async upsert(requestParameters, initOverrides) {
      await this.upsertRaw(requestParameters, initOverrides);
    }
  };

  // clients/ts-public/src/apis/CategoriesApi.ts
  var CategoriesApi = class extends BaseAPI {
    /**
     * Creates request options for create1 without sending the request
     */
    async create1RequestOpts(requestParameters) {
      if (requestParameters["createCategoryRequest"] == null) {
        throw new RequiredError(
          "createCategoryRequest",
          'Required parameter "createCategoryRequest" was null or undefined when calling create1().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/categories`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: CreateCategoryRequestToJSON(requestParameters["createCategoryRequest"])
      };
    }
    /**
     */
    async create1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.create1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => CategoryResponseFromJSON(jsonValue));
    }
    /**
     */
    async create1(requestParameters, initOverrides) {
      const response = await this.create1Raw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for delete1 without sending the request
     */
    async delete1RequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling delete1().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/categories/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "DELETE",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async delete1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.delete1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async delete1(requestParameters, initOverrides) {
      await this.delete1Raw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for list2 without sending the request
     */
    async list2RequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/categories`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async list2Raw(initOverrides) {
      const requestOptions = await this.list2RequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => jsonValue.map(CategoryResponseFromJSON));
    }
    /**
     */
    async list2(initOverrides) {
      const response = await this.list2Raw(initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for update1 without sending the request
     */
    async update1RequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling update1().'
        );
      }
      if (requestParameters["updateCategoryRequest"] == null) {
        throw new RequiredError(
          "updateCategoryRequest",
          'Required parameter "updateCategoryRequest" was null or undefined when calling update1().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/categories/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "PUT",
        headers: headerParameters,
        query: queryParameters,
        body: UpdateCategoryRequestToJSON(requestParameters["updateCategoryRequest"])
      };
    }
    /**
     */
    async update1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.update1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => CategoryResponseFromJSON(jsonValue));
    }
    /**
     */
    async update1(requestParameters, initOverrides) {
      const response = await this.update1Raw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/CheckoutApi.ts
  var CheckoutApi = class extends BaseAPI {
    /**
     * Creates request options for cancel without sending the request
     */
    async cancelRequestOpts(requestParameters) {
      if (requestParameters["draftId"] == null) {
        throw new RequiredError(
          "draftId",
          'Required parameter "draftId" was null or undefined when calling cancel().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/checkout/drafts/{draftId}/cancel`;
      urlPath = urlPath.replace(`{${"draftId"}}`, encodeURIComponent(String(requestParameters["draftId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async cancelRaw(requestParameters, initOverrides) {
      const requestOptions = await this.cancelRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async cancel(requestParameters, initOverrides) {
      await this.cancelRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for getDraft without sending the request
     */
    async getDraftRequestOpts(requestParameters) {
      if (requestParameters["draftId"] == null) {
        throw new RequiredError(
          "draftId",
          'Required parameter "draftId" was null or undefined when calling getDraft().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/checkout/drafts/{draftId}`;
      urlPath = urlPath.replace(`{${"draftId"}}`, encodeURIComponent(String(requestParameters["draftId"])));
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async getDraftRaw(requestParameters, initOverrides) {
      const requestOptions = await this.getDraftRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => DraftResponseFromJSON(jsonValue));
    }
    /**
     */
    async getDraft(requestParameters, initOverrides) {
      const response = await this.getDraftRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for pay without sending the request
     */
    async payRequestOpts(requestParameters) {
      if (requestParameters["draftId"] == null) {
        throw new RequiredError(
          "draftId",
          'Required parameter "draftId" was null or undefined when calling pay().'
        );
      }
      if (requestParameters["payRequest"] == null) {
        throw new RequiredError(
          "payRequest",
          'Required parameter "payRequest" was null or undefined when calling pay().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (requestParameters["idempotencyKey"] != null) {
        headerParameters["Idempotency-Key"] = String(requestParameters["idempotencyKey"]);
      }
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/checkout/drafts/{draftId}/pay`;
      urlPath = urlPath.replace(`{${"draftId"}}`, encodeURIComponent(String(requestParameters["draftId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: PayRequestToJSON(requestParameters["payRequest"])
      };
    }
    /**
     * Creates an order in PENDING_PAYMENT, snapshots shipping address and creates payment intent. Supports Idempotency-Key header.
     * Start payment for a draft
     */
    async payRaw(requestParameters, initOverrides) {
      const requestOptions = await this.payRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => StartPaymentResponseFromJSON(jsonValue));
    }
    /**
     * Creates an order in PENDING_PAYMENT, snapshots shipping address and creates payment intent. Supports Idempotency-Key header.
     * Start payment for a draft
     */
    async pay(requestParameters, initOverrides) {
      const response = await this.payRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for reserve1 without sending the request
     */
    async reserve1RequestOpts(requestParameters) {
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/checkout/reserve`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: ReserveRequestToJSON(requestParameters["reserveRequest"])
      };
    }
    /**
     */
    async reserve1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.reserve1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => DraftResponseFromJSON(jsonValue));
    }
    /**
     */
    async reserve1(requestParameters = {}, initOverrides) {
      const response = await this.reserve1Raw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for submit without sending the request
     */
    async submitRequestOpts(requestParameters) {
      if (requestParameters["draftId"] == null) {
        throw new RequiredError(
          "draftId",
          'Required parameter "draftId" was null or undefined when calling submit().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/checkout/drafts/{draftId}/submit`;
      urlPath = urlPath.replace(`{${"draftId"}}`, encodeURIComponent(String(requestParameters["draftId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async submitRaw(requestParameters, initOverrides) {
      const requestOptions = await this.submitRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => SubmitResponseFromJSON(jsonValue));
    }
    /**
     */
    async submit(requestParameters, initOverrides) {
      const response = await this.submitRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/InventoryApi.ts
  var InventoryApi = class extends BaseAPI {
    /**
     * Creates request options for consume without sending the request
     */
    async consumeRequestOpts(requestParameters) {
      if (requestParameters["reservationId"] == null) {
        throw new RequiredError(
          "reservationId",
          'Required parameter "reservationId" was null or undefined when calling consume().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/inventory/reservations/{reservationId}/consume`;
      urlPath = urlPath.replace(`{${"reservationId"}}`, encodeURIComponent(String(requestParameters["reservationId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async consumeRaw(requestParameters, initOverrides) {
      const requestOptions = await this.consumeRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async consume(requestParameters, initOverrides) {
      await this.consumeRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for release without sending the request
     */
    async releaseRequestOpts(requestParameters) {
      if (requestParameters["reservationId"] == null) {
        throw new RequiredError(
          "reservationId",
          'Required parameter "reservationId" was null or undefined when calling release().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/inventory/reservations/{reservationId}/release`;
      urlPath = urlPath.replace(`{${"reservationId"}}`, encodeURIComponent(String(requestParameters["reservationId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async releaseRaw(requestParameters, initOverrides) {
      const requestOptions = await this.releaseRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async release(requestParameters, initOverrides) {
      await this.releaseRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for reserve without sending the request
     */
    async reserveRequestOpts(requestParameters) {
      if (requestParameters["reserveRequest"] == null) {
        throw new RequiredError(
          "reserveRequest",
          'Required parameter "reserveRequest" was null or undefined when calling reserve().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/inventory/reserve`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: ReserveRequestToJSON(requestParameters["reserveRequest"])
      };
    }
    /**
     */
    async reserveRaw(requestParameters, initOverrides) {
      const requestOptions = await this.reserveRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ReservationResponseFromJSON(jsonValue));
    }
    /**
     */
    async reserve(requestParameters, initOverrides) {
      const response = await this.reserveRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/OrdersApi.ts
  var OrdersApi = class extends BaseAPI {
    /**
     * Creates request options for get2 without sending the request
     */
    async get2RequestOpts(requestParameters) {
      if (requestParameters["orderId"] == null) {
        throw new RequiredError(
          "orderId",
          'Required parameter "orderId" was null or undefined when calling get2().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/orders/{orderId}`;
      urlPath = urlPath.replace(`{${"orderId"}}`, encodeURIComponent(String(requestParameters["orderId"])));
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     * Get my order details
     */
    async get2Raw(requestParameters, initOverrides) {
      const requestOptions = await this.get2RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => OrderDetailResponseFromJSON(jsonValue));
    }
    /**
     * Get my order details
     */
    async get2(requestParameters, initOverrides) {
      const response = await this.get2Raw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for list5 without sending the request
     */
    async list5RequestOpts(requestParameters) {
      const queryParameters = {};
      if (requestParameters["page"] != null) {
        queryParameters["page"] = requestParameters["page"];
      }
      if (requestParameters["size"] != null) {
        queryParameters["size"] = requestParameters["size"];
      }
      if (requestParameters["sort"] != null) {
        queryParameters["sort"] = requestParameters["sort"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/orders`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     * Returns the authenticated user\'s orders (newest first).
     * List my orders
     */
    async list5Raw(requestParameters, initOverrides) {
      const requestOptions = await this.list5RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => PageResponseFromJSON(jsonValue));
    }
    /**
     * Returns the authenticated user\'s orders (newest first).
     * List my orders
     */
    async list5(requestParameters = {}, initOverrides) {
      const response = await this.list5Raw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for refund without sending the request
     */
    async refundRequestOpts(requestParameters) {
      if (requestParameters["orderId"] == null) {
        throw new RequiredError(
          "orderId",
          'Required parameter "orderId" was null or undefined when calling refund().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/orders/{orderId}/refund`;
      urlPath = urlPath.replace(`{${"orderId"}}`, encodeURIComponent(String(requestParameters["orderId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: RefundRequestToJSON(requestParameters["refundRequest"])
      };
    }
    /**
     */
    async refundRaw(requestParameters, initOverrides) {
      const requestOptions = await this.refundRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => RefundResponseFromJSON(jsonValue));
    }
    /**
     */
    async refund(requestParameters, initOverrides) {
      const response = await this.refundRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/PaymentsApi.ts
  var PaymentsApi = class extends BaseAPI {
    /**
     * Creates request options for confirmMock without sending the request
     */
    async confirmMockRequestOpts(requestParameters) {
      if (requestParameters["paymentId"] == null) {
        throw new RequiredError(
          "paymentId",
          'Required parameter "paymentId" was null or undefined when calling confirmMock().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/payments/{paymentId}/confirm-mock`;
      urlPath = urlPath.replace(`{${"paymentId"}}`, encodeURIComponent(String(requestParameters["paymentId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async confirmMockRaw(requestParameters, initOverrides) {
      const requestOptions = await this.confirmMockRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async confirmMock(requestParameters, initOverrides) {
      await this.confirmMockRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for failMock without sending the request
     */
    async failMockRequestOpts(requestParameters) {
      if (requestParameters["paymentId"] == null) {
        throw new RequiredError(
          "paymentId",
          'Required parameter "paymentId" was null or undefined when calling failMock().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/payments/{paymentId}/fail-mock`;
      urlPath = urlPath.replace(`{${"paymentId"}}`, encodeURIComponent(String(requestParameters["paymentId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: FailRequestToJSON(requestParameters["failRequest"])
      };
    }
    /**
     */
    async failMockRaw(requestParameters, initOverrides) {
      const requestOptions = await this.failMockRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async failMock(requestParameters, initOverrides) {
      await this.failMockRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for get1 without sending the request
     */
    async get1RequestOpts(requestParameters) {
      if (requestParameters["paymentId"] == null) {
        throw new RequiredError(
          "paymentId",
          'Required parameter "paymentId" was null or undefined when calling get1().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/payments/{paymentId}`;
      urlPath = urlPath.replace(`{${"paymentId"}}`, encodeURIComponent(String(requestParameters["paymentId"])));
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async get1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.get1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => PaymentResponseFromJSON(jsonValue));
    }
    /**
     */
    async get1(requestParameters, initOverrides) {
      const response = await this.get1Raw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/ProductImagesApi.ts
  var ProductImagesApi = class extends BaseAPI {
    /**
     * Creates request options for add without sending the request
     */
    async addRequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError(
          "productId",
          'Required parameter "productId" was null or undefined when calling add().'
        );
      }
      if (requestParameters["addImageRequest"] == null) {
        throw new RequiredError(
          "addImageRequest",
          'Required parameter "addImageRequest" was null or undefined when calling add().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{productId}/images`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: AddImageRequestToJSON(requestParameters["addImageRequest"])
      };
    }
    /**
     */
    async addRaw(requestParameters, initOverrides) {
      const requestOptions = await this.addRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ProductResponseFromJSON(jsonValue));
    }
    /**
     */
    async add(requestParameters, initOverrides) {
      const response = await this.addRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for delete3 without sending the request
     */
    async delete3RequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError(
          "productId",
          'Required parameter "productId" was null or undefined when calling delete3().'
        );
      }
      if (requestParameters["imageId"] == null) {
        throw new RequiredError(
          "imageId",
          'Required parameter "imageId" was null or undefined when calling delete3().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{productId}/images/{imageId}`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      urlPath = urlPath.replace(`{${"imageId"}}`, encodeURIComponent(String(requestParameters["imageId"])));
      return {
        path: urlPath,
        method: "DELETE",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async delete3Raw(requestParameters, initOverrides) {
      const requestOptions = await this.delete3RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async delete3(requestParameters, initOverrides) {
      await this.delete3Raw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for list1 without sending the request
     */
    async list1RequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError(
          "productId",
          'Required parameter "productId" was null or undefined when calling list1().'
        );
      }
      const queryParameters = {};
      if (requestParameters["page"] != null) {
        queryParameters["page"] = requestParameters["page"];
      }
      if (requestParameters["size"] != null) {
        queryParameters["size"] = requestParameters["size"];
      }
      if (requestParameters["sort"] != null) {
        queryParameters["sort"] = requestParameters["sort"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{productId}/images`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async list1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.list1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => PageResponseFromJSON(jsonValue));
    }
    /**
     */
    async list1(requestParameters, initOverrides) {
      const response = await this.list1Raw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for reorder without sending the request
     */
    async reorderRequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError(
          "productId",
          'Required parameter "productId" was null or undefined when calling reorder().'
        );
      }
      if (requestParameters["reorderRequest"] == null) {
        throw new RequiredError(
          "reorderRequest",
          'Required parameter "reorderRequest" was null or undefined when calling reorder().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{productId}/images/reorder`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "PUT",
        headers: headerParameters,
        query: queryParameters,
        body: ReorderRequestToJSON(requestParameters["reorderRequest"])
      };
    }
    /**
     */
    async reorderRaw(requestParameters, initOverrides) {
      const requestOptions = await this.reorderRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ProductResponseFromJSON(jsonValue));
    }
    /**
     */
    async reorder(requestParameters, initOverrides) {
      const response = await this.reorderRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for upload without sending the request
     */
    async uploadRequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError(
          "productId",
          'Required parameter "productId" was null or undefined when calling upload().'
        );
      }
      if (requestParameters["files"] == null) {
        throw new RequiredError(
          "files",
          'Required parameter "files" was null or undefined when calling upload().'
        );
      }
      const queryParameters = {};
      if (requestParameters["altText"] != null) {
        queryParameters["altText"] = requestParameters["altText"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      const consumes = [
        { contentType: "multipart/form-data" }
      ];
      const canConsumeForm2 = canConsumeForm(consumes);
      let formParams;
      let useForm = false;
      useForm = canConsumeForm2;
      if (useForm) {
        formParams = new FormData();
      } else {
        formParams = new URLSearchParams();
      }
      if (requestParameters["files"] != null) {
        requestParameters["files"].forEach((element) => {
          formParams.append("files", element);
        });
      }
      let urlPath = `/api/v1/products/{productId}/images/upload`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: formParams
      };
    }
    /**
     */
    async uploadRaw(requestParameters, initOverrides) {
      const requestOptions = await this.uploadRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ProductResponseFromJSON(jsonValue));
    }
    /**
     */
    async upload(requestParameters, initOverrides) {
      const response = await this.uploadRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/ProductsApi.ts
  var ProductsApi = class extends BaseAPI {
    /**
     * Creates request options for _delete without sending the request
     */
    async _deleteRequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling _delete().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "DELETE",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async _deleteRaw(requestParameters, initOverrides) {
      const requestOptions = await this._deleteRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async _delete(requestParameters, initOverrides) {
      await this._deleteRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for create without sending the request
     */
    async createRequestOpts(requestParameters) {
      if (requestParameters["createProductRequest"] == null) {
        throw new RequiredError(
          "createProductRequest",
          'Required parameter "createProductRequest" was null or undefined when calling create().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: CreateProductRequestToJSON(requestParameters["createProductRequest"])
      };
    }
    /**
     */
    async createRaw(requestParameters, initOverrides) {
      const requestOptions = await this.createRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ProductResponseFromJSON(jsonValue));
    }
    /**
     */
    async create(requestParameters, initOverrides) {
      const response = await this.createRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for get without sending the request
     */
    async getRequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling get().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async getRaw(requestParameters, initOverrides) {
      const requestOptions = await this.getRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ProductResponseFromJSON(jsonValue));
    }
    /**
     */
    async get(requestParameters, initOverrides) {
      const response = await this.getRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for list without sending the request
     */
    async listRequestOpts(requestParameters) {
      const queryParameters = {};
      if (requestParameters["q"] != null) {
        queryParameters["q"] = requestParameters["q"];
      }
      if (requestParameters["category"] != null) {
        queryParameters["category"] = requestParameters["category"];
      }
      if (requestParameters["page"] != null) {
        queryParameters["page"] = requestParameters["page"];
      }
      if (requestParameters["size"] != null) {
        queryParameters["size"] = requestParameters["size"];
      }
      if (requestParameters["sort"] != null) {
        queryParameters["sort"] = requestParameters["sort"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async listRaw(requestParameters, initOverrides) {
      const requestOptions = await this.listRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response);
    }
    /**
     */
    async list(requestParameters = {}, initOverrides) {
      const response = await this.listRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for update without sending the request
     */
    async updateRequestOpts(requestParameters) {
      if (requestParameters["id"] == null) {
        throw new RequiredError(
          "id",
          'Required parameter "id" was null or undefined when calling update().'
        );
      }
      if (requestParameters["updateProductRequest"] == null) {
        throw new RequiredError(
          "updateProductRequest",
          'Required parameter "updateProductRequest" was null or undefined when calling update().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/products/{id}`;
      urlPath = urlPath.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters["id"])));
      return {
        path: urlPath,
        method: "PUT",
        headers: headerParameters,
        query: queryParameters,
        body: UpdateProductRequestToJSON(requestParameters["updateProductRequest"])
      };
    }
    /**
     */
    async updateRaw(requestParameters, initOverrides) {
      const requestOptions = await this.updateRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => ProductResponseFromJSON(jsonValue));
    }
    /**
     */
    async update(requestParameters, initOverrides) {
      const response = await this.updateRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/ProfileApi.ts
  var ProfileApi = class extends BaseAPI {
    /**
     * Creates request options for changePassword without sending the request
     */
    async changePasswordRequestOpts(requestParameters) {
      if (requestParameters["changePasswordRequest"] == null) {
        throw new RequiredError(
          "changePasswordRequest",
          'Required parameter "changePasswordRequest" was null or undefined when calling changePassword().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/me/password`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: ChangePasswordRequestToJSON(requestParameters["changePasswordRequest"])
      };
    }
    /**
     */
    async changePasswordRaw(requestParameters, initOverrides) {
      const requestOptions = await this.changePasswordRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async changePassword(requestParameters, initOverrides) {
      await this.changePasswordRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for me without sending the request
     */
    async meRequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/me`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async meRaw(initOverrides) {
      const requestOptions = await this.meRequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => MeResponseFromJSON(jsonValue));
    }
    /**
     */
    async me(initOverrides) {
      const response = await this.meRaw(initOverrides);
      return await response.value();
    }
  };

  // clients/ts-public/src/apis/SessionsApi.ts
  var SessionsApi = class extends BaseAPI {
    /**
     * Creates request options for list4 without sending the request
     */
    async list4RequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/sessions`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async list4Raw(initOverrides) {
      const requestOptions = await this.list4RequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse(response, (jsonValue) => jsonValue.map(SessionDtoFromJSON));
    }
    /**
     */
    async list4(initOverrides) {
      const response = await this.list4Raw(initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for rename without sending the request
     */
    async renameRequestOpts(requestParameters) {
      if (requestParameters["sessionId"] == null) {
        throw new RequiredError(
          "sessionId",
          'Required parameter "sessionId" was null or undefined when calling rename().'
        );
      }
      if (requestParameters["renameRequest"] == null) {
        throw new RequiredError(
          "renameRequest",
          'Required parameter "renameRequest" was null or undefined when calling rename().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/sessions/{sessionId}`;
      urlPath = urlPath.replace(`{${"sessionId"}}`, encodeURIComponent(String(requestParameters["sessionId"])));
      return {
        path: urlPath,
        method: "PATCH",
        headers: headerParameters,
        query: queryParameters,
        body: RenameRequestToJSON(requestParameters["renameRequest"])
      };
    }
    /**
     */
    async renameRaw(requestParameters, initOverrides) {
      const requestOptions = await this.renameRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async rename(requestParameters, initOverrides) {
      await this.renameRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for revokeAll without sending the request
     */
    async revokeAllRequestOpts() {
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/sessions/revoke-all`;
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async revokeAllRaw(initOverrides) {
      const requestOptions = await this.revokeAllRequestOpts();
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async revokeAll(initOverrides) {
      await this.revokeAllRaw(initOverrides);
    }
    /**
     * Creates request options for revokeOne without sending the request
     */
    async revokeOneRequestOpts(requestParameters) {
      if (requestParameters["sessionId"] == null) {
        throw new RequiredError(
          "sessionId",
          'Required parameter "sessionId" was null or undefined when calling revokeOne().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/sessions/{sessionId}/revoke`;
      urlPath = urlPath.replace(`{${"sessionId"}}`, encodeURIComponent(String(requestParameters["sessionId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async revokeOneRaw(requestParameters, initOverrides) {
      const requestOptions = await this.revokeOneRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse(response);
    }
    /**
     */
    async revokeOne(requestParameters, initOverrides) {
      await this.revokeOneRaw(requestParameters, initOverrides);
    }
  };

  // clients/ts-admin/src/runtime.ts
  var BASE_PATH2 = "http://localhost".replace(/\/+$/, "");
  var Configuration2 = class {
    constructor(configuration = {}) {
      this.configuration = configuration;
    }
    set config(configuration) {
      this.configuration = configuration;
    }
    get basePath() {
      return this.configuration.basePath != null ? this.configuration.basePath : BASE_PATH2;
    }
    get fetchApi() {
      return this.configuration.fetchApi;
    }
    get middleware() {
      return this.configuration.middleware || [];
    }
    get queryParamsStringify() {
      return this.configuration.queryParamsStringify || querystring2;
    }
    get username() {
      return this.configuration.username;
    }
    get password() {
      return this.configuration.password;
    }
    get apiKey() {
      const apiKey = this.configuration.apiKey;
      if (apiKey) {
        return typeof apiKey === "function" ? apiKey : () => apiKey;
      }
      return void 0;
    }
    get accessToken() {
      const accessToken = this.configuration.accessToken;
      if (accessToken) {
        return typeof accessToken === "function" ? accessToken : async () => accessToken;
      }
      return void 0;
    }
    get headers() {
      return this.configuration.headers;
    }
    get credentials() {
      return this.configuration.credentials;
    }
  };
  var DefaultConfig2 = new Configuration2();
  var _BaseAPI2 = class _BaseAPI2 {
    constructor(configuration = DefaultConfig2) {
      this.configuration = configuration;
      this.fetchApi = async (url, init) => {
        let fetchParams = { url, init };
        for (const middleware of this.middleware) {
          if (middleware.pre) {
            fetchParams = await middleware.pre({
              fetch: this.fetchApi,
              ...fetchParams
            }) || fetchParams;
          }
        }
        let response = void 0;
        try {
          response = await (this.configuration.fetchApi || fetch)(fetchParams.url, fetchParams.init);
        } catch (e) {
          for (const middleware of this.middleware) {
            if (middleware.onError) {
              response = await middleware.onError({
                fetch: this.fetchApi,
                url: fetchParams.url,
                init: fetchParams.init,
                error: e,
                response: response ? response.clone() : void 0
              }) || response;
            }
          }
          if (response === void 0) {
            if (e instanceof Error) {
              throw new FetchError2(e, "The request failed and the interceptors did not return an alternative response");
            } else {
              throw e;
            }
          }
        }
        for (const middleware of this.middleware) {
          if (middleware.post) {
            response = await middleware.post({
              fetch: this.fetchApi,
              url: fetchParams.url,
              init: fetchParams.init,
              response: response.clone()
            }) || response;
          }
        }
        return response;
      };
      this.middleware = configuration.middleware;
    }
    withMiddleware(...middlewares) {
      const next = this.clone();
      next.middleware = next.middleware.concat(...middlewares);
      return next;
    }
    withPreMiddleware(...preMiddlewares) {
      const middlewares = preMiddlewares.map((pre) => ({ pre }));
      return this.withMiddleware(...middlewares);
    }
    withPostMiddleware(...postMiddlewares) {
      const middlewares = postMiddlewares.map((post) => ({ post }));
      return this.withMiddleware(...middlewares);
    }
    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     *   application/json
     *   application/json; charset=UTF8
     *   APPLICATION/JSON
     *   application/vnd.company+json
     * @param mime - MIME (Multipurpose Internet Mail Extensions)
     * @return True if the given MIME is JSON, false otherwise.
     */
    isJsonMime(mime) {
      if (!mime) {
        return false;
      }
      return _BaseAPI2.jsonRegex.test(mime);
    }
    async request(context, initOverrides) {
      const { url, init } = await this.createFetchParams(context, initOverrides);
      const response = await this.fetchApi(url, init);
      if (response && (response.status >= 200 && response.status < 300)) {
        return response;
      }
      throw new ResponseError2(response, "Response returned an error code");
    }
    async createFetchParams(context, initOverrides) {
      let url = this.configuration.basePath + context.path;
      if (context.query !== void 0 && Object.keys(context.query).length !== 0) {
        url += "?" + this.configuration.queryParamsStringify(context.query);
      }
      const headers = Object.assign({}, this.configuration.headers, context.headers);
      Object.keys(headers).forEach((key) => headers[key] === void 0 ? delete headers[key] : {});
      const initOverrideFn = typeof initOverrides === "function" ? initOverrides : async () => initOverrides;
      const initParams = {
        method: context.method,
        headers,
        body: context.body,
        credentials: this.configuration.credentials
      };
      const overriddenInit = {
        ...initParams,
        ...await initOverrideFn({
          init: initParams,
          context
        })
      };
      let body;
      if (isFormData2(overriddenInit.body) || overriddenInit.body instanceof URLSearchParams || isBlob2(overriddenInit.body)) {
        body = overriddenInit.body;
      } else if (this.isJsonMime(headers["Content-Type"])) {
        body = JSON.stringify(overriddenInit.body);
      } else {
        body = overriddenInit.body;
      }
      const init = {
        ...overriddenInit,
        body
      };
      return { url, init };
    }
    /**
     * Create a shallow clone of `this` by constructing a new instance
     * and then shallow cloning data members.
     */
    clone() {
      const constructor = this.constructor;
      const next = new constructor(this.configuration);
      next.middleware = this.middleware.slice();
      return next;
    }
  };
  _BaseAPI2.jsonRegex = new RegExp("^(:?application/json|[^;/ 	]+/[^;/ 	]+[+]json)[ 	]*(:?;.*)?$", "i");
  var BaseAPI2 = _BaseAPI2;
  function isBlob2(value) {
    return typeof Blob !== "undefined" && value instanceof Blob;
  }
  function isFormData2(value) {
    return typeof FormData !== "undefined" && value instanceof FormData;
  }
  var ResponseError2 = class extends Error {
    constructor(response, msg) {
      super(msg);
      this.response = response;
      this.name = "ResponseError";
    }
  };
  var FetchError2 = class extends Error {
    constructor(cause, msg) {
      super(msg);
      this.cause = cause;
      this.name = "FetchError";
    }
  };
  var RequiredError2 = class extends Error {
    constructor(field, msg) {
      super(msg);
      this.field = field;
      this.name = "RequiredError";
    }
  };
  function querystring2(params, prefix = "") {
    return Object.keys(params).map((key) => querystringSingleKey2(key, params[key], prefix)).filter((part) => part.length > 0).join("&");
  }
  function querystringSingleKey2(key, value, keyPrefix = "") {
    const fullKey = keyPrefix + (keyPrefix.length ? `[${key}]` : key);
    if (value instanceof Array) {
      const multiValue = value.map((singleValue) => encodeURIComponent(String(singleValue))).join(`&${encodeURIComponent(fullKey)}=`);
      return `${encodeURIComponent(fullKey)}=${multiValue}`;
    }
    if (value instanceof Set) {
      const valueAsArray = Array.from(value);
      return querystringSingleKey2(key, valueAsArray, keyPrefix);
    }
    if (value instanceof Date) {
      return `${encodeURIComponent(fullKey)}=${encodeURIComponent(value.toISOString())}`;
    }
    if (value instanceof Object) {
      return querystring2(value, fullKey);
    }
    return `${encodeURIComponent(fullKey)}=${encodeURIComponent(String(value))}`;
  }
  var JSONApiResponse2 = class {
    constructor(raw, transformer = (jsonValue) => jsonValue) {
      this.raw = raw;
      this.transformer = transformer;
    }
    async value() {
      return this.transformer(await this.raw.json());
    }
  };
  var VoidApiResponse2 = class {
    constructor(raw) {
      this.raw = raw;
    }
    async value() {
      return void 0;
    }
  };

  // clients/ts-admin/src/models/AdjustRequest.ts
  function AdjustRequestToJSON(json) {
    return AdjustRequestToJSONTyped(json, false);
  }
  function AdjustRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "delta": value["delta"],
      "reason": value["reason"]
    };
  }

  // clients/ts-admin/src/models/OrderItemResponse.ts
  function OrderItemResponseFromJSON2(json) {
    return OrderItemResponseFromJSONTyped3(json, false);
  }
  function OrderItemResponseFromJSONTyped3(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "lineTotal": json["lineTotal"] == null ? void 0 : json["lineTotal"],
      "name": json["name"] == null ? void 0 : json["name"],
      "productId": json["productId"] == null ? void 0 : json["productId"],
      "quantity": json["quantity"] == null ? void 0 : json["quantity"],
      "sku": json["sku"] == null ? void 0 : json["sku"],
      "unitPrice": json["unitPrice"] == null ? void 0 : json["unitPrice"]
    };
  }

  // clients/ts-admin/src/models/ShipmentInfo.ts
  function ShipmentInfoFromJSON2(json) {
    return ShipmentInfoFromJSONTyped3(json, false);
  }
  function ShipmentInfoFromJSONTyped3(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "carrier": json["carrier"] == null ? void 0 : json["carrier"],
      "deliveredAt": json["deliveredAt"] == null ? void 0 : new Date(json["deliveredAt"]),
      "shipmentId": json["shipmentId"] == null ? void 0 : json["shipmentId"],
      "shippedAt": json["shippedAt"] == null ? void 0 : new Date(json["shippedAt"]),
      "status": json["status"] == null ? void 0 : json["status"],
      "trackingNumber": json["trackingNumber"] == null ? void 0 : json["trackingNumber"]
    };
  }

  // clients/ts-admin/src/models/ShippingAddressInfo.ts
  function ShippingAddressInfoFromJSON2(json) {
    return ShippingAddressInfoFromJSONTyped3(json, false);
  }
  function ShippingAddressInfoFromJSONTyped3(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "city": json["city"] == null ? void 0 : json["city"],
      "countryCode": json["countryCode"] == null ? void 0 : json["countryCode"],
      "fullName": json["fullName"] == null ? void 0 : json["fullName"],
      "line1": json["line1"] == null ? void 0 : json["line1"],
      "line2": json["line2"] == null ? void 0 : json["line2"],
      "phone": json["phone"] == null ? void 0 : json["phone"],
      "postalCode": json["postalCode"] == null ? void 0 : json["postalCode"],
      "state": json["state"] == null ? void 0 : json["state"]
    };
  }

  // clients/ts-admin/src/models/AdminOrderDetail.ts
  function AdminOrderDetailFromJSON(json) {
    return AdminOrderDetailFromJSONTyped(json, false);
  }
  function AdminOrderDetailFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "createdAt": json["createdAt"] == null ? void 0 : new Date(json["createdAt"]),
      "currency": json["currency"] == null ? void 0 : json["currency"],
      "items": json["items"] == null ? void 0 : json["items"].map(OrderItemResponseFromJSON2),
      "orderId": json["orderId"] == null ? void 0 : json["orderId"],
      "shipments": json["shipments"] == null ? void 0 : json["shipments"].map(ShipmentInfoFromJSON2),
      "shippingAddress": json["shippingAddress"] == null ? void 0 : ShippingAddressInfoFromJSON2(json["shippingAddress"]),
      "status": json["status"] == null ? void 0 : json["status"],
      "totalAmount": json["totalAmount"] == null ? void 0 : json["totalAmount"],
      "userEmail": json["userEmail"] == null ? void 0 : json["userEmail"]
    };
  }

  // clients/ts-admin/src/models/CancelRequest.ts
  function CancelRequestToJSON(json) {
    return CancelRequestToJSONTyped(json, false);
  }
  function CancelRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "reason": value["reason"]
    };
  }

  // clients/ts-admin/src/models/PageMeta.ts
  function PageMetaFromJSON2(json) {
    return PageMetaFromJSONTyped3(json, false);
  }
  function PageMetaFromJSONTyped3(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "first": json["first"] == null ? void 0 : json["first"],
      "last": json["last"] == null ? void 0 : json["last"],
      "number": json["number"] == null ? void 0 : json["number"],
      "size": json["size"] == null ? void 0 : json["size"],
      "totalElements": json["totalElements"] == null ? void 0 : json["totalElements"],
      "totalPages": json["totalPages"] == null ? void 0 : json["totalPages"]
    };
  }

  // clients/ts-admin/src/models/WebhookEventRow.ts
  function WebhookEventRowFromJSON(json) {
    return WebhookEventRowFromJSONTyped(json, false);
  }
  function WebhookEventRowFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "eventId": json["eventId"] == null ? void 0 : json["eventId"],
      "payloadHash": json["payloadHash"] == null ? void 0 : json["payloadHash"],
      "provider": json["provider"] == null ? void 0 : json["provider"],
      "receivedAt": json["receivedAt"] == null ? void 0 : new Date(json["receivedAt"])
    };
  }

  // clients/ts-admin/src/models/PageResponse.ts
  function PageResponseFromJSON2(json) {
    return PageResponseFromJSONTyped2(json, false);
  }
  function PageResponseFromJSONTyped2(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "items": json["items"] == null ? void 0 : json["items"].map(WebhookEventRowFromJSON),
      "page": json["page"] == null ? void 0 : PageMetaFromJSON2(json["page"])
    };
  }

  // clients/ts-admin/src/models/RestockRequest.ts
  function RestockRequestToJSON(json) {
    return RestockRequestToJSONTyped(json, false);
  }
  function RestockRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "quantity": value["quantity"],
      "reason": value["reason"]
    };
  }

  // clients/ts-admin/src/models/ShipRequest.ts
  function ShipRequestToJSON(json) {
    return ShipRequestToJSONTyped(json, false);
  }
  function ShipRequestToJSONTyped(value, ignoreDiscriminator = false) {
    if (value == null) {
      return value;
    }
    return {
      "carrier": value["carrier"],
      "trackingNumber": value["trackingNumber"]
    };
  }

  // clients/ts-admin/src/models/ShipmentResponse.ts
  function ShipmentResponseFromJSON(json) {
    return ShipmentResponseFromJSONTyped(json, false);
  }
  function ShipmentResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "carrier": json["carrier"] == null ? void 0 : json["carrier"],
      "deliveredAt": json["deliveredAt"] == null ? void 0 : new Date(json["deliveredAt"]),
      "shipmentId": json["shipmentId"] == null ? void 0 : json["shipmentId"],
      "shippedAt": json["shippedAt"] == null ? void 0 : new Date(json["shippedAt"]),
      "status": json["status"] == null ? void 0 : json["status"],
      "trackingNumber": json["trackingNumber"] == null ? void 0 : json["trackingNumber"]
    };
  }

  // clients/ts-admin/src/models/StockResponse.ts
  function StockResponseFromJSON(json) {
    return StockResponseFromJSONTyped(json, false);
  }
  function StockResponseFromJSONTyped(json, ignoreDiscriminator) {
    if (json == null) {
      return json;
    }
    return {
      "stockQuantity": json["stockQuantity"] == null ? void 0 : json["stockQuantity"]
    };
  }

  // clients/ts-admin/src/apis/AdminFulfillmentApi.ts
  var AdminFulfillmentApi = class extends BaseAPI2 {
    /**
     * Creates request options for cancel without sending the request
     */
    async cancelRequestOpts(requestParameters) {
      if (requestParameters["orderId"] == null) {
        throw new RequiredError2(
          "orderId",
          'Required parameter "orderId" was null or undefined when calling cancel().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/orders/{orderId}/cancel`;
      urlPath = urlPath.replace(`{${"orderId"}}`, encodeURIComponent(String(requestParameters["orderId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: CancelRequestToJSON(requestParameters["cancelRequest"])
      };
    }
    /**
     */
    async cancelRaw(requestParameters, initOverrides) {
      const requestOptions = await this.cancelRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse2(response);
    }
    /**
     */
    async cancel(requestParameters, initOverrides) {
      await this.cancelRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for deliver without sending the request
     */
    async deliverRequestOpts(requestParameters) {
      if (requestParameters["orderId"] == null) {
        throw new RequiredError2(
          "orderId",
          'Required parameter "orderId" was null or undefined when calling deliver().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/orders/{orderId}/deliver`;
      urlPath = urlPath.replace(`{${"orderId"}}`, encodeURIComponent(String(requestParameters["orderId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async deliverRaw(requestParameters, initOverrides) {
      const requestOptions = await this.deliverRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new VoidApiResponse2(response);
    }
    /**
     */
    async deliver(requestParameters, initOverrides) {
      await this.deliverRaw(requestParameters, initOverrides);
    }
    /**
     * Creates request options for ship without sending the request
     */
    async shipRequestOpts(requestParameters) {
      if (requestParameters["orderId"] == null) {
        throw new RequiredError2(
          "orderId",
          'Required parameter "orderId" was null or undefined when calling ship().'
        );
      }
      if (requestParameters["shipRequest"] == null) {
        throw new RequiredError2(
          "shipRequest",
          'Required parameter "shipRequest" was null or undefined when calling ship().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/orders/{orderId}/ship`;
      urlPath = urlPath.replace(`{${"orderId"}}`, encodeURIComponent(String(requestParameters["orderId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: ShipRequestToJSON(requestParameters["shipRequest"])
      };
    }
    /**
     */
    async shipRaw(requestParameters, initOverrides) {
      const requestOptions = await this.shipRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => ShipmentResponseFromJSON(jsonValue));
    }
    /**
     */
    async ship(requestParameters, initOverrides) {
      const response = await this.shipRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-admin/src/apis/AdminInventoryApi.ts
  var AdminInventoryApi = class extends BaseAPI2 {
    /**
     * Creates request options for adjust without sending the request
     */
    async adjustRequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError2(
          "productId",
          'Required parameter "productId" was null or undefined when calling adjust().'
        );
      }
      if (requestParameters["adjustRequest"] == null) {
        throw new RequiredError2(
          "adjustRequest",
          'Required parameter "adjustRequest" was null or undefined when calling adjust().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/inventory/products/{productId}/adjust`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: AdjustRequestToJSON(requestParameters["adjustRequest"])
      };
    }
    /**
     */
    async adjustRaw(requestParameters, initOverrides) {
      const requestOptions = await this.adjustRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => StockResponseFromJSON(jsonValue));
    }
    /**
     */
    async adjust(requestParameters, initOverrides) {
      const response = await this.adjustRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for restock without sending the request
     */
    async restockRequestOpts(requestParameters) {
      if (requestParameters["productId"] == null) {
        throw new RequiredError2(
          "productId",
          'Required parameter "productId" was null or undefined when calling restock().'
        );
      }
      if (requestParameters["restockRequest"] == null) {
        throw new RequiredError2(
          "restockRequest",
          'Required parameter "restockRequest" was null or undefined when calling restock().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      headerParameters["Content-Type"] = "application/json";
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/inventory/products/{productId}/restock`;
      urlPath = urlPath.replace(`{${"productId"}}`, encodeURIComponent(String(requestParameters["productId"])));
      return {
        path: urlPath,
        method: "POST",
        headers: headerParameters,
        query: queryParameters,
        body: RestockRequestToJSON(requestParameters["restockRequest"])
      };
    }
    /**
     */
    async restockRaw(requestParameters, initOverrides) {
      const requestOptions = await this.restockRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => StockResponseFromJSON(jsonValue));
    }
    /**
     */
    async restock(requestParameters, initOverrides) {
      const response = await this.restockRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-admin/src/apis/AdminOrdersApi.ts
  var AdminOrdersApi = class extends BaseAPI2 {
    /**
     * Creates request options for get without sending the request
     */
    async getRequestOpts(requestParameters) {
      if (requestParameters["orderId"] == null) {
        throw new RequiredError2(
          "orderId",
          'Required parameter "orderId" was null or undefined when calling get().'
        );
      }
      const queryParameters = {};
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/orders/{orderId}`;
      urlPath = urlPath.replace(`{${"orderId"}}`, encodeURIComponent(String(requestParameters["orderId"])));
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     */
    async getRaw(requestParameters, initOverrides) {
      const requestOptions = await this.getRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => AdminOrderDetailFromJSON(jsonValue));
    }
    /**
     */
    async get(requestParameters, initOverrides) {
      const response = await this.getRaw(requestParameters, initOverrides);
      return await response.value();
    }
    /**
     * Creates request options for list2 without sending the request
     */
    async list2RequestOpts(requestParameters) {
      const queryParameters = {};
      if (requestParameters["status"] != null) {
        queryParameters["status"] = requestParameters["status"];
      }
      if (requestParameters["email"] != null) {
        queryParameters["email"] = requestParameters["email"];
      }
      if (requestParameters["q"] != null) {
        queryParameters["q"] = requestParameters["q"];
      }
      if (requestParameters["from"] != null) {
        queryParameters["from"] = requestParameters["from"].toISOString();
      }
      if (requestParameters["to"] != null) {
        queryParameters["to"] = requestParameters["to"].toISOString();
      }
      if (requestParameters["page"] != null) {
        queryParameters["page"] = requestParameters["page"];
      }
      if (requestParameters["size"] != null) {
        queryParameters["size"] = requestParameters["size"];
      }
      if (requestParameters["sort"] != null) {
        queryParameters["sort"] = requestParameters["sort"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/orders`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     * Filter by status/email/orderId/date range.
     * Admin: list orders
     */
    async list2Raw(requestParameters, initOverrides) {
      const requestOptions = await this.list2RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => PageResponseFromJSON2(jsonValue));
    }
    /**
     * Filter by status/email/orderId/date range.
     * Admin: list orders
     */
    async list2(requestParameters = {}, initOverrides) {
      const response = await this.list2Raw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-admin/src/apis/AdminRefundsApi.ts
  var AdminRefundsApi = class extends BaseAPI2 {
    /**
     * Creates request options for list1 without sending the request
     */
    async list1RequestOpts(requestParameters) {
      const queryParameters = {};
      if (requestParameters["status"] != null) {
        queryParameters["status"] = requestParameters["status"];
      }
      if (requestParameters["email"] != null) {
        queryParameters["email"] = requestParameters["email"];
      }
      if (requestParameters["orderId"] != null) {
        queryParameters["orderId"] = requestParameters["orderId"];
      }
      if (requestParameters["page"] != null) {
        queryParameters["page"] = requestParameters["page"];
      }
      if (requestParameters["size"] != null) {
        queryParameters["size"] = requestParameters["size"];
      }
      if (requestParameters["sort"] != null) {
        queryParameters["sort"] = requestParameters["sort"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/refunds`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     * Filter by status, user email and order id.
     * Admin: list refunds
     */
    async list1Raw(requestParameters, initOverrides) {
      const requestOptions = await this.list1RequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => PageResponseFromJSON2(jsonValue));
    }
    /**
     * Filter by status, user email and order id.
     * Admin: list refunds
     */
    async list1(requestParameters = {}, initOverrides) {
      const response = await this.list1Raw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // clients/ts-admin/src/apis/AdminWebhookEventsApi.ts
  var AdminWebhookEventsApi = class extends BaseAPI2 {
    /**
     * Creates request options for list without sending the request
     */
    async listRequestOpts(requestParameters) {
      const queryParameters = {};
      if (requestParameters["provider"] != null) {
        queryParameters["provider"] = requestParameters["provider"];
      }
      if (requestParameters["eventId"] != null) {
        queryParameters["eventId"] = requestParameters["eventId"];
      }
      if (requestParameters["page"] != null) {
        queryParameters["page"] = requestParameters["page"];
      }
      if (requestParameters["size"] != null) {
        queryParameters["size"] = requestParameters["size"];
      }
      if (requestParameters["sort"] != null) {
        queryParameters["sort"] = requestParameters["sort"];
      }
      const headerParameters = {};
      if (this.configuration && this.configuration.accessToken) {
        const token = this.configuration.accessToken;
        const tokenString = await token("bearerAuth", []);
        if (tokenString) {
          headerParameters["Authorization"] = `Bearer ${tokenString}`;
        }
      }
      let urlPath = `/api/v1/admin/webhook-events`;
      return {
        path: urlPath,
        method: "GET",
        headers: headerParameters,
        query: queryParameters
      };
    }
    /**
     * Filter events by provider and event id.
     * Admin: list webhook events
     */
    async listRaw(requestParameters, initOverrides) {
      const requestOptions = await this.listRequestOpts(requestParameters);
      const response = await this.request(requestOptions, initOverrides);
      return new JSONApiResponse2(response, (jsonValue) => PageResponseFromJSON2(jsonValue));
    }
    /**
     * Filter events by provider and event id.
     * Admin: list webhook events
     */
    async list(requestParameters = {}, initOverrides) {
      const response = await this.listRaw(requestParameters, initOverrides);
      return await response.value();
    }
  };

  // src/js/api-client.ts
  var LS_TOKEN = "auth.token";
  function getToken() {
    return localStorage.getItem(LS_TOKEN) ?? "";
  }
  var publicConfig = new Configuration({
    basePath: "",
    accessToken: () => getToken()
  });
  var adminConfig = new Configuration2({
    basePath: "",
    accessToken: () => getToken()
  });
  var ApiClient = {
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
    adminWebhookEvents: new AdminWebhookEventsApi(adminConfig)
  };
  window.ApiClient = ApiClient;
  async function authedFetch(path) {
    const token = getToken();
    const res = await fetch(path, {
      headers: {
        Accept: "application/json",
        ...token ? { Authorization: `Bearer ${token}` } : {}
      }
    });
    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try {
        const p = await res.json();
        msg = p.detail || p.message || p.title || msg;
      } catch (_) {
      }
      throw new Error(msg);
    }
    return res.status === 204 ? null : res.json();
  }
  var DeptClient = {
    /** Departments the calling user belongs to */
    myDepartments: () => authedFetch("/api/v1/dept/me"),
    /** All departments (admin only) */
    listDepartments: () => authedFetch("/api/v1/dept/list"),
    /** Dashboard for a specific department slug */
    dashboard: (slug) => authedFetch(`/api/v1/dept/${slug}/dashboard`)
  };
  var TierClient = {
    /** Current user's tier info */
    myTier: () => authedFetch("/api/v1/tier/me"),
    /** Full benefit table */
    benefits: () => authedFetch("/api/v1/tier/benefits")
  };
  window.DeptClient = DeptClient;
  window.TierClient = TierClient;
  return __toCommonJS(api_client_exports);
})();
