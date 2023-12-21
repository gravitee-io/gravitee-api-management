# TCP

Here is a docker-compose with a gateway exposing a TCP server on port 4082, allowing to deploy and consume TCP-proxy APIs.

## How to run ?

> ⚠️ TCP-proxy API feature is supported from Gravitee APIM 4.2.0 and latest.

> ⚠️ You need a license file with 'Sharding tags' feature enabled to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.  

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

### Architecture description

This docker-compose deploys two gateways:

- `gateway` service is the unsecured gateway
  - it acts as a regular gateway listening HTTP calls on port `8082`
  - it also declares a TCP server listening calls on port `4082`. It is configured with the basic configuration to enable TCP proxy API requests.
- `gateway_secured` service is the gateway secured with mTLS
  - it is configured with sharding tags `secured`
  - it listens for HTTP call on port `8081` (outside the docker network)

### Configure tags

Don't forget to add the `secured` sharding tag in "Organization > Sharding Tags" to have the provided API deployed on the secured gateway.

### Import APIs

You can use provided APIs as example to run your tests:

- [Secured HTTP API](.resources/Secured-HTTP-API-1.json): It is a classical API targeting `https://api.gravitee.io/echo` and configured with sharding tag `secured` to be deployed on the `gateway_secured`
- [TCP API](.resources/TCP-1.json): It is the API listening for TCP requests.
  - It listens call on host `localhost` and `my-tcp-api`. You can call them with
    - `curl --insecure https://localhost:4082/secured-http-v2`
    - `curl --insecure --resolve my-tcp-api:4082:0.0.0.0 https://my-tcp-api:4082/secured-http-v2`
  - It defines a target backend with this configuration:
    - host: `gateway_secured`
    - port: `8082` (port inside docker network)
    - secured: `true`
    - SSL: configured to use truststore and keystore

## How to generate the certificates

### For the Certification Authority PEM
```
openssl req -newkey rsa:4096 -keyform PEM -keyout ca.key -x509 -days 3650 -subj "/emailAddress=contact@graviteesource.com/CN=localhost/OU=GraviteeSource/O=GraviteeSource/L=Lille/ST=France/C=FR" -passout pass:ca-secret -outform PEM -out ca.pem
```

### For the Certification Authority PKCS12
```
openssl pkcs12 -export -inkey ca.key -in ca.pem -out ca.p12 -passin pass:ca-secret -passout pass:ca-secret -name ca
```

### For the server certificate PKCS12
```
openssl genrsa -out server.key 4096
openssl req -new -key server.key -out server.csr -sha256 -subj "/emailAddress=contact@graviteesource.com/CN=localhost/OU=APIM/O=GraviteeSource/L=Lille/ST=France/C=FR"
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -set_serial 100 -extensions server -days 1460 -outform PEM -out server.cer -sha256 -passin pass:ca-secret
openssl pkcs12 -export -inkey server.key -in server.cer -out server.p12 -passout pass:server-secret -name server
```

### For the client certificate PKCS12
```
openssl genrsa -out client.key 4096
openssl req -new -key client.key -out client.csr -subj "/emailAddress=contact@graviteesource.com/CN=test/OU=Local/O=GraviteeSource/L=Lille/ST=France/C=FR"
openssl x509 -req -in client.csr -CA ca.pem -CAkey ca.key -set_serial 101 -extensions client -days 365 -outform PEM -out client.cer -passin pass:ca-secret
openssl pkcs12 -export -inkey client.key -in client.cer -out client.p12 -passout pass:client-secret -name client
```