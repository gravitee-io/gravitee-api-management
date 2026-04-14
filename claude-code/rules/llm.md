# LLM Utilization Guidelines

## Integration

- **Structured Output**: ALWAYS use structured output (JSON schema).
- **Validation**: Use `zod` to validate all LLM responses.
- **Safety**: Treat all LLM output as untrusted user input.

## API Patterns

- **Azure OpenAI**: Standard endpoint pattern:
  ```
  https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions
  ```
- **Batch Processing**: Use `ThreadPoolExecutor` with ~6 concurrent requests for rate limiting.
- **Rate Limit Handling**: Implement 429 detection with exponential backoff.

## Prompting

- **Persona**: Define a clear system persona.
- **Few-Shot Examples**: Include 2-3 concrete examples in system prompts for complex tasks.
- **Output Format**: Explicitly state "Return ONLY valid JSON: {schema}" at end of system prompt.
- **Temperature**:
  - **0.0 - 0.2**: For code/data tasks.
  - **0.3**: For consistent but creative transformations (e.g., compression).
  - **0.7+**: For creative tasks.

## Models

- **Abstraction**: Abstract model selection to allow easy switching.
- **Azure**: Current standard deployment is `gpt-5-chat` on Azure OpenAI.
