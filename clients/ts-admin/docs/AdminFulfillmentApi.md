# AdminFulfillmentApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**cancel**](AdminFulfillmentApi.md#canceloperation) | **POST** /api/v1/admin/orders/{orderId}/cancel |  |
| [**deliver**](AdminFulfillmentApi.md#deliver) | **POST** /api/v1/admin/orders/{orderId}/deliver |  |
| [**ship**](AdminFulfillmentApi.md#shipoperation) | **POST** /api/v1/admin/orders/{orderId}/ship |  |



## cancel

> cancel(orderId, cancelRequest)



### Example

```ts
import {
  Configuration,
  AdminFulfillmentApi,
} from '@pehlione/api-admin';
import type { CancelOperationRequest } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminFulfillmentApi(config);

  const body = {
    // string
    orderId: orderId_example,
    // CancelRequest (optional)
    cancelRequest: ...,
  } satisfies CancelOperationRequest;

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
| **orderId** | `string` |  | [Defaults to `undefined`] |
| **cancelRequest** | [CancelRequest](CancelRequest.md) |  | [Optional] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
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


## deliver

> deliver(orderId)



### Example

```ts
import {
  Configuration,
  AdminFulfillmentApi,
} from '@pehlione/api-admin';
import type { DeliverRequest } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminFulfillmentApi(config);

  const body = {
    // string
    orderId: orderId_example,
  } satisfies DeliverRequest;

  try {
    const data = await api.deliver(body);
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
| **orderId** | `string` |  | [Defaults to `undefined`] |

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


## ship

> ShipmentResponse ship(orderId, shipRequest)



### Example

```ts
import {
  Configuration,
  AdminFulfillmentApi,
} from '@pehlione/api-admin';
import type { ShipOperationRequest } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminFulfillmentApi(config);

  const body = {
    // string
    orderId: orderId_example,
    // ShipRequest
    shipRequest: ...,
  } satisfies ShipOperationRequest;

  try {
    const data = await api.ship(body);
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
| **orderId** | `string` |  | [Defaults to `undefined`] |
| **shipRequest** | [ShipRequest](ShipRequest.md) |  | |

### Return type

[**ShipmentResponse**](ShipmentResponse.md)

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

