# API Health Check

Health check configuration let you enable or disable health check service, set a frequency rate, choose an `health` relative path and provide some assertions to control endpoint API's health.

How to write assertion:

Example 1: Check the status of the HTTP response
`#response.status == 200`

Example 2: Check the JSON content of the response
`#jsonPath(#response.content, '$.status') == 'green'`
