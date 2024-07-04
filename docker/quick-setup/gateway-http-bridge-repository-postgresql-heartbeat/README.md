# Gateway with Bridge HTTP with postgresql and heartbeat

Here is a docker-compose to run APIM with PostgreSQL as database and 2 gateways with heartbeat enabled:
 - One as a **Bridge Server.** It can make calls to the database and expose HTTP endpoints to be able to call the database.
 - One as a **Bridge Client.** It calls the Gateway Bridge Server through HTTP to fetch data.

You can classically call your apis through your gateway, for example: `http://localhost:[8075-8082]/myapi`

To test the **Bridge Server**, you can call, for example, `http://localhost:18092/_bridge/apis` to list all the apis directly from database.

---
> For more information, please read this doc:
> * https://documentation.gravitee.io/apim/getting-started/hybrid-deployment
> * https://documentation.gravitee.io/apim/getting-started/configuration/repositories/jdbc
---

## Requirements

You need to provide a JDBC driver for postgresql.
Put it in a `.driver` folder
You can download one here: https://jdbc.postgresql.org/download/

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do `export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`.
To target non stable images use: `APIM_REGISTRY=graviteeio.azurecr.io`

If you want to add `i` instances of gateway_client
You can run `docker-compose up --scale gateway_client={i} -d`

On Osx, you have to add the instances one by one, otherwise you will get a port binding error.
For example, if you need to start 3 instances;
```shell
docker-compose up -d
docker-compose up --scale gateway_client=2 -d
docker-compose up --scale gateway_client=3 -d
```
