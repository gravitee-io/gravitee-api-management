# Using Graphene

> **Scope:** These rules only apply when working with `@gravitee/graphene-core` components and styles. Do not apply them to unrelated parts of the codebase.

Browse the live documentation and component catalog on [Storybook](https://graphene.gravitee.io/).

This project uses the Graphene design system (`@gravitee/graphene-core`). Follow these conventions when writing or generating code.

## Package exports

| Export                                                                | What it is                                                                                                                                                    |
| --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `@gravitee/graphene-core`                                             | React components and utilities (`cn`, etc.)                                                                                                                   |
| `@gravitee/graphene-core/styles`                                      | Pre-built CSS (tokens, utilities, base layer). No Tailwind pipeline required in the host to use library styling.                                              |
| `@gravitee/graphene-core/icons`                                       | Icon set used by Graphene components. Browse the catalog in Storybook (Overview / Showcase / Icons).                                                          |
| `@gravitee/graphene-core/fonts`                                       | Loads DM Sans via `@fontsource/dm-sans`. Import once in the app entry.                                                                                        |
| `@gravitee/graphene-core/tailwind-theme`                              | Constrained Tailwind v4 theme: resets default palette/spacing/type and maps utilities to Graphene tokens. Use only if the app runs Tailwind for its own code. |
| `@gravitee/graphene-core/testing`                                     | `renderWithGraphene` plus per-component harnesses (`buttonHarness`, `inputHarness`, …) for tests. See the **Testing** section below.                          |
| `@gravitee/graphene-core/eslint` / `@gravitee/graphene-core/tsconfig` | Shared tooling configs                                                                                                                                        |

## Dependencies (what the host needs)

- **Required:** `react` and `react-dom` (^19).
- **Required:** `@fontsource/dm-sans` (>=5) — loaded by `import '@gravitee/graphene-core/fonts'`.
- Icons: import from `@gravitee/graphene-core/icons` (no separate install needed).
- **Optional:** `tailwindcss` (^4) — only needed if the app imports `@gravitee/graphene-core/tailwind-theme` and processes its own CSS with Tailwind.

Graphene ships pre-compiled `styles`; the host does **not** need PostCSS or `postcss-import` for Graphene’s CSS file itself. If the app uses Tailwind for local files, configure `@tailwindcss/postcss` (or the Vite plugin) for that entry only.

## Integration tiers

Pick the smallest setup that matches how much Tailwind the app uses.

### Tier 1 — Graphene only (no app Tailwind)

Use this when the screen is built only from Graphene components and plain markup.

```ts
// main.ts (order as below)
import '@gravitee/graphene-core/fonts';
import '@gravitee/graphene-core/styles';
```

```tsx
import { Button, Card } from '@gravitee/graphene-core';
```

No `@source` or Tailwind entry is required for Graphene’s own classes to work; `styles` is plain, compiled CSS.

### Tier 2 — App uses Tailwind v4 for local code (recommended for new shells)

Use the constrained theme so utilities in app code map to Graphene tokens (reduces design drift).

**JavaScript entry** — load **app** Tailwind CSS **before** Graphene styles so Graphene’s `@layer base` rules (for example default `border-color`) win over Tailwind preflight.

```ts
import '@gravitee/graphene-core/fonts';
import './styles.css'; // your Tailwind entry — first
import '@gravitee/graphene-core/styles'; // pre-built Graphene CSS — last
```

**`styles.css` (example)**

```css
@import 'tailwindcss';
@import '@gravitee/graphene-core/tailwind-theme';
@source './**/*.{ts,tsx}';
```

Do not add `@source` pointing at Graphene’s package `dist` for styling; Graphene’s utilities are already in `import '@gravitee/graphene-core/styles'`.

## Imports (components)

- All components are named exports from `@gravitee/graphene-core`:

```tsx
import { Button, Badge, Card, Input } from '@gravitee/graphene-core';
```

- Never import from internal paths like `@gravitee/graphene-core/base/Button` or `@gravitee/graphene-core/lib/utils`. These are private and may change without notice.

## Imports (icons)

- Icons are exposed through a dedicated subpath:

```tsx
import { CheckIcon } from '@gravitee/graphene-core/icons';

<CheckIcon aria-hidden="true" />
```

- Graphene components (`Button`, `Item`, `DropdownMenu`, etc.) auto-size child icons to `size-4`. Pass `className="size-N"` to override.
- Decorative icons take `aria-hidden="true"` (as in the example). Icons that carry meaning on their own need an `aria-label` on the parent interactive element — see **Accessibility** below.
- Browse the full set in Storybook (**Overview / Showcase / Icons**). Click any icon in the catalog to copy its import statement.

## Token reference (Tailwind utilities)

The constrained theme exposes only these scales. Prefer them over arbitrary values (`bg-blue-500`, `p-[13px]`, etc.) so UI stays aligned with the system.

**Tier 1** consumers get the semantic-color utilities and the standard layout/spacing scale (`{m,p}{t,b,l,r,x,y}`, `space-{x,y}`, `gap` for sizes 0-12) pre-compiled in the shipped CSS. Per-component utilities (sizing variants, hover states, etc.) only ship if a Graphene primitive uses them — request a primitive or move to Tier 2 if more freedom is needed.

**Tier 2** consumers get every utility their app uses, scoped by the theme. The shared ESLint config below enforces alignment.

The shared ESLint config (`@gravitee/graphene-core/eslint`) enforces this with `better-tailwindcss/no-unknown-classes`. Point its `entryPoint` at a CSS file that imports `@gravitee/graphene-core/tailwind-theme` so lint flags any class outside the allowed scales. Note that the rule catches unknown class names (e.g. `w-99`), not arbitrary values (e.g. `w-[13px]`) - those still compile but should be avoided. Install the peer plugin:

```bash
yarn add -D eslint-plugin-better-tailwindcss
```

Override the `entryPoint` if your CSS lives elsewhere:

```js
// eslint.config.mjs
import graphene from '@gravitee/graphene-core/eslint';

export default [...graphene, { settings: { 'better-tailwindcss': { entryPoint: './src/app.css' } } }];
```

### Colors (semantic)

Maps to CSS variables from Graphene themes. Use utilities like `bg-*`, `text-*`, `border-*`:

`background`, `foreground`, `card`, `card-foreground`, `popover`, `popover-foreground`, `primary`, `primary-foreground`, `secondary`, `secondary-foreground`, `muted`, `muted-foreground`, `accent`, `accent-foreground`, `destructive`, `destructive-foreground`, `success`, `success-foreground`, `warning`, `warning-foreground`, `highlight`, `highlight-foreground`, `border`, `input`, `ring`.

Also: `transparent`, `current`, `black`, `white` (mapped to Graphene neutrals where defined).

### Sidebar

`sidebar`, `sidebar-foreground`, `sidebar-border`, `sidebar-accent`, `sidebar-accent-foreground`, `sidebar-primary`, `sidebar-primary-foreground`, `sidebar-ring`.

### Spacing

Scale: `0`, `0.5`, `1`, `1.5`, `2`, `2.5`, `3`, `3.5`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, `12`, `16`, `20`, `24`, `32`, `48`, `64`, `72`, `80`, `96`, `px`, plus layout tokens `content`, `topnav`, `sidebar-height`, `context-sidebar` (e.g. `p-4`, `gap-2`, `h-topnav`, `w-80`).

### Typography

- **Font:** `font-sans` (DM Sans stack when fonts are loaded), `font-mono` (system monospace stack).
- **Size:** `text-xs` through `text-3xl` (Graphene type scale).

### Radius

`rounded-sm`, `rounded-md`, `rounded-lg`, `rounded-xl` (mapped to semantic `--radius`).

### Layout — content width system

All pages share a single uniform centered container. No per-page width decisions needed — the container never changes between sibling pages, eliminating layout shifts during navigation.

**Decision rule:**

1. Is this a tool/workspace layout, observability dashboard, or log/trace explorer? → `useLayoutConfig({ contentVariant: 'full-bleed' })`
2. Is this a single-column creation wizard or stepper? → Wrap content in `<PageFocused>`
3. Everything else → Do nothing. The default centered container handles tables, forms, dashboards, and settings equally.

```tsx
import { PageFocused } from '@gravitee/graphene-core';

// Wizard/creation page — focused and centered
function CreateApiPage() {
  return (
    <PageFocused>
      <StepProgress steps={steps} activeStep={step} />
      <StepContent />
    </PageFocused>
  );
}

// Everything else — just render content directly
function ApisPage() {
  return (
    <div className="space-y-4">
      <h2>APIs</h2>
      <DataTable ... />
    </div>
  );
}
```

Do NOT set `max-w-*` classes on page wrappers — the design system handles content width via `AppLayout`.

## Styling

- Use semantic design tokens for colors, not raw Tailwind defaults:

```tsx
/* Good */
<div className="bg-primary text-primary-foreground" />
<div className="border-border text-muted-foreground" />
<div className="bg-destructive text-destructive-foreground" />

/* Bad - unconstrained palette */
<div className="bg-blue-600 text-white" />
<div className="border-gray-200 text-gray-500" />
```

- Use the `cn()` utility to merge Tailwind classes. Import it from `@gravitee/graphene-core`:

```tsx
import { cn } from '@gravitee/graphene-core';

<div className={cn('rounded-lg p-4', isActive && 'bg-accent', className)} />;
```

- Dark mode is driven by semantic tokens and the `dark` class on the root. Prefer tokens over ad hoc `dark:` utilities unless you need a one-off exception.

## Layout composition

Graphene has two layers:

- **Base** components (Button, Input, Card, Badge…) are primitives. Use them directly.
- **Composed** components (`AppLayout`, `AppSidebar`, `ContentHeader`, …) encode shell layout: top navigation height, sidebar width, content padding, and main/column structure.

**Prefer composed layout components** for application shells instead of re-implementing the same grid/flex and spacing:

```tsx
/* Good — use AppLayout pieces as in Storybook */
<AppLayout
  sidebar={<AppSidebar onLogoClick={() => navigate('/')} renderNavigation={() => <SidebarNavigation ... />} />}
  subheader={<ContentHeader appContext={<AppContextBar ... />} breadcrumbs={breadcrumbs} />}
>
  {/* page content */}
</AppLayout>

/* Bad — one-off shell with random spacing and widths */
<div className="flex h-screen">
  <aside className="w-64" />
  <main className="p-6 flex-1" />
</div>
```

**`onLogoClick`** — pass a callback so the Gravitee logo navigates home via your SPA router. When omitted the logo is non-interactive (decorative).

```tsx
<AppSidebar onLogoClick={() => navigate('/')} renderNavigation={...} />
```

### Migrating the shell layout

App and environment switching has moved from the sidebar header into the top content header via the new `AppContextBar` component and `ContentHeader`'s `appContext` slot. `AppSidebar` is now a pure navigation container — the active application identity lives in `ContentHeader` where it remains visible regardless of sidebar collapse state.

**`AppContextBar` props:**

| Prop | Type | Required | Notes |
| --- | --- | --- | --- |
| `apps` | `AppDefinition[]` | Yes | Same type used by `AppSwitcher` |
| `activeAppKey` | `string` | Yes | |
| `onAppChange` | `(key: string) => void` | No | |
| `environments` | `SelectorOption[]` | No | Only rendered when provided and non-empty |
| `activeEnvironmentKey` | `string` | No | |
| `onEnvironmentChange` | `(key: string) => void` | No | |

**Before:**

```tsx
<AppLayout
  sidebar={
    <AppSidebar
      apps={apps}
      activeAppKey={activeAppKey}
      onAppChange={handleAppChange}
      environments={envItems}
      activeEnvironmentKey={envHrid}
      onEnvironmentChange={handleEnvironmentChange}
      renderNavigation={() => <SidebarNavigation ... />}
    />
  }
  subheader={
    <ContentHeader leading={slots.leading} breadcrumbs={slots.breadcrumbs} trailing={...} />
  }
>
```

**After:**

```tsx
<AppLayout
  sidebar={
    <AppSidebar onLogoClick={() => navigate('/')} renderNavigation={() => <SidebarNavigation ... />} />
  }
  subheader={
    <ContentHeader
      leading={slots.leading}
      appContext={
        <AppContextBar
          apps={apps}
          activeAppKey={activeAppKey}
          onAppChange={handleAppChange}
          environments={envItems}
          activeEnvironmentKey={envHrid}
          onEnvironmentChange={handleEnvironmentChange}
        />
      }
      breadcrumbs={slots.breadcrumbs}
      trailing={...}
    />
  }
>
```

**Module federation:** `appContext` is host-owned. The host shell passes `<AppContextBar>` directly to `ContentHeader` where it renders the `subheader` slot. Federated modules set `navigation`, `breadcrumbs`, `leading`, `contextSidebar`, `viewMode`, `contextExpanded`, and `contentVariant` via `useLayoutConfig` — they do not set `appContext`.

**Local dev shells:** Module-local dev shells (`LocalDevShell`) that previously passed `apps`/`activeAppKey` to `AppSidebar` need the same migration. For minimal single-app dev setups, `AppContextBar` can be omitted entirely — just remove the old props from `AppSidebar`.

### Breadcrumbs

When using `AppContextBar` in the `appContext` slot, the active application name is already visible in the header bar. Remove the app/module name (e.g. "API Management", "Access Management") from your first breadcrumb segment to avoid duplication. Breadcrumbs should start at the **page level**:

```tsx
/* Good — starts at the page, app name is in AppContextBar */
breadcrumbs={[
  { label: 'API List', onClick: () => navigate('/apis') },
  { label: 'Payment Service' },
  { label: 'Overview' },
]}

/* Bad — duplicates the app name already visible in AppContextBar */
breadcrumbs={[
  { label: 'API Management' },   // ← remove this
  { label: 'API List', onClick: () => navigate('/apis') },
  { label: 'Payment Service' },
  { label: 'Overview' },
]}
```

If you have not migrated to `AppContextBar` yet, keeping the app name as the first breadcrumb is fine as a transitional step.

**Linear breadcrumbs:** When each step maps to a **path string** and React Router’s `navigate`, use **`buildLinearBreadcrumbs(navigate, segments)`** so `ContentHeader` / `useLayoutConfig` get consistent `BreadcrumbEntry[]` without copying the same `onClick` wiring. If the first crumb is a **custom action** (e.g. “back” that is not `navigate('/path')`), build that entry by hand or mix with `buildLinearBreadcrumbs`—see Storybook **Composed/ContentHeader → Linear breadcrumbs from builder** for the canonical route-based pattern. API edge cases are covered in `packages/core/src/lib/breadcrumbs/buildLinearBreadcrumbs.test.ts`.

### `contentVariant` — content width

`AppLayout` accepts `contentVariant` to control content area width and padding:

- `"default"` (default) — centered container with standard padding. All standard pages.
- `"full-bleed"` — no max-width, no padding; content spans edge-to-edge. Tool layouts, observability dashboards, log/trace explorers.

```tsx
// Default — no change needed, AppLayout applies the centered container automatically
<AppLayout>
  <DetailPage />
</AppLayout>

// Full-bleed — for tool layouts and observability pages
useLayoutConfig({ contentVariant: 'full-bleed' }, []);
```

With `useLayoutConfig` (module federation), a nested page can set `contentVariant` without affecting other layout slots owned by parent components. Each hook only resets the keys it owns on unmount.

For creation wizards and steppers, wrap content in `<PageFocused>` inside the default container — see **Layout — content width system** above.

### `banner` — full-width status slot

`AppLayout` accepts a `banner` prop that renders a full-width region above the padded content wrapper but inside the scroll container. Use it for persistent status indicators with an action (deploy status, environment warnings).

```tsx
// Direct prop usage
<AppLayout banner={<DeployStrip />} bannerSticky>
  <DetailPage />
</AppLayout>

// Module federation via useLayoutConfig
useLayoutConfig({
  banner: deployState === 'NEED_REDEPLOY' ? (
    <DeployStrip onDeploy={handleDeploy} isPending={isPending} />
  ) : null,
  bannerSticky: true,
}, [deployState, handleDeploy, isPending]);
```

The banner spans full width regardless of `contentVariant`. When `bannerSticky` is true, it sticks to the top of the scroll area so the action remains reachable while scrolling.

**Button hierarchy:** When a banner contains an action button alongside page content that has its own primary buttons, use an outline treatment on the banner button to avoid competing with the page's primary CTA:

```tsx
function DeployStrip({ onDeploy, isPending }) {
  return (
    <div className="flex items-center gap-2 border-b border-border px-5 py-1.5">
      <span className="size-1.5 shrink-0 rounded-full bg-warning" />
      <span className="text-sm text-muted-foreground">Undeployed changes</span>
      <div className="flex-1" />
      <button
        type="button"
        className="rounded-md border border-warning/25 bg-warning/5 px-2.5 py-0.5 text-sm font-semibold text-warning-foreground transition-colors hover:bg-warning/10 disabled:opacity-50"
        onClick={onDeploy}
        disabled={isPending}
      >
        {isPending ? 'Deploying…' : 'Deploy API'}
      </button>
    </div>
  );
}
```

See Storybook **Composed/AppLayout → BannerSlot** for the full interactive example.

### Adopting the content width system in consumer modules

Follow these steps to apply the layout width system to an existing consumer module:

1. **Identify all pages** in your module that render inside `AppLayout`.
2. **Categorize each page:**
   - **Default** — list pages, detail views, forms, settings, dashboards. No change needed; `AppLayout` applies the centered container automatically.
   - **PageFocused** — single-column creation wizards or steppers. Wrap content in `<PageFocused>`.
   - **Full-bleed** — tool layouts (Policy Studio), observability dashboards, log/trace explorers. Add `useLayoutConfig({ contentVariant: 'full-bleed' }, [])` if not already set.
3. **Remove ad-hoc width constraints** — delete any `max-w-*` classes on page-level wrapper divs. The design system owns content width.
4. **Verify** — resize the browser to confirm content stays centered without clipping on narrow viewports.

See `packages/core/snippets/data-table-list-page.tsx` and `creation-wizard-page.tsx` for complete examples.

### Data table (entity list pages)

Use `DataTable` for any entity list — whether the data is fetched page-by-page from an API or loaded in full on the client. The component provides sorting, filtering, pagination, column visibility, row selection, and bulk actions through a composable slot API.

**To implement:** copy the snippet from `snippets/data-table-list-page.tsx` and replace the entity type, column definitions, and fetch function with your data. See Storybook **Patterns/Data Table → ApiList** for the full interactive example.

#### When to use DataTable vs raw Table

| Use `DataTable` | Use raw `Table` |
|---|---|
| Any list that could exceed 5 rows | Key-value detail panels |
| Needs sorting, filtering, or actions | Static configuration displays |
| Data from an API (server-side) | Inline form grids with <5 fixed rows |
| Static arrays with >10 items (client-side) | |

#### Server-side vs client-side

`DataTable` works in two modes:

| | Server-side (`serverSide` prop) | Client-side (default) |
|---|---|---|
| **When** | Data fetched page-by-page from an API | All data loaded at once |
| **Sorting** | Pass `sorting` + `onSortingChange`, refetch on change | Automatic (TanStack `getSortedRowModel`) |
| **Filtering** | Refetch with filter params | Automatic (TanStack `getFilteredRowModel`) |
| **Pagination** | Pass current page slice to `data` | Pass full array; TanStack paginates |
| **`data` prop** | Current page only (e.g. 10 rows) | Entire dataset |

**Never** mix modes: client-side sort on server-paginated data only sorts the visible page.

#### Pagination

- Default page size: **10** (standard lists), **25** (high-density: catalog).
- Page size options: `[10, 25, 50, 100]`.
- **Recommended:** Use the `pagination` prop for standard pagination (both server-side and client-side):

```tsx
<DataTable
  pagination={{
    page,
    pageSize,
    totalCount,
    onPageChange: setPage,
    onPageSizeChange: (size) => { setPageSize(size); setPage(1); },
    pageSizeOptions: [10, 25, 50, 100],
  }}
/>
```

- The `footer` slot is still available for custom content (rendered above pagination when both are set).
- Reset to page 1 when search/filter/sort criteria change.
- Works for both **server-side** (pass current page of data) and **client-side** (slice data yourself) pagination.

#### Search and filters

- Search and filters live in the DataTable `toolbar` slot.
- Search field: `h-8 w-64` consistent sizing. Debounce: **300ms** for server-side search.
- **Do NOT add a search field to a server-paginated table unless the backend endpoint supports a text search parameter** (`query`, `q`, or equivalent). Client-side search on a single page of server-paginated data is misleading — users will miss matches on other pages. If the backend does not support search, either request the backend change or omit the search field entirely.
- Use `FacetedFilter` for multi-select categorical filters.
- Show a "Reset" button when any filter is active.

#### Row navigation

Use **name-column-as-link**: the primary identifier column (always leftmost) is a clickable link with `font-medium hover:underline`. No full-row-click.

#### Actions column

- Always the rightmost column. `enableSorting: false`, `enableHiding: false`.
- Header: `<span className="sr-only">Actions</span>`.
- **Always use the three-dot menu** (`MoreVerticalIcon` + `DropdownMenu`) — never a horizontal row of inline icon buttons. This applies even for 2 actions.
- **Single-action exception:** exactly 1 action may render as a lone icon button without a dropdown.
- **Quick-action escape hatch:** one high-discoverability action (e.g. "Deploy") may appear as an inline icon *alongside* the dropdown, but the dropdown must still exist.
- Use `MoreVerticalIcon` (⋮) for table rows. Do not use `MoreHorizontalIcon` — the horizontal variant is for toolbars and card headers.
- Destructive actions go last in the menu, after a `DropdownMenuSeparator`.

#### Column ordering

1. Checkbox (if `selectionMode="multi"`) — auto-prepended
2. Name/primary identifier (link)
3. Status badge
4. Category/type badges
5. Dates (relative format)
6. Owner/actor
7. Actions (rightmost)

**Minimum 3 data columns** (excluding actions). If an entity has fewer than 3 meaningful attributes, use a simpler component (Item list, card grid) instead of DataTable.

Target **4-7 visible columns**. More than 7 → enable column visibility and hide low-priority columns by default. Do NOT show raw entity IDs as visible columns.

#### Cell renderers

Import from `@gravitee/graphene-core/composed/DataTable` (or the main package):

| Renderer | Use for | Behavior |
|---|---|---|
| `DateCell` | Timestamps | Relative format ("2h ago") + full date tooltip |
| `BadgeCell` | Status/category | Truncation-safe badge with variant support |
| `MonoCell` | IDs, paths, tokens | Monospace, character-limit truncation + tooltip |
| `CopyableCell` | API keys, IDs | Mono text + copy button on hover |
| `TruncatedCell` | Long text | Max-width constraint + tooltip on overflow |

#### Empty states

Use `DataTableEmptyState` — it enforces the correct structure for both scenarios:

**`variant="first-use"`** — collection is genuinely empty (user has never created entities)
- Render **instead of** `DataTable` (no table chrome needed)
- Wrap in `<div className="rounded-lg border">` for containment
- Props: `icon`, `title`, `description`, `primaryAction`, optional `secondaryAction`, optional `children` for educational content (flow diagrams, feature pillars)
- Decision: `totalCount === 0 && !hasActiveFilters`

**`variant="no-results"`** — active filters/search returned zero matches
- Render **inside** `DataTable` as the `emptyMessage` prop (keep toolbar visible)
- Props: `icon` (use `SearchIcon`), `title`, `description`, `action` (e.g. "Clear filters")
- Decision: `filteredCount === 0 && hasActiveFilters`

```tsx
// Page-level decision pattern
{isFirstUse ? (
  <div className="rounded-lg border">
    <DataTableEmptyState variant="first-use" icon={<GlobeIcon />} title="No APIs yet" ... />
  </div>
) : (
  <DataTable
    emptyMessage={<DataTableEmptyState variant="no-results" icon={<SearchIcon />} ... />}
    ...
  />
)}
```

See Storybook **Composed/DataTableEmptyState → Integration** for the full interactive example.

#### Header button coordination

Pages with a primary "Add Entity" button in the header follow one rule: **the header button only appears when data exists**. During first-use, the empty state owns the primary CTA.

| State | Header button | Content area |
|---|---|---|
| **First-use** (`totalCount === 0 && !hasFilters`) | Hidden | `DataTableEmptyState variant="first-use"` with primary CTA |
| **Has data** (`totalCount > 0`) | Primary `+ Add Entity` | DataTable |
| **No results** (filters active, zero matches) | Primary `+ Add Entity` | `DataTableEmptyState variant="no-results"` with "Clear filters" |

Both the header button and the empty state CTA trigger the same creation flow. No visual downgrade (both are primary variant buttons). No transition animation between states.

```tsx
<div className="flex items-center justify-between">
  <div>
    <h2 className="text-lg font-semibold">Entities</h2>
    <p className="text-sm text-muted-foreground">Manage your entity catalog.</p>
  </div>
  {!isFirstUse && (
    <Button size="sm"><PlusIcon /> Add entity</Button>
  )}
</div>
```

#### Loading state

- Pass `skeletonCount={pageSize}` to prevent layout shift.
- `loadingDelay={200}` (default) prevents flash on fast responses.

#### `serverSide` prop

Set `serverSide` when data is fetched page-by-page from an API. It disables TanStack's built-in sort/filter/pagination models so you control everything via your fetch logic.

When `serverSide` is **not set** (default), pass the entire dataset to `data` and TanStack handles sorting and filtering automatically. For pagination, slice the array yourself and pass the current page via `data`, using the `pagination` prop to render controls.

### Context sidebar (resource detail pages)

Use `ContextSidebar` when a user **drills down from a list into a single resource** (API, Agent, Application…) that has multiple sub-sections. It provides secondary navigation scoped to that entity — set `viewMode: 'context'` on `AppLayout` and wire `ContextToggleButton` in the `leading` slot of `ContentHeader`.

**Do not use** on list/index pages or pages with fewer than 3 sub-sections (use `Tabs` instead).

**To implement:** copy the snippet from `snippets/context-sidebar-detail-page.tsx` and replace the `{PLACEHOLDER}` markers with your resource data, nav groups, and routes. See Storybook **Composed/ContextSidebar** for interactive examples and **Patterns/Module Federation** for the full federated setup.

Compose with **Card**, **Tabs**, and other primitives inside the regions the layout components provide. Keep page-level spacing on the Graphene scale (`p-4`, `gap-2`, …) from the token table above.

For content blocks, prefer Graphene’s primitives over reimplementing them:

```tsx
/* Good */
<Card>
  <CardHeader>
    <CardTitle>Title</CardTitle>
    <CardDescription>Description</CardDescription>
  </CardHeader>
  <CardContent>...</CardContent>
</Card>

/* Bad - reimplementing Card with raw divs */
<div className="rounded-lg border bg-card p-6 shadow-sm">
  <h3 className="font-semibold">Title</h3>
  <p className="text-muted-foreground">Description</p>
</div>
```

## Component variants

Components use `variant` and `size` props. Use the provided variants, don't recreate them with custom classes:

```tsx
/* Good */
<Button variant="outline" size="sm">Cancel</Button>
<Button variant="destructive">Delete</Button>
<Badge variant="default">Active</Badge>

/* Bad - manually styling what a variant already does */
<Button className="border border-border bg-transparent">Cancel</Button>
```

Common variant values:

- **Button**: `variant`: default, outline, secondary, ghost, destructive, link. `size`: default, xs, sm, lg, icon, icon-xs, icon-sm, icon-lg.
- **Badge**: `variant`: default, secondary, outline, destructive.

## Accessibility

- Always pair inputs with a `<Label>` using `htmlFor`:

```tsx
<Label htmlFor="email">Email</Label>
<Input id="email" type="email" />
```

- Use `Button` for actions, not `<div onClick>` or `<a>` without href.
- Mark decorative icons with `aria-hidden`; icons that carry meaning on their own need an `aria-label` on the parent interactive element.
- Error states use `aria-invalid` and `aria-describedby`:

```tsx
<Input id="email" aria-invalid={!!error} aria-describedby="email-error" />;
{
  error && (
    <p id="email-error" className="text-sm text-destructive">
      {error}
    </p>
  );
}
```

## TypeScript

- Use strict TypeScript, no `any`.
- Component props are typed. Use them as-is, don't `as` cast.
- For extending components, spread remaining props:

```tsx
interface MyButtonProps extends React.ComponentProps<typeof Button> {
  loading?: boolean;
}

function MyButton({ loading, children, ...props }: MyButtonProps) {
  return (
    <Button disabled={loading} {...props}>
      {loading ? 'Loading...' : children}
    </Button>
  );
}
```

## Testing

Graphene exposes a testing toolkit at `@gravitee/graphene-core/testing` so you can write tests for components that consume Graphene without re-implementing setup or DOM queries. It is built on top of React Testing Library (RTL) — bring your own runner and DOM environment.

### Dependencies

Install the RTL family in your app's devDependencies:

```bash
yarn add -D @testing-library/react @testing-library/dom @testing-library/user-event
```

Recommended (not required):

- **`happy-dom`** — DOM environment for your test runner. Implements `ResizeObserver`, `IntersectionObserver`, `matchMedia`, and `scrollIntoView` natively, all of which are needed by Radix-based primitives (Select, DropdownMenu, Dialog, …). `jsdom` works too but you will need to polyfill those APIs yourself.
- **`@testing-library/jest-dom`** — extra matchers like `toHaveFocus`, `toBeInTheDocument`. Optional; if you skip it, replace those matchers with plain attribute checks.
- **MSW** — for mocking HTTP calls. Compose with the harnesses; Graphene does not bundle a network mock.

### Vitest setup (typical)

```ts
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'happy-dom',
    setupFiles: ['./vitest.setup.ts'],
  },
});
```

```ts
// vitest.setup.ts
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// happy-dom does not implement the Pointer Capture API; Radix primitives (Select, DropdownMenu, …) call into it on pointer events.
// Stub as no-ops so user.click() can drive Radix triggers in tests.
if (typeof Element !== 'undefined' && typeof Element.prototype.hasPointerCapture !== 'function') {
  Element.prototype.hasPointerCapture = () => false;
  Element.prototype.setPointerCapture = () => {};
  Element.prototype.releasePointerCapture = () => {};
}

afterEach(() => {
  cleanup();
});
```

### Writing tests

`renderWithGraphene` mounts your component inside a Graphene `ThemeProvider`. Each interactive primitive has a matching `xHarness({ name?, selector?, within? })` factory you attach **after** the render to interact with it.

```tsx
import { describe, expect, it, vi } from 'vitest';
import {
  renderWithGraphene,
  buttonHarness,
  inputHarness,
} from '@gravitee/graphene-core/testing';
import { LoginForm } from './LoginForm';

it('submits credentials', async () => {
  const onSubmit = vi.fn();
  renderWithGraphene(<LoginForm onSubmit={onSubmit} />);

  await inputHarness({ name: /email/i }).type('me@test.com');
  await inputHarness({ name: /password/i }).type('s3cret');
  await buttonHarness({ name: /log in/i }).click();

  expect(onSubmit).toHaveBeenCalledWith({ email: 'me@test.com', password: 's3cret' });
});
```

### Query rules

`xHarness({...})` accepts three filters (combined with AND):

| Filter      | Use                                                                                                               |
| ----------- | ----------------------------------------------------------------------------------------------------------------- |
| `name`      | Accessible name (label, `aria-label`, etc.). **Preferred** — RTL priority #1.                                    |
| `selector`  | CSS selector escape hatch (`#id`, `[data-testid="x"]`, `.foo`). Use when accessible name is ambiguous.            |
| `within`    | Restrict the search to a sub-tree (e.g. an `aria-label`-ed `<form>` element).                                     |

If zero or more than one element matches when a `selector` is provided, the harness throws a descriptive error.

### Available harnesses

Every interactive base component exports a matching `xHarness` factory — `Button` → `buttonHarness`, `Input` → `inputHarness`, etc. The current list is whatever `@gravitee/graphene-core/testing` re-exports; IDE autocomplete on that import shows them all. Each harness has a few async actions (`click`, `type`, `select`, `toggle`, …) and synchronous getters (`getValue`, `isChecked`, `isDisabled`, …).

A few cases worth remembering:

- **`RadioGroup` exposes two harnesses** — `radioGroupHarness` (group-level: `select(value)`, `getValue`, `getOptions`) and `radioHarness` (single radio: `select`, `isSelected`, `getValue`).
- **`Table` returns a row sub-harness** — `tableHarness().getRow(matcher)` returns a `TableRowHarness` exposing `getCells`, `getCellText(header)`, `getCellElement(header)`. The cell element is the hook to scope another harness inside a specific row (see the **Tables** section below).
- **`DataTable` is composed but exported via `/testing`** — most harnesses are for base primitives, but `dataTableHarness` is exposed because it adds sorting, selection, empty/loading state on top of the inner `<Table>`. Its rows extend `TableRowHarness` with `isSelected`, `select`, `unselect`.
- **`PasswordInput` is composed but exported via `/testing`** — `passwordInputHarness` wraps the inner `<input>` and its reveal toggle so you don't have to know the internal `data-slot`. Adds `toggle()`, `isMasked()`, `isRevealed()`, `hasToggle()`, `getToggleLabel()`, `isToggleDisabled()` on top of the usual `type`/`getValue`/`isDisabled`.
- **Static primitives have no harness** — `Badge`, `Card`, `Skeleton`, `Avatar`, `Spinner`, `Separator`, `Label`, `Kbd`, `Alert`, etc. carry no behavior to drive. Query them directly with RTL's `getByRole` / `getByText`.

### Async data — wait before attaching

Harness queries throw immediately if the element is not in the DOM yet. For components that fetch data on mount, wait first using RTL's native `findBy*` / `waitFor` and **then** attach the harness:

```tsx
renderWithGraphene(<UserList />);

await screen.findByText('Alice');                            // wait for the fetch
await buttonHarness({ name: /edit alice/i }).click();        // safe to attach now
```

Use `findBy*`/`waitFor` for async **outside** the user's action (network, timers). Use `await user.action()` (already built into our actions) for the time it takes a click/type to propagate through React.

### Multiple instances — pick the right filter

When several Graphene primitives sit in the same screen, use `name` (accessible label) first, then `within` to scope to a fieldset/form/section, and `selector` only as a last resort:

```tsx
// Form group with two buttons
const submit = buttonHarness({ name: /submit/i });
const cancel = buttonHarness({ name: /cancel/i });

// Two forms with the same field labels
const editForm = screen.getByRole('form', { name: 'edit' });
inputHarness({ name: /email/i, within: editForm }).type('new@test.com');

// Disambiguate by data-testid (escape hatch)
buttonHarness({ selector: '[data-testid="row-3-delete"]' }).click();
```

## Common mistakes to avoid

- Importing `@gravitee/graphene-core/styles` **before** the app Tailwind entry when using Tier 2 (preflight can override Graphene base styles such as default border color).
- Overriding Graphene component internals with `!important` or deeply nested selectors.
- Creating wrapper components that only pass through all props without adding value.
- Hardcoding colors, spacing, or border-radius outside the exposed token scales.
- Using `default export` instead of named exports.

## Notice

Brand icons shipped under `@gravitee/graphene-core/icons` (Apigee, AWS, Azure, Bitbucket, GCP, GitHub, GitLab, Kafka, Kubernetes) reference trademarks of their respective owners and are included for identification purposes only. No affiliation or endorsement is implied.
