# Local directives: write-tests

Repo-specific guidance the write-tests skill loads at runtime. It does not repeat the always-on
rules (test topology, naming, real-objects-over-mocks); it adds only what the skill needs and
cannot cheaply detect.

## Scope shortcuts

| Alias | Production | Tests |
| --- | --- | --- |
| automation mapper | `gravitee-apim-rest-api/.../automation/mapper/` | same tree under `src/test/` |
| gateway policy | `gravitee-apim-gateway/.../policy/` | co-located `src/test/` |
| console component | `gravitee-apim-console-webui/src/**` | co-located `*.spec.ts` |

## Exemplars to mirror

- Java mapper / resource test:
  `gravitee-apim-rest-api/gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest/src/test/java/io/gravitee/apim/rest/api/automation/mapper/ApiMapperTest.java`
- Angular harness spec:
  `gravitee-apim-console-webui/src/organization/configuration/roles/org-settings-role-members.component.spec.ts`

## Verify before finishing (APIM anti-patterns to avoid)

- Java: no `Thread.sleep`; do not mock a domain object you could construct; no generic
  `assertThat(x).isNotNull()` where a behavioural assertion is possible.
- Angular: no `document.querySelector` / `querySelectorAll` in specs (use a component harness);
  no `fixture.detectChanges()` loops standing in for `await`.
- Run Java tests from the parent module (`mvn -f <module>/pom.xml test -pl <submodule> -am`);
  run `mvn prettier:write -pl <module>` first.
