# Opentracing with jaeger

Here is a docker-compose to run APIM with Opentracing activated and jaeger as a tracer.

You can classically call your apis through your gateway, for example: `http://localhost:8082/myapi`.

## How to run ?

⚠️ As Jaeger tracer is not bundled by default, do not forget to download the zip file related to the version you want to run.

The zip is downloadable here: https://download.gravitee.io/#graviteeio-apim/plugins/tracers/gravitee-tracer-jaeger/

Then you have to copy it into `opentracing-jaeger/.plugins` directory

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

## And how to see my traces ?

Jaeger comes with an UI, that will allow you to see your calls.

To access this UI, browse to http://localhost:16686.

Then select **gio_apim_gateway** in the _Service_ list and click on the _Find Traces_ button.

![Search from in Jaeger UI](assets/jaeger_search.png)