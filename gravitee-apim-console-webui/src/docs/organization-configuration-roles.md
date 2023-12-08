# Roles

A role can be scoped to an API, an application, the portal or more globally to the platform management.

Each role defines a set of permissions (READ, CREATE, UPDATE, DELETE (CRUD)) on the API Management resources.

A role can be edited by clicking on the *edit* button on each role scope:

* ORGANIZATION role

CRUD operations on: USER, ENVIRONMENT, ROLE.

* ENVIRONMENT role

CRUD operations on: INSTANCE, GROUP, SHARDING TAG, TENANT, API, APPLICATION, AUDIT, PLATFORM, NOTIFICATION, MESSAGE, DICTIONARY, ALERT, ENTRYPOINTS, SETTINGS, DASHBOARD, QUALITY RULES, METADATA, DOCUMENTATION, VIEW, TOP APIS, API HEADERS, IDENTITY PROVIDERS, CLIENT REGISTRATION PROVIDER, PORTAL THEME.

* API role

CRUD operations on: DEFINITION, GATEWAY_DEFINITION, PLAN, SUBSCRIPTION, MEMBER, METADATA, ANALYTICS, EVENT, HEALTH, LOG, DOCUMENTATION, RATINGS, RATINGS ANSWERS, AUSIT, DISCOVERY, NOTIFICATION, MESSAGE, ALERT, RESPONSE TEMPLATES, REVIEWS, QUALITY RULES.

* APPLICATION role

CRUD operations on: DEFINITION, MEMBER, ANALYTICS, LOG, SUBSCRIPTION, NOTIFICATION, ALERT.

## Note

Granting a GROUP permission for MANAGEMENT role will also require the READ operation for the ROLE permission in order to see which roles are provided by a group.
