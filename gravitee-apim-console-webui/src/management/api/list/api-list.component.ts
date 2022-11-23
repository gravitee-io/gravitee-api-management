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
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { isEqual } from 'lodash';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Api, ApiLifecycleState, ApiOrigin, ApiState } from '../../../entities/api';
import { PagedResult } from '../../../entities/pagedResult';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiService } from '../../../services-ngx/api.service';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { Constants } from '../../../entities/Constants';

export type ApisTableDS = {
  id: string;
  name: string;
  contextPath: string;
  tags: string;
  owner: string;
  ownerEmail: string;
  picture: string;
  state: ApiState;
  lifecycleState: ApiLifecycleState;
  workflowBadge?: { text: string; class: string };
  isNotSynced$?: Observable<boolean>;
  qualityScore$?: Observable<{ score: number; class: string }>;
  visibility: { label: string; icon: string };
  origin: ApiOrigin;
  readonly: boolean;
  definitionVersion: { label: string; icon?: string };
}[];

@Component({
  selector: 'api-list',
  template: require('./api-list.component.html'),
  styles: [require('./api-list.component.scss')],
})
export class ApiListComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'states', 'contextPath', 'tags', 'owner', 'definitionVersion', 'visibility', 'actions'];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS = [];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  isQualityDisplayed: boolean;
  searchLabel = 'Search APIs | name:"My api *" ownerName:admin';
  isLoadingData = true;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private visibilitiesIcons = {
    PUBLIC: 'public',
    PRIVATE: 'lock',
  };

  constructor(
    @Inject(UIRouterStateParams) private ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly apiService: ApiService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit(): void {
    this.initFilters();
    this.isQualityDisplayed = this.constants.env.settings.apiQualityMetrics && this.constants.env.settings.apiQualityMetrics.enabled;
    if (this.isQualityDisplayed) {
      this.displayedColumns.splice(5, 0, 'qualityScore');
    }

    this.filters$
      .pipe(
        takeUntil(this.unsubscribe$),
        debounceTime(100),
        distinctUntilChanged(isEqual),
        tap(({ pagination, searchTerm, status, sort }) => {
          // Change url params
          this.ajsState.go(
            '.',
            { q: searchTerm, page: pagination.index, size: pagination.size, status, order: toOrder(sort) },
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
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  onEditActionClicked(api: ApisTableDS[number]) {
    this.ajsState.go('management.apis.detail.portal.general', { apiId: api.id });
  }

  onAddApiClick() {
    this.ajsState.go('management.apis.new');
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

  private toApisTableDS(api: PagedResult<Api>): ApisTableDS {
    return api.page.total_elements > 0
      ? api.data.map((api) => ({
          id: api.id,
          name: api.name,
          contextPath: api.context_path,
          tags: api.tags.join(', '),
          owner: api?.owner?.displayName,
          ownerEmail: api?.owner?.email,
          picture: api.picture_url,
          state: api.state,
          lifecycleState: api.lifecycle_state,
          workflowBadge: this.getWorkflowBadge(api),
          isNotSynced$: this.apiService.isAPISynchronized(api.id).pipe(map((a) => !a.is_synchronized)),
          qualityScore$: this.isQualityDisplayed
            ? this.apiService.getQualityMetrics(api.id).pipe(map((a) => this.getQualityScore(Math.floor(a.score * 100))))
            : null,
          visibility: { label: api.visibility, icon: this.visibilitiesIcons[api.visibility] },
          origin: api.definition_context.origin,
          readonly: api.definition_context?.origin === 'kubernetes',
          definitionVersion: this.getDefinitionVersion(api),
        }))
      : [];
  }

  private getDefinitionVersion(api) {
    switch (api.gravitee) {
      case '4.0.0':
        return { label: 'Event native' };
      case '2.0.0':
        return { label: 'Policy studio' };
      default:
        return { icon: 'gio:alert-circle', label: 'Path based' };
    }
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

  private getQualityScore(score: number) {
    let qualityClass;
    if (score !== undefined) {
      if (score < 50) {
        qualityClass = 'quality-score__bad';
      } else if (score >= 50 && score < 80) {
        qualityClass = 'quality-score__medium';
      } else {
        qualityClass = 'quality-score__good';
      }
    }
    return { score, class: qualityClass };
  }
}
