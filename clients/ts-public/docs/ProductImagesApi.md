# ProductImagesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**add**](ProductImagesApi.md#add) | **POST** /api/v1/products/{productId}/images |  |
| [**delete3**](ProductImagesApi.md#delete3) | **DELETE** /api/v1/products/{productId}/images/{imageId} |  |
| [**reorder**](ProductImagesApi.md#reorderoperation) | **PUT** /api/v1/products/{productId}/images/reorder |  |



## add

> ProductResponse add(productId, addImageRequest)



### Example

```ts
import {
  Configuration,
  ProductImagesApi,
} from '@pehlione/api-public';
import type { AddRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ProductImagesApi(config);

  const body = {
    // number
    productId: 789,
    // AddImageRequest
    addImageRequest: ...,
  } satisfies AddRequest;

  try {
    const data = await api.add(body);
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
| **addImageRequest** | [AddImageRequest](AddImageRequest.md) |  | |

### Return type

[**ProductResponse**](ProductResponse.md)

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


## delete3

> delete3(productId, imageId)



### Example

```ts
import {
  Configuration,
  ProductImagesApi,
} from '@pehlione/api-public';
import type { Delete3Request } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ProductImagesApi(config);

  const body = {
    // number
    productId: 789,
    // number
    imageId: 789,
  } satisfies Delete3Request;

  try {
    const data = await api.delete3(body);
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
| **imageId** | `number` |  | [Defaults to `undefined`] |

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


## reorder

> ProductResponse reorder(productId, reorderRequest)



### Example

```ts
import {
  Configuration,
  ProductImagesApi,
} from '@pehlione/api-public';
import type { ReorderOperationRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ProductImagesApi(config);

  const body = {
    // number
    productId: 789,
    // ReorderRequest
    reorderRequest: ...,
  } satisfies ReorderOperationRequest;

  try {
    const data = await api.reorder(body);
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
| **reorderRequest** | [ReorderRequest](ReorderRequest.md) |  | |

### Return type

[**ProductResponse**](ProductResponse.md)

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

