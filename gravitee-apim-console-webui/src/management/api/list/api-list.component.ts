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
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApiService } from '../../../services-ngx/api.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Constants } from '../../../entities/Constants';
import {
  Api,
  ApiLifecycleState,
  ApisResponse,
  ApiV2,
  ApiV4,
  ApiState,
  OriginEnum,
  HttpListener,
  PagedResult,
  apiSortByParamFromString,
} from '../../../entities/management-api-v2';

export type ApisTableDS = {
  id: string;
  name: string;
  version: string;
  contextPath: string[];
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
  origin: OriginEnum;
  readonly: boolean;
  definitionVersion: { label: string; icon?: string };
  targetRoute: string;
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
    private readonly apiServiceV2: ApiV2Service,
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
        debounceTime(100),
        distinctUntilChanged(isEqual),
        map(({ pagination, searchTerm, status, sort }) => {
          let order: string;
          if (!searchTerm && !sort?.direction) {
            order = 'name';
          } else if (searchTerm && !sort?.direction) {
            order = undefined;
          } else {
            order = toOrder(sort);
          }

          return {
            pagination,
            searchTerm,
            status,
            order,
          };
        }),
        tap(({ pagination, searchTerm, status, order }) => {
          // Change url params
          this.ajsState.go('.', { q: searchTerm, page: pagination.index, size: pagination.size, status, order }, { notify: false });
        }),
        switchMap(({ pagination, searchTerm, order }) => {
          return this.apiServiceV2
            .search({ query: searchTerm }, apiSortByParamFromString(order), pagination.index, pagination.size)
            .pipe(catchError(() => of(new PagedResult<Api>())));
        }),
        tap((apisPage) => {
          this.apisTableDS = this.toApisTableDS(apisPage);
          this.apisTableDSUnpaginatedLength = apisPage.pagination.totalCount;
          this.isLoadingData = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  onEditActionClicked(api: ApisTableDS[number]) {
    this.ajsState.go(api.targetRoute, { apiId: api.id });
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

  private toApisTableDS(apisResponse: ApisResponse): ApisTableDS {
    return apisResponse?.pagination?.totalCount > 0
      ? apisResponse.data.map((api) => {
          const tableDS = {
            id: api.id,
            name: api.name,
            version: api.apiVersion,
            tags: api.tags?.join(', '),
            state: api.state,
            lifecycleState: api.lifecycleState,
            workflowBadge: this.getWorkflowBadge(api),
            visibility: { label: api.visibility, icon: this.visibilitiesIcons[api.visibility] },
            origin: api.definitionContext?.origin,
            readonly: api.definitionContext?.origin === 'KUBERNETES',
            definitionVersion: this.getDefinitionVersion(api),
            owner: api.primaryOwner?.displayName,
            ownerEmail: api.primaryOwner?.email,
            picture: api._links.pictureUrl,
          };
          if (api.definitionVersion === 'V4') {
            const apiv4 = api as ApiV4;
            return {
              ...tableDS,
              contextPath: this.getContextPath(apiv4),
              isNotSynced$: undefined,
              qualityScore$: null,
              targetRoute: 'management.apis.ng.general',
            };
          } else {
            const apiv2 = api as ApiV2;
            return {
              ...tableDS,
              contextPath: [apiv2.contextPath],
              isNotSynced$: this.apiService.isAPISynchronized(apiv2.id).pipe(map((a) => !a.is_synchronized)),
              qualityScore$: this.isQualityDisplayed
                ? this.apiService.getQualityMetrics(apiv2.id).pipe(map((a) => this.getQualityScore(Math.floor(a.score * 100))))
                : null,
              targetRoute: 'management.apis.ng.general',
            };
          }
        })
      : [];
  }

  private getContextPath(api: ApiV4): string[] {
    if (api.listeners?.length > 0) {
      const httpListener = api.listeners.find((listener) => listener.type === 'HTTP');
      if (httpListener) {
        return (httpListener as HttpListener).paths.map((path) => `${path.host ?? ''}${path.path}`);
      }
    }
    return null;
  }

  private getDefinitionVersion(api: Api) {
    switch (api.definitionVersion) {
      case 'V2':
        return { label: api.definitionVersion };
      case 'V4':
        return { label: `${api.definitionVersion} - ${(api as ApiV4).type}` };
      default:
        return { icon: 'gio:alert-circle', label: 'V1' };
    }
  }

  private getWorkflowBadge(api: Api) {
    const state = api.lifecycleState === 'DEPRECATED' ? api.lifecycleState : api.workflowState;
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
