# Response template


## Overview
Response template must be defined according to the `Accept` request header.
You can define as many content-type as you need to manage different response body, for example for `application/json` or `text/xml`.

If none of the defined template is corresponding to the incoming `Accept` request header, you can define
a fallback response using the type `*/*`.

It no fallback is defined, the normal response from the policy will be sent to the API consumer.

## Configuration
For each type, you must have to configure the response with:
* A status code
* HTTP headers
* A body

The response body template is supporting the Expression Language so you can access many elements to provide a clean and clear response message.


## Examples

For the API-KEY:

```json
{
  "message": "{#error.message}",
  "api-key": "{#parameters['api-key']}"
}
```

In this example, `#error.message` is corresponding to the normal message send by the policy.
