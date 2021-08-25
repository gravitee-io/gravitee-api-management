# Internal & External Sharding tags

Here is a docker-compose to run APIM with two gateways:
 - One configured with sharding tag `internal`. It will only sync APIs configured for this tag. To use this gateway, call your API with `http://localhost:8082/myapi`.
 - One configured with sharding tag `external`. It will only sync APIs configured for this tag. To use this gateway, call your API with `http://localhost:8081/myapi`.

---
> For more information, please read this doc: https://docs.gravitee.io/apim/3.x/apim_installguide_gateway_configuration.html#configure_sharding_tags
---

## How to use ?

In the Console UI:
- Go to `Organization settings / Gateway / Sharding tags`
- Create two tags: `internal` and `external` 
- Create your APIs and select configure them with tags (`YourAPI / Proxy / Deployments`)

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`