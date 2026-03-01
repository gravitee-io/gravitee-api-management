# 017 - Expand RAG Context (Documentation Embeddings)

## Goal

Integrate a second RAG (Retrieval-Augmented Generation) step into the agent pipeline that queries an external embeddings database containing the entirety of the Gravitee APIM public-facing documentation.

## Description

Our current RAG implementation (`RagContextStrategy`) queries the user's specific tenant environment (MongoDB) to extract _existing_ flows, entrypoints, and policies. This helps the LLM standardize on the customer's specific configuration style.

This feature adds a _general knowledge_ RAG step. We have a local project (to be located/integrated during this task) that chunks up all APIM public docs and stores them in an embeddings DB. We need to:

1. Perform a cosine similarity search against this DB based on the user's prompt.
2. Inject the relevant documentation chunks into the LLM context.

This provides the agent with "broad info" about what any given flow, policy, or entrypoint _means_ and how it should be configured according to official Gravitee docs, complementing the tenant-specific context.

## Checklist

- [ ] Locate and review the local embeddings DB project/service.
- [ ] Implement an HTTP/gRPC client in `/infra/zee` to query the embeddings service based on the user's prompt.
- [ ] Format the retrieved documentation chunks into a dense Markdown context block.
- [ ] Update `LlmEngineServiceImpl` to execute both RAG strategies (Tenant-Specific + Public Docs) and combine their outputs into the final system prompt.
- [ ] Add unit/integration tests for the new embeddings client.
