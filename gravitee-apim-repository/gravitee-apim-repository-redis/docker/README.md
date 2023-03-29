# Quick APIM setup

You will find here a collection of docker-compose to easily test installations of APIM with Redis rate limit.

## Generate SSL key and certificates

All certificates required to test the different ssl scenarios are pre-generated and located in the config/ssl folder.

There is now need to regenerate them. If you still need to regenerate certificates you can run the following command:

```bash
./redis-regenerate-certificates.sh
```

You will be prompted to regenerate Redis server certificate and client certificate.

## Redis exposed ports

Redis is configured to expose one port with TLS activated (and mTLS) :

 * **SSL + client certificate** (6379): secured (tls) with client certificate and no user authentication.

## Gateway and Redis TLS (aka ssl) and MTLS

The gateway is configured to use redis for its ratelimit policy, so all you need to do is create an API, add a ratelimit policy and deploy it. The Redis client will connect to the Redis server using TLS and store the information needed to apply your ratelimit policy. One way to validate quickly is to configure the `Max requests (static)` with a low number and the `Time duration` with at least 10s. Then just call your api more than two times and you will get this kind of message :

```json
{"message":"Rate limit exceeded ! You reach the limit of 2 requests per 10 seconds","http_status_code":429}
```

## Using the Makefile

You will also need to login to our internal Docker registry in order to get the latest version in development of APIM:

```bash
az acr login -n graviteeio.azurecr.io
```

### Get help

Run `make` or `make help` to see contextual help

### Targeting a specific APIM version

Default docker images come from the `graviteeio.azurecr.io` registry with the `master-latest` version.
You can change them thanks to the `.env` file.

### Running apim + redis

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

### Running redis only

If you want to only run the redis server without apim (because you deploy it in another way), you can use the `make redis` command instead.

First time requires to run a `make redis`:
```bash
make redis
```

You can then stop and start the redis server any time you want.

Start:
```bash
make start-redis
```

Stop:
```bash
make stop-redis
```