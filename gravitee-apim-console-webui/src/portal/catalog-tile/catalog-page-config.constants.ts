/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

export const CATALOG_LIST_COLUMN_OPTIONS = ['name', 'labels', 'category', 'version', 'mcp'] as const;
export type CatalogListColumnId = (typeof CATALOG_LIST_COLUMN_OPTIONS)[number];

export interface CatalogPageConfig {
  topContent: string;
  bottomContent: string;
  categoryFilterEnabled: boolean;
  categoryFilterLabel: string;
  cardsViewEnabled: boolean;
  cardsViewLabel: string;
  listViewEnabled: boolean;
  listViewLabel: string;
  defaultDisplayMode: 'grid' | 'list';
  listColumns: CatalogListColumnId[];
  cardTemplate: string;
}

export const DEFAULT_CATALOG_PAGE_CONFIG: CatalogPageConfig = {
  topContent: '',
  bottomContent: '',
  categoryFilterEnabled: true,
  categoryFilterLabel: 'Category',
  cardsViewEnabled: true,
  cardsViewLabel: 'Cards',
  listViewEnabled: true,
  listViewLabel: 'List',
  defaultDisplayMode: 'grid',
  listColumns: [...CATALOG_LIST_COLUMN_OPTIONS],
  cardTemplate: '',
};

/** Escape attribute values for embedding into GMD form markup. */
function attr(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
}

/**
 * Builds the Catalog configuration GMD form with current values.
 * Field keys are stable so save can map store values back to settings.
 */
export function buildCatalogConfigFormMarkdown(config: CatalogPageConfig): string {
  const columnsValue = config.listColumns.join(',');
  const displayViews = [
    config.cardsViewEnabled ? 'cards' : null,
    config.listViewEnabled ? 'list' : null,
  ]
    .filter(Boolean)
    .join(',');
  return `## Page layout

<gmd-checkbox fieldKey="categoryFilterEnabled" name="categoryFilterEnabled" label="Show category filter" value="${config.categoryFilterEnabled}" />
<gmd-input fieldKey="categoryFilterLabel" name="categoryFilterLabel" label="Category filter label" value="${attr(config.categoryFilterLabel)}" />

## Display modes

<gmd-checkbox-group fieldKey="displayViews" name="displayViews" label="Show cards and list views" options="cards,list" value="${attr(displayViews)}" />
<gmd-input fieldKey="cardsViewLabel" name="cardsViewLabel" label="Cards view label" value="${attr(config.cardsViewLabel)}" />
<gmd-input fieldKey="listViewLabel" name="listViewLabel" label="List view label" value="${attr(config.listViewLabel)}" />

<gmd-select fieldKey="defaultDisplayMode" name="defaultDisplayMode" label="Default display mode" options="grid,list" value="${config.defaultDisplayMode}" />

## List columns

<gmd-checkbox-group fieldKey="listColumns" name="listColumns" label="Columns displayed in list view" options="name,labels,category,version,mcp" value="${attr(columnsValue)}" />
<gmd-input fieldKey="listColumnsOrder" name="listColumnsOrder" label="Column order (left to right, comma-separated)" value="${attr(columnsValue)}" />
`;
}

export function parseListColumns(raw: string | undefined | null): CatalogListColumnId[] {
  if (!raw?.trim()) {
    return [...CATALOG_LIST_COLUMN_OPTIONS];
  }
  const selected = raw
    .split(',')
    .map(part => part.trim())
    .filter((part): part is CatalogListColumnId => (CATALOG_LIST_COLUMN_OPTIONS as readonly string[]).includes(part));
  return selected.length > 0 ? selected : [...CATALOG_LIST_COLUMN_OPTIONS];
}

/** Merge selected columns with an explicit left-to-right order. */
export function orderListColumns(
  selected: CatalogListColumnId[],
  orderRaw: string | undefined | null,
): CatalogListColumnId[] {
  if (selected.length === 0) {
    return [...CATALOG_LIST_COLUMN_OPTIONS];
  }
  const selectedSet = new Set(selected);
  const preferred = (orderRaw ?? '')
    .split(',')
    .map(part => part.trim())
    .filter((part): part is CatalogListColumnId => selectedSet.has(part as CatalogListColumnId));
  const remaining = selected.filter(column => !preferred.includes(column));
  const ordered = [...preferred, ...remaining];
  return ordered.length > 0 ? ordered : [...CATALOG_LIST_COLUMN_OPTIONS];
}

export function catalogConfigFromSettings(catalog: {
  topContent?: string;
  bottomContent?: string;
  categoryFilter?: { enabled?: boolean; label?: string };
  cardsView?: { enabled?: boolean; label?: string };
  listView?: { enabled?: boolean; label?: string };
  defaultDisplayMode?: string;
  listColumns?: string;
  cardTemplate?: string;
} | null | undefined): CatalogPageConfig {
  const defaultMode = catalog?.defaultDisplayMode === 'list' ? 'list' : 'grid';
  return {
    topContent: catalog?.topContent ?? '',
    bottomContent: catalog?.bottomContent ?? '',
    categoryFilterEnabled: catalog?.categoryFilter?.enabled !== false,
    categoryFilterLabel: catalog?.categoryFilter?.label?.trim() || DEFAULT_CATALOG_PAGE_CONFIG.categoryFilterLabel,
    cardsViewEnabled: catalog?.cardsView?.enabled !== false,
    cardsViewLabel: catalog?.cardsView?.label?.trim() || DEFAULT_CATALOG_PAGE_CONFIG.cardsViewLabel,
    listViewEnabled: catalog?.listView?.enabled !== false,
    listViewLabel: catalog?.listView?.label?.trim() || DEFAULT_CATALOG_PAGE_CONFIG.listViewLabel,
    defaultDisplayMode: defaultMode,
    listColumns: parseListColumns(catalog?.listColumns),
    cardTemplate: catalog?.cardTemplate ?? '',
  };
}

export function applyFormValuesToConfig(
  base: CatalogPageConfig,
  fieldValues: Array<{ fieldKey: string; value: string }>,
): CatalogPageConfig {
  const byKey = new Map(fieldValues.map(field => [field.fieldKey, field.value]));
  const bool = (key: string, fallback: boolean) => {
    const value = byKey.get(key);
    if (value == null || value === '') return fallback;
    return value === 'true' || value === 'on' || value === '1';
  };
  const text = (key: string, fallback: string) => {
    const value = byKey.get(key);
    return value == null || value.trim() === '' ? fallback : value;
  };

  const defaultDisplayMode = text('defaultDisplayMode', base.defaultDisplayMode) === 'list' ? 'list' : 'grid';
  const displayViewsRaw = byKey.get('displayViews');
  const displayViews =
    displayViewsRaw == null
      ? null
      : new Set(
          displayViewsRaw
            .split(',')
            .map(part => part.trim())
            .filter(Boolean),
        );
  return {
    ...base,
    categoryFilterEnabled: bool('categoryFilterEnabled', base.categoryFilterEnabled),
    categoryFilterLabel: text('categoryFilterLabel', base.categoryFilterLabel),
    cardsViewEnabled: displayViews ? displayViews.has('cards') : base.cardsViewEnabled,
    cardsViewLabel: text('cardsViewLabel', base.cardsViewLabel),
    listViewEnabled: displayViews ? displayViews.has('list') : base.listViewEnabled,
    listViewLabel: text('listViewLabel', base.listViewLabel),
    defaultDisplayMode,
    listColumns: orderListColumns(
      parseListColumns(byKey.get('listColumns') ?? base.listColumns.join(',')),
      byKey.get('listColumnsOrder') ?? base.listColumns.join(','),
    ),
  };
}
