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
import { isEqual } from 'lodash';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Api, ApiOrigin, ApiState } from '../../../entities/api';
import { Constants } from '../../../entities/Constants';
import { PagedResult } from '../../../entities/pagedResult';
import { ApiService } from '../../../services-ngx/api.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

export type ApisTableDS = {
  id: string;
  name: string;
  version: string;
  contextPath: string;
  tags: string;
  owner: string;
  ownerEmail: string;
  picture: string;
  state: ApiState;
  origin: ApiOrigin;
  statusGaugeValue: number;
};

@Component({
  selector: 'home-api-status',
  template: require('./home-api-status.component.html'),
  styles: [require('./home-api-status.component.scss')],
})
export class HomeApiStatusComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'states', 'status', 'actions'];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS[] = [];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  isLoadingData = true;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  constructor(
    @Inject(UIRouterStateParams) private ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly apiService: ApiService,
  ) {}

  ngOnInit(): void {
    this.filters$
      .pipe(
        takeUntil(this.unsubscribe$),
        debounceTime(200),
        distinctUntilChanged(isEqual),
        tap(({ pagination, searchTerm, sort }) => {
          // Change url params
          this.ajsState.go(
            'home.apiStatus',
            { q: searchTerm, page: pagination.index, size: pagination.size, order: toOrder(sort) },
            { notify: false },
          );
        }),
        switchMap(({ pagination, searchTerm, sort }) => this.apiService.list(searchTerm, toOrder(sort), pagination.index, pagination.size)),
        tap((apisPage) => {
          this.apisTableDS = this.toApisTableDS(apisPage);
          this.apisTableDSUnpaginatedLength = apisPage.page.total_elements;
          this.isLoadingData = false;
        }),
        catchError(() => of(new PagedResult<Api>())),
      )
      .subscribe();

    this.initFilters();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  onViewHealthCheckClicked() {
    this.ajsState.go('management.apis.detail.proxy.healthCheckDashboard');
  }

  private initFilters() {
    const initialSearchValue = this.ajsStateParams.q ?? this.filters.searchTerm;
    const initialPageNumber = this.ajsStateParams.page ? Number(this.ajsStateParams.page) : this.filters.pagination.index;
    const initialPageSize = this.ajsStateParams.size ? Number(this.ajsStateParams.size) : this.filters.pagination.size;
    const initialSort = toSort(this.ajsStateParams.order, this.filters.sort);
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

  private toApisTableDS(api: PagedResult<Api>): ApisTableDS[] {
    if (api.page.total_elements === 0) {
      return [];
    }

    return api.data.map(
      (api) =>
        ({
          id: api.id,
          name: api.name,
          version: api.version,
          contextPath: api.context_path,
          tags: api.tags.join(', '),
          owner: api?.owner?.displayName,
          ownerEmail: api?.owner?.email,
          picture: api.picture_url,
          state: api.state,
          lifecycleState: api.lifecycle_state,
          workflowBadge: this.getWorkflowBadge(api),
          healthcheck_enabled: api.healthcheck_enabled,
          origin: api.definition_context.origin,
          statusGaugeValue: 0.7,
        } as ApisTableDS),
    );
  }

  private getWorkflowBadge(api) {
    const state = api.lifecycle_state === 'DEPRECATED' ? api.lifecycle_state : api.workflow_state;
    const toReadableState = {
      DEPRECATED: { text: 'Deprecated', class: 'gio-badge-error' },
      DRAFT: { text: 'Draft', class: 'gio-badge-primary' },
      IN_REVIEW: { text: 'In Review', class: 'gio-badge-error' },
      REQUEST_FOR_CHANGES: { text: 'Need changes', class: 'gio-badge-error' },
    };
    return toReadableState[state];
  }
}
