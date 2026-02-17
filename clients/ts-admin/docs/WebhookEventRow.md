
# WebhookEventRow


## Properties

Name | Type
------------ | -------------
`eventId` | string
`payloadHash` | string
`provider` | string
`receivedAt` | Date

## Example

```typescript
import type { WebhookEventRow } from '@pehlione/api-admin'

// TODO: Update the object below with actual values
const example = {
  "eventId": null,
  "payloadHash": null,
  "provider": null,
  "receivedAt": null,
} satisfies WebhookEventRow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WebhookEventRow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


