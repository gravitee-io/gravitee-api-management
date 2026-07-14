# Gravitee.io APIm - LLM Proxy / MCP Server / AI Agent Proxy

This folder provides a quick-run setup to showcase the new **AI-related features** introduced in **Gravitee 4.8.0**, including the **A2A Agents Hub** and **A2A Agents UI** components.

## Prerequisites

1. Put your GCP API Key in the `.env` file.

    ```bash
    GOOGLE_API_KEY=your-google-api-key-here
    ```

2. Place your Gravitee license file (`license.key`) inside the `license/` folder.

3. Build Required Services

    ```bash
    docker compose build a2a-agents-hub
    ```
    ```bash
    docker compose build a2a-agents-ui
    ```

## Launch the Demo Environment
```bash
docker compose up -d
```

## Demo Steps

TODO



