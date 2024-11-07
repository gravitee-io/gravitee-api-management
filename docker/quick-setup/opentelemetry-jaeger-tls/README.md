
# OpenTelemetry With Jaeger

This docker-compose allows you to run APIM with OpenTelemetry activated and Jaeger as a tracer.

You can call your APIs through your gateway classically (for example: `http://localhost:8082/myapi`). 

## How To Run This Docker Compose 

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

Be sure to fetch last version of images by running this command: 
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

## How To See Your Traces 

Jaeger comes with a helpful, user-friendly UI that allows you to see your calls. To access this UI, visit http://localhost:16686.

Then, select **gio_apim_gateway** in the _Service_ list and click on the _Find Traces_ button.

## Generate the TLS certificates 

There is a script to generate the TLS certificates `./generate-certs.sh`, if certificates have expired you can regenerate them by running the script.
