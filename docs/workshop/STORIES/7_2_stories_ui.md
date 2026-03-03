### Story 7.2: UI Error States

**Title:** Display error states in all widgets

**Description:**
As an API publisher, I want to see clear error messages when analytics fail so that I know what went wrong.

**Acceptance Criteria:**
- [ ] All widgets show error state UI
- [ ] Error messages are user-friendly
- [ ] Retry button allows manual refresh
- [ ] Error details logged to console for debugging

**Layer:** Frontend UI
**Complexity:** S (2 days)
**Dependencies:** Story 7.1

#### Subtask 7.2.0: Write Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

#### Subtask 7.2.1: Update Components

- Enhance error handling in all widget components
- Add retry functionality
- Improve error messages

#### Subtask 7.2.2: Run Tests (GREEN)

---

