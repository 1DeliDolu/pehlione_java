
# CartItemResponse


## Properties

Name | Type
------------ | -------------
`currency` | string
`lineTotal` | number
`name` | string
`primaryImageUrl` | string
`productId` | number
`quantity` | number
`sku` | string
`unitPrice` | number

## Example

```typescript
import type { CartItemResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "currency": null,
  "lineTotal": null,
  "name": null,
  "primaryImageUrl": null,
  "productId": null,
  "quantity": null,
  "sku": null,
  "unitPrice": null,
} satisfies CartItemResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CartItemResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


