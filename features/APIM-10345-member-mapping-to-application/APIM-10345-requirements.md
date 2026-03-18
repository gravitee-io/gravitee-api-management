# APIM-10345 Member Mapping to Application

## Jira References
- Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- Related UX umbrella epic: [EXT-49](https://gravitee.atlassian.net/browse/EXT-49)
- Related application creation baseline: [APIM-11540](https://gravitee.atlassian.net/browse/APIM-11540)
- Design tickets linked to this Epic: [APIM-11484](https://gravitee.atlassian.net/browse/APIM-11484), [APIM-12893](https://gravitee.atlassian.net/browse/APIM-12893)
- Documentation ticket linked to this Epic: [APIM-11584](https://gravitee.atlassian.net/browse/APIM-11584)
- Source PRD from Epic: <https://docs.google.com/document/d/1omU7tgT4M23BFt335HYgg_Et1nmxvHoWhORs1uW5EX4/edit?tab=t.0#heading=h.o34b5a884kfz>

## Figma References
- Developer Portal master file referenced by [EXT-49](https://gravitee.atlassian.net/browse/EXT-49): <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=35780-13686&t=IY9htKjVPfbwj8PM-4>
- Application creation flow referenced by [APIM-11540](https://gravitee.atlassian.net/browse/APIM-11540): <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=35471-46251&t=km886KcJVeA9B9nv-0>
- Member-management design anchor provided during requirements refinement: <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=41780-5585&t=uovLbARmhIgnnWov-4>
- Exported assets for the Epic should be stored in `features/APIM-10345-member-mapping-to-application/screens/`.
- Precise screen-to-story mapping is currently provisional because the exported screens are not yet present locally and the exact Figma subframes could not be inspected from the current environment.

## Context
The Next Gen Developer Portal is being expanded from basic application creation and viewing flows toward full application collaboration. Application owners currently lack a consistent way to manage application-level access directly from the new portal. This Epic is intended to close that gap while staying aligned with the ongoing portal redesign and the console's functional rules.

## Problem Statement
Application collaboration in the Next Gen Portal is incomplete. Owners cannot yet manage registered and invited application members from one coherent feature area, which forces them to switch context, rely on older interfaces, or postpone collaboration tasks entirely. The missing functionality includes member discovery, onboarding, role management, ownership transfer, and visibility into pending invitations.

## Goal
Provide a first-class application member-management capability in the Next Gen Portal so that an application owner can:
- view current members and pending invites
- add existing registered users
- invite new users by email
- update roles for active and invited members
- remove unintended access
- transfer ownership safely
- respect feature toggles and role-based visibility rules

## Scope
- Add member-management capabilities to the Next Gen Portal application area.
- Support both registered members and invited-but-not-yet-registered users.
- Allow searching for existing system users before adding them to an application.
- Allow searching within the invited-members list by email.
- Allow inviting non-registered users by email with an application role.
- Allow entering multiple email addresses in the invite flow.
- Display member identity, email, role, and available actions in the `Members` surface, and invitation identity, lifecycle state, role, and available actions in the `Invitations` surface.
- Allow role updates for active members.
- Allow deleting active members from the application.
- Allow role updates for pending invited members.
- Allow deleting pending invitations.
- Allow resending pending invitations.
- Support the invitation-acceptance transition from invitation state to active application membership.
- Support ownership transfer to another registered user.
- Add admin-level feature toggles for member mapping, invitation sending, and transfer of ownership.
- Add supporting documentation for the new portal experience.

## Out of Scope
- Legacy Portal implementation changes.
- Broad portal look-and-feel work already covered by [EXT-49](https://gravitee.atlassian.net/browse/EXT-49).
- Console endpoint reuse as a frontend contract for this feature.
- Group-based member management. The linked Epic and user stories only describe user-based collaboration.
- Final UX parity for every detail not covered by confirmed Figma sources.
- Application creation API catalog work from [APIM-12773](https://gravitee.atlassian.net/browse/APIM-12773) and [APIM-12774](https://gravitee.atlassian.net/browse/APIM-12774). They remain linked in Jira, but they stay outside the local member-mapping slice documented in this directory.

## Impacted Modules
- `gravitee-apim-portal-webui-next` / Nx app `portal-next`
- `gravitee-apim-webui-libs`
- `gravitee-apim-rest-api`
- Portal OpenAPI contract exposed to the frontend
- `gravitee-apim-console-webui` or equivalent admin-settings UI if feature toggles are managed there
- Invitation email and token handling components
- Product documentation for the new developer portal

## Domain Model
### Application
An application is the collaboration boundary for this Epic. Member-management actions are always performed in the context of one application, and the business rules in this Epic determine who can access that application and in which role.

The business-significant relationships are:
- an application has one owner at any given time
- an application has active members
- an application can also have pending invitations for people who are not members yet

### Application Owner
The application owner is the primary actor for the member-management experience. The owner is a registered user with elevated responsibility for the application, including managing members and transferring ownership when that capability is enabled.

This concept matters separately from a generic member because ownership carries distinct business rules. The application must never be left without an owner, and ownership transfer is not the same action as a regular role change.

### Registered User
A registered user is a person who already has a platform account and can be granted active access to an application immediately. In this Epic, registered users can be searched, added as members, assigned roles, and selected as ownership-transfer targets.

The business-significant attributes are the user's identity label, email address, and account existence in the platform. Account existence matters because it determines whether the person follows the direct add flow or the invitation flow.

Registered user status and application membership must remain separate concepts. A person can become a registered platform user before they are an active application member. If an invited person completes platform registration but has not yet accepted the invitation, the system may know that the person is now registered, but that does not by itself create application membership.

### Application Membership
Application membership is the business relationship between an application and a registered user who already has access to it. It should be treated as a first-class concept, not just a property on the user or the application, because the relationship carries business meaning of its own.

The business-significant attributes are:
- the member role within the application
- the member identity displayed in the list
- the fact that membership exists only after a direct add or a completed invitation acceptance

Application membership is distinct from an invitation. A member already has granted access, while an invited person does not.

### Member Role Assignment
Member role assignment describes the level of access a member or invited person is intended to receive for the application. This is a separate business concept because roles are not merely descriptive labels; they drive what level of application access is granted or will be granted.

The business-significant relationships are:
- each application membership has a role assignment
- each invitation also carries a target role assignment
- ownership is not just another ordinary member role update when ownership-transfer rules apply

### Invitation
An invitation is an application-scoped request for a person to gain access to the application later. It is not the same thing as an active membership. This distinction is important because invitations can be pending, expired, updated, or deleted without the invited person ever becoming an active member.

The business-significant attributes are:
- the invited email address
- the target application role
- the invitation status, for example pending or expired
- the invitation lifecycle timing, such as when it was created and whether it is still valid
- whether the invitation now corresponds to a platform account without yet having been accepted

### Invited Person
An invited person is the target of an invitation, identified in the current scope primarily by email address. This concept should not be merged with a registered user because the key business rule is that the person may not yet have a platform account.

This distinction matters for:
- choosing between direct add and invite-by-email flows
- deciding what identity data can be shown in the members experience
- determining what can be used as the visible identity label before registration exists

If the invited person later creates a platform account before accepting the invitation, the application-access state is still invitation-based. In that state, the person may be a registered user in platform terms, but they remain an invited person in application terms until the invitation is accepted or otherwise replaced by an explicit add-member action. Any invitation-row indication that a platform account now exists must therefore be interpreted as "registered account exists, invitation still pending" rather than "active member".

### Ownership Transfer
Ownership transfer is the controlled business action that changes which registered user is the application owner. It is not just a role-edit operation because it changes responsibility and continuity of control over the application.

The business-significant rules are:
- the transfer target must be a registered user
- the application must continue to have exactly one owner
- the previous owner may remain involved, but their post-transfer role still requires explicit clarification

### Member-Management Capability
Member-management capability is the business capability that allows application owners to manage application access from the portal. It is part of the product scope for this Epic because the experience may be enabled or disabled administratively.

This concept matters because access to the member-management experience is not only determined by user role, but also by whether the capability is enabled for the relevant product context.

### Ownership-Transfer Capability
Ownership-transfer capability is a separate business capability that controls whether ownership transfer is available. It must be treated distinctly from general member management because the Epic requirements state that these controls can be enabled independently.

This distinction matters because a product configuration may allow member management while still forbidding ownership transfer.

## User Stories in Scope
- `APIM-10345-story-01` [APIM-12280](https://gravitee.atlassian.net/browse/APIM-12280): view existing application members.
- `APIM-10345-story-02` [APIM-11539](https://gravitee.atlassian.net/browse/APIM-11539): search among existing application members.
- `APIM-10345-story-03` [APIM-12764](https://gravitee.atlassian.net/browse/APIM-12764): search for existing system users before adding them.
- `APIM-10345-story-04` [APIM-12765](https://gravitee.atlassian.net/browse/APIM-12765): add registered users to an application.
- `APIM-10345-story-05` [APIM-12770](https://gravitee.atlassian.net/browse/APIM-12770): update roles for active members.
- `APIM-10345-story-15` [APIM-11532](https://gravitee.atlassian.net/browse/APIM-11532): delete a registered member from the application.
- `APIM-10345-story-06` [APIM-12766](https://gravitee.atlassian.net/browse/APIM-12766): invite a new user by email.
- `APIM-10345-story-07` [APIM-12767](https://gravitee.atlassian.net/browse/APIM-12767): view pending invitations and their status.
- `APIM-10345-story-17` [APIM-11538](https://gravitee.atlassian.net/browse/APIM-11538): search within invited members by email.
- `APIM-10345-story-08` [APIM-12768](https://gravitee.atlassian.net/browse/APIM-12768): delete a pending invitation.
- `APIM-10345-story-09` [APIM-12771](https://gravitee.atlassian.net/browse/APIM-12771): update roles for invited members.
- `APIM-10345-story-16` [APIM-11535](https://gravitee.atlassian.net/browse/APIM-11535): resend a pending invitation.
- `APIM-10345-story-10` [APIM-12772](https://gravitee.atlassian.net/browse/APIM-12772): transfer ownership to another registered member.
- `APIM-10345-story-11` [APIM-12888](https://gravitee.atlassian.net/browse/APIM-12888): admin toggles for member mapping, invitation sending, and ownership transfer.
- `APIM-10345-story-14` [APIM-11531](https://gravitee.atlassian.net/browse/APIM-11531): accept an invitation and transition from invitation state to active membership.

## Jira-Linked Stories Outside the Local Scope
- `APIM-10345-story-12` [APIM-12773](https://gravitee.atlassian.net/browse/APIM-12773): API selection wizard during application creation. This story remains linked in Jira, but it is outside the local member-mapping slice documented here.
- `APIM-10345-story-13` [APIM-12774](https://gravitee.atlassian.net/browse/APIM-12774): finalize API mapping at application creation time. This story remains linked in Jira, but it is outside the local member-mapping slice documented here.

## Suggested Implementation Order
### APIM-10345-story-01 [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280)
- Suggested order: `APIM-10345-story-01`
- Prerequisite stories: none at story level
- Dependency reason: this is the baseline members view. It establishes the members tab, the table structure, the empty state, and the first stable rendering target that the rest of the member-management stories extend.

### APIM-10345-story-02 [APIM-11539 Search among existing members](https://gravitee.atlassian.net/browse/APIM-11539)
- Suggested order: `APIM-10345-story-02`
- Prerequisite stories: `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280)
- Dependency reason: search is an enhancement of the baseline list view. It reuses the same table, empty-state behavior, and member dataset, so implementing it after the base list minimizes rework.

### APIM-10345-story-03 [APIM-12764 Search for Existing System Users](https://gravitee.atlassian.net/browse/APIM-12764)
- Suggested order: `APIM-10345-story-03`
- Prerequisite stories: `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280)
- Dependency reason: searching for existing system users is launched from the members area and needs awareness of the current application member set to avoid duplicates. The base members view provides that context.

### APIM-10345-story-04 [APIM-12765 Add Registered User to Application](https://gravitee.atlassian.net/browse/APIM-12765)
- Suggested order: `APIM-10345-story-04`
- Prerequisite stories: `APIM-10345-story-03` [APIM-12764 Search for Existing System Users](https://gravitee.atlassian.net/browse/APIM-12764), `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280)
- Dependency reason: adding registered users depends on being able to search and select eligible platform users first, and it must refresh the existing members table once the add action succeeds.

### APIM-10345-story-05 [APIM-12770 Update Roles for Active Members](https://gravitee.atlassian.net/browse/APIM-12770)
- Suggested order: `APIM-10345-story-05`
- Prerequisite stories: `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280), `APIM-10345-story-04` [APIM-12765 Add Registered User to Application](https://gravitee.atlassian.net/browse/APIM-12765)
- Dependency reason: active-member role updates make sense only once active members are visible and there are enough non-owner members in the list to exercise the edit flow. The add-member story creates that reusable path.

### APIM-10345-story-15 [APIM-11532 Delete Registered Member](https://gravitee.atlassian.net/browse/APIM-11532)
- Suggested order: `APIM-10345-story-15`
- Prerequisite stories: `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280), `APIM-10345-story-04` [APIM-12765 Add Registered User to Application](https://gravitee.atlassian.net/browse/APIM-12765)
- Dependency reason: deleting a registered member depends on the baseline members table and the active-member management flow because the delete action is executed in the same `Members` surface and changes the same active-members dataset.

### APIM-10345-story-06 [APIM-12766 Invite New User via Email](https://gravitee.atlassian.net/browse/APIM-12766)
- Suggested order: `APIM-10345-story-06`
- Prerequisite stories: `APIM-10345-story-03` [APIM-12764 Search for Existing System Users](https://gravitee.atlassian.net/browse/APIM-12764), `APIM-10345-story-04` [APIM-12765 Add Registered User to Application](https://gravitee.atlassian.net/browse/APIM-12765)
- Dependency reason: the invite flow shares the same add-members entry point, role-selection patterns, and post-submit refresh behavior as the registered-user add flow. Implementing it after that flow allows reuse of UX and backend conventions.

### APIM-10345-story-07 [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767)
- Suggested order: `APIM-10345-story-07`
- Prerequisite stories: `APIM-10345-story-06` [APIM-12766 Invite New User via Email](https://gravitee.atlassian.net/browse/APIM-12766)
- Dependency reason: pending invitations cannot be meaningfully displayed until invitation records can be created. This story turns the side effect of the invite flow into a visible and manageable invitations list in the same feature area.

### APIM-10345-story-17 [APIM-11538 Search among Invited Members](https://gravitee.atlassian.net/browse/APIM-11538)
- Suggested order: `APIM-10345-story-17`
- Prerequisite stories: `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767)
- Dependency reason: invited-members search is an enhancement of the dedicated invitations surface. It reuses the same invitation list and filters it in place, so it should come after the base invitations view exists.

### APIM-10345-story-08 [APIM-12768 Delete Pending Invitation](https://gravitee.atlassian.net/browse/APIM-12768)
- Suggested order: `APIM-10345-story-08`
- Prerequisite stories: `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767)
- Dependency reason: deleting a pending invite depends on pending invites being visible and actionable in the dedicated invitations list or section. That list provides the row context for delete actions.

### APIM-10345-story-09 [APIM-12771 Update Roles for Invited Members](https://gravitee.atlassian.net/browse/APIM-12771)
- Suggested order: `APIM-10345-story-09`
- Prerequisite stories: `APIM-10345-story-06` [APIM-12766 Invite New User via Email](https://gravitee.atlassian.net/browse/APIM-12766), `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767)
- Dependency reason: role updates for invited members rely on invitations already existing and being represented in the UI. It is a refinement of the pending-invite management flow, not a foundation step.

### APIM-10345-story-16 [APIM-11535 Resend Invitation](https://gravitee.atlassian.net/browse/APIM-11535)
- Suggested order: `APIM-10345-story-16`
- Prerequisite stories: `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767)
- Dependency reason: resend is an invitation-lifecycle action exposed from the invitations list. It should be introduced only after invitation rows and their actionable state are visible in the dedicated invitations surface.

### APIM-10345-story-14 [APIM-11531 Acceptance of Invitation](https://gravitee.atlassian.net/browse/APIM-11531)
- Suggested order: `APIM-10345-story-14`
- Prerequisite stories: `APIM-10345-story-06` [APIM-12766 Invite New User via Email](https://gravitee.atlassian.net/browse/APIM-12766), `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767)
- Dependency reason: invitation acceptance is the lifecycle transition that turns a pending invitation into active application membership. It depends on invitation creation and on a stable understanding of the invitation state exposed in the dedicated invitations surface.

### APIM-10345-story-10 [APIM-12772 Transfer Ownership to Registered Member](https://gravitee.atlassian.net/browse/APIM-12772)
- Suggested order: `APIM-10345-story-10`
- Prerequisite stories: `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280), `APIM-10345-story-03` [APIM-12764 Search for Existing System Users](https://gravitee.atlassian.net/browse/APIM-12764), `APIM-10345-story-04` [APIM-12765 Add Registered User to Application](https://gravitee.atlassian.net/browse/APIM-12765)
- Dependency reason: ownership transfer depends on a stable members list, a known set of eligible registered members, and a proven ability to search and surface those members in the portal. It should come after the core member-management paths are stable because it is the highest-risk administrative action.

### APIM-10345-story-11 [APIM-12888 Administrative Toggle Controls](https://gravitee.atlassian.net/browse/APIM-12888)
- Suggested order: `APIM-10345-story-11`
- Prerequisite stories: `APIM-10345-story-01` [APIM-12280 View existing application members](https://gravitee.atlassian.net/browse/APIM-12280), `APIM-10345-story-06` [APIM-12766 Invite New User via Email](https://gravitee.atlassian.net/browse/APIM-12766), `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](https://gravitee.atlassian.net/browse/APIM-12767), `APIM-10345-story-10` [APIM-12772 Transfer Ownership to Registered Member](https://gravitee.atlassian.net/browse/APIM-12772)
- Dependency reason: feature toggles should wrap concrete capabilities that already exist and can be hidden, exposed, and audited. Implementing them after the main feature paths makes it easier to verify that each toggle gates the correct UI and behavior.

## Functional Requirements
- The Next Gen Portal must expose an application-member management experience from the application context.
- The experience must cover both active members and pending invites. Per `APIM-10345-decision-01`, the first delivery must expose them as separate list surfaces within the same feature area rather than as one unified paginated table.
- The members view must display a table with the columns `Name`, `Email`, `Role`, and `Actions`.
- The `Name` column must include avatar when available and the display name, while email is rendered in a separate `Email` column.
- The members view must always present pagination controls.
- The members view must support searching active members and must provide a meaningful empty state when no results match.
- The invitations view must expose pending and expired invitations with its own list behavior consistent with `APIM-10345-decision-01`.
- The invitations view must support searching invitation entries by email.
- The system must allow application owners to search for registered platform users before adding them to an application.
- Search results for registered users must show the user's email and clearly mark users already attached to the application as `Already Added`.
- The system must allow adding one or more registered users to an application with an assigned role. The default role for this flow is the lowest role present in the system unless another role is selected.
- The add-registered-user role picker must not expose `PRIMARY OWNER` or the equivalent default owner role created by Gravitee.
- The system must allow inviting one or more non-registered email addresses, validate each email format, create secure invitation tokens, assign an application role, and send an email containing the application context and invitation link.
- If an invitation target email already belongs to a registered user, the flow must not create a duplicate invitation and must redirect the user toward the registered-user add flow.
- The system must show the status of pending invitations, including whether an invite is still active or expired.
- The system must allow resending a pending invitation and refresh its expiration timer.
- Accepting a valid invitation must create active application membership and move the person from the invitations lifecycle into the members lifecycle.
- If an invited person becomes a registered platform user before accepting the invitation, the invitation may expose that the person is now registered, but the record must remain invitation-scoped until acceptance. That state must not be treated as active membership.
- The system must allow updating the role of an active member.
- The system must allow updating the role of a pending invited member without invalidating the invitation itself.
- The system must allow deleting a pending invitation and invalidate that invitation for later use.
- The system must allow removing active members from an application, with explicit confirmation, and removed users must no longer be able to see the application.
- Ownership transfer must be restricted to a registered user and must ensure that the application is never left without an owner.
- Member-mapping, invitation-sending, and transfer-of-ownership capabilities must be independently controllable through admin settings.
- When a feature toggle is disabled, the related UI entry points must be hidden or inaccessible in the Next Gen Portal.
- Audit logging must exist for admin changes to feature-toggle state.
- Existing consumers must not be broken by the backend changes needed to support this feature.

## Non-Functional Requirements
- Members page load time should be within 2 to 4 seconds, excluding network latency.
- Confirmation dialogs or pop-ups should be displayed within 1 second.
- Data-changing actions should complete within 2 to 4 seconds in normal operating conditions.
- Invitation tokens must be unique, secure, and time-bound.
- Feature-toggle state must persist across sessions and restarts.
- Toggle changes should take effect in real time or near real time without requiring a manual service restart.
- Audit records for toggle changes must capture actor, timestamp, action, and module name.

## Cross-Story Rules and Constraints
- The final user experience must follow the validated Figma design, using the frame `node-id=41780:5585` as the current design anchor until more granular exports are added in `screens/`.
- Product behavior should remain consistent with existing APIM business rules where this Epic does not explicitly redefine them.
- Permissions must be enforced server-side for all read and write operations.
- The application owner is the primary target persona for management actions in this first version.
- The solution should avoid exposing internal console-only contracts directly to the portal frontend.
- Error handling should reuse existing Next Gen Portal patterns where possible.
- Ownership-transfer behavior is feature-flagged separately from the general member-mapping capability.
- Platform account existence and application membership must remain separate states. An invitation may point to a now-registered user without granting active application access until invitation acceptance occurs.

## Acceptance Criteria
- An authorized application owner can open the member-management area for an application in the Next Gen Portal.
- The owner can see current members and pending invitations through separate `Members` and `Invitations` list surfaces in the same feature area, with invitation-specific status remaining in the invitations surface.
- The members list always presents pagination controls.
- The owner can search within the current members list and receives a clear empty state when nothing matches.
- The owner can search within the invitations list by email and receives a clear empty state when nothing matches.
- The owner can search for existing system users and add one or more of them to the application without duplicating existing memberships.
- The owner can invite one or more non-registered email addresses, assign a role, and trigger invitation emails.
- The owner can remove an active member from the application and that user loses visibility to the application.
- The owner can resend a pending invitation and refresh its validity period.
- A successfully accepted invitation grants application access and moves the person into the `Members` list as active membership.
- A person who registers before accepting the invitation remains in the invitation lifecycle until acceptance and must not appear as an active member only because an account now exists.
- The owner can update roles for active members and invited members.
- The owner can delete a pending invitation and that invitation can no longer be used to gain access.
- The owner can transfer ownership to another registered member when the transfer feature is enabled.
- An administrator can enable or disable member mapping, invitation sending, and ownership transfer independently, and the state change is audited and persisted.

## Project Decisions
- `APIM-10345-decision-01` [Members and Invitations List Composition](./decisions/APIM-10345-decision-01-members-and-invitations-list-composition.md): agreed on `2025-03-18` during `APIM Core Refinement`. The approved direction, confirmed by `Varun Goel`, is to change the UX and separate the `Members` and `Invitations` lists for the first delivery.

## Open Questions
### APIM-10345-OQ-1
- Question: Is the product direction a single unified table for active and invited members, or a main list plus a dedicated pending-invites section?
- Answer: `APIM-10345-decision-01` was agreed on `2025-03-18` during `APIM Core Refinement`. The approved direction is a `Members` list for active members and a separate `Invitations` list or section for pending and expired invitations in the same feature area.
- Status: Resolved

### APIM-10345-OQ-2
- Question: Are [APIM-12280](https://gravitee.atlassian.net/browse/APIM-12280) and [APIM-11539](https://gravitee.atlassian.net/browse/APIM-11539) intended to stay separate, or should they be merged into one view/search story?
- Answer: TBD
- Status: Open

### APIM-10345-OQ-3
- Question: Is removing an active member explicitly in scope for this Epic? It appears in member-table actions but does not have a dedicated standalone story.
- Answer: Jira now defines [APIM-11532](https://gravitee.atlassian.net/browse/APIM-11532) as a dedicated story for deleting a registered member, so this behavior is explicitly in scope.
- Status: Resolved

### APIM-10345-OQ-4
- Question: What is the exact default role for newly added or invited members if the user does not change the selection?
- Answer: Jira now differentiates the two flows. Adding a registered user defaults to the lowest role present in the system, while inviting by email defaults to `USER`.
- Status: Resolved

### APIM-10345-OQ-5
- Question: What is the exact invitation expiration period in days?
- Answer: TBD
- Status: Open

### APIM-10345-OQ-6
- Question: Is invitation resend required in v1, optional, or out of scope?
- Answer: Jira now defines [APIM-11535](https://gravitee.atlassian.net/browse/APIM-11535) as a dedicated story for resend, so resend is in scope for the local documentation.
- Status: Resolved

### APIM-10345-OQ-7
- Question: What is the final confirmation UX for ownership transfer: typed keyword, password, selected replacement owner only, or a different design-driven pattern?
- Answer: The currently exported screens only show the entry-point button and do not include a dedicated transfer-confirmation state.
- Status: Open

### APIM-10345-OQ-8
- Question: After ownership transfer, what role should the previous owner retain by default?
- Answer: TBD
- Status: Open

### APIM-10345-OQ-9
- Question: Are [APIM-12773](https://gravitee.atlassian.net/browse/APIM-12773) and [APIM-12774](https://gravitee.atlassian.net/browse/APIM-12774) intentionally part of this Epic or mislinked application-creation stories?
- Answer: They remain linked in Jira, but for the local documentation in this directory they stay out of scope because their content aligns with application creation rather than member mapping.
- Status: Resolved

### APIM-10345-OQ-10
- Question: Which concrete exported screens in `screens/` should be treated as the source of truth for each story under this Epic?
- Answer: A first-pass mapping has been added to the story files, but it remains provisional where dedicated dialog or confirmation exports are missing.
- Status: Open

### APIM-10345-OQ-11
- Question: Does [APIM-12888](https://gravitee.atlassian.net/browse/APIM-12888) intentionally define three toggles even though one sentence in Jira still says "two distinct toggles"?
- Answer: The Jira story body now lists three toggle names: member mapping, invitation sending, and transfer of ownership. The older "two distinct toggles" wording appears stale and should be treated as a Jira inconsistency rather than the current intent.
- Status: Resolved

## Assumptions
- This first version targets the Next Gen Developer Portal only.
- Application owners are allowed to perform member-management actions for their applications.
- Role values will reuse existing application-level role semantics rather than define a new permission model.
- Pending invites are modeled as application-scoped records, not as active members with partial access.
- A platform account created after invitation issuance does not by itself convert the invitation into active application membership.
- Admin feature toggles are environment- or organization-scoped according to existing portal settings patterns unless product states otherwise.

## Dependencies
- Validated Figma designs for the member-management screens
- Backend API work for member queries, invitations, role updates, ownership transfer, and feature-toggle persistence
- Email delivery and invitation token support
- Portal and console permission model alignment
- Documentation work tracked in [APIM-11584](https://gravitee.atlassian.net/browse/APIM-11584)
