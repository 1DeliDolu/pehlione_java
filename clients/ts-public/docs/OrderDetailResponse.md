
# OrderDetailResponse


## Properties

Name | Type
------------ | -------------
`createdAt` | Date
`currency` | string
`items` | [Array&lt;OrderItemResponse&gt;](OrderItemResponse.md)
`orderId` | string
`shipments` | [Array&lt;ShipmentInfo&gt;](ShipmentInfo.md)
`shippingAddress` | [ShippingAddressInfo](ShippingAddressInfo.md)
`status` | string
`totalAmount` | number

## Example

```typescript
import type { OrderDetailResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "createdAt": null,
  "currency": null,
  "items": null,
  "orderId": null,
  "shipments": null,
  "shippingAddress": null,
  "status": null,
  "totalAmount": null,
} satisfies OrderDetailResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as OrderDetailResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


