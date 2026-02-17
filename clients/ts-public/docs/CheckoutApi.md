# CheckoutApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**cancel**](CheckoutApi.md#cancel) | **POST** /api/v1/checkout/drafts/{draftId}/cancel |  |
| [**getDraft**](CheckoutApi.md#getdraft) | **GET** /api/v1/checkout/drafts/{draftId} |  |
| [**pay**](CheckoutApi.md#payoperation) | **POST** /api/v1/checkout/drafts/{draftId}/pay | Start payment for a draft |
| [**reserve1**](CheckoutApi.md#reserve1) | **POST** /api/v1/checkout/reserve |  |
| [**submit**](CheckoutApi.md#submit) | **POST** /api/v1/checkout/drafts/{draftId}/submit |  |



## cancel

> cancel(draftId)



### Example

```ts
import {
  Configuration,
  CheckoutApi,
} from '@pehlione/api-public';
import type { CancelRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CheckoutApi(config);

  const body = {
    // string
    draftId: draftId_example,
  } satisfies CancelRequest;

  try {
    const data = await api.cancel(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **draftId** | `string` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **400** | Bad Request / Validation |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **409** | Conflict |  -  |
| **429** | Too Many Requests |  -  |
| **500** | Internal Server Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getDraft

> DraftResponse getDraft(draftId)



### Example

```ts
import {
  Configuration,
  CheckoutApi,
} from '@pehlione/api-public';
import type { GetDraftRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CheckoutApi(config);

  const body = {
    // string
    draftId: draftId_example,
  } satisfies GetDraftRequest;

  try {
    const data = await api.getDraft(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **draftId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**DraftResponse**](DraftResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `*/*`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **400** | Bad Request / Validation |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **409** | Conflict |  -  |
| **429** | Too Many Requests |  -  |
| **500** | Internal Server Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## pay

> StartPaymentResponse pay(draftId, payRequest, idempotencyKey)

Start payment for a draft

Creates an order in PENDING_PAYMENT, snapshots shipping address and creates payment intent. Supports Idempotency-Key header.

### Example

```ts
import {
  Configuration,
  CheckoutApi,
} from '@pehlione/api-public';
import type { PayOperationRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CheckoutApi(config);

  const body = {
    // string | Draft public id
    draftId: drf_1a2b3c4d,
    // PayRequest
    payRequest: ...,
    // string | Idempotency key for safe retries (optional)
    idempotencyKey: checkout-42,
  } satisfies PayOperationRequest;

  try {
    const data = await api.pay(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **draftId** | `string` | Draft public id | [Defaults to `undefined`] |
| **payRequest** | [PayRequest](PayRequest.md) |  | |
| **idempotencyKey** | `string` | Idempotency key for safe retries | [Optional] [Defaults to `undefined`] |

### Return type

[**StartPaymentResponse**](StartPaymentResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Payment started |  -  |
| **400** | Validation error |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **409** | Draft not reserved/expired |  -  |
| **429** | Too Many Requests |  -  |
| **500** | Internal Server Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## reserve1

> DraftResponse reserve1(reserveRequest)



### Example

```ts
import {
  Configuration,
  CheckoutApi,
} from '@pehlione/api-public';
import type { Reserve1Request } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CheckoutApi(config);

  const body = {
    // ReserveRequest (optional)
    reserveRequest: ...,
  } satisfies Reserve1Request;

  try {
    const data = await api.reserve1(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **reserveRequest** | [ReserveRequest](ReserveRequest.md) |  | [Optional] |

### Return type

[**DraftResponse**](DraftResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `*/*`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **400** | Bad Request / Validation |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **409** | Conflict |  -  |
| **429** | Too Many Requests |  -  |
| **500** | Internal Server Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## submit

> SubmitResponse submit(draftId)



### Example

```ts
import {
  Configuration,
  CheckoutApi,
} from '@pehlione/api-public';
import type { SubmitRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CheckoutApi(config);

  const body = {
    // string
    draftId: draftId_example,
  } satisfies SubmitRequest;

  try {
    const data = await api.submit(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **draftId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**SubmitResponse**](SubmitResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `*/*`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **400** | Bad Request / Validation |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **409** | Conflict |  -  |
| **429** | Too Many Requests |  -  |
| **500** | Internal Server Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

