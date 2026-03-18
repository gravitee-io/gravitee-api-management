# <EPIC-KEY> Phase <phase-number-2d> Task <task-number-2d> <Area> <Task Title>

## Task Metadata
- Epic: `<EPIC-KEY> <Epic Title>`
- Phase: `<phase-number-2d> <phase-title>`
- Task number: `<task-number-2d>`
- Area: `backend` | `frontend` | `contract` | `integration` | `mixed`
- Suggested filename: `<EPIC-KEY>-phase-<phase-number-2d>-task-<task-number-2d>-<area>-<short-slug>.md`

## Goal
- `<describe the concrete purpose of the task>`

## Related Requirements and Stories
- Epic requirements: `../<epic-jira-id>-requirements.md`
- Related stories:
- `<story-reference-1>`
- `<story-reference-2>`
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- `<explanation>`

## Concrete Implementation Changes
- `<backend/frontend/contract change 1>`
- `<backend/frontend/contract change 2>`

## Planned File Changes
### Create
- `<new-file-path>`

### Update
- `<existing-file-path>`

### Delete
- `<file-to-remove>` or `None`

## API Contract Changes
- `None` or document each endpoint under its own subheading with:
- HTTP method
- Path
- Path parameters
- Query parameters
- Request body
- Expected responses

## Impacted Modules
- `<module-1>`
- `<module-2>`

## Validation
- Automated checks:
- `<command-or-test>`
- Manual checks:
- `<manual-validation-step>`

## Open Questions or Blockers
### <EPIC-KEY>-TASK-OQ-1
- Question: `<question>`
- Answer: `TBD`
- Status: `Open`
