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
import { Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, startWith, switchMap } from 'rxjs/operators';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Api } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';

export interface MappedApi {
  id: string;
  name: string;
}

/**
 * The APIs a subscription form is shown for: a searchable page of every API of the environment, each row mapping or
 * unmapping its API. An API belongs to a single form, so one mapped elsewhere cannot be picked here.
 * The selection itself is owned by the parent, which receives every toggle.
 */
@Component({
  selector: 'subscription-form-apis',
  imports: [
    ReactiveFormsModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatTableModule,
    MatTooltipModule,
  ],
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
  readonly pageSize = 25;
  readonly searchControl = new FormControl('', { nonNullable: true });
  private readonly pageIndexChanges = new Subject<number>();

  private readonly selectedIds = computed(() => new Set(this.selectedApis().map(api => api.id)));

  private readonly page = toSignal(
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      map(term => term.trim()),
      startWith(''),
      distinctUntilChanged(),
      // Restarting the page stream on each search brings a new search back to the first page in a single request.
      switchMap(term =>
        this.pageIndexChanges.pipe(
          startWith(1),
          map(pageIndex => ({ term, pageIndex })),
        ),
      ),
      switchMap(({ term, pageIndex }) =>
        this.apiService.search(term ? { query: term } : {}, undefined, pageIndex, this.pageSize, false).pipe(
          map(response => ({ apis: response.data ?? [], total: response.pagination?.totalCount ?? 0, pageIndex })),
          catchError(() => of({ apis: [] as Api[], total: 0, pageIndex })),
        ),
      ),
    ),
    { initialValue: { apis: [] as Api[], total: 0, pageIndex: 1 } },
  );
  readonly apis = computed(() => this.page().apis);
  readonly total = computed(() => this.page().total);
  readonly pageIndex = computed(() => this.page().pageIndex);

  isSelected(api: Api): boolean {
    return this.selectedIds().has(api.id);
  }

  mappedTo(api: Api): string | undefined {
    return this.isSelected(api) ? undefined : this.mappedElsewhere()[api.id];
  }

  toggle(api: Api): void {
    this.apiToggled.emit({ id: api.id, name: api.name });
  }

  onPage(event: PageEvent): void {
    this.pageIndexChanges.next(event.pageIndex + 1);
  }
}
