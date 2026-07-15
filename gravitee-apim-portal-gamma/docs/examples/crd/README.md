# Full Portal CRD Examples

Reference artifacts for the **Gamma Portal Design System** GitOps CRD format (plan section 9 — Automation / GitOps CRD Format).

## Files

| File | Purpose |
|------|---------|
| [`full-developer-portal.crd.yaml`](./full-developer-portal.crd.yaml) | Multi-document Kubernetes-style export: `Portal`, `PortalListing`, `PortalTheme`, `PortalBlockPage`, `PortalPage`, `PortalDocumentation` |
| [`full-developer-portal.blocks.json`](./full-developer-portal.blocks.json) | Editor block JSON (`PortalBlockPage.spec.document`) extracted for side-by-side comparison |

## How the formats relate

Each block page is exported as **two** CRD documents:

1. **`PortalBlockPage`** — canonical editor/runtime payload. `spec.document` is the BlockNote block array stored in IndexedDB today.
2. **`PortalPage`** — automation-friendly markup. A 1:1 tag projection of the same blocks using `<gravitee-*>` tags (not GMD).

```
Block JSON (PortalBlockPage)          PortalPage markup (CRD)
─────────────────────────────         ─────────────────────────
{ type: "heading", level: 1 }    →    <gravitee-heading level="1" hrid="welcome-title">…</gravitee-heading>
{ type: "graviteeButton", … }    →    <gravitee-button label="…" variant="filled" instance-style-background="hero-btn-bg" />
{ type: "graviteeApiCatalog" }   →    <gravitee-component type="api-catalog" layout="grid" columns="3" />
{ type: "graviteeHtml", html/css } →  <gravitee-html><content>…</content><style>…</style></gravitee-html>
```

### Per-instance styling

Custom variables live in **`PortalTheme.spec.customVariables`**. Blocks reference them by name:

- **Block JSON**: `instanceStyle` prop is a JSON string, e.g. `{"background":"hero-btn-bg"}`.
- **PortalPage markup**: flattened attributes, e.g. `instance-style-background="hero-btn-bg"`.

At render time both resolve to `var(--portal-custom-hero-btn-bg)` on the element.

### What is intentionally not in block JSON

- Theme tokens (tier 1/2) and custom variable **values** → `PortalTheme` CRD only.
- Navigation tree, footer links, API listings → `Portal` and `PortalListing` CRDs.
- OpenAPI/HTML page bodies → `PortalDocumentation` CRD (`type: OPENAPI` or `type: HTML`).

## Document inventory (this example)

| Kind | Count | Notes |
|------|-------|-------|
| Portal | 1 | Sidebar layout, nested navigation, footer + user menu links |
| PortalListing | 1 | Two API entries under `/apis` |
| PortalTheme | 1 | Sparse foundation + element overrides + 4 custom variables |
| PortalBlockPage | 2 | Home + Quick Start block documents |
| PortalPage | 2 | Markup projection of the block pages above |
| PortalDocumentation | 2 | Commerce OpenAPI spec + standalone HTML guide |
