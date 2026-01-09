Project prerequisites (first-time setup)

OS + tooling (generic by platform)
- Docker runtime:
  - macOS/Windows: Docker Desktop installed and running.
  - Linux: Docker Engine + Docker Compose v2 installed.
- Docker CLI available: `docker`.
- Docker Compose v2 available: `docker compose`.
- Node.js 20.11.x (project engines require ^20.11).
- Corepack enabled for Yarn 4, or use `npx -y @yarnpkg/cli-dist@4.1.1`.
- Java 17 (LTS) and Maven 3.9+ for backend build (`mvn`).
- Git (if working from a clone).

Repository build inputs
- Network access for dependency installs and pulling Docker images.
- Optional: license key for APIM Enterprise features (used as LICENSE_KEY env var in compose).
- Disk space: allow several GB for Maven + Yarn caches and Docker images.
- RAM: 8 GB minimum recommended when building UIs and running Docker stack.

UI build prerequisites
- Console UI: `gravitee-apim-console-webui` uses Yarn 4 and builds Angular + shared libs.
- Portal UI: `gravitee-apim-portal-webui` uses Yarn 4 and builds Angular.
- Both produce `dist/` used by Dockerfiles for UI images.

Backend build prerequisites
- Maven multi-module build from repo root produces backend artifacts used by runtime.
- Default build command (from CONTRIBUTING): `mvn clean install -T 2C`
- Faster local build: `mvn clean install -T 2C -DskipTests=true -Dskip.validation=true`
- Full bundle with plugins: `mvn clean install -T 2C -P bundle-default,bundle-dev`

Suggested first-run steps (local full stack with MongoDB compose)
0) (Optional) Build backend artifacts
   - `mvn clean install -T 2C -DskipTests=true -Dskip.validation=true`
1) Build console UI
   - `cd gravitee-apim-console-webui`
   - `npx -y @yarnpkg/cli-dist@4.1.1 install`
   - `npx -y @yarnpkg/cli-dist@4.1.1 build`
2) Build portal UI
   - `cd gravitee-apim-portal-webui`
   - `npx -y @yarnpkg/cli-dist@4.1.1 install`
   - `npx -y @yarnpkg/cli-dist@4.1.1 build`
3) Build local UI Docker images (from repo root)
   - `docker build -t graviteeio/apim-management-ui:latest -f gravitee-apim-console-webui/docker/Dockerfile gravitee-apim-console-webui`
   - `docker build -t graviteeio/apim-portal-ui:latest -f gravitee-apim-portal-webui/docker/Dockerfile gravitee-apim-portal-webui`
4) Start full stack with MongoDB compose
   - `cd docker/quick-setup/mongodb`
   - `LICENSE_KEY=... docker compose up -d`

Runtime endpoints (default compose)
- Gateway: http://localhost:8082
- Management API: http://localhost:8083
- Management UI: http://localhost:8084
- Portal UI: http://localhost:4100
- Mailhog: http://localhost:8025

Ports used by default (must be free)
- 8082, 8083, 8084, 4100, 8025, 27017, 9200, 9300

Notes
- If `LICENSE_KEY` is not set, compose will warn and proceed without enterprise features.
- Docker Desktop on Apple Silicon may warn on amd64 images (e.g., Mailhog); it usually still runs.
- Node 25.x is not supported (odd-numbered); use Node 20.11.x.
- If Corepack is missing, do not use Yarn 1.x; use the `npx @yarnpkg/cli-dist@4.1.1` fallback.
- If Docker pull/build fails, ensure Docker Desktop is running and you are logged in if required by the registry.

Environment checks (agent can run to confirm prerequisites)
- `docker --version` and `docker compose version`
- `node -v` (expect 20.11.x)
- `java -version` (expect 17)
- `mvn -v` (expect 3.9+)
- `git --version`

If a prerequisite is missing
- Ask the user to install the missing tool with platform-appropriate steps.
- If the platform is unclear, ask the user which OS and install method they prefer.
