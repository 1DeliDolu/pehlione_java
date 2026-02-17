
# ReservationResponse


## Properties

Name | Type
------------ | -------------
`expiresAt` | string
`remainingStock` | number
`reservationId` | string

## Example

```typescript
import type { ReservationResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "expiresAt": null,
  "remainingStock": null,
  "reservationId": null,
} satisfies ReservationResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ReservationResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


