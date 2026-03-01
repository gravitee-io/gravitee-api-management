# Session Handoff: Simplify V4 SDK API

## Initial Instructions

1. Read `tmp/feat-roadmap/015-simplify-v4-sdk-api.md` for your specific task checklist for this session.
2. **CRITICAL:** Read through the core architectural `.md` files in `tmp/feat-roadmap/` to gain necessary contextual awareness. Specifically, read all non-numeric markdown files (e.g., `architecture-overview.md`, `how-to-rebuild-backend.md`, `json-schema-llm-integration.md`, `frontend-widget.md`, etc.).
3. **Do NOT** read the historic `001` through `014` tracking documents. Only read `015` and the core architectural files.

## Background Context

In the previous session, we traced a `jsl error: missing field 'op'` down to a Jackson serialization type erasure bug in the upstream `json-schema-llm` code generator.

The user is currently fixing that upstream, and concurrently enhancing the generator to output:

1. A unified Enum for all generated models (e.g., `V4Component`)
2. A single static factory method for generation (eliminating the need for our manual `ComponentInvoker` mapping).

## Your Mission

Your job is to update the Gravitee backend to use the newly vendored V4 SDK that includes these fixes. You will strip out the messy string-to-method mapping in `LlmEngineServiceImpl` and streamline the `/ai/generate` endpoint to leverage the new Enum-based architecture. Finally, you will test the end-to-end V4 generation (e.g., `Api`, `Flow`) from the Console UI to ensure the JSON Patch and generation succeed.
