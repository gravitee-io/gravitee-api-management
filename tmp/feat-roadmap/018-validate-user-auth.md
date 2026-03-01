# 018 - Validate User Authorization in RAG Queries

## Goal

Ensure that the `RagContextStrategy` implementations strictly adhere to the current user's Role-Based Access Control (RBAC) permissions to prevent data leakage.

## Description

When the Zee agent performs tenant-specific RAG (e.g., retrieving existing flows, APIs, and policies to build context), it currently scopes queries to the `envId` and `orgId`.

However, we must verify that the underlying `CrudService` or `QueryService` calls automatically apply the authenticated user's permissions, or if we need to explicitly pass the user's context/roles.
If a user only has permission to view APIs in a specific group, the RAG context must **not** inject flows or API definitions from groups they cannot access.

## Checklist

- [ ] Deep dive into the existing Gravitee APIM application auth approach (Spring Security, `@PreAuthorize` annotations, `PermissionService`).
- [ ] Audit `FlowRagContextAdapter` and any other RAG adapters to confirm how they retrieve data.
- [ ] Verify if the REST API execution context automatically filters results at the DAO/Service layer based on the logged-in user.
- [ ] If filtering is not automatic, refactor the RAG adapters to explicitly validate permissions or filter the results using the `PermissionService`.
- [ ] Write integration tests simulating users with different RBAC roles to guarantee no out-of-scope context is leaked to the LLM.
