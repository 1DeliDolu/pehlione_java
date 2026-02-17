# AdminInventoryApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**adjust**](AdminInventoryApi.md#adjustoperation) | **POST** /api/v1/admin/inventory/products/{productId}/adjust |  |
| [**restock**](AdminInventoryApi.md#restockoperation) | **POST** /api/v1/admin/inventory/products/{productId}/restock |  |



## adjust

> StockResponse adjust(productId, adjustRequest)



### Example

```ts
import {
  Configuration,
  AdminInventoryApi,
} from '@pehlione/api-admin';
import type { AdjustOperationRequest } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminInventoryApi(config);

  const body = {
    // number
    productId: 789,
    // AdjustRequest
    adjustRequest: ...,
  } satisfies AdjustOperationRequest;

  try {
    const data = await api.adjust(body);
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
| **productId** | `number` |  | [Defaults to `undefined`] |
| **adjustRequest** | [AdjustRequest](AdjustRequest.md) |  | |

### Return type

[**StockResponse**](StockResponse.md)

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


## restock

> StockResponse restock(productId, restockRequest)



### Example

```ts
import {
  Configuration,
  AdminInventoryApi,
} from '@pehlione/api-admin';
import type { RestockOperationRequest } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminInventoryApi(config);

  const body = {
    // number
    productId: 789,
    // RestockRequest
    restockRequest: ...,
  } satisfies RestockOperationRequest;

  try {
    const data = await api.restock(body);
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
| **productId** | `number` |  | [Defaults to `undefined`] |
| **restockRequest** | [RestockRequest](RestockRequest.md) |  | |

### Return type

[**StockResponse**](StockResponse.md)

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

