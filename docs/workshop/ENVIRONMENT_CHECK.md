# Environment Check Results

**Date:** 2026-03-02
**Branch:** `workshop/v4-analytics-base`

## Summary

| # | Check | Status | Details |
|---|-------|--------|---------|
| 1 | Java version is 21.x | PASS | `openjdk 21.0.8` (Temurin via SDKMAN at `~/.sdkman/candidates/java/21.0.8-tem`) |
| 2 | Maven version is 3.9+ | PASS | `Apache Maven 3.9.12` |
| 3 | Node.js version is 20+ | PASS | `v24.7.0` |
| 4 | Maven compile (`gravitee-apim-rest-api`) | PASS | Compiles cleanly after `mvn license:format` |
| 5 | Console UI `node_modules` installed | PASS | `gravitee-apim-console-webui/node_modules/` exists |
| 6a | `ApiAnalyticsResource.java` (backend) | PASS | Found in v1 and v2 REST modules |
| 6b | `ApiAnalyticsProxyComponent` (frontend) | PASS | Found at `api-traffic-v4/analytics/api-analytics-proxy/` |
| 6c | `ApiAnalyticsV2Service` (frontend service) | PASS | Found at `services-ngx/api-analytics-v2.service.ts` |
| 7 | `ApiAnalyticsResourceTest` passes | PASS | All tests pass, 0 failures |

**Overall: ALL CHECKS PASSED**

---

## Notes

### JAVA_HOME must be set explicitly

The default `java` on PATH (`/usr/bin/java`) is broken (macOS stub with no JRE installed). Maven picks up Homebrew OpenJDK 25 by default which causes issues. Always use the SDKMAN-managed Java 21:

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current
export PATH=$JAVA_HOME/bin:$PATH
```

Or source SDKMAN in your shell:
```bash
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 21.0.8-tem
```

### License headers

New files without license headers will fail the `license-maven-plugin` check during compile. Fix with:

```bash
mvn license:format
```

### Key Maven invocation pattern

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current
export PATH=$JAVA_HOME/bin:$PATH
mvn compile -pl gravitee-apim-rest-api -am -q
```

---

## Key File Locations

### Backend
- **V1 REST Resource:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/src/main/java/io/gravitee/rest/api/management/rest/resource/ApiAnalyticsResource.java`
- **V2 REST Resource:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`

### Frontend
- **Proxy Component:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
- **Analytics Service:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts`
