
# MeResponse


## Properties

Name | Type
------------ | -------------
`createdAt` | Date
`email` | string
`enabled` | boolean
`locked` | boolean
`roles` | Array&lt;string&gt;
`updatedAt` | Date

## Example

```typescript
import type { MeResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "createdAt": null,
  "email": null,
  "enabled": null,
  "locked": null,
  "roles": null,
  "updatedAt": null,
} satisfies MeResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MeResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


