# Zee Mode — Architecture Overview

## System Context

Zee Mode is an AI-assisted resource creation feature for Gravitee APIM. Users describe what they want in natural language (+ file uploads), and the system generates structured, schema-valid Gravitee resource definitions via LLM structured output.

## Three-Component Architecture

```
┌─────────────────────────────────────────────────┐
│               Console UI (Angular 19)            │
│                                                  │
│   ┌──────────────┐    ┌───────────────────┐      │
│   │  Zee Widget   │──▶│ Resource Adapter   │─────▶│ Existing Save/Update REST calls
│   │  (NgModule)   │    │ (per-type)        │      │
│   └──────┬───────┘    └───────────────────┘      │
│          │                                        │
└──────────┼────────────────────────────────────────┘
           │ POST /v2/ai/generate
           │ { resourceType, prompt, files[] }
┌──────────▼────────────────────────────────────────┐
│               REST API (Java 17+)                  │
│                                                    │
│   ┌────────────────┐                               │
│   │ ZeeResource    │ JAX-RS endpoint               │
│   │ (auth + rate   │                               │
│   │  limiting)     │                               │
│   └──────┬─────────┘                               │
│          ▼                                         │
│   ┌────────────────────────┐                       │
│   │ GenerateResourceUseCase │ Orchestrator          │
│   └──────┬────────┬────────┘                       │
│          │        │                                │
│    ┌─────▼──┐  ┌──▼──────────────┐                 │
│    │ RAG    │  │ LlmEngineService │                 │
│    │Strategy│  │ (HttpTransport)  │                 │
│    └───┬────┘  └──────┬──────────┘                 │
│        │              │                            │
│   Domain Query    json-schema-llm                  │
│   Services        Java SDK                         │
│        │              │                            │
└────────┼──────────────┼────────────────────────────┘
         │              │
    ┌────▼────┐    ┌────▼─────────────┐
    │ MongoDB │    │ Azure OpenAI     │
    │         │    │ (gpt-5-nano)     │
    └─────────┘    │ Structured Output│
                   └──────────────────┘
```

## Onion Architecture (Mandatory)

APIM enforces Onion Architecture via ArchUnit tests. All Zee Mode backend code follows this structure:

```
io.gravitee.apim.core.zee/          ← Domain layer (no infra dependencies)
├── model/                           ← ZeeRequest, ZeeResult, ZeeResourceType
├── usecase/                         ← GenerateResourceUseCase
└── domain_service/                  ← LlmEngineService, RagContextStrategy interfaces

io.gravitee.apim.infra.zee/         ← Infrastructure layer (implements domain interfaces)
├── LlmEngineServiceImpl.java        ← HttpTransport to Azure OpenAI
└── adapter/                         ← RAG adapters using domain query services

io.gravitee.rest.api.management.v2.rest.resource.ai/  ← API layer
└── ZeeResource.java                 ← JAX-RS @Path("/v2/ai/generate")
```

**Rules:**

- Domain layer has ZERO dependencies on infrastructure
- Use cases orchestrate domain services
- Infrastructure adapters implement domain interfaces
- RAG strategies use existing `*QueryService` interfaces (FlowQueryService, etc.)

## Data Flow (Single Round-Trip)

1. **User** types prompt + uploads files in Zee Widget
2. **Widget** sends `POST /v2/ai/generate` with `{ resourceType, prompt, files[] }`
3. **ZeeResource** validates auth (existing Console JWT), applies rate limit
4. **GenerateResourceUseCase** orchestrates:
   a. Resolves component from `ZeeResourceType` → SDK component (e.g., `Flow`)
   b. Retrieves RAG context via `RagContextStrategy` (domain query services → MongoDB)
   c. Builds enriched prompt (user prompt + RAG context + file contents)
   d. Calls `LlmEngineService.generate()` → `HttpTransport` → Azure OpenAI
   e. Receives structured JSON, rehydrates via codec → original Gravitee shape
5. **Response** returns `{ generated: {...}, metadata: {...} }`
6. **Widget** renders preview (JSON tree + structured cards toggle)
7. **User** accepts → adapter transforms → existing save/update REST call fires

## Key Design Decisions

| Decision                      | Rationale                                                          |
| ----------------------------- | ------------------------------------------------------------------ |
| Embed in APIM REST API        | v0.1 prototype — no operational overhead of separate service       |
| No LangChain4J                | Azure is just URL + `api-key` header; SDK's HttpTransport suffices |
| Direct HttpTransport          | `json-schema-llm` Java engine handles the full round-trip pipeline |
| Strategy pattern RAG          | Different resource types need different context                    |
| Domain query services for RAG | Onion Architecture compliance + built-in tenant scoping            |
| NgModule (not standalone)     | Matches existing Console UI patterns                               |
| Typed ZeeResourceType enum    | Compile-time safety for resource type dispatch                     |

## Environment

```bash
# Required env vars
OPENAI_API_URL=https://saturn-poc1.openai.azure.com/openai/deployments/gpt-5-nano/chat/completions?api-version=2025-01-01-preview
OPENAI_API_KEY=<key>
```
