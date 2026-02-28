# Zee Mode — Feature Roadmap

AI-assisted resource creation for Gravitee APIM. Describe what you want in natural language → get structured, schema-valid Gravitee API definitions.

## Common Reference Docs

| Doc                                                                | Purpose                                                             |
| ------------------------------------------------------------------ | ------------------------------------------------------------------- |
| [architecture-overview.md](./architecture-overview.md)             | System architecture, component diagram, data flow, design decisions |
| [json-schema-llm-integration.md](./json-schema-llm-integration.md) | SDK integration guide, Maven setup, usage patterns, Phase 0 results |
| [agent-backend.md](./agent-backend.md)                             | Backend Onion Architecture, domain models, use case, service layer  |
| [rag-strategy.md](./rag-strategy.md)                               | RAG context retrieval per resource type, strategy pattern, rules    |
| [frontend-widget.md](./frontend-widget.md)                         | Angular NgModule component, adapter pattern, UI states, service     |

## Steps (each = 1 commit)

| #   | File                                                              | Description                                          | Phase      |
| --- | ----------------------------------------------------------------- | ---------------------------------------------------- | ---------- |
| 001 | [project-setup-dependencies](./001-project-setup-dependencies.md) | Maven deps, vendored JARs, config properties         | Backend    |
| 002 | [core-domain-models](./002-core-domain-models.md)                 | ZeeResourceType, ZeeRequest, ZeeResult records       | Backend    |
| 003 | [llm-engine-service](./003-llm-engine-service.md)                 | LlmEngineService + HttpTransport to Azure OpenAI     | Backend    |
| 004 | [rag-context-flow-strategy](./004-rag-context-flow-strategy.md)   | RagContextStrategy interface + FlowRagContextAdapter | Backend    |
| 005 | [generate-resource-usecase](./005-generate-resource-usecase.md)   | GenerateResourceUseCase orchestrator                 | Backend    |
| 006 | [jaxrs-endpoint-dtos](./006-jaxrs-endpoint-dtos.md)               | /v2/ai/generate endpoint + DTOs + auth               | Backend    |
| 007 | [backend-integration-test](./007-backend-integration-test.md)     | Unit tests + live curl test                          | Backend    |
| 008 | [frontend-zee-module-widget](./008-frontend-zee-module-widget.md) | ZeeModule + ZeeWidgetComponent                       | Frontend   |
| 009 | [preview-component](./009-preview-component.md)                   | JSON tree + structured cards toggle                  | Frontend   |
| 010 | [flow-adapter-integration](./010-flow-adapter-integration.md)     | FlowAdapter + wire into flow creation page           | Frontend   |
| 011 | [expand-resource-types](./011-expand-resource-types.md)           | Plan, Endpoint, Api, Entrypoint                      | Full Stack |
| 012 | [polish-error-handling](./012-polish-error-handling.md)           | Validation, rate limiting UX, error handling         | Full Stack |

## Timeline

~10-12 days total. Steps 001-007 (backend) ≈ 3-4 days. Steps 008-010 (frontend) ≈ 2-3 days. Steps 011-012 (expansion) ≈ 3 days.
