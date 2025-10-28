# APIM with Kafka Console

Here is a docker-compose to run APIM with Kafka Console enabled.

## Run docker-compose

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

Docker compose will create the following services :
- `gio_apim_mongodb` : MongoDB database
- `gio_apim_elasticsearch` : Elasticsearch database
- `gio_apim_mailhog` : Mailhog service
- `gio_apim_gateway` : Famous Gravitee Gateway with Kafka enabled
- `gio_apim_management_api` : Gravitee Management Rest-API
- `gio_apim_management_ui` : Gravitee Management Console UI
- `gio_apim_portal_ui` : Gravitee Portal UI
- `gio_apim_kafka` : Kafka broker which must be accessible via Gravitee Gateway
- `gio_apim_kafka_console-ui` : Gravitee Kafka console

Docker volumes :
- `./.license` : License file
- `./.ssl` : Contains SSL certificates for this example

Up docker-compose :
`APIM_VERSION={APIM_VERSION} docker-compose up -d` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`
Or you can do `export APIM_REGISTRY=graviteeio.azurecr.io  && export APIM_VERSION=master-latest && export APIM_KAFKA_CONSOLE_VERSION=4.9.0-alpha.3 && docker-compose up -d` to use the latest version. but you need to have access to the internal registry.



# APIM Quick Setup with Kafka Console

In this example, we will create a Kafka Cluster and access it through Kafka Console.

## General Configuration

By default, Clusters management is hidden to basic users. For our example, we need to add the CLUSTER_READ permission at environment scope.
- Go to the console `http://localhost:8084/` (user: admin, password: admin) 
- Then: Organization > Roles > USER. 
- Check the READ box for the CLUSTER permission.

## Create Kafka Clusters
1. Go to the console `http://localhost:8084/` > Kafka Cluster > Add Cluster
2. Enter the name `cluster for api1` and for bootstrap server use `localhost:9091`. Save
3. Create a second cluster with the name `secured cluster for application1` and for bootstrap server use `localhost:9095`. Save
4. Open the second cluster, go to Configuration tab
5. Select  SASL_PLAINTEXT, then PLAIN and use `gravitee_user` and `gravitee_password`for credentials. Save
6. Go to User permissins tab. Add `application1` as a member (role USER). Save
7. Open the first cluster, and add `api1` (role USER). Save

## Going to Kafka Console
1. Go to the console `http://localhost:8084/` (user: admin, password: admin)
2. Go to Kafka Clusters
3. Click on "Open Kafka Console". You should see 2 clusters

4. Repeat steps 1 & 2 & 3 with `application1`. You should see only `secured cluster for application1`
5. Repeat steps 1 & 2 & 3 with `api1`. You should see only `cluster for api1`
