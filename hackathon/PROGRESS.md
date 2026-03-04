# Application Members Feature - Progress Tracker

## Phase & Story Checklist

### Phase 1: View Members
- [ ] 1.0 - BE: Define Application Members V2 Portal API Contract
- [ ] 1.1 - BE: List Application Members (UseCase)
- [ ] 1.2 - BE: List Application Roles (UseCase)
- ✅ 1.3 - FE: Members Tab & Routing
- [ ] 1.4 - FE: Members Table
- [ ] 1.5 - FE: Extend Paginated Table for Custom Action Cells

### Phase 2: Edit & Delete Members
- [ ] 2.1 - BE: Update Member Role (UseCase)
- [ ] 2.2 - BE: Delete Application Member (UseCase)
- [ ] 2.3 - FE: Edit Member Role Dialog
- [ ] 2.4 - FE: Delete Member Dialog

### Phase 3: Add Members
- [ ] 3.1 - BE: Search Users for Application (UseCase)
- [ ] 3.2 - BE: Add Application Member (UseCase)
- [ ] 3.3 - FE: Search Users Dialog (Add Members)

### Phase 4: Invite Users via Email
- [ ] 4.1 - BE: Invite User to Application (UseCase)
- [ ] 4.2 - BE: List Pending Invitations (UseCase)
- [ ] 4.3 - BE: Delete Pending Invitation (UseCase)
- [ ] 4.4 - BE: Update Invited Member Role (UseCase)
- [ ] 4.5 - BE: Invitation Acceptance to Membership Flow
- [ ] 4.6 - BE: Unified Members + Invitations Query (UseCase)
- [ ] 4.7 - FE: Invite User Dialog
- [ ] 4.8 - FE: Display Invited Members in Table

### Phase 5: Transfer Ownership
- [ ] 5.1 - BE: Transfer Application Ownership (UseCase)
- [ ] 5.2 - FE: Transfer Ownership Dialog

---

## Summary of Completed Work

- **Story 1.3 (FE: Members Tab & Routing)** — Application tab members shell component, route `members` under `:applicationId`, Members tab link in application nav (gated by `MEMBER` read permission), component spec. Aligned with portal-next design system (theme SCSS, m3 typography) and Angular rules (signal inputs, standalone).

## Key Decisions

- **New V2 paths instead of modifying legacy endpoints** -- e.g. `/membersV2` instead of `/members`. Avoids breaking existing consumers (AC3).
- **Onion Architecture with @UseCase classes** -- All new backend code follows the UseCase pattern (like `PortalNavigationItemResource`), not the legacy service-direct pattern (like `ApplicationMembersResource`).
- **API contract defined upfront (Story 1.0)** -- Enables parallel FE/BE development from day one.
- **Unified members + invitations query (Story 4.6)** -- Single backend endpoint returns both active members and pending invitations, instead of forcing frontend to merge two separate responses.
- **Dedicated paginated-table extension (Story 1.5)** -- Existing `paginated-table` only supports `text`/`date` columns. Actions column needs explicit work on the shared component.
- **No page 1 / page 2 navigation** -- "Add members" and "Invite users" are dialog-driven from a dropdown button, not a separate page view.
- **APPLICATION_MEMBER[CREATE] for user search** -- The existing `POST /users/_search` requires `ORGANIZATION_USERS[READ]` which portal users lack. New search endpoint uses application-scoped permission.

## Per-Agent Logs

Detailed insights, gotchas, surprises, blockers, and effective prompts are tracked per agent:

- [agent-ivan.md](agent-ivan.md) -- Ivan's agent
- [agent-jarek.md](agent-jarek.md) -- Jarek's agent
