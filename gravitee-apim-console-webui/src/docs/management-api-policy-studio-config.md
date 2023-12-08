## Configuration

As an API editor, you can define flows. 

Each flow will apply to a path in the order defined.

You can define a name and a description but if empty the name of the flow will be generated with the operator ("Starts with" or "Equals"), the path and the HTTP methods.

You can also define an Expression Language (EL) type condition.

This expression language is a powerful language that supports querying and manipulating an object graph and is based on the SpEL (Spring Expression Language). This means that you can do everything described in the link.

##### Some examples :
- Get the value of the property my-property defined in properties: `{#properties['my-property']}`
- Get the value of the Content-Type header for an incoming HTTP request: `{#request.headers['content-type']}`
- Get the second part of the request path: `{#request.paths[1]}`
- Get the value of the user-id attribute for an incoming HTTP request `{#context.attributes['user-id']}`
- Get the value of the plan attribute for an incoming HTTP request `{#context.attributes['plan']}`
