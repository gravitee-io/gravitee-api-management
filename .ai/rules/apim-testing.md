---
name: APIM Test Topology
layer: local
description: Where each kind of test lives in APIM (unit, integration, e2e, migration)
---

> Root-scoped: test placement spans the backend Maven modules and the web apps.

# APIM Test Topology

Where a test goes depends on its kind, and the kinds live in different places:

- **Unit** tests sit with the code: under each module's `src/test/` for Java, beside the
  component as `*.spec.ts` for Angular.
- **Integration** tests (cross-module, full-stack) go in the dedicated
  `gravitee-apim-integration-tests` module, not in the module under test.
- **End-to-end** tests go in `gravitee-apim-e2e`.
- **Migration / upgrader** tests follow the existing pattern: `.../upgrade/upgrader/*Test.java`
  in `gravitee-apim-repository`, and the `*MigrationTest.java` classes in
  `gravitee-apim-rest-api`.
- **Performance** tests go in `gravitee-apim-perf`.

The shared test stack and naming are in the Java and Angular conventions (JUnit 5, AssertJ,
Mockito, Testcontainers, `should_...` names; Angular component harnesses). Detailed
test-writing patterns, exemplars to copy, and anti-patterns to avoid are not here: the
`write-tests` skill loads them from its own directives when it runs, so they cost nothing on
tasks that are not about tests.
