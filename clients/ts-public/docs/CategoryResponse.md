
# CategoryResponse


## Properties

Name | Type
------------ | -------------
`createdAt` | Date
`id` | number
`name` | string
`slug` | string
`updatedAt` | Date

## Example

```typescript
import type { CategoryResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "createdAt": null,
  "id": null,
  "name": null,
  "slug": null,
  "updatedAt": null,
} satisfies CategoryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CategoryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


