# Identity provider

Identity provider is mainly based on OAuth2 / OIDC protocols.

## Configuration

This part is providing a way to configure access to the authorization server. Depending on the type of identity 
provider, some advanced configuration may be mandatory. 

## User Profile

When a user is logged into Gravitee.io from an external identity provider / social provider, we must retrieve the 
information of the authenticated user and store them (partially) in the Gravitee.io database.

The user profile mapping enabled you to configure the user's properties from Gravitee and the associated value 
(attribute / claim) from the user_info profile

## Groups Mapping   

Groups mapping allows you to automatically associate a user to a group according to a given condition (supports EL).

Example:
```
{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}
```

## Roles Mapping   

Roles mapping allows you to automatically associate role to the user according to a given condition (supports EL).

Example:
```
{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}
```
