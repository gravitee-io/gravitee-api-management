/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FilterCondition } from '../filter.model';
import { FilterChipComponent } from './filter-chip.component';

// ─── Story wrapper ────────────────────────────────────────────────────────────

@Component({
  standalone: true,
  selector: 'gd-filter-chip-story-wrapper',
  imports: [FilterChipComponent],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <div class="story-layout">
      <gd-filter-chip [filter]="filter" (removed)="onEvent('removed')" (clicked)="onEvent('clicked')" />

      <div class="story-events">
        <span class="story-events__label">Events</span>
        <span class="story-events__row">
          <span class="story-events__name">clicked()</span>
          <span class="story-events__count">×{{ counts.clicked }}</span>
        </span>
        <span class="story-events__row">
          <span class="story-events__name">removed()</span>
          <span class="story-events__count">×{{ counts.removed }}</span>
        </span>
      </div>
    </div>
  `,
  styles: [
    `
      .story-layout {
        display: inline-flex;
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
      }

      .story-events {
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12px;
        font-family: 'Kanit', 'Roboto', sans-serif;
        color: #555;
      }

      .story-events__label {
        font-weight: 600;
        color: #333;
        margin-bottom: 2px;
      }

      .story-events__row {
        display: flex;
        gap: 6px;
      }

      .story-events__name {
        font-family: monospace;
        font-size: 11px;
        color: #444;
        min-width: 80px;
      }

      .story-events__count {
        color: #888;
      }
    `,
  ],
})
class FilterChipWrapperComponent {
  @Input() filter!: FilterCondition;

  protected counts = { removed: 0, clicked: 0 };

  protected onEvent(name: 'removed' | 'clicked'): void {
    this.counts[name]++;
  }
}

// ─── Args ─────────────────────────────────────────────────────────────────────

interface StoryArgs {
  // Filter Condition
  label: string;
  operator: string;
  valuesInput: string;
  // CSS Tokens
  background: string;
  color: string;
  height: string;
  radius: string;
  paddingH: string;
  fontFamily: string;
  fontSize: string;
  fontWeight: string;
  operatorFontWeight: string;
  borderWidth: string;
  borderColor: string;
}

// ─── Token defaults — mirrors filter-chip.component.scss ─────────────────────

const TOKEN_DEFAULTS: Pick<
  StoryArgs,
  | 'background'
  | 'color'
  | 'height'
  | 'radius'
  | 'paddingH'
  | 'fontFamily'
  | 'fontSize'
  | 'fontWeight'
  | 'operatorFontWeight'
  | 'borderWidth'
  | 'borderColor'
> = {
  background: '#fff3eb',
  color: '#f15115',
  height: '24px',
  radius: '6px',
  paddingH: '8px',
  fontFamily: 'inherit',
  fontSize: '12px',
  fontWeight: '600',
  operatorFontWeight: '400',
  borderWidth: '0px',
  borderColor: '#f15115',
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

function buildFilter(args: Pick<StoryArgs, 'label' | 'operator' | 'valuesInput'>): FilterCondition {
  return {
    field: 'HTTP_STATUS',
    label: args.label,
    operator: args.operator,
    values: args.valuesInput
      .split(',')
      .map(v => v.trim())
      .filter(Boolean),
  };
}

function buildTokenStyle(args: Omit<StoryArgs, 'label' | 'operator' | 'valuesInput'>): string {
  return [
    `--gd-filter-chip-background: ${args.background}`,
    `--gd-filter-chip-color: ${args.color}`,
    `--gd-filter-chip-height: ${args.height}`,
    `--gd-filter-chip-radius: ${args.radius}`,
    `--gd-filter-chip-padding-h: ${args.paddingH}`,
    `--gd-filter-chip-font-family: ${args.fontFamily}`,
    `--gd-filter-chip-font-size: ${args.fontSize}`,
    `--gd-filter-chip-font-weight: ${args.fontWeight}`,
    `--gd-filter-chip-operator-font-weight: ${args.operatorFontWeight}`,
    `--gd-filter-chip-border-width: ${args.borderWidth}`,
    `--gd-filter-chip-border-color: ${args.borderColor}`,
  ].join('; ');
}

// ─── Meta ─────────────────────────────────────────────────────────────────────

const COMPONENT_DESCRIPTION = `
\`gd-filter-chip\` displays an active filter condition as a compact chip.
It is a **stateless, presentational component** — it receives a \`FilterCondition\`
and emits two events; all state management is the consumer's responsibility.

---

### Usage

\`\`\`html
<gd-filter-chip
  [filter]="myCondition"
  (clicked)="openEditModal(myCondition)"
  (removed)="removeFilter(myCondition.field)"
/>
\`\`\`

\`\`\`typescript
import { FilterChipComponent, FilterCondition } from '@gravitee/gravitee-dashboard';

const condition: FilterCondition = {
  field: 'HTTP_STATUS',
  label: 'Status Code',
  operator: 'IN',
  values: ['200', '204'],
};
\`\`\`

---

### Label display rules

| Condition                         | Label displayed              | Example                        |
|-----------------------------------|------------------------------|--------------------------------|
| Single value, scalar operator     | name op value                | \`Status Code = 200\`            |
| Single value, \`IN\`                | displayed as \`=\`             | \`Status Code = 200\`            |
| Multiple values, \`IN\`             | name **in** (count)          | \`Status Code in (2)\`           |
| Multiple values, \`NOT_IN\`         | name **not in** (count)      | \`Status Code not in (2)\`       |
| Unknown operator                  | name RAW_OP value            | \`HTTP Path CONTAINS /api\`      |

The full expression (e.g. \`Status Code in [200, 204]\`) is always available in the tooltip.

---

### Theming (CSS custom properties)

Override any token on the host element or a parent selector:

\`\`\`css
gd-filter-chip {
  --gd-filter-chip-background:            #f3e5f5;  /* chip fill */
  --gd-filter-chip-color:                 #6a1b9a;  /* text + icon */
  --gd-filter-chip-height:                24px;
  --gd-filter-chip-radius:                6px;
  --gd-filter-chip-padding-h:             8px;
  --gd-filter-chip-font-family:           inherit;
  --gd-filter-chip-font-size:             12px;
  --gd-filter-chip-font-weight:           600;      /* name + value */
  --gd-filter-chip-operator-font-weight:  400;      /* operator word */
  --gd-filter-chip-border-width:          0px;
  --gd-filter-chip-border-color:          currentColor;
}
\`\`\`

Default colors follow the application's **Angular Material M3 primary palette**
(\`--mat-sys-primary-container\` / \`--mat-sys-on-primary-container\`), so the chip
automatically adapts to the consuming application's theme without extra configuration.

---

### One condition per field

The chip itself is stateless; the **filter-bar** (Phase 2) enforces the constraint
that only one \`FilterCondition\` per \`field\` exists at any time.
`;

export default {
  title: 'Gravitee Dashboard/Components/Filter/Filter Chip',
  component: FilterChipWrapperComponent,
  tags: ['autodocs'],
  decorators: [
    moduleMetadata({
      imports: [FilterChipWrapperComponent],
    }),
  ],
  parameters: {
    docs: {
      description: { component: COMPONENT_DESCRIPTION },
    },
  },
  argTypes: {
    // ── Filter Condition ──────────────────────────────────────────────────────
    label: {
      control: { type: 'text' },
      description: 'Human-readable field name displayed in the chip.',
      table: { category: 'Filter Condition', defaultValue: { summary: 'Status Code' } },
    },
    operator: {
      control: { type: 'select' },
      options: ['EQ', 'NEQ', 'IN', 'NOT_IN', 'GTE', 'LTE', 'CONTAINS'],
      description:
        'Filter operator. `EQ` `NEQ` `GTE` `LTE` render as symbols. `IN` / `NOT_IN` render as words. ' +
        'Unknown values (e.g. `CONTAINS`) fall back to their raw name.',
      table: { category: 'Filter Condition', defaultValue: { summary: 'EQ' } },
    },
    valuesInput: {
      name: 'values',
      control: { type: 'text' },
      description: 'Comma-separated list of values (e.g. `200, 204`). Multiple values trigger the count display.',
      table: { category: 'Filter Condition', defaultValue: { summary: '200' } },
    },
    // ── CSS Tokens ────────────────────────────────────────────────────────────
    background: {
      name: '--gd-filter-chip-background',
      control: { type: 'color' },
      description: 'Chip fill color.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.background } },
    },
    color: {
      name: '--gd-filter-chip-color',
      control: { type: 'color' },
      description:
        'Text and remove icon color. Always set together with `--gd-filter-chip-background` to ensure WCAG contrast (min 4.5:1).',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.color } },
    },
    height: {
      name: '--gd-filter-chip-height',
      control: { type: 'text' },
      description: 'Chip height.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.height } },
    },
    radius: {
      name: '--gd-filter-chip-radius',
      control: { type: 'text' },
      description: 'Border radius.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.radius } },
    },
    paddingH: {
      name: '--gd-filter-chip-padding-h',
      control: { type: 'text' },
      description: 'Left padding of the label area.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.paddingH } },
    },
    fontFamily: {
      name: '--gd-filter-chip-font-family',
      control: { type: 'text' },
      description: "Font family. Defaults to `inherit` — picks up the consuming application's body font.",
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.fontFamily } },
    },
    fontSize: {
      name: '--gd-filter-chip-font-size',
      control: { type: 'text' },
      description: 'Font size.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.fontSize } },
    },
    fontWeight: {
      name: '--gd-filter-chip-font-weight',
      control: { type: 'select' },
      options: ['400', '500', '600', '700'],
      description: 'Font weight applied to the **name** and **value** parts of the label.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.fontWeight } },
    },
    operatorFontWeight: {
      name: '--gd-filter-chip-operator-font-weight',
      control: { type: 'select' },
      options: ['300', '400', '500', '600'],
      description:
        'Font weight of the operator word (`in`, `not in`, `=`, `≥`…). Typically lighter than the name/value to visually recede.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.operatorFontWeight } },
    },
    borderWidth: {
      name: '--gd-filter-chip-border-width',
      control: { type: 'text' },
      description: 'Outline width. Set to `1px` to enable the border.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.borderWidth } },
    },
    borderColor: {
      name: '--gd-filter-chip-border-color',
      control: { type: 'color' },
      description: 'Outline color — only visible when `--gd-filter-chip-border-width > 0`.',
      table: { category: 'CSS Tokens', defaultValue: { summary: TOKEN_DEFAULTS.borderColor } },
    },
  },
} satisfies Meta<StoryArgs>;

// ─── Stories ──────────────────────────────────────────────────────────────────

const DISABLE_TOKENS: Partial<Record<keyof StoryArgs, { table: { disable: boolean } }>> = {
  background: { table: { disable: true } },
  color: { table: { disable: true } },
  height: { table: { disable: true } },
  radius: { table: { disable: true } },
  paddingH: { table: { disable: true } },
  fontFamily: { table: { disable: true } },
  fontSize: { table: { disable: true } },
  fontWeight: { table: { disable: true } },
  operatorFontWeight: { table: { disable: true } },
  borderWidth: { table: { disable: true } },
  borderColor: { table: { disable: true } },
};

const DISABLE_FILTER: Partial<Record<keyof StoryArgs, { table: { disable: boolean } }>> = {
  label: { table: { disable: true } },
  operator: { table: { disable: true } },
  valuesInput: { table: { disable: true } },
};

const PLAYGROUND_DESCRIPTION = `
Use the **Controls** panel to explore all display variants interactively:
change the operator (\`EQ\`, \`IN\`, \`NOT_IN\`, \`GTE\`…), enter multiple
comma-separated values to trigger the count display, or try an unknown
operator like \`CONTAINS\` to see the graceful fallback.

### Component

\`\`\`html
<gd-filter-chip
  [filter]="condition"
  (clicked)="openEditModal(condition)"
  (removed)="removeFilter(condition.field)"
/>
\`\`\`

\`\`\`typescript
import { FilterChipComponent, FilterCondition } from '@gravitee/gravitee-dashboard';

const condition: FilterCondition = {
  field: 'HTTP_STATUS',   // unique key — one condition per field
  label: 'Status Code',  // display label
  operator: 'IN',
  values: ['200', '204'],
};
\`\`\`
`;

export const Playground: StoryObj<StoryArgs> = {
  parameters: {
    docs: {
      description: { story: PLAYGROUND_DESCRIPTION },
    },
  },
  argTypes: DISABLE_TOKENS,
  args: {
    label: 'Status Code',
    operator: 'EQ',
    valuesInput: '200',
    ...TOKEN_DEFAULTS,
  },
  render: (args: StoryArgs) => ({
    template: `<gd-filter-chip-story-wrapper [filter]="filter" />`,
    props: { filter: buildFilter(args) },
  }),
};

const CUSTOM_TOKENS_DESCRIPTION = `
Tweak every CSS custom property from the **Controls** panel and see the chip
update in real time. Copy the values you like into your application's stylesheet.

### Styling API

| Token | Type | Default value |
|---|---|---|
| \`--gd-filter-chip-background\` | color | \`#fff3eb\` ← \`var(--mat-sys-primary-container)\` |
| \`--gd-filter-chip-color\` | color | \`#f15115\` ← \`var(--mat-sys-on-primary-container)\` |
| \`--gd-filter-chip-height\` | dimension | \`24px\` |
| \`--gd-filter-chip-radius\` | dimension | \`6px\` |
| \`--gd-filter-chip-padding-h\` | dimension | \`8px\` |
| \`--gd-filter-chip-font-family\` | string | \`inherit\` |
| \`--gd-filter-chip-font-size\` | dimension | \`12px\` |
| \`--gd-filter-chip-font-weight\` | number | \`600\` |
| \`--gd-filter-chip-operator-font-weight\` | number | \`400\` |
| \`--gd-filter-chip-border-width\` | dimension | \`0px\` |
| \`--gd-filter-chip-border-color\` | color | \`#f15115\` ← \`var(--mat-sys-on-primary-container)\` |

> **Color resolution order:** consumer \`--gd-filter-chip-*\` → M3 theme \`--mat-sys-*\` → hardcoded fallback.
> Always set \`--gd-filter-chip-background\` and \`--gd-filter-chip-color\` together to guarantee WCAG contrast (min 4.5:1).
`;

export const CustomTokens: StoryObj<StoryArgs> = {
  name: 'Custom Tokens',
  parameters: {
    docs: {
      description: { story: CUSTOM_TOKENS_DESCRIPTION },
    },
  },
  argTypes: DISABLE_FILTER,
  args: {
    label: 'Status Code',
    operator: 'IN',
    valuesInput: '200, 204',
    ...TOKEN_DEFAULTS,
  },
  render: (args: StoryArgs) => ({
    template: `<gd-filter-chip-story-wrapper [filter]="filter" [style]="themedStyle" />`,
    props: {
      filter: buildFilter(args),
      themedStyle: buildTokenStyle(args),
    },
  }),
};
