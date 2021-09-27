# Keycloak

Here is a docker-compose to run APIM with a Keycloak container available as an Identity Provider.

---
> For more information, please read :
> https://docs.gravitee.io/apim/3.x/apim_installguide_authentication_keycloak.html
---

Keycloak UI is accessible here http://localhost:8081/auth

## How to use ?

In the Keycloak UI (https://docs.gravitee.io/apim/3.x/apim_installguide_authentication_keycloak.html#create_a_new_client):
- Create a new client for your application.
- Create a user in the realm.

In the Console UI (https://docs.gravitee.io/apim/3.x/apim_installguide_authentication_keycloak.html#apim_console_configuration):
- Go to Organization/Settings
- Configure the Identity provider following documentation [http://localhost:8081/auth/realms/master/.well-known/openid-configuration](Configuration URLs can be find here)

You should now be able to use it to log to Console UI.

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

