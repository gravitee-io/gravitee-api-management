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
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { isEqual } from 'lodash';

import { Api } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

export interface MappedApi {
  id: string;
  name: string;
}

/**
 * The APIs a subscription form is shown for: the mapped ones as chips, and a searchable page of every API of the
 * environment to map or unmap. An API belongs to a single form, so one mapped elsewhere cannot be picked here.
 * The selection itself is owned by the parent, which receives every toggle.
 */
@Component({
  selector: 'subscription-form-apis',
  imports: [MatCheckboxModule, MatChipsModule, MatIconModule, MatTableModule, MatTooltipModule, GioTableWrapperModule],
  templateUrl: './subscription-form-apis.component.html',
  styleUrl: './subscription-form-apis.component.scss',
})
export class SubscriptionFormApisComponent {
  private readonly apiService = inject(ApiV2Service);

  readonly selectedApis = input.required<MappedApi[]>();
  /** Name of the form each API outside this one is mapped to, by API id. */
  readonly mappedElsewhere = input<Record<string, string>>({});
  readonly canUpdate = input(false);

  readonly apiToggled = output<MappedApi>();

  readonly displayedColumns = ['select', 'name', 'version', 'mapped'];
  readonly filters = signal<GioTableWrapperFilters>({ pagination: { index: 1, size: 25 }, searchTerm: '' });

  private readonly selectedIds = computed(() => new Set(this.selectedApis().map(api => api.id)));

  private readonly page = toSignal(
    toObservable(this.filters).pipe(
      distinctUntilChanged(isEqual),
      switchMap(({ pagination, searchTerm }) => {
        const query = searchTerm?.trim() ? { query: searchTerm.trim() } : {};
        return this.apiService.search(query, undefined, pagination.index, pagination.size, false).pipe(
          map(response => ({ apis: response.data ?? [], total: response.pagination?.totalCount ?? 0 })),
          catchError(() => of({ apis: [] as Api[], total: 0 })),
        );
      }),
    ),
    { initialValue: { apis: [] as Api[], total: 0 } },
  );
  readonly apis = computed(() => this.page().apis);
  readonly total = computed(() => this.page().total);

  isSelected(api: Api): boolean {
    return this.selectedIds().has(api.id);
  }

  mappedTo(api: Api): string | undefined {
    return this.isSelected(api) ? undefined : this.mappedElsewhere()[api.id];
  }

  toggle(api: Api): void {
    this.apiToggled.emit({ id: api.id, name: api.name });
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters.set(filters);
  }
}
