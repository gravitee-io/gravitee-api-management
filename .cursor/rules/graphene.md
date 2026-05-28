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

### Layout

- Content max width is defined on the theme token `--container-content` (maps to `--content-max-width`). Use the max-width utility Tailwind emits for that token in your setup.

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
<AppLayout>
  <AppSidebar>...</AppSidebar>
  <AppLayout.Main>
    <ContentHeader title="..." />
    {/* page content */}
  </AppLayout.Main>
</AppLayout>

/* Bad — one-off shell with random spacing and widths */
<div className="flex h-screen">
  <aside className="w-64" />
  <main className="p-6 flex-1" />
</div>
```

**Linear breadcrumbs:** When each step maps to a **path string** and React Router’s `navigate`, use **`buildLinearBreadcrumbs(navigate, segments)`** so `ContentHeader` / `useLayoutConfig` get consistent `BreadcrumbEntry[]` without copying the same `onClick` wiring. If the first crumb is a **custom action** (e.g. “back” that is not `navigate('/path')`), build that entry by hand or mix with `buildLinearBreadcrumbs`—see Storybook **Composed/ContentHeader → Linear breadcrumbs from builder** for the canonical route-based pattern. API edge cases are covered in `packages/core/src/lib/breadcrumbs/buildLinearBreadcrumbs.test.ts`.

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
