# Quick APIM setup

You will find here a collection of docker-compose to easily test installations of APIM with mqtt5 endpoint.

## HiveMQ exposed ports

The Hivemq docker container exposes different ports : 

  * **1883** (1883 internally): the MQTT Broker port
  * **8086** (8080 internally)': the HiveMQ Control Center
  * **8007** (8000 internally)': the Websockets access

## Using the Makefile

You will also need to log in to our internal Docker registry in order to get the latest version in development of APIM:

```bash
az acr login -n graviteeio.azurecr.io
```

### Get help

Run `make` or `make help` to see contextual help

### Targeting a specific APIM version

Default docker images come from the `graviteeio.azurecr.io` registy with the `master-latest` version.
You can change them thanks to the `.env` file.

### Running apim + hivemq

Simply use `make` followed by the target you want (see in `help`).

First time requires to run a `make all`:
```bash
make all
```

You can then stop and start the stack any time you want.

Start:
```bash
make start-all
```
Stop:
```bash
make stop-all
```

### Running hivemq only

If you want to only run the hivemq broker without apim (because you deploy it in another way), you can use the `make hivemq` command instead.

First time requires to run a `make hivemq`:
```bash
make hivemq
```

You can then stop and start the hivemq broker any time you want.

Start:
```bash
make start-hivemq
```

Stop:
```bash
make stop-hivemq
```

## Using scripts

There are additionnal python script that could be used to test the containers and the behaviour of the endpoint.

You will need first to execute in order to install python library for mqtt:
```bash
pip3 install paho-mqtt
```

Then if you want to subscribe to MQTT Broker execute the following:

```bash
python3 scripts/subscribe.py
```

and to start publishing messages every second:

```bash
python3 scripts/publish.py
```

_Note_: By default, both scripts are using the following configuration:
```
broker = 'localhost'
port = 1883
topic = "python/mqtt"
```