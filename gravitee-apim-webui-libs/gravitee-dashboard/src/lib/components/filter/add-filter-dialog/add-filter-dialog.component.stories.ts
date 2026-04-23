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
import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { of } from 'rxjs';

import { DynamicFilterBarComponent } from '../dynamic-filter-bar/dynamic-filter-bar.component';
import {
  FILTER_DEFINITION_PROVIDER,
  FILTER_VALUES_PROVIDER,
  FilterDefinitionProvider,
  FilterValuesProvider,
  FilterValuesQuery,
  FilterValuesResult,
} from '../filter-providers';
import { FilterCondition, FilterDefinition } from '../filter.model';
import { AddFilterDialogComponent, AddFilterDialogData } from './add-filter-dialog.component';

// ─── Sample data ──────────────────────────────────────────────────────────────

const SAMPLE_DEFINITIONS: FilterDefinition[] = [
  {
    name: 'HTTP_METHOD',
    label: 'HTTP Method',
    type: 'ENUM',
    operators: ['EQ', 'NEQ'],
    values: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'],
    // Shown when the dashboard context is limited to HTTP proxy analytics.
    apiTypes: ['HTTP_PROXY'],
  },
  {
    name: 'HTTP_STATUS',
    label: 'Status Code',
    type: 'NUMBER',
    operators: ['GTE', 'LTE'],
    range: { min: 100, max: 599 },
    apiTypes: ['HTTP_PROXY'],
  },
  {
    name: 'API',
    label: 'API',
    type: 'KEYWORD',
    operators: ['IN', 'NOT_IN'],
    // Keyword pickers apply across API kinds in observability.
    apiTypes: ['HTTP_PROXY', 'MESSAGE', 'NATIVE'],
  },
  {
    name: 'APPLICATION',
    label: 'Application',
    type: 'KEYWORD',
    operators: ['IN', 'NOT_IN'],
    // No badge: applies to any API type in the story dataset.
  },
  {
    name: 'PLAN',
    label: 'Plan',
    type: 'KEYWORD',
    operators: ['IN'],
    apiTypes: ['HTTP_PROXY', 'MESSAGE'],
  },
  {
    name: 'API_TYPE',
    label: 'API Type',
    type: 'ENUM',
    operators: ['EQ'],
    values: ['HTTP_PROXY', 'MESSAGE', 'NATIVE'],
  },
  {
    name: 'RESPONSE_TIME',
    label: 'Response Time (ms)',
    type: 'NUMBER',
    operators: ['GTE', 'LTE'],
    range: { min: 0, max: 60000 },
  },
  {
    name: 'HTTP_PATH',
    label: 'HTTP Path',
    type: 'STRING',
    operators: ['EQ'],
    apiTypes: ['HTTP_PROXY'],
  },
];

const MOCK_VALUES_BY_FILTER: Record<string, { value: string; label: string; id: string }[]> = {
  API: [
    { value: 'api-uuid-1', label: 'My First API', id: 'api-uuid-1' },
    { value: 'api-uuid-2', label: 'Payments API', id: 'api-uuid-2' },
    { value: 'api-uuid-3', label: 'Auth Service', id: 'api-uuid-3' },
    { value: 'api-uuid-4', label: 'Notification Hub', id: 'api-uuid-4' },
    { value: 'api-uuid-5', label: 'Data Pipeline', id: 'api-uuid-5' },
  ],
  APPLICATION: [
    { value: 'app-uuid-1', label: 'Customer Portal', id: 'app-uuid-1' },
    { value: 'app-uuid-2', label: 'Partner B2B App', id: 'app-uuid-2' },
    { value: 'app-uuid-3', label: 'Mobile Client', id: 'app-uuid-3' },
  ],
  PLAN: [
    { value: 'plan-uuid-1', label: 'Gold', id: 'plan-uuid-1' },
    { value: 'plan-uuid-2', label: 'Silver', id: 'plan-uuid-2' },
    { value: 'plan-uuid-3', label: 'Free tier', id: 'plan-uuid-3' },
  ],
};

class MockFilterDefinitionProvider implements FilterDefinitionProvider {
  getDefinitions() {
    return of(SAMPLE_DEFINITIONS);
  }
}

class MockFilterValuesProvider implements FilterValuesProvider {
  getValues(query: FilterValuesQuery) {
    const pool = MOCK_VALUES_BY_FILTER[query.filterName] ?? [];
    const q = (query.query ?? '').trim().toLowerCase();
    const data = pool.filter(item => !q || item.label.toLowerCase().includes(q));
    const all: FilterValuesResult = {
      data,
      hasNextPage: false,
    };
    return of(all);
  }
}

/** 42 items → 5 pages at perPage 10, for manual scroll-pagination QA on the API keyword field. */
const LARGE_API_VALUE_POOL: FilterValuesResult['data'] = Array.from({ length: 42 }, (_, i) => ({
  id: `api-scroll-${i}`,
  value: `api-scroll-${i}`,
  label: `Scroll demo API ${String(i + 1).padStart(2, '0')}`,
}));

class MockFilterValuesProviderPaginatedApi implements FilterValuesProvider {
  getValues(query: FilterValuesQuery) {
    const perPage = query.perPage;
    if (query.filterName === 'API') {
      const q = (query.query ?? '').trim().toLowerCase();
      const pool = LARGE_API_VALUE_POOL.filter(item => !q || item.label.toLowerCase().includes(q));
      const start = (query.page - 1) * perPage;
      const slice = pool.slice(start, start + perPage);
      const hasNextPage = start + perPage < pool.length;
      return of({ data: slice, hasNextPage });
    }
    const pool = MOCK_VALUES_BY_FILTER[query.filterName] ?? [];
    const q = (query.query ?? '').trim().toLowerCase();
    const data = pool.filter(item => !q || item.label.toLowerCase().includes(q));
    return of({ data, hasNextPage: false });
  }
}

const mockDefinitionProvider = new MockFilterDefinitionProvider();
const mockValuesProvider = new MockFilterValuesProvider();
const mockValuesProviderPaginatedApi = new MockFilterValuesProviderPaginatedApi();

// ─── Shared providers for direct-render stories ───────────────────────────────

function makeDirectRenderProviders(data: AddFilterDialogData = {}, valuesProvider: FilterValuesProvider = mockValuesProvider) {
  return [
    { provide: MAT_DIALOG_DATA, useValue: data },
    {
      provide: MatDialogRef,
      useValue: {
        close: (result?: FilterCondition) => {
          // eslint-disable-next-line no-console
          console.log('[story] dialog closed with', result);
        },
      },
    },
    { provide: FILTER_DEFINITION_PROVIDER, useValue: mockDefinitionProvider },
    { provide: FILTER_VALUES_PROVIDER, useValue: valuesProvider },
  ];
}

// ─── Story: dialog content rendered directly (no overlay) ─────────────────────

@Component({
  standalone: true,
  selector: 'gd-add-filter-dialog-direct-story',
  imports: [AddFilterDialogComponent],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <div class="story-dialog-frame">
      <gd-add-filter-dialog />
    </div>
  `,
  styles: [
    `
      .story-dialog-frame {
        display: inline-block;
        background: #fff;
        border-radius: 8px;
        box-shadow:
          0 11px 15px -7px rgba(0, 0, 0, 0.2),
          0 24px 38px 3px rgba(0, 0, 0, 0.14),
          0 9px 46px 8px rgba(0, 0, 0, 0.12);
        min-width: 520px;
        padding: 24px;
        font-family: 'Kanit', 'Roboto', sans-serif;
      }
    `,
  ],
})
class AddFilterDialogDirectStoryComponent {}

// ─── Story: dynamic filter bar + dialog (integration) ─────────────────────────

@Component({
  standalone: true,
  selector: 'gd-add-filter-dialog-trigger-story',
  imports: [DynamicFilterBarComponent, MatDialogModule, JsonPipe],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <div class="story-layout">
      <div class="story-description">
        Uses <code>gd-dynamic-filter-bar</code> like the console: <strong>Add filter</strong> opens an empty dialog. Click a chip to edit
        that filter with its current settings. Applying again replaces a chip with the same field + operator, or appends a new chip.
      </div>

      <gd-dynamic-filter-bar
        [conditions]="conditions()"
        [editable]="true"
        (addRequested)="onAddRequested()"
        (editRequested)="onEditRequested($event)"
        (removeRequested)="onRemoveRequested($event)"
        (clearRequested)="onClearRequested()"
      />

      @if (lastCondition()) {
        <div class="story-result">
          <span class="story-result__label">Last applied condition</span>
          <code class="story-result__code">{{ lastCondition() | json }}</code>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .story-layout {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        gap: 24px;
        font-family: 'Kanit', 'Roboto', sans-serif;
        padding: 32px;
      }

      .story-description {
        font-size: 13px;
        color: #555;
        max-width: 480px;
        line-height: 1.5;
      }

      .story-result {
        display: flex;
        flex-direction: column;
        gap: 6px;
        padding: 12px 16px;
        background: #f5f5f5;
        border-radius: 6px;
        border-left: 4px solid #6750a4;
      }

      .story-result__label {
        font-size: 11px;
        font-weight: 600;
        color: #6750a4;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .story-result__code {
        font-family: monospace;
        font-size: 12px;
        color: #333;
        white-space: pre-wrap;
        word-break: break-all;
      }
    `,
  ],
})
class AddFilterDialogTriggerStoryComponent {
  private readonly dialog = inject(MatDialog);

  protected readonly conditions = signal<FilterCondition[]>([]);
  protected readonly lastCondition = signal<FilterCondition | null>(null);
  private editIndex: number | null = null;

  onAddRequested(): void {
    this.editIndex = null;
    this.openDialog({});
  }

  onEditRequested(event: { index: number; condition: FilterCondition }): void {
    this.editIndex = event.index;
    this.openDialog({ existingCondition: event.condition });
  }

  onRemoveRequested(index: number): void {
    this.conditions.update(c => c.filter((_, i) => i !== index));
  }

  onClearRequested(): void {
    this.conditions.set([]);
    this.lastCondition.set(null);
  }

  private openDialog(data: AddFilterDialogData): void {
    const ref = this.dialog.open<AddFilterDialogComponent, AddFilterDialogData, FilterCondition>(AddFilterDialogComponent, {
      data,
      autoFocus: 'dialog',
    });

    ref.afterClosed().subscribe(result => {
      if (!result) {
        this.editIndex = null;
        return;
      }
      this.lastCondition.set(result);
      const idx = this.editIndex;
      if (idx != null) {
        this.conditions.update(arr => {
          const next = [...arr];
          next[idx] = result;
          return next;
        });
        this.editIndex = null;
        return;
      }
      this.conditions.update(list => {
        const at = list.findIndex(c => c.field === result.field && c.operator === result.operator);
        if (at >= 0) {
          const next = [...list];
          next[at] = result;
          return next;
        }
        return [...list, result];
      });
    });
  }
}

// ─── Meta ─────────────────────────────────────────────────────────────────────

export default {
  title: 'Gravitee Dashboard/Components/Filter/Add Filter Dialog',
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component: `
\`gd-add-filter-dialog\` is an Angular Material dialog that lets the user build a single \`FilterCondition\`.

The dialog displays three always-visible rows — **Filter by**, **Choose operator** and **Filter value** — and uses
\`MatAutocomplete\` on the field and operator inputs so the user can type to filter and click to pick. Re-clicking
the field input clears it and re-triggers the dropdown, so there is no dedicated "Change" button.

Required DI tokens:

| Token | Purpose |
|---|---|
| \`FILTER_DEFINITION_PROVIDER\` | Returns the list of filterable fields (\`FilterDefinition[]\`) |
| \`FILTER_VALUES_PROVIDER\` | Provides autocomplete values for \`KEYWORD\` fields |

### Opening the dialog

\`\`\`typescript
// Add — empty form
this.dialog.open(AddFilterDialogComponent, { data: {} });

// Edit — pre-fill from the chip
this.dialog.open(AddFilterDialogComponent, {
  data: { existingCondition } satisfies AddFilterDialogData,
});
\`\`\`

The dialog emits a \`FilterCondition\` via \`dialogRef.close(result)\` when the user clicks **Apply**/**Update**, or \`undefined\` when they cancel.
        `,
      },
    },
  },
} satisfies Meta;

// ─── Story 1: Add mode (empty) ───────────────────────────────────────────────

export const AddMode: StoryObj = {
  name: 'Add mode (empty)',
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogDirectStoryComponent] }),
    applicationConfig({ providers: makeDirectRenderProviders() }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-direct-story />` }),
};

// ─── Story 2: Edit mode — enum filter (HTTP Method) ──────────────────────────

export const EditEnumFilter: StoryObj = {
  name: 'Edit mode — Enum filter',
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogDirectStoryComponent] }),
    applicationConfig({
      providers: makeDirectRenderProviders({
        existingCondition: { field: 'HTTP_METHOD', label: 'HTTP Method', operator: 'EQ', values: ['GET'] },
      }),
    }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-direct-story />` }),
};

// ─── Story 3: Edit mode — number filter (Status Code ≥ 400) ──────────────────

export const EditNumberFilter: StoryObj = {
  name: 'Edit mode — Number filter',
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogDirectStoryComponent] }),
    applicationConfig({
      providers: makeDirectRenderProviders({
        existingCondition: { field: 'HTTP_STATUS', label: 'Status Code', operator: 'GTE', values: ['400'] },
      }),
    }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-direct-story />` }),
};

// ─── Story 4: Edit mode — keyword filter (API IN [...]) ──────────────────────

export const EditKeywordFilter: StoryObj = {
  name: 'Edit mode — Keyword filter',
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogDirectStoryComponent] }),
    applicationConfig({
      providers: makeDirectRenderProviders({
        existingCondition: {
          field: 'API',
          label: 'API',
          operator: 'IN',
          values: ['api-uuid-1', 'api-uuid-2'],
        },
      }),
    }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-direct-story />` }),
};

// ─── Story 5: Edit mode — string filter (HTTP Path) ──────────────────────────

export const EditStringFilter: StoryObj = {
  name: 'Edit mode — String filter',
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogDirectStoryComponent] }),
    applicationConfig({
      providers: makeDirectRenderProviders({
        existingCondition: { field: 'HTTP_PATH', label: 'HTTP Path', operator: 'EQ', values: ['/api/v1/users'] },
      }),
    }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-direct-story />` }),
};

// ─── Story 6: Badge trigger (opens real dialog overlay) ──────────────────────

export const WithDialogTrigger: StoryObj = {
  name: 'With dynamic filter bar + dialog',
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogTriggerStoryComponent] }),
    applicationConfig({
      providers: [
        { provide: FILTER_DEFINITION_PROVIDER, useValue: mockDefinitionProvider },
        { provide: FILTER_VALUES_PROVIDER, useValue: mockValuesProvider },
      ],
    }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-trigger-story />` }),
};

// ─── Story 7: KEYWORD scroll pagination (API field) ──────────────────────────

export const AddModeKeywordScrollPagination: StoryObj = {
  name: 'Add mode — KEYWORD scroll pagination (API)',
  parameters: {
    docs: {
      description: {
        story: `
Uses a **paginated** \`FILTER_VALUES_PROVIDER\` mock for the **API** field only (\`hasNextPage\` + 10 items per page, 42 values total).
Other KEYWORD fields (**Application**, **Plan**) still behave like a **fixed single-page** list (\`hasNextPage: false\`).

**How to try scroll loading**
1. Choose **Filter by → API**, operator **IN** (or **NOT IN**).
2. Focus **Filter value** and open the autocomplete.
3. Scroll the panel to the bottom: the next page should load automatically (no "Load more" button).

**Fixed lists**
Pick **Application** or **Plan** instead: every value is returned in one response; scrolling must not trigger extra network calls in the real app (here the mock simply returns \`hasNextPage: false\`).
        `,
      },
    },
  },
  decorators: [
    moduleMetadata({ imports: [AddFilterDialogDirectStoryComponent] }),
    applicationConfig({ providers: makeDirectRenderProviders({}, mockValuesProviderPaginatedApi) }),
  ],
  render: () => ({ template: `<gd-add-filter-dialog-direct-story />` }),
};
