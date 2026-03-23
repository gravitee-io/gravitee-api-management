# JWT Profile for OAuth2 — Quick Setup

Validates the `jwtProfileOauth2` webhook authentication flow (RFC 7523 — JWT Client Authentication).

**Stack:** APIM · Keycloak 26.2 · WireMock (mock webhook subscriber) · MongoDB · Elasticsearch

> **Security note:** `private_key.pem` and `public_key.pem` are throwaway demo keys generated solely for
> this local docker-compose stack. They are pre-loaded into the Keycloak realm config and have no value
> outside this setup. Do not use them in any real environment.

## What this tests

The APIM Gateway fetches an OAuth2 access token from Keycloak by authenticating with a
**signed JWT assertion** (RFC 7523 client authentication):

```
POST /realms/jwt-test/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
&client_assertion=<signed JWT>
```

The pre-loaded Keycloak realm has a client `apim-webhook-client` configured with:
- Authentication method: **Signed JWT** (`client-jwt` authenticator)
- Public key: embedded from `public_key.pem`

The private key from `private_key.pem` is what you configure in the APIM subscription.

---

## Start the stack

```bash
cd docker/quick-setup/jwt-profile-keycloak
mkdir -p .license && docker compose up -d
```

Wait ~60s for all services to be healthy:

```bash
docker compose ps
docker compose logs keycloak --tail=20
```

**Endpoints:**
| Service        | URL                                   |
|----------------|---------------------------------------|
| APIM Console   | http://localhost:8084                 |
| APIM Gateway   | http://localhost:8082                 |
| APIM Mgmt API  | http://localhost:8083                 |
| Keycloak Admin | http://localhost:8080 (admin / admin) |
| WireMock       | http://localhost:8090                 |

Default APIM credentials: `admin` / `admin`

---

## Verify Keycloak is ready

```bash
curl -s http://localhost:8080/realms/jwt-test/.well-known/openid-configuration | python3 -m json.tool | grep token_endpoint
```

Expected: `"token_endpoint": "http://localhost:8080/realms/jwt-test/protocol/openid-connect/token"`

---

## Create a Push API with JWT Profile OAuth2 webhook subscription

### 1. Create a v4 Push API

In the APIM Console (http://localhost:8084), create a new API:
- Type: **Event Native**
- Entrypoint: **Webhook**
- Endpoint: **Mock** (message content: `Hello JWT`, interval: 1000ms)

### 2. Create a Plan + Subscription

Create a **Push plan** and subscribe with a webhook application.

### 3. Configure the subscription with JWT Profile OAuth2

In the subscription consumer configuration, set:

| Field               | Value                                                                                |
|---------------------|--------------------------------------------------------------------------------------|
| Auth type           | `JWT Profile for OAuth2`                                                             |
| Token endpoint URL  | `http://keycloak:8080/realms/jwt-test/protocol/openid-connect/token`                 |
| Issuer (`iss`)      | `apim-webhook-client`                                                                |
| Subject (`sub`)     | `apim-webhook-client`                                                                |
| Audience (`aud`)    | `http://keycloak:8080/realms/jwt-test/protocol/openid-connect/token`                 |
| Signature algorithm | `RSA_RS256`                                                                          |
| Key source          | `PEM`                                                                                |
| Key content         | *(absolute path to `private_key.pem`, e.g. `/path/to/docker/quick-setup/jwt-profile-keycloak/private_key.pem`)* |
| Expiration time     | `30` SECONDS                                                                         |
| Callback URL        | `http://wiremock:8080/webhook`                                                       |

> **Note:** Token endpoint and callback URL use Docker service hostnames (`keycloak`, `wiremock`), not `localhost`.

---

## Manually verify the token flow

Use `private_key.pem` to build and send a JWT Bearer assertion directly to Keycloak:

```bash
python3 - <<'EOF'
import jwt, time, requests, json
from cryptography.hazmat.primitives.serialization import load_pem_private_key

with open("private_key.pem", "rb") as f:
    private_key = load_pem_private_key(f.read(), password=None)

token_endpoint = "http://localhost:8080/realms/jwt-test/protocol/openid-connect/token"
now = int(time.time())

assertion = jwt.encode({
    "iss": "apim-webhook-client",
    "sub": "apim-webhook-client",
    "aud": token_endpoint,
    "iat": now,
    "exp": now + 30,
    "jti": f"test-{now}"
}, private_key, algorithm="RS256")

resp = requests.post(token_endpoint, data={
    "grant_type": "client_credentials",
    "client_assertion_type": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
    "client_assertion": assertion
})
print(f"Status: {resp.status_code}")
print(json.dumps(resp.json(), indent=2))
EOF
```

Expected: `200` with `{"access_token": "...", "token_type": "Bearer", ...}`

---

## Verify the webhook flow

After the subscription is active, check WireMock received calls with a valid Bearer token:

```bash
curl -s "http://localhost:8090/__admin/requests?limit=10" | python3 -c "
import json, sys
reqs = json.load(sys.stdin)['requests']
for r in reqs:
    print(r['request']['method'], r['request']['url'], '->', r['response']['status'])
    auth = r['request']['headers'].get('Authorization', '')
    if auth:
        print('  Auth:', auth[:50], '...')
"
```

A `200` response with `Authorization: Bearer <token>` confirms the JWT Profile OAuth2 flow is working.

**If you see 401:** check gateway logs:

```bash
docker compose logs gateway --tail=50 | grep -i "oauth\|jwt\|token\|auth"
```

---

## Stop the stack

```bash
docker compose down -v
```
