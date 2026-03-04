# Manual test – Rebuild and restart

Steps to pick up the V4 API Analytics changes in your local environment.

---

## 1. Backend (Management API / REST API)

Our changes are in **gravitee-apim-rest-api** (unified analytics endpoint, permission, validation).

### 1.1 Rebuild

From the **repository root** (`gravitee-api-management/`):

```bash
# Rebuild only the REST API and its dependencies (faster than full install)
mvn clean install -pl gravitee-apim-rest-api -am -DskipTests -q
```

- `-pl gravitee-apim-rest-api` – build the Management API module and its submodules  
- `-am` – include dependencies (e.g. rest-api-service, rest-api-management-v2)  
- `-DskipTests` – optional, speeds up the build  
- Omit `-q` if you want to see full logs  

If you use a **full distribution** (e.g. you run the API from a `target/distribution` bundle), build the rest-api distribution so that folder is updated:

```bash
mvn clean install -pl gravitee-apim-rest-api -am -DskipTests
# Then use the distribution at gravitee-apim-rest-api/target/distribution (if your setup uses it)
```

### 1.2 Restart the Management API

- **IntelliJ / IDE:** Stop the **“Rest API - MongoDB”** (or your Management API) run configuration, then start it again.  
- **CLI (distribution):** Stop the process that runs the Management API, then start it again (e.g. `./gravitee` from `${GRAVITEE_HOME}/bin` with `GRAVITEE_HOME` pointing at the rest-api distribution).  
- **Docker:** If the Management API runs in a container, rebuild the image (if you build your own) and restart the container; or mount the built artifacts and restart the container.

After restart, the unified endpoint is available at:

`GET /v2/environments/{envId}/apis/{apiId}/analytics?type=COUNT|STATS|GROUP_BY|DATE_HISTO&from=...&to=...` (and optional `field`, `interval`, `size`, `order`).

---

## 2. Frontend (Console UI)

Our changes are in **gravitee-apim-console-webui** (proxy component, service, empty/error states).

### 2.1 Dependencies (once)

From the repo root or from `gravitee-apim-console-webui/`:

```bash
cd gravitee-apim-console-webui
yarn
# or: npm install
```

### 2.2 Serve in dev (with live reload)

```bash
cd gravitee-apim-console-webui
yarn serve
# or: npm run serve
```

- Default URL is usually **http://localhost:4200** (see the “Local:” line in the terminal).  
- With `ng serve`, saving files triggers a rebuild and reload, so you often only need to **restart `yarn serve`** if you changed config or had stopped it.

### 2.3 If the UI is already running

- If **`yarn serve`** is already running, Angular usually reloads when you save. If you don’t see the new behavior (e.g. “No analytics data for this period”, or updated stats/pie), do a **hard refresh** (Ctrl+F5 / Cmd+Shift+R) or restart `yarn serve`.

---

## 3. Prerequisites (if not already up)

- **MongoDB** and **Elasticsearch** must be running (e.g. Docker as in CONTRIBUTING).  
- You need a **v4 HTTP Proxy API** with **Analytics enabled** in settings.  
- The Management API must be configured to use the same Elasticsearch (and v4-metrics index) for analytics.

---

## 4. Quick check that backend is updated

After restarting the Management API, call the unified endpoint (replace `{envId}`, `{apiId}`, and timestamps):

```bash
curl -s -u "admin:admin" \
  "http://localhost:8083/management/v2/environments/DEFAULT/apis/YOUR_API_ID/analytics?type=COUNT&from=0&to=9999999999999"
```

Expected: `200` and JSON like `{"type":"COUNT","count":...}`.  
If you get `404` or an old response shape, the backend wasn’t rebuilt/restarted correctly.

---

## 5. Where to test in the UI

1. Log in to the Console.  
2. Open **APIs** → select a **v4 HTTP Proxy API** → **API Traffic** → **Analytics** tab.  
3. You should see:  
   - Timeframe filter bar  
   - Four stats cards (Total Requests, Avg Gateway/Upstream Response Time, Avg Content Length)  
   - HTTP Status pie  
   - Response status over time and Response time over time line charts  
   - **Empty state** “No analytics data for this period” when the selected period has no data  
   - **Error state** “Analytics unavailable” when the analytics backend fails  
