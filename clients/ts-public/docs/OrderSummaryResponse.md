
# OrderSummaryResponse


## Properties

Name | Type
------------ | -------------
`createdAt` | Date
`currency` | string
`orderId` | string
`status` | string
`totalAmount` | number

## Example

```typescript
import type { OrderSummaryResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "createdAt": null,
  "currency": null,
  "orderId": null,
  "status": null,
  "totalAmount": null,
} satisfies OrderSummaryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as OrderSummaryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


