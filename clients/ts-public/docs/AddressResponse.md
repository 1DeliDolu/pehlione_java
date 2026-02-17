
# AddressResponse


## Properties

Name | Type
------------ | -------------
`city` | string
`countryCode` | string
`createdAt` | Date
`fullName` | string
`id` | number
`isDefault` | boolean
`label` | string
`line1` | string
`line2` | string
`phone` | string
`postalCode` | string
`state` | string
`updatedAt` | Date

## Example

```typescript
import type { AddressResponse } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "city": null,
  "countryCode": null,
  "createdAt": null,
  "fullName": null,
  "id": null,
  "isDefault": null,
  "label": null,
  "line1": null,
  "line2": null,
  "phone": null,
  "postalCode": null,
  "state": null,
  "updatedAt": null,
} satisfies AddressResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AddressResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


