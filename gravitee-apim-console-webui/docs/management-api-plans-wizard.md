# API Plan wizard

This wizard allows you to define a plan, define its security and its policies (quota / rate limit..).

4 types are available for plans:

* OAuth2

The OAuth2 plan type allows you to check an access token and identify the application with its client ID

* JWT

The JWT plan type allows you to check that the given JSON web token is valid and not expired

* API key

The API key plan type allows you to identify and to check that the given API key is valid and not expired

* Keyless

Nothing is required: public access
