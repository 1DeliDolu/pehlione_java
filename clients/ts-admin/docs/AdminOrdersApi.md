# AdminOrdersApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**get**](AdminOrdersApi.md#get) | **GET** /api/v1/admin/orders/{orderId} |  |
| [**list2**](AdminOrdersApi.md#list2) | **GET** /api/v1/admin/orders | Admin: list orders |



## get

> AdminOrderDetail get(orderId)



### Example

```ts
import {
  Configuration,
  AdminOrdersApi,
} from '@pehlione/api-admin';
import type { GetRequest } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminOrdersApi(config);

  const body = {
    // string
    orderId: orderId_example,
  } satisfies GetRequest;

  try {
    const data = await api.get(body);
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

[**AdminOrderDetail**](AdminOrderDetail.md)

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


## list2

> PageResponse list2(status, email, q, from, to, page, size, sort)

Admin: list orders

Filter by status/email/orderId/date range.

### Example

```ts
import {
  Configuration,
  AdminOrdersApi,
} from '@pehlione/api-admin';
import type { List2Request } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminOrdersApi(config);

  const body = {
    // 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'PAYMENT_FAILED' | 'REFUND_PENDING' | 'REFUNDED' | 'PLACED' | 'CANCELLED' | 'FULFILLED' | Order status filter (optional)
    status: status_example,
    // string | User email contains (optional)
    email: gmail.com,
    // string | Order id contains (optional)
    q: 6b7a,
    // Date | Created-at lower bound (ISO date-time) (optional)
    from: 2026-02-01T00:00:00Z,
    // Date | Created-at upper bound (ISO date-time) (optional)
    to: 2026-02-28T23:59:59Z,
    // number | Zero-based page index (0..N) (optional)
    page: 56,
    // number | The size of the page to be returned (optional)
    size: 56,
    // Array<string> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. (optional)
    sort: ...,
  } satisfies List2Request;

  try {
    const data = await api.list2(body);
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
| **status** | `PENDING_PAYMENT`, `PAID`, `SHIPPED`, `PAYMENT_FAILED`, `REFUND_PENDING`, `REFUNDED`, `PLACED`, `CANCELLED`, `FULFILLED` | Order status filter | [Optional] [Defaults to `undefined`] [Enum: PENDING_PAYMENT, PAID, SHIPPED, PAYMENT_FAILED, REFUND_PENDING, REFUNDED, PLACED, CANCELLED, FULFILLED] |
| **email** | `string` | User email contains | [Optional] [Defaults to `undefined`] |
| **q** | `string` | Order id contains | [Optional] [Defaults to `undefined`] |
| **from** | `Date` | Created-at lower bound (ISO date-time) | [Optional] [Defaults to `undefined`] |
| **to** | `Date` | Created-at upper bound (ISO date-time) | [Optional] [Defaults to `undefined`] |
| **page** | `number` | Zero-based page index (0..N) | [Optional] [Defaults to `0`] |
| **size** | `number` | The size of the page to be returned | [Optional] [Defaults to `20`] |
| **sort** | `Array<string>` | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. | [Optional] |

### Return type

[**PageResponse**](PageResponse.md)

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

