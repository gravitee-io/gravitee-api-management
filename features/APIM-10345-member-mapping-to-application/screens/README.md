# Screens Directory

This directory is reserved for exported screen assets from Figma for Epic `APIM-10345`.

Current design anchors referenced by the requirements:
- Member-management anchor frame: <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=41780-5585&t=uovLbARmhIgnnWov-4>
- Application-creation baseline: <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=35471-46251&t=km886KcJVeA9B9nv-0>

Recommended usage:
- Export the exact frames used as source of truth for implementation into this directory.
- Use stable, descriptive filenames that can be referenced from story files.
- Update each story file to reference the concrete exported screen once it exists.

Suggested naming pattern:
- `<order>-<area>-<state>.png`
- Example: `01-members-overview-default.png`
- Example: `02-members-search-results.png`
- Example: `03-members-invite-dialog.png`
- Example: `04-members-pending-invites.png`
- Example: `05-members-transfer-ownership.png`
