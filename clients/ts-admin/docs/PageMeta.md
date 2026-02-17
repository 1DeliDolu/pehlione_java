
# PageMeta


## Properties

Name | Type
------------ | -------------
`first` | boolean
`last` | boolean
`number` | number
`size` | number
`totalElements` | number
`totalPages` | number

## Example

```typescript
import type { PageMeta } from '@pehlione/api-admin'

// TODO: Update the object below with actual values
const example = {
  "first": true,
  "last": false,
  "number": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6,
} satisfies PageMeta

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PageMeta
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


