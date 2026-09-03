/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import type { LocalizeFn } from '@angular/localize/init';

declare const $localize: LocalizeFn;

export type CatalogFilterKey = 'access' | 'category' | 'protocol' | 'tag' | 'publisher';

export interface CatalogFilterValue {
  value: string;
  label: string;
  count: number;
}

export interface CatalogFilterField {
  key: CatalogFilterKey;
  label: string;
  values: CatalogFilterValue[];
}

export type CatalogFilterSelection = Partial<Record<CatalogFilterKey, string[]>>;

export interface FilterableItem {
  access?: string;
  accessLabel?: string;
  categoryIds?: string[];
  protocol?: string;
  labels?: string[];
  publisher?: string;
}

export function valuesOf(item: FilterableItem, key: CatalogFilterKey): string[] {
  switch (key) {
    case 'access':
      return item.access ? [item.access] : [];
    case 'category':
      return item.categoryIds ?? [];
    case 'protocol':
      return item.protocol ? [item.protocol] : [];
    case 'tag':
      return item.labels ?? [];
    default:
      return item.publisher ? [item.publisher] : [];
  }
}

export function pruneSelection(selection: CatalogFilterSelection, fields: CatalogFilterField[]): CatalogFilterSelection {
  const offered = new Map(fields.map(field => [field.key, new Set(field.values.map(value => value.value))]));
  const pruned: CatalogFilterSelection = {};
  Object.entries(selection).forEach(([key, values]) => {
    const kept = (values ?? []).filter(value => offered.get(key as CatalogFilterKey)?.has(value));
    if (kept.length) {
      pruned[key as CatalogFilterKey] = kept;
    }
  });
  return pruned;
}

export function matchesFilters(item: FilterableItem, selection: CatalogFilterSelection): boolean {
  return Object.entries(selection).every(([key, selected]) => {
    if (!selected?.length) {
      return true;
    }
    return valuesOf(item, key as CatalogFilterKey).some(value => selected.includes(value));
  });
}

export function buildFilterFields(
  items: FilterableItem[],
  names: { category: Map<string, string>; access: Map<string, string> },
  selection: CatalogFilterSelection = {},
): CatalogFilterField[] {
  const fields: { key: CatalogFilterKey; label: string; name: (value: string) => string; order?: string[] }[] = [
    {
      key: 'access',
      label: $localize`:@@catalogFilterAccess:Access`,
      name: value => names.access.get(value) ?? value,
      order: [...names.access.keys()],
    },
    { key: 'category', label: $localize`:@@catalogFilterCategory:Category`, name: value => names.category.get(value) ?? value },
    { key: 'protocol', label: $localize`:@@catalogFilterProtocol:Protocol`, name: value => value },
    { key: 'tag', label: $localize`:@@catalogFilterTag:Tags`, name: value => value },
    { key: 'publisher', label: $localize`:@@catalogFilterPublisher:Published by`, name: value => value },
  ];

  return fields
    .map(({ key, label, name, order }) => {
      const counts = new Map<string, number>();
      (selection[key] ?? []).forEach(value => counts.set(value, 0));
      items.forEach(item => valuesOf(item, key).forEach(value => counts.set(value, (counts.get(value) ?? 0) + 1)));
      const values = [...counts.entries()].map(([value, count]) => ({ value, label: name(value), count }));
      const ordered = order
        ? order.flatMap(entry => values.filter(value => value.value === entry))
        : values.sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
      return { key, label, values: ordered };
    })
    .filter(field => field.values.some(value => value.count < items.length) || (selection[field.key] ?? []).length > 0);
}
