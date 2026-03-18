# APIM-10345 Invitation Refinement

## Meeting Date
- `2026-03-23`

## Goal
- Align on invitation-related requirements, lifecycle states, and UX expectations before implementation continues.

## Agenda

### Missing Story:  Delete existing member
- There does not seem to be a dedicated standalone story for deleting an existing member, even though this action appears in the member-table actions.

### `APIM-12767` View pending invitation dashboard
- Agreed point: the `Type` column should not be shown.
- Current gap: there is no invitation-specific mockup available.
- Current mismatch: the story description mentions a `Resend` button, but it does not appear anywhere in the available mockups.
- Please clarify the acceptance criterion `Once the invited member has accepted the invitation, the same shall be displayed under "Registered Members" tab` so that it explicitly states that invitation acceptance should create the corresponding application member and remove the invitation record.
- As a consequence, we do not support the acceptance criterion `In case the invited member has not accepted the invitation however has registered then the same shall be displayed under the “Invited members” tab however the Type=Registered.`, since the `Type` column will not be presented.

### Missing Story: Search among invitations
- There does not seem to be a dedicated story for searching among invitations, analogous to searching among registered members.

### `APIM-12766` Invite new user via email
- Current implementation note: when creating an invitation, the backend already checks whether a user with the given email exists, and if it does, it adds the user as a member instead of creating and sending an invitation.
- Please clarify whether invitations are expected to be added one by one.
- If not, is there a mockup that shows a flow with multiple invitations prepared before sending?

### `APIM-11531` Acceptance of invitation
- Please clarify what exactly should happen after the invited user clicks the invitation acceptance link.
- It would be helpful to specify which page the user should be redirected to after acceptance.
- It would be helpful to have a mockup or explicit description of what information or confirmation message the user should see after accepting the invitation.

### `APIM-12768` Delete pending invitation
- Please clarify what should be shown to a user who clicks an invitation link that has already been deleted.

### `APIM-12771` Update roles for invited members
- To be defined.

### `APIM-12772` Transfer ownership to registered member
- Please clarify which role should be assigned to the user who gives up the `PRIMARY_OWNER` role.

### Mislinked stories
- Can `APIM-12773` and `APIM-12774` be unlinked from this Epic, since they seem outside the member-mapping scope and currently create confusion?


## Decisions Made
- Missing Story:  Delete existing member -> added
- A "Resend" button must be available to refresh the 7-day expiration timer and trigger a new email - If technically feasible with minimal efforts. -> moved to nother story
- `APIM-12768` Delete pending invitation - curent implementation needs to be explored
- Please clarify which role should be assigned to the user who gives up the `PRIMARY_OWNER` role -> OWNER


## Open Questions
- None captured yet.

## Follow-Up Actions
- Update the affected story files after the meeting if scope or wording changes.
- Create a decision record in `../decisions/` if the meeting produces a durable Epic-level decision.
