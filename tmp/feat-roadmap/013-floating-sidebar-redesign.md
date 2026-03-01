# 013: Floating Sidebar Redesign

> **Common docs**: [architecture-overview.md](./architecture-overview.md) · [frontend-widget.md](./frontend-widget.md)

## Objective

Refactor the Zee widget from an inline block into a **floating sidebar** that is always available but unobtrusive. A small Floating Action Button (FAB) lives on the right edge of the screen; clicking it toggles a slide-out panel containing the full Zee form (prompt, file upload, preview, accept/reject).

## Motivation

The inline widget takes up significant vertical space and competes with the primary page content. By collapsing it into a persistent, edge-anchored FAB:

- Zee is **always accessible** on every page without scrolling to find it.
- The form slides out only when the user actively wants to generate something.
- Page layout is unaffected — no `zee-widget-container` div needed in host pages.

## Tasks

### Frontend — `zee-widget.component.ts`

- [ ] Add `isOpen: boolean = false` toggle state
- [ ] Add `togglePanel()` method to flip the state
- [ ] Auto-close panel on `reset()` (after accept/reject)

### Frontend — `zee-widget.component.html`

- [ ] Wrap entire widget in a `zee-fab-wrapper` container with `position: fixed`
- [ ] Add the FAB trigger button (✨ `auto_awesome` icon) that is always visible
- [ ] Wrap the form body (idle/loading/preview/error states) in a `zee-panel` div gated by `*ngIf="isOpen"`
- [ ] Add a close (✕) button in the panel header

### Frontend — `zee-widget.component.scss`

- [ ] `zee-fab-wrapper`: `position: fixed; bottom: 24px; right: 24px; z-index: 1000`
- [ ] `zee-fab`: Angular Material FAB with gradient background, glow/pulse effect, hover scale-up
- [ ] `zee-panel`: slide-up from the FAB position, `max-height: 70vh`, `width: 420px`, rounded corners, elevated shadow, `overflow-y: auto`
- [ ] CSS transitions for panel open/close (`transform`, `opacity`)

### Host Pages (5 files)

- [ ] Remove the `<div class="zee-widget-container">` wrapper from all 5 host templates — the floating widget no longer needs positional context from the parent

## Acceptance Criteria

- [ ] FAB is always visible in the bottom-right of the viewport on all 5 pages
- [ ] Clicking FAB opens the panel with a smooth slide-up animation
- [ ] Clicking ✕ or accepting/rejecting closes the panel
- [ ] Panel does not interfere with underlying page scrolling or content
- [ ] FAB has a subtle glow/pulse to look "alive" and clickable

## Affected Files

| File                                         | Change                                      |
| -------------------------------------------- | ------------------------------------------- |
| `zee-widget.component.ts`                    | Add `isOpen` state + `togglePanel()`        |
| `zee-widget.component.html`                  | Wrap in FAB + expanding panel               |
| `zee-widget.component.scss`                  | Fixed positioning, transitions, FAB styling |
| `api-v4-policy-studio-design.component.html` | Remove wrapper div                          |
| `api-plan-edit.component.html`               | Remove wrapper div                          |
| `api-endpoint.component.html`                | Remove wrapper div                          |
| `api-entrypoints-v4-edit.component.html`     | Remove wrapper div                          |
| `api-general-info.component.html`            | Remove wrapper div                          |

## Commit Message

```
feat(zee): refactor widget to floating sidebar with FAB toggle

Replace the inline zee-widget with a fixed-position floating action
button that expands into a slide-out panel. The FAB lives in the
bottom-right corner with a glow effect and toggles the full form
(prompt, file upload, preview, accept/reject) as a floating sidebar.
```
