# Policy studio

## Design

Policies let you customize, enhance API behavior and functionalities. Policies can enforce security, enhance performance, transform request/response and let you implement some logic via the Groovy Policy or dynamic routing policy.

Policy can be chained, applied to root path (/**) or for specific paths.

You can drag and drop policy at the center of the screen to apply policy for a specific path, and the right part of the screen let you configure your policy.

The (+) button let you create a new path to specify its behavior via the policies.

## Configuration

As an API editor, you can define flows.

Each flow will apply to a path in the order defined.

You can define a name and a description but if empty the name of the flow will be generated with the operator ("Starts with" or "Equals"), the path and the HTTP methods.

You can also define an Expression Language (EL) type condition.

This expression language is a powerful language that supports querying and manipulating an object graph and is based on the SpEL (Spring Expression Language). This means that you can do everything described in the link.

##### Some examples:
- Get the value of the property my-property defined in properties: `{#properties['my-property']}`
- Get the value of the Content-Type header for an incoming HTTP request: `{#request.headers['content-type']}`
- Get the second part of the request path: `{#request.paths[1]}`
- Get the value of the user-id attribute for an incoming HTTP request `{#context.attributes['user-id']}`
- Get the value of the plan attribute for an incoming HTTP request `{#context.attributes['plan']}`

## Properties

As an API publisher, you can define properties for your API. These properties are automatically injected into the expression language context to be used later.

For example, to get the value of the property `my-property` defined in API properties: `{#properties['my-property']}`

The (+) button let you create a new property.

### Dynamic properties

To get access to dynamic properties click on the _settings_ button. Dynamic properties let you fetch API properties from a remote resource, such as web service.

You can set a polling frequency property and apply response transformation thanks to the [JOLT framework](http://bazaarvoice.github.io/jolt/).

Do not forget to switch on dynamic properties and re-deploy the API to enable dynamic properties. Like classical properties, dynamic properties will be injected into the expression language context.

## Resources

Resources are link to the API lifecycle. They are initialized when the API is starting and released when API is stopped.
Resources are used via the API policies to enhance API behavior.

Different resource types are available:

* Cache

Used to store re-usable data to avoid subsequent calls.

* OAuth 2.0

Used to introspect an access_token via an external OAuth 2.0 authorization server.

The (+) button let you create a new resource.
