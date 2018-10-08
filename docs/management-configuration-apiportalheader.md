# API Portal Headers

Add a list of name/value to display in the api header.
The name could be used as is, or could be a translated key.

To add a translation, you need to update the json file (see on [Github](https://github.com/gravitee-io/gravitee-management-webui/tree/master/src/portal/i18n)).

The value can also be an "hard coded" value or you can use templating like in documentation pages.

See following examples :

Name | Value
----- | ------
api.version | `${api.version}`
api.endpoint | `${api.proxy.contextPath}`
api.publishedAt | `${(api.deployedAt?date)!}`
My hard coded name | `My hard coded value`

You are able to combine api attributes, metadata and strings.