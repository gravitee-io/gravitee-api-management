# Gravitee.io APIM - Performance

This folder contains a performance tools of Gravitee.io API Management.

They are based on k6 and can be run against a locally running APIM Rest API.

## Prerequisites

- [nvm](https://github.com/nvm-sh/nvm)
- [k6](https://k6.io/docs/getting-started/installation)
- [NodeJS](https://nodejs.org/en/download/)
- [Golang](https://go.dev/dl/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [jq](https://stedolan.github.io/jq/download/)
- [Yarn](https://yarnpkg.com/getting-started/install) (optional)

Use with `nvm use` or install with `nvm install` the version of Node.js declared in `.nvmrc`

## Installation

### Install dependencies

```bash
$ yarn install # install what is needed to compile the file generated for the test cases

```

### Build k6 cli

We need to install k6 cli to run our tests.

---
**NOTE**

Please use at least K6 0.42.0 to support 'experimental-prometheus-rw'  output mode

---

```shell
brew install k6
```

## Collecting the metrics

### Activate gateway metrics

To properly collect all the gateway's metrics, it is required to enable metrics prometheus endpoint:

```bash
gravitee.services.metrics.enabled=true
gravitee.services.metrics.include.http.client[0]=remote
gravitee.services.metrics.exclude.http.client[0]=local
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

#### Grafana

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

### Generate PDF report

Go to: http://localhost:8686/api/v5/report/evKJrjgVk?orgId=1&from=1660884060000&to=1660890060000, which will output the grafana-reporter PDF of the dashboard `evKJrjgVk` on the time interval specified with the parameters `from` and `to`.

#### Local environment

It is possible to run the scenario without a grafana to report metrics.

To do so, override [dotenv file](./.env)'s variable `K6_OUTPUT_MODE` with `csv` or `json` (other values are: `cloud`, `influxdb`, `statsd`, `experimental-prometheus-rw`)

## Recommanded Gateway Configuration

To be able to run the scenario in the exact same condition, you can run your gateway with those java options:

```
-Xms256m
-Xmx256m
-XX:MaxMetaspaceSize=128m
-XX:CompressedClassSpaceSize=48m
-XX:ReservedCodeCacheSize=32m
-XX:+UseStringDeduplication
-XX:MaxTenuringThreshold=1
-XX:+ParallelRefProcEnabled
-XX:InitiatingHeapOccupancyPercent=25
-Xss256k
```

## Running the test

1. Start an APIM (Rest API and Gateway) instance with `gravitee_services_sync_delay=1000` on Gateway. It allows to have a quick synchronisation when deploying an API.
2. Check the configuration on [config.json](scripts/config.json) file. It will override settings from [environment.ts](src/env/environment.ts)
3. Write your test in [src/scenarions](src/scenarios) folder
4. Build your tests package with `yarn build`. (Needed after each modification on a scenario as tests are run from `dist`). Or, you can run the following with `-d` flag.
5. Call the runner:

```bash
$ ./scripts/run.sh  -f ./dist/src/scenarios/keyless-api-body-policies/keyless.v3.test.js 
```

---
**⚠️ WARNING**

[config.json](scripts/config.json) is merged with [environment.ts](src/env/environment.ts), but it **is not a deep copy**.
That means complex objects from `config.json` will override the one from `environment.ts`

---

### Options

`test-runner.ts` comes with some options:

| Option      	 | Description                      	                                                                                                                  | Required |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| -f       	    | The <test-name>.test.js file to run	                                                                                                                | X        |
| -d	           | Developer mode. Build the projects before running a script.           	                                                                             |          |
| -v            | K6 Verbose mode. Enable --verbose and --http-debug on k6 command                                                                                    |          |
| -r            | Prometheus Remote URL. Useful when reporting to prometheus. If not provided, use the one from config.json"	                                         |          |
| -o            | K6 output mode. If not provided, use the one from config.json. Possible values are 'cloud, csv, experimental-prometheus-rw, influxdb, json, statsd' |          |
| -h            | Display help	                                                                                                                                       |          |


## Writing a test

Tests are written in [Typescript](https://www.typescriptlang.org/).

A test will have this structure [see more on K6](https://k6.io/docs/using-k6/test-lifecycle/):

- a `setup()` function, called once by K6 to prepare your test. (Creates some APIs, deploy them, etc.). Optional
- a `default` function, called on each iteration by k6.
- a `teardown()` function, called once by K6 to do some actions after the test is executed. (Remove APis, plans, etc.). Optional

House rules for writing tests:
- The test code is located in [src/scenarios](src/scenarios) folder
- Test file run by K6 should be written in this format: `<testName>.test.ts`

---
**💡GOOD TO KNOW**

On the `setup()` and `teardown()` phases, we use the default `k6/http` client. That means stats are reported to prometheus.
It's usual to see `INFO[0001] Error: [GET] [http://localhost:8082/bmNPjEPYwn-1675413556] returned HTTP 404  source=console
` at the beginning of a test because we call the gateway several times to check if API is available.

Notice that client methods from [src/lib/clients](src/lib/clients) uses a special param `tags: { name : OUT_OF_SCENARIO},` which will allow filtering in prometheus to avoid false data.

![img.png](doc/img.png)

---

## Docker 

A docker image that embeds all the simulations can be created.
```bash
$ docker build -t apim-k6-runner .
```

Once the image is available, you can run a simulation by providing the path as parameter (relative to the src/scenario directory).
```bash
docker run --rm apim-k6-runner get-200-status-nosetup.test.js
docker run --rm apim-k6-runner keyless-api-no-policy/keyless.v4.test.js
```

You can also run it in interaction mode to be able to update the config.json file and run the simulation with different parameters.
```bash
docker run -it --rm apim-k6-runner 
~ $ ./scripts/run.sh -v -f dist/src/scenarios/get-200-status-nosetup.test.js
```
