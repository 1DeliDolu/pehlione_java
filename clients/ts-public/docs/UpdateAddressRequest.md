
# UpdateAddressRequest


## Properties

Name | Type
------------ | -------------
`city` | string
`countryCode` | string
`fullName` | string
`label` | string
`line1` | string
`line2` | string
`phone` | string
`postalCode` | string
`state` | string

## Example

```typescript
import type { UpdateAddressRequest } from '@pehlione/api-public'

// TODO: Update the object below with actual values
const example = {
  "city": null,
  "countryCode": null,
  "fullName": null,
  "label": null,
  "line1": null,
  "line2": null,
  "phone": null,
  "postalCode": null,
  "state": null,
} satisfies UpdateAddressRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateAddressRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


