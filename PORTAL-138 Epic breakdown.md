# **PORTAL-138 Epic breakdown**

Oct 23, 2026 · @Marek Mazur

PORTAL-138 (Gamma Portal \- Multi-Dev Portal Config) splits into three epics delivered in order, 11 dev-weeks in total. Stories keep their numbers from the tech review; every story of 1 dev-week or more is split into technical stories estimated in dev-days (1 dev-week \= 5 dev-days).

## **Overview**

| Step | Epic | User stories | Dev-weeks |
| :---- | :---- | :---- | :---- |
| 1 | Portal-aware foundation | 1, 2, 5 | 4.5 |
| 2 | Multiple portals with shared settings | 3, 4, 7, 8, 9 | 4.5 |
| 3 | Per-portal settings and automation | 6, 10 | 2 |
|  | **Total** |  | **11** |

Proposed estimate changes, to be confirmed:

* Stories 7 and 8: 0.5 dev-week each, down from 1\. The homepage is a navigation item, so both only connect PORTAL-137 screens to the multi-portal API.  
* Story 2: 1.5 dev-weeks, up from 1\. It now also builds the Management API v2 multi-portal navigation endpoints that stories 7 and 8 use (technical story 2.3).

Moving technical story 3.2 (license limit) to step 3 would give 4.5 / 4 / 2.5 dev-weeks; to be confirmed with Product.

## **Step 1 — Portal-aware foundation**

**Goal:** everything runs on a portal model while customers still see exactly one portal. Every environment has a real default portal, and every navigation item, the homepage included, belongs to a portal after the migration. The next-gen portal loads a portal from portalId in its config, or the default portal when there is none, and makes all its calls through the multi-portal Portal API endpoints /portal/{portalId}/..., where navigation items and API search are limited to that portal. The existing Portal API endpoints keep working for the default portal.

**Done when:** an upgraded install looks exactly as before; a next-gen portal deployment with or without portalId shows the default portal's navigation and APIs through /portal/{portalId}/...; the Gamma dashboard lists the default portal.

**Depends on:** PORTAL-137 (dashboard).

### **Story 1 · Default portal created on upgrade — 2 dev-weeks**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 1.1 | Core model: remove PortalId.ZERO; default marker and public URL on Portal; repository fields with JDBC migration and Mongo mapping | 2 |
| 1.2 | Seed a default portal for new environments (CreateDefaultPortalUseCase, Cockpit environment hook) | 2 |
| 1.3 | Upgrader for existing environments: create the default portal and re-point everything on the zero UUID (portal row, navigation items' referenceId, portal listings, documentation); leave the navigation rootId sentinel untouched | 3 |
| 1.4 | Default portal URL from the environment portal URL setting; default portal id in the Portal API /ui/bootstrap response | 1 |
| 1.5 | Upgrade tests on JDBC and Mongo with existing navigation items, listings and pages | 2 |

### **Story 2 · Portal list on Gamma dashboard — 1.5 dev-weeks**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 2.1 | Management API v2 (OpenAPI first): list and get portals with name, URL, APIs published and creation date | 3 |
| 2.2 | Connect the PORTAL-137 dashboard to list and get: tile/row per portal, "View Portal" opens its URL, "Configure" opens its Overview page | 2 |
| 2.3 | Management API v2 multi-portal navigation endpoints (all areas, homepage included) for the Portal Designer and the homepage screen; existing endpoints act on the default portal | 2.5 |

### **Story 5 · Next-gen portal serves a specific portal — 1 dev-week**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 5.1 | Portal API multi-portal endpoints /portal/{portalId}/... for navigation items (all areas, homepage included) and their search; existing endpoints keep serving the default portal | 2 |
| 5.2 | Portal API multi-portal API search and API lists, limited to the portal's listings | 1.5 |
| 5.3 | Next-gen portal: optional portalId in assets/config.json, read by ConfigService, else the default portal id from /ui/bootstrap; all Portal API calls go through /portal/{portalId}/... | 1.5 |

## **Step 2 — Multiple portals with shared settings**

**Goal:** admins create several portals, each with its own homepage, navigation and API catalog, while all portals of an environment share the same theme, subscription forms and identity providers. When an API is published from its Portal tab in Gamma, the admin chooses which portals it goes to.

**Done when:** within the license limit, an admin creates portal B, gives it its own homepage, top navbar and APIs, publishes an API proxy to B only, and B shows them with the environment's theme; the default portal is unchanged.

**Depends on:** step 1; PORTAL-137 (New Portal, Portal Designer, homepage screen); PORTAL-140 (General settings, delete); licensing (portal limit per tier); the Portal tab of the API proxy in the Gamma APIM module, which is not on master yet (owning epic to be named).

### **Story 3 · Create portal within license limit — 1 dev-week**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 3.1 | Management API v2: create a portal with name and required URL; validation and exactly one default portal | 2.5 |
| 3.2 | Portal limit per license tier (trial 1, Planet 2, Galaxy 3, Universe 5, to be confirmed): checked on create in Management API v2 and the Automation API; quota usage in the list response | 2.5 |

### **Story 4 · Rename and delete portal — 0.5 dev-week**

Not split: Management API v2 update and delete; the default portal cannot be deleted; a deleted portal's content is removed. Screens from PORTAL-140.

### **Story 7 · Homepage per portal — 0.5 dev-week**

Not split. The homepage is a navigation item, so it is already portal-scoped after story 1's migration and served by the multi-portal endpoints from story 5\. What is left: connect the homepage screen (PORTAL-137) to the multi-portal API from technical story 2.3, and check that portal B's homepage is independent of the default portal's.

### **Story 8 · Top navbar and pages per portal — 0.5 dev-week**

Not split. Connect the Portal Designer (PORTAL-137) to the multi-portal API from technical story 2.3; navigation items are already portal-scoped after story 1\.

### **Story 9 · API catalog per portal — 2 dev-weeks**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 9.1 | Management API v2: publish an API to selected portals (per-portal PortalListing), and read which portals an API is published to | 3 |
| 9.2 | Gamma API proxy Portal tab: show and edit the portals the API is published to | 3 |
| 9.3 | Portal Designer API sections scoped by portal | 2 |
| 9.4 | Portal API: catalog and subscription from a non-default portal with applications shared across portals; E2E with two portals | 2 |

## **Step 3 — Per-portal settings and automation**

**Goal:** portals stop sharing settings: each portal gets its own theme, and portals can be managed as code through the Automation API, the Kubernetes operator and Terraform.

**Done when:** portal B shows its own theme while the default portal keeps the one it had; a portal created through the Automation API or GKO appears on the dashboard, counts towards the license limit and opens from "View Portal".

**Depends on:** step 2; PORTAL-137 (theme screen); GKO (CRDs, Terraform export). Per-portal identity providers are covered by [PORTAL-142](https://gravitee.atlassian.net/browse/PORTAL-142); per-portal subscription forms have no epic yet.

### **Story 6 · Theme per portal — 1 dev-week**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 6.1 | Resolve the theme from Portal.activeThemeId only; retire the environment-wide activation (CurrentThemeDomainService) | 2 |
| 6.2 | Upgrader: the default portal's activeThemeId comes from the environment's enabled theme | 1.5 |
| 6.3 | Management API v2 portal-scoped theme endpoint for the theme screen; existing theme endpoints act on the default portal | 1.5 |

Theme migration moved here from story 1: until this step, all portals use the environment's theme.

### **Story 10 · Manage portals via Automation API — 1 dev-week**

| \# | Technical story | Dev-days |
| :---- | :---- | :---- |
| 10.1 | Automation API (OpenAPI, mappers): default marker, public URL and active theme on portals; license limit on create | 3 |
| 10.2 | GKO CRDs and Terraform export mirror the new portal fields, with the GKO team | 2 |

The Automation API, CRDs and Terraform export are owned by @gravitee-io/gko; agree on 10.1 and 10.2 with them before the step starts.