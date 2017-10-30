# API Documentation

At this date, three types of document are supported :

* Swagger
* RAML
* Markdown (MD)

You can access to your API data (name, version, description ...) on your API’s documentation by writing: `${api.name}` or `${api.metadata['foo-bar']}` in your documentation's content.

By default, documentation pages are in staging mode and will be visible to API owners and API members with specific documentation roles.
To make documentation visible for all users, you can switch on the *published* button.

You can set a page to be a *homepage* by clicking on the *house* button. API homepage will be visible in the Portal main page of the API.

You can also configure a page by clicking on the *settings* button. Page's configuration let you fetch page's content from an external resource such as Git repositories, or HTTP urls. 

(+) button let you create a new documentation page.
