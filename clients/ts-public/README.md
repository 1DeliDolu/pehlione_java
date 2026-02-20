# @pehlione/api-public@v1

A TypeScript SDK client for the localhost API.

## Usage

First, install the SDK from npm.

```bash
npm install @pehlione/api-public --save
```

Next, try it out.


```ts
import {
  Configuration,
  AddressesApi,
} from '@pehlione/api-public';
import type { Create2Request } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AddressesApi(config);

  const body = {
    // CreateAddressRequest
    createAddressRequest: ...,
  } satisfies Create2Request;

  try {
    const data = await api.create2(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```


## Documentation

### API Endpoints

All URIs are relative to *http://localhost*

| Class | Method | HTTP request | Description
| ----- | ------ | ------------ | -------------
*AddressesApi* | [**create2**](docs/AddressesApi.md#create2) | **POST** /api/v1/addresses | 
*AddressesApi* | [**delete2**](docs/AddressesApi.md#delete2) | **DELETE** /api/v1/addresses/{id} | 
*AddressesApi* | [**list3**](docs/AddressesApi.md#list3) | **GET** /api/v1/addresses | 
*AddressesApi* | [**setDefault**](docs/AddressesApi.md#setdefault) | **POST** /api/v1/addresses/{id}/default | 
*AddressesApi* | [**update2**](docs/AddressesApi.md#update2) | **PUT** /api/v1/addresses/{id} | 
*AuthApi* | [**login**](docs/AuthApi.md#loginoperation) | **POST** /api/v1/auth/login | 
*AuthApi* | [**logout**](docs/AuthApi.md#logout) | **POST** /api/v1/auth/logout | 
*AuthApi* | [**refresh**](docs/AuthApi.md#refresh) | **POST** /api/v1/auth/refresh | 
*AuthPasswordApi* | [**forgot**](docs/AuthPasswordApi.md#forgotoperation) | **POST** /api/v1/auth/password/forgot | 
*AuthPasswordApi* | [**reset**](docs/AuthPasswordApi.md#resetoperation) | **POST** /api/v1/auth/password/reset | 
*CartApi* | [**clear**](docs/CartApi.md#clear) | **DELETE** /api/v1/cart | 
*CartApi* | [**get3**](docs/CartApi.md#get3) | **GET** /api/v1/cart | 
*CartApi* | [**remove**](docs/CartApi.md#remove) | **DELETE** /api/v1/cart/items/{productId} | 
*CartApi* | [**upsert**](docs/CartApi.md#upsert) | **POST** /api/v1/cart/items | 
*CategoriesApi* | [**create1**](docs/CategoriesApi.md#create1) | **POST** /api/v1/categories | 
*CategoriesApi* | [**delete1**](docs/CategoriesApi.md#delete1) | **DELETE** /api/v1/categories/{id} | 
*CategoriesApi* | [**list2**](docs/CategoriesApi.md#list2) | **GET** /api/v1/categories | 
*CategoriesApi* | [**update1**](docs/CategoriesApi.md#update1) | **PUT** /api/v1/categories/{id} | 
*CheckoutApi* | [**cancel**](docs/CheckoutApi.md#cancel) | **POST** /api/v1/checkout/drafts/{draftId}/cancel | 
*CheckoutApi* | [**getDraft**](docs/CheckoutApi.md#getdraft) | **GET** /api/v1/checkout/drafts/{draftId} | 
*CheckoutApi* | [**pay**](docs/CheckoutApi.md#payoperation) | **POST** /api/v1/checkout/drafts/{draftId}/pay | Start payment for a draft
*CheckoutApi* | [**reserve1**](docs/CheckoutApi.md#reserve1) | **POST** /api/v1/checkout/reserve | 
*CheckoutApi* | [**submit**](docs/CheckoutApi.md#submit) | **POST** /api/v1/checkout/drafts/{draftId}/submit | 
*DeptInfoControllerApi* | [**listDepartments**](docs/DeptInfoControllerApi.md#listdepartments) | **GET** /api/v1/dept/list | 
*DeptInfoControllerApi* | [**myDepartments**](docs/DeptInfoControllerApi.md#mydepartments) | **GET** /api/v1/dept/me | 
*DeptInfoControllerApi* | [**myTier**](docs/DeptInfoControllerApi.md#mytier) | **GET** /api/v1/tier/me | 
*DeptInfoControllerApi* | [**tierBenefits**](docs/DeptInfoControllerApi.md#tierbenefits) | **GET** /api/v1/tier/benefits | 
*FinanceDeptControllerApi* | [**dashboard5**](docs/FinanceDeptControllerApi.md#dashboard5) | **GET** /api/v1/dept/finance/dashboard | 
*HomeControllerApi* | [**apiRoot**](docs/HomeControllerApi.md#apiroot) | **GET** /api/v1 | 
*HomeControllerApi* | [**apiRoot1**](docs/HomeControllerApi.md#apiroot1) | **POST** /api/v1 | 
*HomeControllerApi* | [**apiRoot2**](docs/HomeControllerApi.md#apiroot2) | **PUT** /api/v1 | 
*HomeControllerApi* | [**apiRoot3**](docs/HomeControllerApi.md#apiroot3) | **DELETE** /api/v1 | 
*HomeControllerApi* | [**apiRoot4**](docs/HomeControllerApi.md#apiroot4) | **PATCH** /api/v1 | 
*HomeControllerApi* | [**apiRoot5**](docs/HomeControllerApi.md#apiroot5) | **HEAD** /api/v1 | 
*HomeControllerApi* | [**apiRoot6**](docs/HomeControllerApi.md#apiroot6) | **OPTIONS** /api/v1 | 
*HomeControllerApi* | [**status**](docs/HomeControllerApi.md#status) | **GET** /api/v1/status | 
*HrDeptControllerApi* | [**dashboard4**](docs/HrDeptControllerApi.md#dashboard4) | **GET** /api/v1/dept/hr/dashboard | 
*InventoryApi* | [**consume**](docs/InventoryApi.md#consume) | **POST** /api/v1/inventory/reservations/{reservationId}/consume | 
*InventoryApi* | [**release**](docs/InventoryApi.md#release) | **POST** /api/v1/inventory/reservations/{reservationId}/release | 
*InventoryApi* | [**reserve**](docs/InventoryApi.md#reserveoperation) | **POST** /api/v1/inventory/reserve | 
*ItDeptControllerApi* | [**dashboard3**](docs/ItDeptControllerApi.md#dashboard3) | **GET** /api/v1/dept/it/dashboard | 
*MarketingDeptControllerApi* | [**dashboard2**](docs/MarketingDeptControllerApi.md#dashboard2) | **GET** /api/v1/dept/marketing/dashboard | 
*OrdersApi* | [**get2**](docs/OrdersApi.md#get2) | **GET** /api/v1/orders/{orderId} | Get my order details
*OrdersApi* | [**list5**](docs/OrdersApi.md#list5) | **GET** /api/v1/orders | List my orders
*OrdersApi* | [**refund**](docs/OrdersApi.md#refundoperation) | **POST** /api/v1/orders/{orderId}/refund | 
*PaymentsApi* | [**confirmMock**](docs/PaymentsApi.md#confirmmock) | **POST** /api/v1/payments/{paymentId}/confirm-mock | 
*PaymentsApi* | [**failMock**](docs/PaymentsApi.md#failmock) | **POST** /api/v1/payments/{paymentId}/fail-mock | 
*PaymentsApi* | [**get1**](docs/PaymentsApi.md#get1) | **GET** /api/v1/payments/{paymentId} | 
*ProcessDeptControllerApi* | [**dashboard1**](docs/ProcessDeptControllerApi.md#dashboard1) | **GET** /api/v1/dept/process/dashboard | 
*ProductImagesApi* | [**add**](docs/ProductImagesApi.md#add) | **POST** /api/v1/products/{productId}/images | 
*ProductImagesApi* | [**delete3**](docs/ProductImagesApi.md#delete3) | **DELETE** /api/v1/products/{productId}/images/{imageId} | 
*ProductImagesApi* | [**list1**](docs/ProductImagesApi.md#list1) | **GET** /api/v1/products/{productId}/images | 
*ProductImagesApi* | [**reorder**](docs/ProductImagesApi.md#reorderoperation) | **PUT** /api/v1/products/{productId}/images/reorder | 
*ProductImagesApi* | [**upload**](docs/ProductImagesApi.md#upload) | **POST** /api/v1/products/{productId}/images/upload | 
*ProductsApi* | [**_delete**](docs/ProductsApi.md#_delete) | **DELETE** /api/v1/products/{id} | 
*ProductsApi* | [**create**](docs/ProductsApi.md#create) | **POST** /api/v1/products | 
*ProductsApi* | [**get**](docs/ProductsApi.md#get) | **GET** /api/v1/products/{id} | 
*ProductsApi* | [**list**](docs/ProductsApi.md#list) | **GET** /api/v1/products | 
*ProductsApi* | [**update**](docs/ProductsApi.md#update) | **PUT** /api/v1/products/{id} | 
*ProfileApi* | [**changePassword**](docs/ProfileApi.md#changepasswordoperation) | **POST** /api/v1/me/password | 
*ProfileApi* | [**me**](docs/ProfileApi.md#me) | **GET** /api/v1/me | 
*SessionsApi* | [**list4**](docs/SessionsApi.md#list4) | **GET** /api/v1/sessions | 
*SessionsApi* | [**rename**](docs/SessionsApi.md#renameoperation) | **PATCH** /api/v1/sessions/{sessionId} | 
*SessionsApi* | [**revokeAll**](docs/SessionsApi.md#revokeall) | **POST** /api/v1/sessions/revoke-all | 
*SessionsApi* | [**revokeOne**](docs/SessionsApi.md#revokeone) | **POST** /api/v1/sessions/{sessionId}/revoke | 
*SupportDeptControllerApi* | [**dashboard**](docs/SupportDeptControllerApi.md#dashboard) | **GET** /api/v1/dept/support/dashboard | 


### Models

- [AddImageRequest](docs/AddImageRequest.md)
- [AddressResponse](docs/AddressResponse.md)
- [ApiProblem](docs/ApiProblem.md)
- [ApiProblemViolationsInner](docs/ApiProblemViolationsInner.md)
- [ApiRoot](docs/ApiRoot.md)
- [ApiStatus](docs/ApiStatus.md)
- [CartItemResponse](docs/CartItemResponse.md)
- [CartResponse](docs/CartResponse.md)
- [CategoryRef](docs/CategoryRef.md)
- [CategoryResponse](docs/CategoryResponse.md)
- [ChangePasswordRequest](docs/ChangePasswordRequest.md)
- [CreateAddressRequest](docs/CreateAddressRequest.md)
- [CreateCategoryRequest](docs/CreateCategoryRequest.md)
- [CreateProductRequest](docs/CreateProductRequest.md)
- [DraftItemResponse](docs/DraftItemResponse.md)
- [DraftResponse](docs/DraftResponse.md)
- [FailRequest](docs/FailRequest.md)
- [ForgotRequest](docs/ForgotRequest.md)
- [ImagePageItem](docs/ImagePageItem.md)
- [ImageRef](docs/ImageRef.md)
- [LoginRequest](docs/LoginRequest.md)
- [MeResponse](docs/MeResponse.md)
- [OrderDetailResponse](docs/OrderDetailResponse.md)
- [OrderItemResponse](docs/OrderItemResponse.md)
- [PageMeta](docs/PageMeta.md)
- [PageResponse](docs/PageResponse.md)
- [PayRequest](docs/PayRequest.md)
- [PaymentResponse](docs/PaymentResponse.md)
- [ProductResponse](docs/ProductResponse.md)
- [RefundRequest](docs/RefundRequest.md)
- [RefundResponse](docs/RefundResponse.md)
- [RenameRequest](docs/RenameRequest.md)
- [ReorderItem](docs/ReorderItem.md)
- [ReorderRequest](docs/ReorderRequest.md)
- [ReservationResponse](docs/ReservationResponse.md)
- [ReserveRequest](docs/ReserveRequest.md)
- [ResetRequest](docs/ResetRequest.md)
- [SessionDto](docs/SessionDto.md)
- [ShipmentInfo](docs/ShipmentInfo.md)
- [ShippingAddressInfo](docs/ShippingAddressInfo.md)
- [StartPaymentResponse](docs/StartPaymentResponse.md)
- [SubmitResponse](docs/SubmitResponse.md)
- [TokenResponse](docs/TokenResponse.md)
- [UpdateAddressRequest](docs/UpdateAddressRequest.md)
- [UpdateCategoryRequest](docs/UpdateCategoryRequest.md)
- [UpdateProductRequest](docs/UpdateProductRequest.md)
- [UpsertCartItemRequest](docs/UpsertCartItemRequest.md)

### Authorization


Authentication schemes defined for the API:
<a id="bearerAuth"></a>
#### bearerAuth


- **Type**: HTTP Bearer Token authentication (JWT)

## About

This TypeScript SDK client supports the [Fetch API](https://fetch.spec.whatwg.org/)
and is automatically generated by the
[OpenAPI Generator](https://openapi-generator.tech) project:

- API version: `v1`
- Package version: `v1`
- Generator version: `7.20.0`
- Build package: `org.openapitools.codegen.languages.TypeScriptFetchClientCodegen`

The generated npm module supports the following:

- Environments
  * Node.js
  * Webpack
  * Browserify
- Language levels
  * ES5 - you must have a Promises/A+ library installed
  * ES6
- Module systems
  * CommonJS
  * ES6 module system


## Development

### Building

To build the TypeScript source code, you need to have Node.js and npm installed.
After cloning the repository, navigate to the project directory and run:

```bash
npm install
npm run build
```

### Publishing

Once you've built the package, you can publish it to npm:

```bash
npm publish
```

## License

[]()
