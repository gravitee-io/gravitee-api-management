# Internal & External Sharding tags

Here is a docker-compose to run APIM with two gateways:
 - One configured with sharding tag `internal`. It will only sync APIs configured for this tag. To use this gateway, call your API with `http://localhost:8082/myapi`.
 - One configured with sharding tag `external`. It will only sync APIs configured for this tag. To use this gateway, call your API with `http://localhost:8081/myapi`.

You can classically call your apis through your gateway, for example: `http://localhost:8082/myapi`.

To test the **Bridge Server**, you can call, for example, `http://localhost:18092/_bridge/apis` to list all the apis.

---
**NOTE**
For more information, please read this doc: To use this gateway, call your API with `http://localhost:8082/myapi`.
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