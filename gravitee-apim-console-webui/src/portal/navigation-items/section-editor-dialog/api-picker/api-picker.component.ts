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
import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs/operators';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api } from '../../../../entities/management-api-v2';

type ApiRow = {
  id: string;
  name: string;
  path: string;
  labels?: string[];
  isDisabled: boolean;
};

type SelectedApi = {
  id: string;
  name: string;
};

@Component({
  selector: 'api-picker',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatChipsModule,
    AsyncPipe,
  ],
  templateUrl: './api-picker.component.html',
  styleUrls: ['./api-picker.component.scss'],
})
export class ApiPickerComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  @Input()
  disabledApiIds: string[] = [];

  @Input()
  selectedApiIds: string[] = [];

  @Output()
  selectedApiIdsChange = new EventEmitter<string[]>();

  searchControl = new FormControl<string>('', { nonNullable: true });

  pageIndex = signal(0);
  pageSize = signal(10);

  total = signal(0);

  private readonly refresh$ = new Subject<void>();

  private selectedOrderedApis = signal<SelectedApi[]>([]);

  selectedCount = computed(() => this.selectedOrderedApis().length);
  selectedApis = computed(() => this.selectedOrderedApis());
  selectedIds = computed(() => new Set(this.selectedOrderedApis().map((a) => a.id)));

  displayedColumns = ['select', 'name', 'path', 'labels'];

  rows$: Observable<ApiRow[]>;

  selectedPanelExpanded = signal(true);

  ngOnInit(): void {
    this.selectedOrderedApis.set(this.selectedApiIds.map((id) => ({ id, name: id })));

    const disabledSet = new Set(this.disabledApiIds ?? []);

    const getApiPath = (api: Api): string => {
      const anyApi = api as any;

      const listeners = anyApi.listeners as Array<any> | undefined;
      const firstListener = listeners?.[0];

      const firstPathFromListener = firstListener?.paths?.[0]?.path;
      if (typeof firstPathFromListener === 'string' && firstPathFromListener.length > 0) {
        return firstPathFromListener;
      }

      const firstPathMapping = firstListener?.pathMappings?.[0];
      if (typeof firstPathMapping === 'string' && firstPathMapping.length > 0) {
        return firstPathMapping;
      }

      if (typeof anyApi.contextPath === 'string') {
        return anyApi.contextPath;
      }

      if (typeof anyApi.path === 'string') {
        return anyApi.path;
      }

      return '';
    };

    this.rows$ = this.searchControl.valueChanges.pipe(
      startWith(this.searchControl.value),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query) =>
        this.refresh$.pipe(
          startWith(undefined),
          switchMap(() => this.apiService.search({ query }, undefined, this.pageIndex() + 1, this.pageSize(), false)),
        ),
      ),
      tap((resp) => {
        const pagination = (resp as any).pagination;
        this.total.set(pagination?.totalCount ?? pagination?.totalElements ?? pagination?.total ?? 0);
      }),
      map((resp) =>
        (resp.data ?? []).map((api: Api) => ({
          id: api.id,
          name: api.name,
          path: getApiPath(api),
          labels: (api as any).labels ?? [],
          isDisabled: disabledSet.has(api.id),
        })),
      ),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.pageIndex.set(0);
    this.refresh$.next();
  }

  isChecked(apiId: string): boolean {
    return this.selectedIds().has(apiId);
  }

  toggle(apiId: string, checked: boolean, apiName?: string): void {
    const current = this.selectedOrderedApis();

    if (checked) {
      if (!current.some((a) => a.id === apiId)) {
        const next = [...current, { id: apiId, name: apiName ?? apiId }];
        this.selectedOrderedApis.set(next);
        this.selectedApiIdsChange.emit(next.map((a) => a.id));
      }
    } else {
      const next = current.filter((a) => a.id !== apiId);
      this.selectedOrderedApis.set(next);
      this.selectedApiIdsChange.emit(next.map((a) => a.id));
    }
  }

  removeSelected(apiId: string): void {
    this.toggle(apiId, false);
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.refresh$.next();
  }

  trackSelectedById(_index: number, api: SelectedApi): string {
    return api.id;
  }

  toggleSelectedPanel(): void {
    this.selectedPanelExpanded.set(!this.selectedPanelExpanded());
  }

  constructor(private readonly apiService: ApiV2Service) {}
}
