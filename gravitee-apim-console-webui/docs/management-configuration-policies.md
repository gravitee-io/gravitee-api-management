# Policy studio

## Design

Policies let you customize, enhance API behavior and functionalities. Policies can enforce security, enhance performance, transform request/response and let you implement some logic via the Groovy Policy or dynamic routing policy.

Policy can be chained, applied to root path (/**) or for specific paths.

You can drag and drop policy at the center of the screen to apply policy for a specific path, and the right part of the screen let you configure your policy.

The (+) button let you create a new path to specify its behavior via the policies.

## Configuration

As an Organization user, you can define platform flows.

Each flow will apply to a path in the order defined.

You can define a name and a description but if empty the name of the flow will be generated with the operator ("Starts with" or "Equals"), the path and the HTTP methods.

You can configure one or more sharding tags.
The flows will only be executed on APIs with a sharding tag that matches.
If it is empty, the flows will be executed on all the APIs.

You can also define an Expression Language (EL) type condition.

This expression language is a powerful language that supports querying and manipulating an object graph and is based on the SpEL (Spring Expression Language). This means that you can do everything described in the link.

##### Some examples:
- Get the value of the property my-property defined in properties: `{#properties['my-property']}`
- Get the value of the Content-Type header for an incoming HTTP request: `{#request.headers['content-type']}`
- Get the second part of the request path: `{#request.paths[1]}`
- Get the value of the user-id attribute for an incoming HTTP request `{#context.attributes['user-id']}`
- Get the value of the plan attribute for an incoming HTTP request `{#context.attributes['plan']}`
