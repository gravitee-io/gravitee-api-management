# Quick APIM setup

You will find here a collection of docker-compose to easily test _complex_ installations of APIM.

## Using the Makefile

### Get help

Run `make` or `make help` to see contextual help

### Run a docker-compose

Simply use `make` followed by the target you want (see in `help`).

For example: `make tags-internal-external`.

Default version of the docker images is `nightly`. You can use a specific version with: `make tags-internal-external APIM_VERSION=3.9.0`.

### Stop a docker-compose

You can use `stop` to stop all the components of a running docker-compose or only specific services.

You have to specify the `TARGET` parameter to indicate which docker-compose to stop. TARGET is the name of the folder containing the docker-compose, also listed in `make help`

For example: `make stop TARGET=tags-internal-external`.

You may want to stop only one service (see the name directly in the related docker-compose). To do that, simply use the `SERVICES` parameter.

For example, to stop elasticsearch for tags-internal-external: `make stop TARGET=tags-internal-external SERVICES=elasticsearch`

### Start a docker-compose

You can use `start` to stop all the components of a running docker-compose or only specific services.

You have to specify the `TARGET` parameter to indicate which docker-compose to start. TARGET is the name of the folder containing the docker-compose, also listed in `make help`

For example: `make start TARGET=tags-internal-external`.

You may want to start only one service (see the name directly in the related docker-compose). To do that, simply use the `SERVICES` parameter.

For example, to start elasticsearch for tags-internal-external: `make start TARGET=tags-internal-external SERVICES=elasticsearch`

### Down a docker-compose

You can use `down` to stop all the components of a running docker-compose or only specific services.

You have to specify the `TARGET` parameter to indicate which docker-compose to down. TARGET is the name of the folder containing the docker-compose, also listed in `make help`

For example: `make down TARGET=tags-internal-external`.

> To know exactly the consequences of the `down`, please read https://docs.docker.com/compose/reference/down/