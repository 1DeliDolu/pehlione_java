
# ReserveRequest


## Properties

Name | Type
------------ | -------------
`productId` | number
`quantity` | number
`ttlMinutes` | number

## Example

```typescript
import type { ReserveRequest } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "productId": null,
  "quantity": null,
  "ttlMinutes": null,
} satisfies ReserveRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ReserveRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


