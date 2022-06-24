# Keycloak

Here is a docker-compose to run APIM with a Keycloak container available as an Identity Provider.

---
> For more information, please read :
> https://docs.gravitee.io/apim/3.x/apim_installguide_authentication_keycloak.html
---

Keycloak UI is accessible here http://localhost:8081/auth

## How to use ?

In the Keycloak UI (https://docs.gravitee.io/apim/3.x/apim_installguide_authentication_keycloak.html#create_a_new_client):
- Read how to create a new client for your application. (Already imported with `realm/realm-gio.json`)
- Create a user in the "GIO" realm.

You can read how to [https://docs.gravitee.io/apim/3.x/apim_installguide_authentication_keycloak.html#apim_console_configuration](configure the Identity provider following documentation) but it's already configured with docker environment.

### How to test an api with `gravitee-resource-oauth2-provider-keycloak`

1. Download the resource `./download-plugins-ext.sh`
2. Start the stack with `APIM_VERSION={APIM_VERSION} docker-compose up -d`
3. Import the `secured-api.json`
4. Start and deploy the api
5. Add `gravitee-client` as client_id of your app.
6. Subscribe to Oauth plan of secured api with your app.
7. Ask a token to Keycloak
```
curl --location --request POST 'http://auth.localhost/auth/realms/gio/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'client_id=gravitee-client' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_secret=00dc0118-2a0d-4249-86a3-3e133f5de145'
```
8. Call the api `Authorization: Bearer <token>`
```
curl --location --request GET 'http://localhost:8082/worldtimeapi' \
--header 'Authorization: Bearer <token>'
```

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

