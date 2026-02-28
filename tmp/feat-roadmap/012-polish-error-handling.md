# 012: Polish, Error Handling & Rate Limiting

> **Common docs**: [architecture-overview.md](./architecture-overview.md) · [frontend-widget.md](./frontend-widget.md)

## Objective

Final polish pass — input validation, error handling, rate limit UX, preview improvements, and edge cases.

## Tasks

### Backend

- [ ] **Input validation**: Max prompt length (e.g., 5000 chars), max file size (1MB per file, 5 files max), max total payload (10MB)
- [ ] **Error responses**: Structured error DTOs with actionable messages (e.g., "Rate limit exceeded, try again in X seconds", "Unsupported resource type")
- [ ] **Rate limiting**: Return `429 Too Many Requests` with `Retry-After` header
- [ ] **Logging**: Log generation requests (resource type, token usage, latency) for observability
- [ ] **Input sanitization**: Strip potential prompt injection patterns from file contents
- [ ] **Graceful Azure failures**: Timeout handling (30s max), retry on 5xx (once), clear error message on 4xx

### Frontend

- [ ] **Loading UX**: Show elapsed time during loading ("Zee is thinking... 3s")
- [ ] **Error UX**: Distinguish between rate-limit errors (show retry timer) and other errors (show generic message)
- [ ] **File upload UX**: Show selected file names, allow removal, validate extensions
- [ ] **Preview improvements**: Syntax-highlighted JSON view (use a lightweight library or Angular Material code highlighting)
- [ ] **Keyboard shortcuts**: Enter to submit, Escape to reject/reset
- [ ] **Accessibility**: ARIA labels, focus management

### Styling

- [ ] Match existing Console UI design system (Angular Material theme)
- [ ] Zee-specific branding: subtle icon or label to identify AI-generated content
- [ ] Responsive layout for the widget (works in narrow sidebars and wide main content areas)

## Acceptance Criteria

- [ ] Input validation rejects oversized prompts/files with clear error messages
- [ ] Rate limiting returns 429 with Retry-After header
- [ ] Frontend shows rate-limit countdown
- [ ] All error states have clear, actionable messages
- [ ] Widget styling matches Console UI design system
- [ ] Edge cases: empty response from LLM, malformed JSON, timeout

## Commit Message

```
feat(zee): add polish, validation, and error handling

Add input validation, rate limit UX, error handling improvements,
loading state enhancements, and styling polish for Zee Mode.
```
