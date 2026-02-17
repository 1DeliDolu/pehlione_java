# PaymentsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**confirmMock**](PaymentsApi.md#confirmmock) | **POST** /api/v1/payments/{paymentId}/confirm-mock |  |
| [**failMock**](PaymentsApi.md#failmock) | **POST** /api/v1/payments/{paymentId}/fail-mock |  |
| [**get1**](PaymentsApi.md#get1) | **GET** /api/v1/payments/{paymentId} |  |



## confirmMock

> confirmMock(paymentId)



### Example

```ts
import {
  Configuration,
  PaymentsApi,
} from '@pehlione/api-public';
import type { ConfirmMockRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PaymentsApi(config);

  const body = {
    // string
    paymentId: paymentId_example,
  } satisfies ConfirmMockRequest;

  try {
    const data = await api.confirmMock(body);
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
| **paymentId** | `string` |  | [Defaults to `undefined`] |

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


## failMock

> failMock(paymentId, failRequest)



### Example

```ts
import {
  Configuration,
  PaymentsApi,
} from '@pehlione/api-public';
import type { FailMockRequest } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PaymentsApi(config);

  const body = {
    // string
    paymentId: paymentId_example,
    // FailRequest (optional)
    failRequest: ...,
  } satisfies FailMockRequest;

  try {
    const data = await api.failMock(body);
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
| **paymentId** | `string` |  | [Defaults to `undefined`] |
| **failRequest** | [FailRequest](FailRequest.md) |  | [Optional] |

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


## get1

> PaymentResponse get1(paymentId)



### Example

```ts
import {
  Configuration,
  PaymentsApi,
} from '@pehlione/api-public';
import type { Get1Request } from '@pehlione/api-public';

async function example() {
  console.log("🚀 Testing @pehlione/api-public SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PaymentsApi(config);

  const body = {
    // string
    paymentId: paymentId_example,
  } satisfies Get1Request;

  try {
    const data = await api.get1(body);
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
| **paymentId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**PaymentResponse**](PaymentResponse.md)

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

