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

/**
 * Story-only wrapper that displays the chip alongside a live event badge.
 * The badge shows the last event name and a cumulative counter, then fades
 * out after 2 s so that a subsequent interaction is always visible.
 */
@Component({
  standalone: true,
  selector: 'gd-filter-chip-story-wrapper',
  imports: [FilterChipComponent],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <div style="display: inline-flex; flex-direction: column; align-items: flex-start; gap: 12px;">
      <gd-filter-chip [filter]="filter" (removed)="onEvent('removed')" (clicked)="onEvent('clicked')" />
      <div class="story-event-row">
        <span class="story-event-badge story-event-badge--click"> clicked ×{{ counts.clicked }} </span>
        <span class="story-event-badge story-event-badge--remove"> removed ×{{ counts.removed }} </span>
      </div>
    </div>
  `,
  styles: [
    `
      .story-event-row {
        display: flex;
        gap: 8px;
      }
      .story-event-badge {
        padding: 2px 10px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;
        border: 1px solid;
      }
      .story-event-badge--remove {
        background: #fce4ec;
        color: #c62828;
        border-color: #ef9a9a;
      }
      .story-event-badge--click {
        background: #e3f2fd;
        color: #1565c0;
        border-color: #90caf9;
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

interface FilterChipStoryArgs {
  storyId?: string;
  filter: FilterCondition;
}

export default {
  title: 'Gravitee Dashboard/Components/Filter/Filter Chip',
  component: FilterChipWrapperComponent,
  decorators: [
    moduleMetadata({
      imports: [FilterChipWrapperComponent],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'Displays an active filter condition as a chip. Click to edit, remove icon to delete.',
      },
    },
  },
  argTypes: {
    storyId: {
      table: { disable: true },
    },
    filter: {
      control: { type: 'object' },
      description: 'The FilterCondition to display',
    },
  },
  render: (args: FilterChipStoryArgs) => ({
    template: `<gd-filter-chip-story-wrapper [filter]="filter" />`,
    props: { filter: args.filter },
  }),
} satisfies Meta<FilterChipStoryArgs>;

export const Default: StoryObj<FilterChipStoryArgs> = {
  args: {
    storyId: 'default',
    filter: {
      field: 'HTTP_STATUS',
      label: 'Status Code',
      operator: 'EQ',
      values: ['200'],
    },
  },
};

export const MultipleValuesIn: StoryObj<FilterChipStoryArgs> = {
  name: 'Multiple Values — IN',
  args: {
    storyId: 'multiple-values-in',
    filter: {
      field: 'HTTP_STATUS',
      label: 'Status Code',
      operator: 'IN',
      values: ['200', '204'],
    },
  },
};

export const MultipleValuesNotIn: StoryObj<FilterChipStoryArgs> = {
  name: 'Multiple Values — NOT IN',
  args: {
    storyId: 'multiple-values-not-in',
    filter: {
      field: 'HTTP_STATUS',
      label: 'Status Code',
      operator: 'NOT_IN',
      values: ['300', '404'],
    },
  },
};

export const GreaterThanOrEqual: StoryObj<FilterChipStoryArgs> = {
  args: {
    storyId: 'gte',
    filter: {
      field: 'HTTP_STATUS',
      label: 'Status Code',
      operator: 'GTE',
      values: ['500'],
    },
  },
};

export const UnknownOperator: StoryObj<FilterChipStoryArgs> = {
  args: {
    storyId: 'unknown-operator',
    filter: {
      field: 'HTTP_PATH',
      label: 'HTTP Path',
      operator: 'CONTAINS',
      values: ['/api/gravitee'],
    },
  },
};

export const CustomTokens: StoryObj<FilterChipStoryArgs> = {
  name: 'Custom Tokens (purple)',
  args: {
    storyId: 'custom-tokens',
    filter: {
      field: 'HTTP_STATUS',
      label: 'Status Code',
      operator: 'EQ',
      values: ['200'],
    },
  },
  render: args => ({
    template: `
      <gd-filter-chip-story-wrapper [filter]="filter"
        style="
          --gd-filter-chip-background: #f3e5f5;
          --gd-filter-chip-color: #6a1b9a;
        " />
    `,
    props: { filter: args.filter },
  }),
};
