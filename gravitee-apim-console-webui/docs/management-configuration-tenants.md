# Multi-tenancy

Multi-tenancy let you manage APIs across multiple business entities, such as countries, areas, data centers, etc...
Tenants maximize resource sharing for a single API deployment.

For example:

* You have a cluster of 2 gateway instances, one in ASIA region and another in EUROPE region.
* You have an API with 2 endpoints, one in ASIA region and another in EUROPE region.

To minimize cost, you can set a *europe* tenant to the EUROPE Gateway instance and the EUROPE API's endpoint and the same thing applies for the ASIA region with an *asia* tenant.

You can create a new shard tag by clicking on the *New tenant* input and press *SAVE* button. A tenant's ID will be automatically created.

To make the association between API endpoints and Gateway instances,
you must choose this tenant in the API endpoints configuration page and copy/paste the tenant's ID to the `gravitee.yml` file of the gateway instance:

```yaml
# Multi-tenant configuration
# Allow only a single-value
# tenant: europe
```
