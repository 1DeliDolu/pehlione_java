
# ApiStatus


## Properties

Name | Type
------------ | -------------
`app` | string
`status` | string
`time` | Date

## Example

```typescript
import type { ApiStatus } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "app": null,
  "status": null,
  "time": null,
} satisfies ApiStatus

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiStatus
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


