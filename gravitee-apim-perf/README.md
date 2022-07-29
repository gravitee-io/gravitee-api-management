# Gravitee.io APIM - performance

This folder contains a performance tools of Gravitee.io API Management.

They are based on k6 and can be run against a locally running APIM Rest API.

## Prerequisites
- 
- [nvm](https://github.com/nvm-sh/nvm)
- [k6](https://k6.io/docs/getting-started/installation)
- [NodeJS](https://nodejs.org/en/download/)
- [Golang](https://go.dev/dl/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Yarn](https://yarnpkg.com/getting-started/install) (optional)

Use with `nvm use` or install with `nvm install` the version of Node.js declared in `.nvmrc`

## Installation

### Install dependencies

```bash
$ yarn install
```

### Build k6 cli

In order to run k6 and collect metrics in the same time (see Collecting metrics), you'll need to build a specific k6 client that includes the prometheus metrics exporter.

```shell
go install go.k6.io/xk6/cmd/xk6@latest
xk6 build --output ./bin/k6 --with github.com/grafana/xk6-output-prometheus-remote@latest
```

---
**NOTE**

If the `xk6` command is not found, make sure you have properly exported you GO path in your `.zshrc` or `.bashrc` file:

```bash
export GOPATH="$HOME/go"
export PATH="$GOPATH/bin:$PATH"
```

---

## Collecting the metrics

### Activate gateway metrics

To properly collect all the gateway's metrics, it is required to enable metrics prometheus endpoint:

```bash
gravitee.services.metrics.enabled=true
```

Once the gateway has started, you can verify that metrics are properly enabled by typing the following:

```bash
curl -H "Authorization: Basic YWRtaW46YWRtaW5hZG1pbg==" http://localhost:18082/_node/metrics/prometheus
```

---
**NOTE**

If you have changed the default admin password, make sure to reflect the changes into the config/prometheus.yaml file to allow scrapping of metrics by prometheus.

---

### The stack

Metrics collection and visualization is based on grafana. A `docker-compose` file is there to quickly run the whole metrics stack.
To start the whole stack, just type the following command:

```shell
cd ./grafana
docker compose up -d
```

The metrics stack is composed with the following tools:
- grafana: dashboard to visualize the metrics (http://localhost:3001)
- prometheus: to collect all the metrics under the hood. (You can query prometheus directly from http://localhost:9090/graph)
- tempo: to collect all the traces if you wish to enable apim tracing

When starting the stack for the first time, Grafana is automatically configured with the proper datasource and Apim dashboard and ready to be used.

Also, prometheus is automatically configured to scrap the gateway metrics every second.

### Apim performance dashboard

Once the grafana stack is up and running, you can start visualize the Apim gateway metric directly through the following url: http://localhost:3001/d/evKJrjgVk/apim-performances?orgId=1

## Running the test

1. Start an APIM instance with `gravitee_services_sync_delay=1000` on Gateway.
2. Check the configuration on `.env` file.
3. Write your test at root of `src` folder, here `src/get-200-status.setup.ts`;
4. Call the runner:

```bash
$ ./scripts/test-runner.ts  -f src/get-200-status-nosetup.test.js
```

## Writing own tests

House rules for writing tests:
- The test code is located in `src` folder

