# Endpoint discovery

Endpoint discovery is the automatic detection of services offered by a provider.

Discovered endpoints are automatically added / removed to / from the endpoints pool without overriding _manually defined_ endpoint(s).  

## Kubernetes endpoint-level discovery (v4 APIs)

Kubernetes endpoint-level discovery watches Kubernetes Endpoints directly and bypasses the Service routing layer. This enables advanced load-balancing strategies (session affinity, maglev, etc.) while keeping endpoint membership in sync with the cluster.

### Configure the discovery service

1. Create or edit a **v4 API**.
2. Go to **Proxy** > **Endpoints** and open the endpoint group that should use Kubernetes discovery.
3. Open the **Service discovery** tab.
4. Enable **Service discovery** and select **Kubernetes**.
5. Fill in the configuration:

- `namespace`: Kubernetes namespace to watch. Defaults to the current namespace; falls back to `default` if none is available.
- `service`: Kubernetes Service name to resolve endpoints from. This is required.
- `port`: Port to use when the service exposes multiple ports.
- `scheme`: Scheme to use when building endpoint targets (`http` or `https`). Default is `http`.
- `path`: Base path appended to each discovered endpoint target. Default is `/`.

### Verify the configuration

After saving, the endpoint group should include a `services.discovery` block similar to:

```json
{
  "services": {
    "discovery": {
      "enabled": true,
      "type": "kubernetes-service-discovery",
      "overrideConfiguration": false,
      "configuration": {
        "namespace": "default",
        "service": "demo",
        "port": "8080",
        "scheme": "http",
        "path": "/"
      }
    }
  }
}
```

Quick checks:

- The **Service discovery** tab appears only for v4 **HTTP Proxy** APIs.
- The Kubernetes option appears in the dropdown.
- Saving persists the configuration and it remains after a refresh.

### Permissions and runtime requirements

- The gateway must be able to reach the Kubernetes API and authenticate either via in-cluster service account or a local kubeconfig.
- The service account or user must be allowed to `get`, `list`, and `watch` `endpoints` in the target namespace.
