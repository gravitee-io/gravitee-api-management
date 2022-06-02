# API Properties

As an API publisher, you can define properties for your API. These properties are automatically injected into the expression language context to be used later.

For example, to get the value of the property `my-property` defined in API properties: `{#properties['my-property']}`

The (+) button let you create a new property.

## Dynamic properties

To get access to dynamic properties click on the _settings_ button. Dynamic properties let you fetch API properties from a remote resource, such as web service.

You can set a polling frequency property and apply response transformation thanks to the [JOLT framework](http://bazaarvoice.github.io/jolt/).

Do not forget to switch on dynamic properties and re-deploy the API to enable dynamic properties. Like classical properties, dynamic properties will be injected into the expression language context.
