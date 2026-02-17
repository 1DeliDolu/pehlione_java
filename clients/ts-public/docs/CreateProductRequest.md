
# CreateProductRequest


## Properties

Name | Type
------------ | -------------
`categoryIds` | Set&lt;number&gt;
`currency` | string
`description` | string
`name` | string
`price` | number
`sku` | string
`status` | string
`stockQuantity` | number

## Example

```typescript
import type { CreateProductRequest } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "categoryIds": null,
  "currency": null,
  "description": null,
  "name": null,
  "price": null,
  "sku": null,
  "status": null,
  "stockQuantity": null,
} satisfies CreateProductRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateProductRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


