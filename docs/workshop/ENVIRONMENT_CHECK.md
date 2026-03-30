# Environment Check — V4 Analytics Workshop

Date: 2026-03-30  
Repo: `gravitee-api-management`

---

## Results (pass/fail)

1. **Java version is 21.x**: **PASS**
   - `openjdk version "21.0.6" 2025-01-21 LTS`

2. **Maven version is 3.9+**: **PASS**
   - `Apache Maven 3.9.9`

3. **Node.js version is 20+**: **PASS**
   - `v20.11.1`

4. **Full Maven build (compile) for `gravitee-apim-rest-api` succeeds**: **PASS**
   - Command: `mvn compile -pl gravitee-apim-rest-api -am -q`

5. **Console UI dependencies installed (`node_modules` exists)**: **PASS**
   - Path: `gravitee-apim-console-webui/node_modules`

6. **Existing v4 analytics code present**: **PASS**
   - **Backend** `ApiAnalyticsResource.java`:
     - `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`
   - **Frontend** `ApiAnalyticsProxyComponent`:
     - `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
   - **Frontend service** `ApiAnalyticsV2Service`:
     - `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts`

7. **Existing backend tests pass (`ApiAnalyticsResourceTest`)**: **PASS**
   - Command: `mvn test -pl gravitee-apim-rest-api -Dtest="ApiAnalyticsResourceTest" -q`

---

## Notes

- No failures detected in the requested checks.

