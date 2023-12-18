# TCP

Here is a docker-compose with a gateway exposing a TCP server on port 4082, allowing to deploy and consume TCP-proxy APIs.

## How to run ?

⚠️ TCP-proxy API feature is supported from Gravitee APIM 4.2.0 and latest.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

