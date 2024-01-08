# TCP

Here is a docker-compose with a gateway exposing a TCP server on port 4082, allowing to deploy and consume TCP-proxy APIs.

## How to run ?

> ⚠️ TCP-proxy API feature is supported from Gravitee APIM 4.2.0 and latest.

`export APIM_REGISTRY={APIM_REGISTRY} && export APIM_VERSION={APIM_VERSION} && docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

### Architecture description

The deployed gateway is exposing:
- HTTP server to listen to HTTP-proxy API requests
  - port: `8082`
  - secured: `true`
  - keystore type: `self-signed`
- TCP server to listen to TCP-proxy API requests
  - port: `4082`
  - secured: `true`
  - keystore type: `self-signed`
  - sni: `true`

Both servers are configured with the same security level to have the same latency (caused by handshake) in both scenario.


### Metrics

```shell
# HTTP client metrics
- gravitee_services_metrics_include_http_client[0]=remote
- gravitee_services_metrics_exclude_http_client[0]=local
# Net client metrics
- gravitee_services_metrics_include_net_client[0]=remote
- gravitee_services_metrics_exclude_net_client[0]=local
```