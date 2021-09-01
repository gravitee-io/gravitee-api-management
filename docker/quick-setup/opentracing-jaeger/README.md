
# OpenTracing With Jaeger

This docker-compose allows you to run APIM with OpenTracing activated and Jaeger as a tracer.

You can call your APIs through your gateway classically (for example: `http://localhost:8082/myapi`). 

## How To Run This Docker Compose 

⚠️ 1. Since the Jaeger tracer is not bundled by default, **you must download the zip file** related to the version you want to run. [Click here](https://download.gravitee.io/#graviteeio-apim/plugins/tracers/gravitee-tracer-jaeger/) to download the .ZIP.

2. Next, **copy the .ZIP file into `opentracing-jaeger/.plugins` directory** using the command below:

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

3. Be sure to fetch last version of images by running this command: 
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

## How To See Your Traces 

Jaeger comes with a helpful, user-friendly UI that allows you to see your calls. To access this UI, visit http://localhost:16686.

Then, select **gio_apim_gateway** in the _Service_ list and click on the _Find Traces_ button.

![Search from in Jaeger UI](assets/jaeger_search.png)
