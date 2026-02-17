# InventoryApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**consume**](InventoryApi.md#consume) | **POST** /api/v1/inventory/reservations/{reservationId}/consume |  |
| [**release**](InventoryApi.md#release) | **POST** /api/v1/inventory/reservations/{reservationId}/release |  |
| [**reserve**](InventoryApi.md#reserveoperation) | **POST** /api/v1/inventory/reserve |  |



## consume

> consume(reservationId)



### Example

```ts
import {
  Configuration,
  InventoryApi,
} from '@pehlione/api-public';
import type { ConsumeRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new InventoryApi(config);

  const body = {
    // string
    reservationId: reservationId_example,
  } satisfies ConsumeRequest;

  try {
    const data = await api.consume(body);
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
| **reservationId** | `string` |  | [Defaults to `undefined`] |

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


## release

> release(reservationId)



### Example

```ts
import {
  Configuration,
  InventoryApi,
} from '@pehlione/api-public';
import type { ReleaseRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new InventoryApi(config);

  const body = {
    // string
    reservationId: reservationId_example,
  } satisfies ReleaseRequest;

  try {
    const data = await api.release(body);
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
| **reservationId** | `string` |  | [Defaults to `undefined`] |

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


## reserve

> ReservationResponse reserve(reserveRequest)



### Example

```ts
import {
  Configuration,
  InventoryApi,
} from '@pehlione/api-public';
import type { ReserveOperationRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new InventoryApi(config);

  const body = {
    // ReserveRequest
    reserveRequest: ...,
  } satisfies ReserveOperationRequest;

  try {
    const data = await api.reserve(body);
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
| **reserveRequest** | [ReserveRequest](ReserveRequest.md) |  | |

### Return type

[**ReservationResponse**](ReservationResponse.md)

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

