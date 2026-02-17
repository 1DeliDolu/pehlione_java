
# ApiProblemViolationsInner


## Properties

Name | Type
------------ | -------------
`field` | string
`invalidValue` | object
`message` | string
`path` | string
`rejectedValue` | object

## Example

```typescript
import type { ApiProblemViolationsInner } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "field": null,
  "invalidValue": null,
  "message": null,
  "path": null,
  "rejectedValue": null,
} satisfies ApiProblemViolationsInner

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiProblemViolationsInner
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


