
# ShippingAddressInfo


## Properties

Name | Type
------------ | -------------
`city` | string
`countryCode` | string
`fullName` | string
`line1` | string
`line2` | string
`phone` | string
`postalCode` | string
`state` | string

## Example

```typescript
import type { ShippingAddressInfo } from '@pehlione/api-admin'

// TODO: Update the object below with actual values
const example = {
  "city": null,
  "countryCode": null,
  "fullName": null,
  "line1": null,
  "line2": null,
  "phone": null,
  "postalCode": null,
  "state": null,
} satisfies ShippingAddressInfo

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ShippingAddressInfo
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


