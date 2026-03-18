# APIM-12765 Add Registered User to Application

## Jira Story Reference
- Story: [APIM-12765](https://gravitee.atlassian.net/browse/APIM-12765)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers granting application access to existing registered platform users.

## Design References
### Screen 01 `../screens/Member - Add Dropdown-1.png`
![APIM-12765 entry point](../screens/Member%20-%20Add%20Dropdown-1.png)
- Coverage: this screen shows the add-members entry point used for the registered-user add flow.

### Screen 02 `../screens/Member - Add Dropdown - User Search Modal.png`
![APIM-12765 modal screen](../screens/Member%20-%20Add%20Dropdown%20-%20User%20Search%20Modal.png)
- Coverage: this screen shows the modal before user selection and before the add action is confirmed.
- Observed mismatch: the export does not show post-submit success feedback, which is required by `Acceptance Criteria`.

### Screen 03 `../screens/Member - Add Dropdown - User Search Modal-1.png`
![APIM-12765 filled modal](../screens/Member%20-%20Add%20Dropdown%20-%20User%20Search%20Modal-1.png)
- Coverage: this is the strongest reference for selected users and role assignment before submission.
- Observed mismatch: the export supports multi-select visually, but it does not confirm the final success-message pattern or the exact default role value after submission.

## Functional Requirements
### Member creation path
The owner must be able to add one or more already registered platform users to the application from the members area.

### Role assignment at add time
The add-member flow must assign an application role as part of the submission, rather than forcing the user to perform a separate role-edit action immediately after the member is created.

The current Jira wording further constrains this selector. The default value must be the lowest role present in the system, and `PRIMARY OWNER` or the equivalent default owner role must not be offered as a selectable value in this flow.

## Acceptance Criteria
- Clicking "Add members" links the selected user or users to the application.
- Added users default to the lowest role present in the system unless another role is selected.
- `PRIMARY OWNER` or the equivalent default owner role is not displayed in the role selector.
- A success confirmation is shown after the operation succeeds, using the approved portal feedback pattern rather than assuming a toast implementation.

## Edge Cases and Failure Cases
- Multiple users are added in one action.
- One or more selected users were added by another admin just before submission.
- Partial backend failures must not create an ambiguous UI state.

## Open Questions
### APIM-12765-OQ-1
- Question: What is the exact default role name and value?
- Answer: Jira now defines the default as the lowest role present in the system. The exact display label still depends on the configured role set, but the selection rule is resolved.
- Status: Resolved

### APIM-12765-OQ-2
- Question: What is the approved success-message pattern for single and bulk add operations?
- Answer: Jira notes that the original toast wording should be challenged with UX and may need to support bulk additions.
- Status: Open

## Test Considerations
- Verify single-user add and bulk add.
- Verify role assignment at creation time.
- Verify duplicate-prevention and retry behavior.
