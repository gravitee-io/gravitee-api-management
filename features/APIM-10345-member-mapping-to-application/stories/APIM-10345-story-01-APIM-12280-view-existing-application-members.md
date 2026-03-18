# APIM-12280 View existing application members

## Jira Story Reference
- Story: [APIM-12280](https://gravitee.atlassian.net/browse/APIM-12280)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story defines the baseline member list visible in the members experience.

## Design References
### Screen 01 `../screens/Member - Populated list-1.png`
![APIM-12280 primary screen](../screens/Member%20-%20Populated%20list-1.png)
- Coverage: this is the clearest baseline members-table screen because it shows registered active members and the default table layout.
- Observed mismatch: this export covers the active-members baseline well, but it predates `APIM-10345-decision-01` and therefore does not by itself define the final relationship to invitations.

### Screen 02 `../screens/Members - Empty.png`
![APIM-12280 empty state](../screens/Members%20-%20Empty.png)
- Coverage: this screen supports the empty-state branch of the members view.
- Observed mismatch: the export confirms the empty view, but it does not evidence the populated column structure or the now-agreed separation between `Members` and `Invitations`.

## Functional Requirements
### Members table baseline
The application owner must be able to open the members area and view existing application members in a table. The baseline interpretation of this story is a table of registered members with active access to the application.

### Table structure and content
The member list must expose core identity and role information together with row-level actions. The table structure must use the columns `Name`, `Email`, `Role`, and `Actions`. The `Name` column must render the best available display label for the member, including avatar when available, while the email address is shown in its own `Email` column.

### Default sorting
The default sort order of the baseline members table must be ascending by member name. Based on the current Jira story wording, the practical interpretation is ordering by first name and then last name for registered active members.

### Protected actions
Row actions must stay available only where the user is allowed to use them. The current Jira story wording states that `Update` and `Delete` actions are hidden for the primary owner.

### Relationship to pending invitations
Per `APIM-10345-decision-01`, this story remains scoped to the `Members` list only. Pending and expired invitations must be exposed through a separate invitations list or section in the same feature area and are not part of the phase 01 members table contract.

## Acceptance Criteria
- A table view displays the columns `Name`, `Email`, `Role`, and `Actions`.
- The `Name` column displays avatar when available and the user name, while email is displayed in the `Email` column.
- The baseline members list is sorted ascending by first name and then last name.
- Pagination is always presented in the members list.
- Row actions are hidden for the primary owner.

## Edge Cases and Failure Cases
- Empty applications with no members beyond the owner.
- Applications that have active members and separate invitations at the same time.
- Permission-denied access must not expose member data.

## Open Questions
### APIM-12280-OQ-1
- Question: Is the owner always included and protected from destructive actions?
- Answer: The current Jira story wording says `Update` and `Delete` actions are hidden for the primary owner, so the owner remains visible but protected from those row actions.
- Status: Resolved

### APIM-12280-OQ-2
- Question: Should invited members appear in this same table or in a separate pending-invites section?
- Answer: `APIM-10345-decision-01` was agreed on `2025-03-18`. The members table stays focused on active members, while invitations move to a separate `Invitations` list or section in the same feature area.
- Status: Resolved

### APIM-12280-OQ-3
- Question: What is the default sort order for the baseline members list?
- Answer: The latest Jira wording for `APIM-12280` says the details shall be ordered by first name and then last name. That Jira decision overrides the earlier generic display-name assumption for the local story description.
- Status: Resolved

### APIM-12280-OQ-4
- Question: If invited members are included in the same table, should the default sorting remain a single ascending display-name order across both registered and invited rows?
- Answer: This is no longer applicable for the first delivery because `APIM-10345-decision-01` confirmed separate `Members` and `Invitations` lists. The members table default sort remains ascending by member display name.
- Status: Resolved

## Test Considerations
- Verify the default list rendering with populated and empty datasets.
- Verify column content and ordering.
- Verify that pagination controls are presented in the members list.
