# APIM-12888 Administrative Toggle Controls

## Jira Story Reference
- Story: [APIM-12888](https://gravitee.atlassian.net/browse/APIM-12888)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story controls the rollout and governance of the feature.

## Design References
### Screen Status
- No concrete admin-settings Figma screen is linked yet.
- No exported admin-settings screen exists yet in `../screens/`.
- Observed mismatch: the current design set does not visually cover any of the required toggle controls, default states, or admin placement decisions from `Functional Requirements`.

## Functional Requirements
### Toggle scope
Administrators must be able to enable or disable member mapping, invitation sending, and transfer of ownership independently. These capabilities must not be forced behind a single combined switch.

### Runtime behavior
Toggle state must persist and affect portal visibility without a manual restart. The portal should react to the stored toggle state as part of normal application behavior rather than through operational intervention.

### Auditability
Toggle changes must be audit logged so rollout and governance decisions are traceable.

## Acceptance Criteria
- The admin console exposes three toggles:
- `Enable/Disable Team Member Mapping`
- `Enable/Disable Team Invitation Sending`
- `Enable/Disable Transfer of Ownership`
- All toggles are disabled by default.
- When disabled, related portal UI elements are hidden or inaccessible.
- When enabled, related portal functionality is available to authorized users.
- Toggle changes are logged with user, timestamp, action, and module name.
- Toggle state persists across sessions and system restarts.
- Changes take effect in real time or near real time.

## Edge Cases and Failure Cases
- Toggle is changed while users are already on the affected portal page.
- Audit logging fails even though the toggle state changes.
- Portal and admin console state drift after restart.

## Open Questions
### APIM-12888-OQ-1
- Question: What exact admin settings location should host these toggles?
- Answer: No admin-settings design has been exported yet, so the placement is still unresolved.
- Status: Open

### APIM-12888-OQ-2
- Question: Are toggles scoped per environment, organization, or installation?
- Answer: TBD
- Status: Open

### APIM-12888-OQ-3
- Question: Jira still contains the phrase "two distinct toggles" while listing three toggle names. Which interpretation is correct?
- Answer: The listed toggle names are the stronger and newer signal in the Jira body. Local documentation therefore follows the three-toggle interpretation: member mapping, invitation sending, and transfer of ownership.
- Status: Resolved

## Test Considerations
- Verify default disabled state for all three toggles.
- Verify hide-versus-show behavior in the Next Gen Portal.
- Verify persistence, audit logging, and near-real-time propagation.
