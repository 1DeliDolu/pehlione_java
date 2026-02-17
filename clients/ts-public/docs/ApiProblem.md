
# ApiProblem


## Properties

Name | Type
------------ | -------------
`code` | string
`detail` | string
`instance` | string
`requestId` | string
`retryAfterSeconds` | number
`status` | number
`timestamp` | string
`title` | string
`type` | string
`violations` | [Array&lt;ApiProblemViolationsInner&gt;](ApiProblemViolationsInner.md)

## Example

```typescript
import type { ApiProblem } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "code": VALIDATION_FAILED,
  "detail": Validation failed,
  "instance": /api/v1/products,
  "requestId": 3f9d...,
  "retryAfterSeconds": 12,
  "status": 400,
  "timestamp": 2026-02-17T12:34:56Z,
  "title": Validation error,
  "type": urn:problem:validation,
  "violations": null,
} satisfies ApiProblem

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiProblem
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


