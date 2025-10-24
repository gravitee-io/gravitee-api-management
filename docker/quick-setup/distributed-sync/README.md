# Gateway with Distributed Sync

Here is a docker-compose to run APIM with two gateways in a primary(master)-secondary setup:

You can classically call your apis through any of your gateway, for example: `http://localhost:8082/myapi` or `http://localhost:8081/myapi` .

## How to run ?

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d `

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

If you want to run only one gateway first and then the other, you can do
`export APIM_REGISTRY=graviteeio.azurecr.io && export APIM_VERSION=master-latest && docker compose up -d redis-stack mongodb elasticsearch gateway_primary gateway_secondary management_api management_ui`

and then to start the secondary gateway, you can do
`export APIM_REGISTRY=graviteeio.azurecr.io && export APIM_VERSION=master-latest && docker compose up -d gateway_secondary`

To see what is inside Redis, for example the distributed sync data, you can do
`redis-cli`

and then inside redis-cli, you can do
`Keys distributed*`

## Scenario Testing

Below are the two scenarios to test distributed sync process. Logs/Info/Success messages are printed as output.

### Scenario1: Test that a new gateway must sync without access to DB

Run the script `test-new-gateway-sync-without-db.sh`.

The script above starts all the services along with primary gateway.
It then kills the primary gateway and the database, starts the new gateway and verifies that it synced via redis.

### Scenario2: If the master node of the cluster crashes, test that a new master must be elected

Run the script `test-master-crash-new-master.sh`.

The script above starts all the services along with primary and secondary gateway.
It then kills the primary/master gateway and verifies that the secondary gateway takes over as the primary/master gateway.