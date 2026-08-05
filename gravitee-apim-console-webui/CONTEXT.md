# Console UI

The admin frontend for Gravitee API Management, backed by the rest-api Management API. Operators create/manage every rest-api-owned entity here — Api, Plan, Subscription, Application, API Product, Group, Role, Organization, Environment — plus platform-operational concerns (License, Cockpit, Alert, Audit, API Score, Cluster, Instance, Integration). Full definitions live in [rest-api/CONTEXT.md](../gravitee-apim-rest-api/CONTEXT.md).

## Language

No console-ui-exclusive domain concepts surfaced in this pass — everything it displays is Management API data typed into TS entities, not something console-ui itself owns. Revisit if a genuinely UI-owned concept turns up later (portal-next, by contrast, does own a few — see its CONTEXT.md).
