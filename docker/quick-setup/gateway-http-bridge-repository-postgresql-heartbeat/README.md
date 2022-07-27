# Gateway with Bridge HTTP with postgresql and heartbeat

Here is a docker-compose to run APIM with PostgreSQL as database and three gateways with heartbeat enabled:
 - One as a **Bridge Server.** It can make calls to the database and expose HTTP endpoints to be able to call the database.
 - Two as a **Bridge Client.** It calls the Gateway Bridge Server through HTTP to fetch data.

You can classically call your apis through your gateway, for example: `http://localhost:8082/myapi` or `http://localhost:8882/myapi`

To test the **Bridge Server**, you can call, for example, `http://localhost:18092/_bridge/apis` to list all the apis directly from database.

---
> For more information, please read this doc: https://docs.gravitee.io/apim/3.x/apim_installguide_hybrid_deployment.html#apim_gateway_http_bridge_server
> and https://docs.gravitee.io/apim/3.x/apim_installguide_repositories_jdbc.html
---

## Requirements

You need to provide a JDBC driver for postgresql.
Put it in a `.driver` folder
You can download one here: https://jdbc.postgresql.org/download.html

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`
