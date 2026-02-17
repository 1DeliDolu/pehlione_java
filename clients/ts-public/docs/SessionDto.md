
# SessionDto


## Properties

Name | Type
------------ | -------------
`createdAt` | Date
`current` | boolean
`deviceName` | string
`ip` | string
`lastSeenAt` | Date
`revoked` | boolean
`revokedAt` | Date
`sessionId` | string
`userAgent` | string

## Example

```typescript
import type { SessionDto } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "createdAt": null,
  "current": null,
  "deviceName": null,
  "ip": null,
  "lastSeenAt": null,
  "revoked": null,
  "revokedAt": null,
  "sessionId": null,
  "userAgent": null,
} satisfies SessionDto

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as SessionDto
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


