# Multi-DCR Competitive Analysis: Gravitee vs Kong vs Tyk

## Overview

This document compares how Gravitee, Kong Konnect, and Tyk handle Dynamic Client Registration (DCR) for developer portal applications, and recommends an architecture for multi-DCR support in Gravitee.

---

## 1. Gravitee (Current State)

### Architecture

- **Scope:** Single DCR provider per environment
- **Configuration level:** Environment settings
- **Developer choice:** None -- all applications use the same provider

### How It Works

1. Admin configures one `ClientRegistrationProvider` per environment via Console UI (Settings > Client Registration).
2. The provider stores an OIDC discovery endpoint, credentials (initial access token or client credentials), and optional TLS settings.
3. When a developer creates an application with an OAuth type (Browser, Web, Native, Backend-to-Backend), the system calls `ClientRegistrationService.register()`.
4. `register()` retrieves all providers for the environment and picks the first one (`providers.iterator().next()`).
5. The DCR response (`client_id`, `client_secret`, `registration_access_token`) is stored in application metadata.
6. Applications do **not** store which provider was used.

### Key Limitations

| Limitation | Impact |
|---|---|
| Only one provider per environment (enforced at creation) | Cannot support multiple IdPs |
| No provider reference stored in application metadata | Cannot route updates/renewals to the correct provider |
| No per-API or per-plan provider selection | All APIs in the environment share the same IdP |
| No developer choice | Developers cannot pick which IdP to register with |

### Pros

- Simple to configure and understand
- No ambiguity in provider selection
- Works well for single-IdP organizations

### Cons

- Cannot support multiple IdPs (e.g., Okta for external, Keycloak for internal)
- Cannot migrate between IdPs gradually
- No multi-tenant or multi-business-unit support within an environment
- No per-API granularity for authentication requirements

---

## 2. Kong Konnect

### Architecture

- **Scope:** Two-tier model -- DCR Providers + Authentication Strategies
- **Configuration level:** Per API / per API package (via strategy assignment)
- **Developer choice:** Yes -- developers select strategy when creating an application

### How It Works

1. Admin creates **DCR Providers** representing raw IdP connections (Okta, Auth0, Curity, Azure, Kong Identity). Each defines the auth server URL and provider type.
2. Admin creates **Authentication Strategies** that reference a DCR provider and add policy-level settings: scopes, credential claims, auth methods (client_credentials, bearer, session), auto-approve, etc.
3. Multiple strategies can share the same provider with different configurations (e.g., "okta-readonly" with read scope, "okta-readwrite" with read+write scopes).
4. Strategies are applied **per API** or **per API package** when publishing to a Dev Portal.
5. A **default auth strategy** can be set at the Dev Portal level.
6. When a developer creates an application, they select an auth strategy. One application can serve multiple APIs only if those APIs share the same strategy.

### Key Design Decisions

| Decision | Rationale |
|---|---|
| Separate provider from strategy | Reuse one IdP connection with different policies |
| Strategy assigned at API level | Different APIs can enforce different IdPs/policies |
| One strategy per application | Simplifies credential management |
| Support for custom HTTP DCR bridge | Extend to non-natively-supported IdPs |

### Supported Strategy Types

- **Key Auth**: Built-in key authentication (no DCR)
- **OpenID Connect (Self-managed)**: Developer brings their own pre-registered client_id
- **DCR**: Automatic client registration with supported IdPs

### Pros

- Most flexible model of the three
- Separation of infrastructure (provider) from policy (strategy) enables fine-grained access control
- Per-API strategy assignment allows different APIs to use different IdPs
- Reusable strategies reduce configuration duplication
- HTTP DCR bridge pattern supports custom/unsupported IdPs
- Developer has choice of strategy

### Cons

- More complex configuration (two layers of abstraction)
- Developers must understand which strategy to use
- One auth strategy per application means multiple applications needed for different strategies
- Requires Kong Konnect (SaaS) -- not available for self-hosted Kong OSS

---

## 3. Tyk

### Architecture

- **Scope:** Provider-centric model -- DCR configured at the API Provider level
- **Configuration level:** Per API Product (implicitly, via Provider binding)
- **Developer choice:** None -- dictated by the API Product's provider

### How It Works

1. Admin creates **Providers** representing connections to Tyk Dashboard instances. Each provider can be in a different region, environment, or team.
2. DCR is configured per provider: identity provider connection, initial access token, OAuth scopes.
3. Each **API Product** is tied to a single Provider.
4. Since the DCR configuration lives on the Provider, and each API Product uses one Provider, DCR is effectively per API Product.
5. OAuth scopes are configured per API Product/Plan combination for access control and rate limiting.
6. The same authentication method must be used for all APIs within an API Product.
7. When developers request access, credentials are issued through the provider's DCR flow.

### Key Design Decisions

| Decision | Rationale |
|---|---|
| Provider = Tyk Dashboard instance | Multi-region/multi-env infrastructure mapping |
| Each API Product bound to one Provider | Credentials are provider-specific |
| No developer choice of provider | Provider is dictated by the API Product |
| Multi-provider for infrastructure, not IdP choice | Designed for geographic/organizational separation |

### Pros

- Natural fit for multi-region / multi-environment setups
- API Product-level granularity
- Clean separation between portal (presentation) and provider (access control)
- Supports multiple popular IdPs (Okta, Keycloak, Curity, Gluu)

### Cons

- Confusing conceptual model -- "Provider" means Tyk Dashboard instance, not IdP
- Developers don't choose the DCR provider; it is dictated by the API Product
- Credentials from one Provider can't be used with APIs from another Provider
- Less flexible than Kong's strategy model -- no reuse of provider configs with different policies
- Primarily designed for infrastructure topology, not for multi-IdP within a single environment
- Requires Tyk Enterprise for full DCR support

---

## Summary Comparison

| Capability | Gravitee | Kong Konnect | Tyk |
|---|---|---|---|
| **DCR assignment granularity** | Environment | Per API (via strategy) | Per API Product (via provider) |
| **Multiple IdPs per environment** | No | Yes | Yes (via multiple providers) |
| **Developer choice of provider** | No | Yes (select strategy) | No (implicit via product) |
| **Reusable configurations** | N/A | Yes (strategies) | No |
| **Configuration layers** | 1 (provider) | 2 (provider + strategy) | 1 (provider) |
| **Custom IdP support** | Any OIDC-compliant | HTTP DCR bridge | Via guides |
| **Self-hosted availability** | Yes | No (Konnect SaaS only) | Enterprise only |
| **Auth method flexibility** | Per provider | Per strategy | Per provider |

---

## Recommendation

Adopt a two-tier model inspired by Kong's approach but adapted for Gravitee's architecture:

1. **Keep `ClientRegistrationProvider`** as the raw IdP connection layer (environment-scoped, multiple allowed)
2. **Introduce `AuthenticationStrategy`** as the policy layer that references a provider and adds scopes, auth methods, credential claims, etc.
3. **Assign strategies at the Plan level** for OAuth2/JWT plans, with a default strategy at the environment level
4. **Store strategy reference in application metadata** so updates/renewals route to the correct provider
5. **Migrate existing single-provider setups** automatically via data migration

This gives Gravitee the flexibility of Kong's model while remaining self-hosted and backward-compatible.
