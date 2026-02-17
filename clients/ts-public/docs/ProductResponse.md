
# ProductResponse


## Properties

Name | Type
------------ | -------------
`categories` | [Array&lt;CategoryRef&gt;](CategoryRef.md)
`createdAt` | Date
`currency` | string
`description` | string
`id` | number
`images` | [Array&lt;ImageRef&gt;](ImageRef.md)
`name` | string
`price` | number
`primaryImage` | [ImageRef](ImageRef.md)
`sku` | string
`status` | string
`stockQuantity` | number
`updatedAt` | Date

## Example

```typescript
import type { ProductResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "categories": null,
  "createdAt": null,
  "currency": null,
  "description": null,
  "id": null,
  "images": null,
  "name": null,
  "price": null,
  "primaryImage": null,
  "sku": null,
  "status": null,
  "stockQuantity": null,
  "updatedAt": null,
} satisfies ProductResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProductResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


