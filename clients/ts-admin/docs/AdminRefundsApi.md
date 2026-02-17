# AdminRefundsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**list1**](AdminRefundsApi.md#list1) | **GET** /api/v1/admin/refunds | Admin: list refunds |



## list1

> PageResponse list1(status, email, orderId, page, size, sort)

Admin: list refunds

Filter by status, user email and order id.

### Example

```ts
import {
  Configuration,
  AdminRefundsApi,
} from '@pehlione/api-admin';
import type { List1Request } from '@pehlione/api-admin';

async function example() {
  console.log("🚀 Testing @pehlione/api-admin SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminRefundsApi(config);

  const body = {
    // 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | Refund status filter (optional)
    status: status_example,
    // string | User email contains (optional)
    email: gmail.com,
    // string | Order id exact match (optional)
    orderId: 6b7a2f42,
    // number | Zero-based page index (0..N) (optional)
    page: 56,
    // number | The size of the page to be returned (optional)
    size: 56,
    // Array<string> | Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. (optional)
    sort: ...,
  } satisfies List1Request;

  try {
    const data = await api.list1(body);
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
| **status** | `PENDING`, `SUCCEEDED`, `FAILED`, `CANCELLED` | Refund status filter | [Optional] [Defaults to `undefined`] [Enum: PENDING, SUCCEEDED, FAILED, CANCELLED] |
| **email** | `string` | User email contains | [Optional] [Defaults to `undefined`] |
| **orderId** | `string` | Order id exact match | [Optional] [Defaults to `undefined`] |
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

