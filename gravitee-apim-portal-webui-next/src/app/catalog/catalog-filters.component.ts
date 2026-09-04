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
import { Component, computed, ElementRef, input, model, signal, viewChild } from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

import { CatalogFilterField, CatalogFilterKey, CatalogFilterSelection } from './catalog-filters';

@Component({
  selector: 'app-catalog-filters',
  imports: [MatCheckboxModule, MatIconModule],
  templateUrl: './catalog-filters.component.html',
  styleUrl: './catalog-filters.component.scss',
})
export class CatalogFiltersComponent {
  readonly fields = input<CatalogFilterField[]>([]);
  readonly selection = model<CatalogFilterSelection>({});

  private readonly collapsed = signal<CatalogFilterKey[]>([]);
  private readonly firstHeader = viewChild<ElementRef<HTMLButtonElement>>('firstHeader');

  protected readonly sections = computed(() =>
    this.fields().map(field => ({
      ...field,
      collapsed: this.collapsed().includes(field.key),
      values: field.values.map(value => ({ ...value, picked: !!this.selection()[field.key]?.includes(value.value) })),
    })),
  );

  focusFirstField(): void {
    this.firstHeader()?.nativeElement.focus();
  }

  protected toggleSection(key: CatalogFilterKey): void {
    this.collapsed.update(keys => (keys.includes(key) ? keys.filter(entry => entry !== key) : [...keys, key]));
  }

  protected toggle(key: CatalogFilterKey, value: string): void {
    const picked = this.selection()[key] ?? [];
    const next = picked.includes(value) ? picked.filter(entry => entry !== value) : [...picked, value];
    this.selection.set({ ...this.selection(), [key]: next });
  }
}
