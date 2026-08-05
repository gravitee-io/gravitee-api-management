# Portal Next

The developer portal — a self-service frontend for API *consumers*, backed by the rest-api Portal API. No admin/management surface; consumers browse a catalog of published Apis, subscribe Applications to Plans, and manage their own Applications/Subscriptions. Distinct from console-ui (admin-facing).

## Language

**Portal Navigation Item**:
A node in the portal's own content/navigation tree — typed `PAGE`, `FOLDER`, `LINK`, or `API`, scoped to an area (`HOMEPAGE`, `TOP_NAVBAR`). Drives the homepage and documentation nav. Not a rest-api concept — owned entirely by the portal.

**Portal Configuration**:
Portal-only settings fetched once per Environment (site title, homepage banner, catalog view mode, application-membership toggles). Distinct from the Environment entity itself, which is rest-api's concern.

**Application Invitation**:
An invite to join membership on an Application, initiated from the portal. Tied to Application membership, not part of rest-api's core Application model.
