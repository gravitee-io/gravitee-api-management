# Gateway with Bridge HTTP and Distributed Sync

Here is a docker-compose to run APIM with two or three gateways:
 - One as a **Bridge Server.** It can make calls to the database and expose HTTP endpoints to be able to call the database.
 - Two as a **Bridge Client.** It calls the Gateway Bridge Server through HTTP to fetch data.

You can classically call your apis through any of your client gateway, for example: `http://localhost:8082/myapi` or `http://localhost:8081/myapi` .

To test the **Bridge Server**, you can call, for example, `http://localhost:18092/_bridge/apis` to list all the apis directly from database.

## How to run ?

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

If you want to run only one client gateway first and then the other, you can do
`export APIM_REGISTRY=graviteeio.azurecr.io && export APIM_VERSION=master-latest && docker compose up -d redis-stack mongodb elasticsearch gateway_server gateway_client management_api management_ui`
 
and then to start the second client gateway, you can do
`export APIM_REGISTRY=graviteeio.azurecr.io && export APIM_VERSION=master-latest && docker compose up -d gateway_client_2`

To see what is inside Redis, for example the distributed sync data, you can do
`redis-cli`

and then inside redis-cli, you can do
`Keys distributed*`

## Scenario Testing

Below is the scenario to test distributed sync process with Bridge Server. Logs/Info/Success messages are printed as output.

### Scenario: Test that a new gateway must sync without access to DB

Run the script `test-bridge-crash-new-gateway-sync.sh`.

The script above starts all the services along with bridge server.
It then kills the bridge server and the database, adds a new gateway and verifies that it synced via redis.