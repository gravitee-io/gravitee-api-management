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
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FilterCondition } from '../filter.model';
import { DynamicFilterBarComponent } from './dynamic-filter-bar.component';

const SAMPLE_CONDITIONS: FilterCondition[] = [
  { field: 'API', label: 'API', operator: 'IN', values: ['my-api-uuid-1', 'my-api-uuid-2'] },
  { field: 'HTTP_STATUS', label: 'Status Code', operator: 'GTE', values: ['400'] },
  { field: 'API_TYPE', label: 'API Type', operator: 'EQ', values: ['HTTP_PROXY'] },
];

@Component({
  standalone: true,
  selector: 'gd-dynamic-filter-bar-story-wrapper',
  imports: [DynamicFilterBarComponent],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <div class="story-layout">
      <gd-dynamic-filter-bar
        [conditions]="conditions()"
        [editable]="editable"
        (addRequested)="onAdd()"
        (editRequested)="onEdit($event)"
        (removeRequested)="onRemove($event)"
        (clearRequested)="onClear()"
      />

      <div class="story-events">
        <span class="story-events__label">Events log</span>
        @for (event of events(); track $index) {
          <span class="story-events__row">{{ event }}</span>
        }
        @if (events().length === 0) {
          <span class="story-events__row story-events__row--empty">No events yet — interact with the bar above.</span>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .story-layout {
        display: flex;
        flex-direction: column;
        gap: 24px;
        font-family: 'Kanit', 'Roboto', sans-serif;
      }
      .story-events {
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12px;
        color: #555;
        max-height: 200px;
        overflow-y: auto;
      }
      .story-events__label {
        font-weight: 600;
        color: #333;
      }
      .story-events__row {
        font-family: monospace;
        font-size: 11px;
      }
      .story-events__row--empty {
        color: #999;
        font-style: italic;
      }
    `,
  ],
})
class DynamicFilterBarWrapperComponent implements OnInit, OnChanges {
  @Input() initialConditions: FilterCondition[] = [];
  @Input() editable = true;

  conditions = signal<FilterCondition[]>([]);
  events = signal<string[]>([]);

  ngOnInit(): void {
    this.conditions.set([...this.initialConditions]);
  }

  ngOnChanges(): void {
    this.conditions.set([...this.initialConditions]);
  }

  onAdd(): void {
    this.events.update(e => [`addRequested()`, ...e]);
  }

  onEdit(event: { index: number; condition: FilterCondition }): void {
    this.events.update(e => [`editRequested(index=${event.index}, field=${event.condition.field})`, ...e]);
  }

  onRemove(index: number): void {
    this.conditions.update(c => c.filter((_, i) => i !== index));
    this.events.update(e => [`removeRequested(index=${index}) → chip removed`, ...e]);
  }

  onClear(): void {
    this.conditions.set([]);
    this.events.update(e => [`clearRequested() → all chips removed`, ...e]);
  }
}

interface StoryArgs {
  initialConditions: FilterCondition[];
  editable: boolean;
}

export default {
  title: 'Gravitee Dashboard/Components/Filter/Dynamic Filter Bar',
  component: DynamicFilterBarWrapperComponent,
  tags: ['autodocs'],
  decorators: [
    moduleMetadata({ imports: [DynamicFilterBarWrapperComponent] }),
    applicationConfig({ providers: [provideNoopAnimations()] }),
  ],
  parameters: {
    docs: {
      description: {
        component: `
\`gd-dynamic-filter-bar\` renders a horizontal bar of filter chips with **Add filter** and **Clear all** controls.

It is a **stateless, presentational component** — it displays the provided \`FilterCondition[]\` and delegates all
mutations (add, edit, remove, clear) to the parent via outputs.

### Usage

\`\`\`html
<gd-dynamic-filter-bar
  [conditions]="store.conditions()"
  [editable]="canEdit()"
  (addRequested)="openAddFilterDialog()"
  (editRequested)="openEditDialog($event.index, $event.condition)"
  (removeRequested)="store.remove($event)"
  (clearRequested)="store.clear()"
/>
\`\`\`

### Inputs

| Input | Type | Default | Description |
|---|---|---|---|
| \`conditions\` | \`FilterCondition[]\` | \`[]\` | Active filter conditions to display as chips |
| \`editable\` | \`boolean\` | \`true\` | When \`false\`, hides Add/Clear buttons and sets chips to read-only |

### Outputs

| Output | Payload | Description |
|---|---|---|
| \`addRequested\` | \`void\` | User clicked "Add filter" |
| \`editRequested\` | \`{ index, condition }\` | User clicked a chip to edit it |
| \`removeRequested\` | \`number\` | User removed a chip (by index) |
| \`clearRequested\` | \`void\` | User clicked "Clear all" |
        `,
      },
    },
  },
  argTypes: {
    editable: {
      control: { type: 'boolean' },
      description: 'When `false`, Add/Clear buttons are hidden and chips are read-only.',
      table: { defaultValue: { summary: 'true' } },
    },
    initialConditions: {
      control: { type: 'object' },
      description: 'Array of FilterCondition objects to display.',
    },
  },
} satisfies Meta<StoryArgs>;

export const Empty: StoryObj<StoryArgs> = {
  args: { initialConditions: [], editable: true },
  render: (args: StoryArgs) => ({
    template: `<gd-dynamic-filter-bar-story-wrapper [initialConditions]="initialConditions" [editable]="editable" />`,
    props: args,
  }),
};

export const WithFilters: StoryObj<StoryArgs> = {
  args: { initialConditions: SAMPLE_CONDITIONS, editable: true },
  render: (args: StoryArgs) => ({
    template: `<gd-dynamic-filter-bar-story-wrapper [initialConditions]="initialConditions" [editable]="editable" />`,
    props: args,
  }),
};

export const ReadOnly: StoryObj<StoryArgs> = {
  args: { initialConditions: SAMPLE_CONDITIONS, editable: false },
  render: (args: StoryArgs) => ({
    template: `<gd-dynamic-filter-bar-story-wrapper [initialConditions]="initialConditions" [editable]="editable" />`,
    props: args,
  }),
};
