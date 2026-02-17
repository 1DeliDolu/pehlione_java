
# PaymentResponse


## Properties

Name | Type
------------ | -------------
`amount` | number
`createdAt` | Date
`currency` | string
`orderId` | string
`paymentId` | string
`provider` | string
`status` | string
`updatedAt` | Date

## Example

```typescript
import type { PaymentResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "amount": null,
  "createdAt": null,
  "currency": null,
  "orderId": null,
  "paymentId": null,
  "provider": null,
  "status": null,
  "updatedAt": null,
} satisfies PaymentResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PaymentResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


