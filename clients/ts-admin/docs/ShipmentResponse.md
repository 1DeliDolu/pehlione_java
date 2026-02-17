
# ShipmentResponse


## Properties

Name | Type
------------ | -------------
`carrier` | string
`deliveredAt` | Date
`shipmentId` | string
`shippedAt` | Date
`status` | string
`trackingNumber` | string

## Example

```typescript
import type { ShipmentResponse } from '@pehlione/api-admin'

// TODO: Update the object below with actual values
const example = {
  "carrier": null,
  "deliveredAt": null,
  "shipmentId": null,
  "shippedAt": null,
  "status": null,
  "trackingNumber": null,
} satisfies ShipmentResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ShipmentResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


