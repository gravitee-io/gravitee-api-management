# Monitoring with Prometheus

Here is a docker-compose to run APIM with one gateway and Prometheus scrapping enabled.

To test the **Prometheus scrapping endpoint**, you can call, for example, `http://localhost:18092/_node/metrics/prometheus` to see values.

---
> For more information, please read this doc:
> https://documentation.gravitee.io/apim/getting-started/configuration/apim-gateway/logging#expose-metrics-to-prometheus
---

## How to run ?

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

## Prometheus Dashboard

Here is a quick access to access the Prometheus dashboard to see the JVM memory consumption:

http://localhost:9090/graph?g0.expr=jvm_memory_max_bytes&g0.tab=0&g0.stacked=0&g0.show_exemplars=0&g0.range_input=1h
