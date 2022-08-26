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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { isEqual } from 'lodash';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Api } from '../../../entities/api';
import { PagedResult } from '../../../entities/pagedResult';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiService } from '../../../services-ngx/api.service';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

type ApisTableDS = { id: string; name: string }[];

@Component({
  selector: 'api-list',
  template: require('./api-list.component.html'),
  styles: [require('./api-list.component.scss')],
})
export class ApiListComponent implements OnInit, OnDestroy {
  displayedColumns = ['name'];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS = [];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  constructor(
    @Inject(UIRouterStateParams) private $stateParams,
    @Inject(UIRouterState) private readonly $state: StateService,
    private readonly apiService: ApiService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit(): void {
    this.initFilters();

    this.filters$
      .pipe(
        takeUntil(this.unsubscribe$),
        debounceTime(100),
        distinctUntilChanged(isEqual),
        tap(({ pagination, searchTerm, status, sort }) => {
          // Change url params
          this.$state.go(
            '.',
            { q: searchTerm, page: pagination.index, size: pagination.size, status, order: toOrder(sort) },
            { notify: false },
          );
        }),
        switchMap(({ pagination, searchTerm, sort }) => {
          return this.apiService.list(searchTerm, toOrder(sort), pagination.index, pagination.size).pipe(
            tap((apisPage) => {
          this.apisTableDS = this.toApisTableDS(apisPage);
          this.apisTableDSUnpaginatedLength = apisPage.page.total_elements;
        }),
            catchError(() => of(new PagedResult<Api>())),
          );
        }),
      )
      .subscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  onEditActionClicked(api: ApisTableDS[number]) {
    this.$state.go('management.apis.detail.portal.general', { apiId: api.id });
  }

  onAddApiClick() {
    this.$state.go('management.apis.new');
  }

  private initFilters() {
    const initialSearchValue = this.$stateParams.q ?? this.filters.searchTerm;
    const initialPageNumber = this.$stateParams.page ? Number(this.$stateParams.page) : this.filters.pagination.index;
    const initialPageSize = this.$stateParams.size ? Number(this.$stateParams.size) : this.filters.pagination.size;
    const initialSort = toSort(this.$stateParams.order, this.filters.sort);
    this.filters = {
      searchTerm: initialSearchValue,
      sort: initialSort,
      pagination: {
        ...this.filters$.value.pagination,
        index: initialPageNumber,
        size: initialPageSize,
      },
    };
    this.filters$.next(this.filters);
  }

  private toApisTableDS(api: PagedResult<Api>): ApisTableDS {
    return api.page.total_elements > 0
      ? api.data.map((api) => ({
          id: api.id,
          name: api.name,
        }))
      : [];
  }
}
