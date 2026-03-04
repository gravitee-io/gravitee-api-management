---
name: react-component-scaffold
description: Scaffolds new React components with consistent file structure for the Gamma app or Baros design system. Use when the user asks to create a component, add a new component, scaffold a component, or generate a component in gravitee-apim-gamma-react or gravitee-apim-baros.
---

# React Component Scaffold

## Instructions

When the user asks to create a new React component, follow this workflow. The scaffold differs based on which project the component belongs to.

### Step 1: Determine the Target Project

Check the target path provided by the user:

- **Gamma** (app): path starts with or is inside `gravitee-apim-gamma-react/`
- **Baros** (design system): path starts with or is inside `gravitee-apim-baros/`

If the user does not specify a path, ask which project the component belongs to.

### Step 2: Create the Component Folder

Create a folder named after the component in PascalCase at the target location. Every folder gets these files:

**For both Gamma and Baros:**

```
ComponentName/
  ComponentName.tsx
  ComponentName.test.tsx
  index.ts
```

**Baros adds:**

```
ComponentName/
  ComponentName.tsx
  ComponentName.test.tsx
  ComponentName.stories.tsx    # mandatory for design system
  index.ts
```

### Step 3: Generate Files

#### `ComponentName.tsx` — Gamma (App Component)

```tsx
interface ComponentNameProps {
  readonly children: React.ReactNode;
}

export function ComponentName({ children }: ComponentNameProps) {
  return (
    <div>{children}</div>
  );
}
```

Rules applied:
- Named export, no default export
- Props as standalone `interface` with `readonly` fields
- Semantic HTML elements

#### `ComponentName.tsx` — Baros (Design System Component)

```tsx
import { forwardRef, type ComponentPropsWithRef } from 'react';
import { twMerge } from 'tailwind-merge';
import { cva, type VariantProps } from 'class-variance-authority';

const componentNameVariants = cva('', {
  variants: {
    variant: {
      default: '',
    },
    size: {
      md: '',
    },
  },
  defaultVariants: { variant: 'default', size: 'md' },
});

interface ComponentNameProps
  extends Omit<ComponentPropsWithRef<'div'>, 'className'>,
    VariantProps<typeof componentNameVariants> {
  /** Additional CSS classes to merge with component styles. */
  readonly className?: string;
}

export const ComponentName = forwardRef<HTMLDivElement, ComponentNameProps>(
  ({ variant, size, className, children, ...props }, ref) => (
    <div
      ref={ref}
      className={twMerge(componentNameVariants({ variant, size }), className)}
      {...props}
    >
      {children}
    </div>
  ),
);

ComponentName.displayName = 'ComponentName';
```

Rules applied:
- `forwardRef` wrapping
- `className` prop merged via `twMerge`
- `cva` for variant management
- JSDoc on props
- `displayName` set

#### `ComponentName.test.tsx` — Both Projects

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ComponentName } from './ComponentName';

function setupComponentNameHarness(props?: Partial<ComponentNameProps>) {
  const user = userEvent.setup();
  render(<ComponentName {...props} />);

  return {
    // Actions
    // click: () => user.click(screen.getByRole('button')),

    // Getters
    getRoot: () => screen.getByRole('...'),
  };
}

describe('ComponentName', () => {
  it('renders without crashing', () => {
    const harness = setupComponentNameHarness();
    expect(harness.getRoot()).toBeInTheDocument();
  });
});
```

Rules applied:
- Harness pattern with actions and getters
- `userEvent.setup()` pre-configured
- `getByRole` as primary query

#### `ComponentName.stories.tsx` — Baros Only

```tsx
import type { Meta, StoryObj } from '@storybook/react';
import { ComponentName } from './ComponentName';

const meta = {
  title: 'Components/ComponentName',
  component: ComponentName,
  tags: ['autodocs'],
} satisfies Meta<typeof ComponentName>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
```

#### `index.ts` — Both Projects

```ts
export { ComponentName } from './ComponentName';
export type { ComponentNameProps } from './ComponentName';
```

### Step 4: Update Parent Barrel Export

If a parent `index.ts` exists (e.g., `src/index.ts` for Baros, or a feature folder `index.ts` for Gamma), add the new component to it.

### Step 5: Confirm

After generating all files, summarize what was created and remind the user to:
1. Fill in the component's semantic HTML structure
2. Define appropriate harness actions and getters for the test
3. (Baros only) Add variant stories to the Storybook file
