# Authz Editable Schema (Phase 0) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generated, read-only GAPL schema with an **independent, opt-in, user-authored schema document** that has full CRUD (create/read/update/delete) and lives only in the authz module. Entities and policies keep working with or without a schema. No generation, no seed.

**Architecture:** A per-environment stored document (`authz_schemas`, `_id = environmentId`) behind a hexagonal port. The schema service becomes a thin CRUD wrapper over that store — the old generator and its `AuthzEntityRepository`/`AuthzPolicyRepository` deps are deleted. `invalidate` (which only busted the generated cache) is dead and is removed, including its 10 call sites and the `schemaService` injection into `AuthzEntityServiceImpl`/`AuthzPolicyServiceImpl` (which existed solely for it). The frontend `SchemaPage` gains create/edit/delete with client-side GAPL validation.

**Tech Stack:** Java 21, Spring manual `@Bean` wiring, Spring Data Mongo (`MongoOperations`), JUnit 5 + AssertJ + Mockito, Jersey resource tests (`AbstractAuthorizationResourceTest`). Frontend: React 19, TanStack Query, Monaco (`components/MonacoEditor`), Vitest + Testing Library.

**Module root (backend):** `gravitee-gamma/gravitee-gamma-module-authz/src/main/java/io/gravitee/gamma/authorization`
**Module root (frontend):** `gravitee-gamma/gravitee-gamma-module-authz/src/main/ui`

## Prerequisites

- **JDK 21** for backend tasks (`.sdkmanrc` → `21-tem`).
- **Frontend tasks need `node_modules`.** Fresh worktree off `origin/master` with none installed. Run a real `yarn install` (Node 22 per `.nvmrc`) at repo root so the branch's pinned `@gravitee/graphene-core` is used. A symlink to a sibling worktree's `node_modules` works for a quick run but can produce false test failures on version skew.

## Verified facts (from codebase + independent review)

- `currentGaplSchema(...)` has **exactly one caller**: `AuthzSchemaResource:51` (the GET). Everywhere else only `invalidate(...)` is called. Generation feeds nothing but the GET view → safe to delete.
- `invalidate(env)` call sites (10): `AuthzEntityServiceImpl` lines 115, 146, 258, 353; `AuthzPolicyServiceImpl` lines 86, 120, 150, 183, 247.
- `schemaService` is injected into `AuthzEntityServiceImpl` (field line 64, ctor) and `AuthzPolicyServiceImpl` (field 46, ctor) **only** to call `invalidate` — no other use.
- `TimeProvider.instantNow()` is the module clock accessor (used `AuthzEntityServiceImpl:166,183,254`); tests override via `TimeProvider.overrideClock(...)`.
- PUT pattern precedent: `AuthzPoliciesResource` uses `@PUT + @Consumes + @Valid`, permission `ENVIRONMENT_AUTHORIZATION.UPDATE`. FE client `put<T>(path, body)` exists (`authz-api-client.ts:61`).
- Existing tests to extend: `AuthzSchemaServiceImplTest`, `AuthzSchemaResourceTest`. `SchemaPage.test.tsx` mocks `useSchema` via `useSchemaMock` (~line 22) and a Monaco stub rendering `<pre>{value}</pre>`; it imports `render, screen, waitFor, userEvent` — **`fireEvent` is NOT imported**.

---

## Task 1: Schema storage layer

**Files:**
- Create: `api/AuthzSchemaRepository.java`
- Create: `infra/repository/document/AuthzSchemaMongo.java`
- Create: `infra/repository/MongoAuthzSchemaRepository.java`
- Create: `src/test/java/io/gravitee/gamma/authorization/repository/InMemoryAuthzSchemaRepository.java`
- Test: `src/test/java/io/gravitee/gamma/authorization/repository/InMemoryAuthzSchemaRepositoryTest.java`

- [ ] **Step 1: Port interface**

`api/AuthzSchemaRepository.java`:
```java
package io.gravitee.gamma.authorization.api;

import java.time.Instant;
import java.util.Optional;

public interface AuthzSchemaRepository {
    Optional<String> find(String environmentId);

    void save(String environmentId, String schemaText, Instant updatedAt);

    boolean delete(String environmentId);
}
```

- [ ] **Step 2: Mongo document**

`infra/repository/document/AuthzSchemaMongo.java`:
```java
package io.gravitee.gamma.authorization.infra.repository.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}authz_schemas")
public record AuthzSchemaMongo(@Id String environmentId, String schemaText, Instant updatedAt) {}
```

- [ ] **Step 3: Failing test for the in-memory double**

`InMemoryAuthzSchemaRepositoryTest.java`:
```java
package io.gravitee.gamma.authorization.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryAuthzSchemaRepositoryTest {

    @Test
    void find_returns_empty_when_nothing_saved() {
        assertThat(new InMemoryAuthzSchemaRepository().find("env-1")).isEmpty();
    }

    @Test
    void save_then_find_returns_latest_per_environment() {
        InMemoryAuthzSchemaRepository repo = new InMemoryAuthzSchemaRepository();
        repo.save("env-1", "entity User {};", Instant.parse("2026-06-04T00:00:00Z"));
        repo.save("env-1", "entity User {}; entity Group {};", Instant.parse("2026-06-04T01:00:00Z"));
        repo.save("env-2", "entity API {};", Instant.parse("2026-06-04T00:00:00Z"));
        assertThat(repo.find("env-1")).contains("entity User {}; entity Group {};");
        assertThat(repo.find("env-2")).contains("entity API {};");
    }

    @Test
    void delete_removes_the_schema() {
        InMemoryAuthzSchemaRepository repo = new InMemoryAuthzSchemaRepository();
        repo.save("env-1", "entity User {};", Instant.parse("2026-06-04T00:00:00Z"));
        assertThat(repo.delete("env-1")).isTrue();
        assertThat(repo.find("env-1")).isEmpty();
        assertThat(repo.delete("env-1")).isFalse();
    }
}
```

- [ ] **Step 4: Run, confirm failure**

Run: `cd gravitee-gamma/gravitee-gamma-module-authz && mvn -o -q surefire:test -Dtest=InMemoryAuthzSchemaRepositoryTest`
Expected: compile failure — class missing.

- [ ] **Step 5: Implement in-memory double**

`InMemoryAuthzSchemaRepository.java`:
```java
package io.gravitee.gamma.authorization.repository;

import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAuthzSchemaRepository implements AuthzSchemaRepository {

    private final ConcurrentMap<String, String> byEnv = new ConcurrentHashMap<>();

    @Override
    public Optional<String> find(String environmentId) {
        return Optional.ofNullable(byEnv.get(environmentId));
    }

    @Override
    public void save(String environmentId, String schemaText, Instant updatedAt) {
        byEnv.put(environmentId, schemaText);
    }

    @Override
    public boolean delete(String environmentId) {
        return byEnv.remove(environmentId) != null;
    }
}
```

- [ ] **Step 6: Implement Mongo repository**

`MongoAuthzSchemaRepository.java`:
```java
package io.gravitee.gamma.authorization.infra.repository;

import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import io.gravitee.gamma.authorization.infra.repository.document.AuthzSchemaMongo;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@RequiredArgsConstructor
public class MongoAuthzSchemaRepository implements AuthzSchemaRepository {

    private final MongoOperations mongo;

    @Override
    public Optional<String> find(String environmentId) {
        var query = new Query(Criteria.where("_id").is(environmentId));
        return Optional.ofNullable(mongo.findOne(query, AuthzSchemaMongo.class)).map(AuthzSchemaMongo::schemaText);
    }

    @Override
    public void save(String environmentId, String schemaText, Instant updatedAt) {
        mongo.save(new AuthzSchemaMongo(environmentId, schemaText, updatedAt));
    }

    @Override
    public boolean delete(String environmentId) {
        var query = new Query(Criteria.where("_id").is(environmentId));
        return mongo.remove(query, AuthzSchemaMongo.class).getDeletedCount() > 0;
    }
}
```

- [ ] **Step 7: Run test (green) + compile**

Run: `mvn -o -q surefire:test -Dtest=InMemoryAuthzSchemaRepositoryTest && mvn -o -q -DskipTests compile`
Expected: PASS + BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add gravitee-gamma/gravitee-gamma-module-authz/src/main/java/io/gravitee/gamma/authorization/api/AuthzSchemaRepository.java \
        gravitee-gamma/gravitee-gamma-module-authz/src/main/java/io/gravitee/gamma/authorization/infra/repository/document/AuthzSchemaMongo.java \
        gravitee-gamma/gravitee-gamma-module-authz/src/main/java/io/gravitee/gamma/authorization/infra/repository/MongoAuthzSchemaRepository.java \
        gravitee-gamma/gravitee-gamma-module-authz/src/test/java/io/gravitee/gamma/authorization/repository/InMemoryAuthzSchemaRepository.java \
        gravitee-gamma/gravitee-gamma-module-authz/src/test/java/io/gravitee/gamma/authorization/repository/InMemoryAuthzSchemaRepositoryTest.java
git commit -m "feat(authz): add AuthzSchemaRepository (port + mongo + in-memory)"
```

---

## Task 2: Decommission `invalidate` and drop `schemaService` from entity/policy services

`invalidate` only busted the generated cache that Task 3 deletes. Remove its calls and the now-pointless `schemaService` dependency from both services.

**Files:**
- Modify: `service/AuthzEntityServiceImpl.java`, `service/AuthzPolicyServiceImpl.java`
- Modify: `config/SpringConfig.java`
- Modify: all tests constructing those services.

- [ ] **Step 1: Delete the 10 `invalidate` calls**

Remove every line `schemaService.invalidate(...)` in `AuthzEntityServiceImpl` (4) and `AuthzPolicyServiceImpl` (6).

- [ ] **Step 2: Remove the `schemaService` field + constructor param from both services**

In each: delete the `private final AuthzSchemaAdminApi schemaService;` field, the constructor parameter, and the `this.schemaService = schemaService;` assignment. Remove the now-unused `import ...AuthzSchemaAdminApi;`. (In `AuthzEntityServiceImpl` the param appears in both the public ctor and the delegating ctor at line ~77 — update both, and the `this(...)` delegation arg list.)

- [ ] **Step 3: Update Spring wiring**

In `SpringConfig.java`, the `entityService` and `policyService` beans construct these services with `schemaService` — drop that argument from both bean bodies (and the bean method params if `schemaService` is no longer otherwise needed there).

- [ ] **Step 4: Find and update all test call sites**

Run: `grep -rn "new AuthzEntityServiceImpl(\|new AuthzPolicyServiceImpl(" gravitee-gamma/gravitee-gamma-module-authz/src/test`
For each match, delete the `schemaService` argument. Affected files include `AuthzEntityServiceImplTest`, `AuthzPolicyServiceImplTest`, `AuthzAuditEmissionTest` — update every occurrence the grep returns. The local `AuthzSchemaServiceImpl schemaService = ...` lines in those tests can stay for now (removed/rewritten in Task 3) but are no longer passed to the entity/policy services.

- [ ] **Step 5: Compile + run affected suites**

Run: `mvn -o -q test-compile && mvn -o -q surefire:test -Dtest='AuthzEntityServiceImplTest,AuthzPolicyServiceImplTest,AuthzAuditEmissionTest'`
Expected: BUILD SUCCESS + PASS (behaviour unchanged; `invalidate` was side-effect-free for users).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(authz): drop dead schema-cache invalidate from entity/policy services"
```

---

## Task 3: Schema service becomes CRUD (delete generation)

**Files:**
- Modify: `api/AuthzSchemaAdminApi.java`
- Rewrite: `service/AuthzSchemaServiceImpl.java`
- Modify: `rest/resource/AuthzSchemaResource.java` (GET only, this task)
- Modify: `config/SpringConfig.java`
- Rewrite: `src/test/java/io/gravitee/gamma/authorization/service/AuthzSchemaServiceImplTest.java`

- [ ] **Step 1: New port**

`api/AuthzSchemaAdminApi.java`:
```java
package io.gravitee.gamma.authorization.api;

import java.util.Optional;

public interface AuthzSchemaAdminApi {
    Optional<String> getSchema(String environmentId);

    void saveSchema(String environmentId, String schemaText);

    boolean deleteSchema(String environmentId);
}
```

- [ ] **Step 2: Rewrite the failing service test**

Replace the body of `AuthzSchemaServiceImplTest.java`:
```java
package io.gravitee.gamma.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.authorization.repository.InMemoryAuthzSchemaRepository;
import org.junit.jupiter.api.Test;

class AuthzSchemaServiceImplTest {

    @Test
    void getSchema_is_empty_when_nothing_stored() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThat(service.getSchema("env-1")).isEmpty();
    }

    @Test
    void saveSchema_then_getSchema_returns_it() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        service.saveSchema("env-1", "entity Edited {};");
        assertThat(service.getSchema("env-1")).contains("entity Edited {};");
    }

    @Test
    void deleteSchema_removes_it() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        service.saveSchema("env-1", "entity Edited {};");
        assertThat(service.deleteSchema("env-1")).isTrue();
        assertThat(service.getSchema("env-1")).isEmpty();
    }
}
```

- [ ] **Step 3: Run, confirm failure**

Run: `mvn -o -q surefire:test -Dtest=AuthzSchemaServiceImplTest`
Expected: compile failure — old constructor/methods gone.

- [ ] **Step 4: Rewrite the service**

`service/AuthzSchemaServiceImpl.java` (delete generation, cache, entity/policy deps entirely):
```java
package io.gravitee.gamma.authorization.service;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import java.util.Objects;
import java.util.Optional;

public class AuthzSchemaServiceImpl implements AuthzSchemaAdminApi {

    private final AuthzSchemaRepository schemaRepository;

    public AuthzSchemaServiceImpl(AuthzSchemaRepository schemaRepository) {
        this.schemaRepository = Objects.requireNonNull(schemaRepository, "schemaRepository must not be null");
    }

    @Override
    public Optional<String> getSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        return schemaRepository.find(environmentId);
    }

    @Override
    public void saveSchema(String environmentId, String schemaText) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(schemaText, "schemaText must not be null");
        schemaRepository.save(environmentId, schemaText, TimeProvider.instantNow());
    }

    @Override
    public boolean deleteSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        return schemaRepository.delete(environmentId);
    }
}
```

- [ ] **Step 5: Update the REST GET to use `getSchema`**

In `AuthzSchemaResource.java`, change `currentSchema()` and delete the stale read-only TODO comment block:
```java
@GET
@Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
public AuthzSchemaResponse currentSchema() {
    return AuthzCalls.execute(() ->
        new AuthzSchemaResponse(schemaService.getSchema(GraviteeContext.getCurrentEnvironment()).orElse("")));
}
```

- [ ] **Step 6: Update Spring wiring**

In `SpringConfig.java`: add a repository bean and change the schema service bean to take only it.
```java
@Bean
public AuthzSchemaRepository authzSchemaRepository(@Qualifier("managementMongoTemplate") MongoOperations mongoOperations) {
    return new MongoAuthzSchemaRepository(mongoOperations);
}

@Bean
public AuthzSchemaAdminApi schemaService(@Lazy @Qualifier("authzSchemaRepository") AuthzSchemaRepository schemaRepository) {
    return new AuthzSchemaServiceImpl(schemaRepository);
}
```
Add imports for `AuthzSchemaRepository` and `MongoAuthzSchemaRepository`; remove the now-unused `AuthzEntityRepository`/`AuthzPolicyRepository` params from the schema bean (they may still be used by other beans — only remove from this one).

- [ ] **Step 7: Run service test + compile**

Run: `mvn -o -q surefire:test -Dtest=AuthzSchemaServiceImplTest && mvn -o -q -DskipTests compile`
Expected: PASS + BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(authz): schema service is CRUD over a store, generation removed"
```

---

## Task 4: REST `PUT` + `DELETE`

**Files:**
- Create: `rest/dto/AuthzSchemaRequest.java`
- Modify: `rest/resource/AuthzSchemaResource.java`
- Modify: `src/test/java/io/gravitee/gamma/authorization/rest/resource/AuthzSchemaResourceTest.java`

- [ ] **Step 1: Request DTO**

`rest/dto/AuthzSchemaRequest.java`:
```java
package io.gravitee.gamma.authorization.rest.dto;

import jakarta.validation.constraints.NotNull;

public record AuthzSchemaRequest(@NotNull String schema) {}
```

- [ ] **Step 2: Failing resource tests**

Add to `AuthzSchemaResourceTest.java` (mock field `schemaService` exists on `AbstractAuthorizationResourceTest`):
```java
@Test
void put_saves_schema_and_returns_200() {
    when(schemaService.getSchema(any())).thenReturn(java.util.Optional.of("entity Edited {};"));
    try (Response response = target("/schema").request().put(jakarta.ws.rs.client.Entity.json(new AuthzSchemaRequest("entity Edited {};")))) {
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(AuthzSchemaResponse.class).schema()).isEqualTo("entity Edited {};");
    }
    verify(schemaService).saveSchema(any(), eq("entity Edited {};"));
}

@Test
void delete_removes_schema_and_returns_204() {
    when(schemaService.deleteSchema(any())).thenReturn(true);
    try (Response response = target("/schema").request().delete()) {
        assertThat(response.getStatus()).isEqualTo(204);
    }
    verify(schemaService).deleteSchema(any());
}
```
Add imports: `io.gravitee.gamma.authorization.rest.dto.AuthzSchemaRequest`, `io.gravitee.gamma.authorization.rest.dto.AuthzSchemaResponse`, `jakarta.ws.rs.core.Response`, `static org.mockito.Mockito.verify`, `static org.mockito.Mockito.when`, `static org.mockito.ArgumentMatchers.any`, `static org.mockito.ArgumentMatchers.eq`.

- [ ] **Step 3: Run, confirm failure**

Run: `mvn -o -q surefire:test -Dtest=AuthzSchemaResourceTest`
Expected: FAIL — no PUT/DELETE handlers.

- [ ] **Step 4: Add handlers to `AuthzSchemaResource`**

```java
@PUT
@Consumes(MediaType.APPLICATION_JSON)
@Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
public AuthzSchemaResponse updateSchema(@Valid AuthzSchemaRequest request) {
    return AuthzCalls.execute(() -> {
        String env = GraviteeContext.getCurrentEnvironment();
        schemaService.saveSchema(env, request.schema());
        return new AuthzSchemaResponse(schemaService.getSchema(env).orElse(""));
    });
}

@DELETE
@Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
public Response deleteSchema() {
    return AuthzCalls.execute(() -> {
        schemaService.deleteSchema(GraviteeContext.getCurrentEnvironment());
        return Response.noContent().build();
    });
}
```
Add imports: `io.gravitee.gamma.authorization.rest.dto.AuthzSchemaRequest`, `jakarta.validation.Valid`, `jakarta.ws.rs.Consumes`, `jakarta.ws.rs.PUT`, `jakarta.ws.rs.DELETE`, `jakarta.ws.rs.core.Response`. (Verify `AuthzCalls.execute` has an overload usable with a `Response` return; it accepts `Callable<T>`, so `Response` is fine.)

- [ ] **Step 5: Run, confirm green**

Run: `mvn -o -q surefire:test -Dtest=AuthzSchemaResourceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(authz): PUT + DELETE /schema endpoints"
```

---

## Task 5: Frontend schema service calls

**Files:**
- Modify: `shared/api/authz-api.service.ts`

- [ ] **Step 1: Add `updateSchema` + `deleteSchema`**

Next to `getSchema`:
```ts
updateSchema: async (environmentId: string, schemaText: string): Promise<SchemaResponse> => {
    const c = await authzCoreApiClient.put<CanonicalSchema>(corePath(environmentId, '/schema'), { schema: schemaText });
    return { environmentId, schemaText: c.schema ?? '', updatedAt: null };
},

deleteSchema: (environmentId: string): Promise<void> => authzCoreApiClient.delete<void>(corePath(environmentId, '/schema')),
```

- [ ] **Step 2: Type-check**

Run: `cd gravitee-gamma/gravitee-gamma-module-authz && ../../node_modules/.bin/tsc -p tsconfig.app.json --noEmit`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/shared/api/authz-api.service.ts
git commit -m "feat(authz-ui): add updateSchema/deleteSchema API calls"
```

---

## Task 6: Frontend save-gating helper (pure, TDD)

**Files:**
- Create: `features/policy-structure/schema-validation.ts`
- Test: `features/policy-structure/__tests__/schema-validation.test.ts`

- [ ] **Step 1: Failing test**

`schema-validation.test.ts`:
```ts
import { describe, expect, it } from 'vitest';
import { schemaDiagnostics } from '../schema-validation';

describe('schemaDiagnostics', () => {
    it('returns no diagnostics for a valid schema', () => {
        expect(schemaDiagnostics('entity User {};')).toEqual([]);
    });
    it('flags an action missing appliesTo', () => {
        expect(schemaDiagnostics('action "x" { principal: [User] };').length).toBeGreaterThan(0);
    });
    it('treats blank as a diagnostic', () => {
        expect(schemaDiagnostics('   ').length).toBeGreaterThan(0);
    });
});
```

- [ ] **Step 2: Run, confirm failure**

Run: `../../node_modules/.bin/vitest run schema-validation`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement**

`schema-validation.ts`:
```ts
import { parseGaplSchema } from '../../shared/gapl-parser';

export function schemaDiagnostics(schemaText: string): readonly string[] {
    if (schemaText.trim() === '') return ['Schema must not be empty.'];
    return parseGaplSchema(schemaText).diagnostics;
}
```

- [ ] **Step 4: Run, confirm green**

Run: `../../node_modules/.bin/vitest run schema-validation`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/features/policy-structure/schema-validation.ts \
        gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/features/policy-structure/__tests__/schema-validation.test.ts
git commit -m "feat(authz-ui): schemaDiagnostics save-gating helper"
```

---

## Task 7: Frontend mutation hooks

**Files:**
- Create: `shared/hooks/useUpdateSchema.ts`
- Create: `shared/hooks/useDeleteSchema.ts`
- Test: `shared/hooks/__tests__/useUpdateSchema.test.tsx`

- [ ] **Step 1: Failing test (update hook)**

`useUpdateSchema.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authzApiService } from '../../api/authz-api.service';
import { authzQueryKeys } from '../../api/query-keys';
import { useUpdateSchema } from '../useUpdateSchema';

function makeWrapper(client: QueryClient) {
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useUpdateSchema', () => {
    beforeEach(() => vi.restoreAllMocks());

    it('saves and invalidates the schema query', async () => {
        const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const invalidate = vi.spyOn(client, 'invalidateQueries');
        vi.spyOn(authzApiService, 'updateSchema').mockResolvedValue({ environmentId: 'env-1', schemaText: 'x', updatedAt: null });

        const { result } = renderHook(() => useUpdateSchema('env-1'), { wrapper: makeWrapper(client) });
        result.current.mutate('entity Edited {};');

        await waitFor(() => expect(authzApiService.updateSchema).toHaveBeenCalledWith('env-1', 'entity Edited {};'));
        await waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: authzQueryKeys.schema('env-1') }));
    });
});
```

- [ ] **Step 2: Run, confirm failure**

Run: `../../node_modules/.bin/vitest run useUpdateSchema`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement both hooks**

`useUpdateSchema.ts`:
```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authzApiService } from '../api/authz-api.service';
import { authzQueryKeys } from '../api/query-keys';

export function useUpdateSchema(environmentId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (schemaText: string) => authzApiService.updateSchema(environmentId, schemaText),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: authzQueryKeys.schema(environmentId) }),
    });
}
```
`useDeleteSchema.ts`:
```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authzApiService } from '../api/authz-api.service';
import { authzQueryKeys } from '../api/query-keys';

export function useDeleteSchema(environmentId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => authzApiService.deleteSchema(environmentId),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: authzQueryKeys.schema(environmentId) }),
    });
}
```

- [ ] **Step 4: Run, confirm green**

Run: `../../node_modules/.bin/vitest run useUpdateSchema`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/shared/hooks/useUpdateSchema.ts \
        gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/shared/hooks/useDeleteSchema.ts \
        gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/shared/hooks/__tests__/useUpdateSchema.test.tsx
git commit -m "feat(authz-ui): useUpdateSchema/useDeleteSchema mutation hooks"
```

---

## Task 8: SchemaPage — create / edit / delete

**Files:**
- Modify: `features/policy-structure/SchemaPage.tsx`
- Test: `features/policy-structure/__tests__/SchemaPage.test.tsx`

Behaviour:
- **No schema** (empty/notFound): show a "Create schema" button → enters edit mode with an empty draft.
- **Schema exists**: "Edit" → edit mode (draft = current text); "Delete" → `useDeleteSchema().mutate()`.
- **Edit mode**: Monaco editable (`readOnly={false}`, `onChange` updates draft); "Save" disabled while `schemaDiagnostics(draft)` non-empty or mutation pending; "Cancel" exits. Save → `useUpdateSchema().mutate(draft, { onSuccess: exit })`.
- Drop the "This view is read-only." copy.

- [ ] **Step 1: Failing tests**

In `SchemaPage.test.tsx`: add `fireEvent` to the `@testing-library/react` import (currently absent). Add mocks for the two hooks and tests:
```tsx
const mutateMock = vi.fn();
const deleteMock = vi.fn();
vi.mock('../../../shared/hooks/useUpdateSchema', () => ({ useUpdateSchema: () => ({ mutate: mutateMock, isPending: false }) }));
vi.mock('../../../shared/hooks/useDeleteSchema', () => ({ useDeleteSchema: () => ({ mutate: deleteMock, isPending: false }) }));

it('edits and saves a valid schema', () => {
    useSchemaMock.mockReturnValue({ schema: { environmentId: 'e', schemaText: 'entity User {};', updatedAt: null }, notFound: false, isLoading: false, error: undefined });
    render(<SchemaPage />);
    fireEvent.click(screen.getByRole('button', { name: /edit/i }));
    fireEvent.click(screen.getByRole('button', { name: /save/i }));
    expect(mutateMock).toHaveBeenCalledWith('entity User {};', expect.anything());
});

it('disables save when the draft has diagnostics', () => {
    useSchemaMock.mockReturnValue({ schema: { environmentId: 'e', schemaText: 'action "x" { principal: [User] };', updatedAt: null }, notFound: false, isLoading: false, error: undefined });
    render(<SchemaPage />);
    fireEvent.click(screen.getByRole('button', { name: /edit/i }));
    expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
});

it('offers create when no schema exists', () => {
    useSchemaMock.mockReturnValue({ schema: null, notFound: true, isLoading: false, error: undefined });
    render(<SchemaPage />);
    expect(screen.getByRole('button', { name: /create schema/i })).toBeInTheDocument();
});
```
(`mutate` is called with `(draft, { onSuccess })` — assert the 2nd arg with `expect.anything()`.)

- [ ] **Step 2: Run, confirm failure**

Run: `../../node_modules/.bin/vitest run SchemaPage`
Expected: FAIL — no Edit/Save/Create buttons.

- [ ] **Step 3: Implement edit mode in `SchemaPage.tsx`**

Add imports:
```tsx
import { useUpdateSchema } from '../../shared/hooks/useUpdateSchema';
import { useDeleteSchema } from '../../shared/hooks/useDeleteSchema';
import { schemaDiagnostics } from './schema-validation';
```
In the component:
```tsx
const update = useUpdateSchema(environmentId);
const remove = useDeleteSchema(environmentId);
const [editing, setEditing] = useState(false);
const [draft, setDraft] = useState('');
const draftDiagnostics = useMemo(() => (editing ? schemaDiagnostics(draft) : []), [editing, draft]);

function startEdit() { setDraft(schemaText); setEditing(true); }
function startCreate() { setDraft(''); setEditing(true); }
function save() { update.mutate(draft, { onSuccess: () => setEditing(false) }); }
```
In the empty state block, add a Create button calling `startCreate`. In the `schema.gapl` tab header add controls:
```tsx
{!editing ? (
    <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={startEdit}>Edit</Button>
        <Button variant="ghost" size="sm" onClick={() => remove.mutate()} disabled={remove.isPending}>Delete</Button>
    </div>
) : (
    <div className="flex gap-2">
        <Button variant="ghost" size="sm" onClick={() => setEditing(false)}>Cancel</Button>
        <Button size="sm" onClick={save} disabled={draftDiagnostics.length > 0 || update.isPending}>Save</Button>
    </div>
)}
```
Editor in the code tab:
```tsx
<MonacoEditor
    value={editing ? draft : schemaText}
    onChange={editing ? setDraft : undefined}
    readOnly={!editing}
    height={560}
    ariaLabel={editing ? 'Schema definition (editing)' : 'Schema definition (read-only)'}
/>
```
When `editing`, render `draftDiagnostics` in the existing destructive `Alert` (reuse the existing diagnostics list markup, sourced from `draftDiagnostics`). When the empty state is showing and the user clicked Create, render the editor (editing=true) instead of the Empty block. Remove "This view is read-only." from the header copy.

- [ ] **Step 4: Run, confirm green**

Run: `../../node_modules/.bin/vitest run SchemaPage`
Expected: PASS (existing + 3 new).

- [ ] **Step 5: Full module suite**

Run: `../../node_modules/.bin/vitest run`
Expected: all green (use the branch's own `node_modules` install for a trustworthy result — see Prerequisites).

- [ ] **Step 6: Commit**

```bash
git add gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/features/policy-structure/SchemaPage.tsx \
        gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/features/policy-structure/__tests__/SchemaPage.test.tsx
git commit -m "feat(authz-ui): create/edit/delete schema in SchemaPage"
```

---

## Notes / deferred (NOT in this plan)

- **Behaviour change:** removing generation means domains lose the auto-generated read-only schema view; the schema is blank until created. Intended (schema is opt-in).
- **Backend GAPL parser** — deferred; Phase 0 validates client-side and stores as-is (`@NotNull` only on the server).
- **Schema↔entity consistency** (drift warnings/blocks) — deferred; Phase 0 neither warns nor blocks.
- **Canonical naming / `entityType` population / FE parsing-tower removal** — Phases 1–2, separate plans. With generation gone, `typeNameFor` and its `Mcp` spelling are already deleted here.
- **`entity-types.ts` (`getEntityCategoryId`) in SchemaPage** still classifies parsed entity names for the outline/KPIs by name heuristics — unchanged in Phase 0; revisit when the registry/schema becomes the classification source (Phase 2).
