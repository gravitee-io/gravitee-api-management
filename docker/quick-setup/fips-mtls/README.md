# APIM on the FIPS images, with an mTLS plan

Brings up APIM on the `chainguard-fips` images and drives one API behind an mTLS plan, end to
end. Its purpose is reproduction: when something breaks under FIPS, start here rather than
rebuilding a stack by hand.

Two things make a FIPS deployment different, and both are wired in on purpose:

- **The keystore is PEM.** Under BC-FIPS *approved only*, PKCS12 key derivation has no provider
  and JKS is answered read-only, so PEM is the only format that loads.
- **The backend images are `-chainguard-fips`.** They register BC-FIPS as provider 1 and drop
  SunJCE and SunJSSE, which is what surfaces crypto problems the ordinary images never show. The
  console stays on `-chainguard`: it is a static nginx, with no JVM crypto involved.

## Run it

The FIPS images are published to the private registry only, so log in first.

```sh
docker login graviteeio.azurecr.io                       # or: az acr login -n graviteeio
mkdir -p .license && cp /path/to/license.key .license/   # an EE licence is required for mTLS plans
./generate-certificates.sh
docker compose up -d
./setup-mtls-api.sh                                      # waits for the management API on its own
```

`setup-mtls-api.sh` creates the API, publishes an mTLS plan, registers the client certificate on
an application, subscribes it and starts the API. It prints the call to make.

## Check it

```sh
curl -k --cert .certificates/client.crt --key .certificates/client.key https://localhost:8082/fips-mtls   # 200
curl -k https://localhost:8082/fips-mtls                                                                  # 401
```

A certificate that is not registered on the application is rejected during the handshake, with
`certificate unknown` — the gateway never sees the request.

Console on <http://localhost:8084> (`admin` / `admin`), management API on
<http://localhost:8083>, Elasticsearch on <http://localhost:9200> for the analytics.

## Teardown

```sh
docker compose down -v && rm -rf .certificates
```
