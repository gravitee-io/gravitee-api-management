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
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { isEqual } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApiService } from '../../../services-ngx/api.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Constants } from '../../../entities/Constants';
import {
  Api,
  ApiLifecycleState,
  apiSortByParamFromString,
  ApisResponse,
  ApiState,
  ApiV2,
  ApiV4,
  HttpListener,
  Listener,
  ListenerType,
  Origin,
  PagedResult,
  TcpListener,
} from '../../../entities/management-api-v2';

export type ApisTableDS = {
  id: string;
  name: string;
  version: string;
  access: string[];
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
  origin: Origin;
  readonly: boolean;
  definitionVersion: { label: string; icon?: string };
  listenerTypes?: ListenerType[];
}[];

@Component({
  selector: 'api-list',
  templateUrl: './api-list.component.html',
  styleUrls: ['./api-list.component.scss'],
})
export class ApiListComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'states', 'access', 'tags', 'owner', 'definitionVersion', 'visibility', 'actions'];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS = [];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
    sort: { active: 'name', direction: 'asc' },
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
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    @Inject(Constants) private readonly constants: Constants,
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
        tap(({ pagination, searchTerm, status, sort }) => {
          // Change url params
          this.router.navigate([], {
            relativeTo: this.activatedRoute,
            queryParams: { q: searchTerm, page: pagination.index, size: pagination.size, status, order: toOrder(sort) },
            queryParamsHandling: 'merge',
          });
        }),
        switchMap(({ pagination, searchTerm, sort }) =>
          this.apiServiceV2
            .search({ query: searchTerm }, apiSortByParamFromString(toOrder(sort)), pagination.index, pagination.size, false)
            .pipe(catchError(() => of(new PagedResult<Api>()))),
        ),
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

  private initFilters() {
    const initialSearchValue = this.activatedRoute.snapshot.queryParams.q ?? this.filters.searchTerm;
    const initialPageNumber = this.activatedRoute.snapshot.queryParams.page
      ? Number(this.activatedRoute.snapshot.queryParams.page)
      : this.filters.pagination.index;
    const initialPageSize = this.activatedRoute.snapshot.queryParams.size
      ? Number(this.activatedRoute.snapshot.queryParams.size)
      : this.filters.pagination.size;
    const initialSort = toSort(this.activatedRoute.snapshot.queryParams.order, this.filters.sort);
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
            origin: api.originContext?.origin,
            readonly: api.originContext?.origin === 'KUBERNETES',
            definitionVersion: this.getDefinitionVersion(api),
            owner: api.primaryOwner?.displayName,
            ownerEmail: api.primaryOwner?.email,
            picture: api._links.pictureUrl,
          };
          if (api.definitionVersion === 'V4') {
            return {
              ...tableDS,
              access: this.getApiAccess(api),
              listenerTypes: api.listeners.map((listener: Listener) => listener.type),
              isNotSynced$: undefined,
              qualityScore$: null,
            };
          } else if (api.definitionVersion === 'FEDERATED') {
            return {
              ...tableDS,
              access: [],
              isNotSynced$: undefined,
              qualityScore$: null,
            };
          } else {
            const apiv2 = api as ApiV2;
            return {
              ...tableDS,
              access: this.getApiAccess(apiv2),
              isNotSynced$: this.apiService.isAPISynchronized(apiv2.id).pipe(map((a) => !a.is_synchronized)),
              qualityScore$: this.isQualityDisplayed
                ? this.apiService.getQualityMetrics(apiv2.id).pipe(map((a) => this.getQualityScore(Math.floor(a.score * 100))))
                : null,
            };
          }
        })
      : [];
  }

  private getDefinitionVersion(api: Api) {
    switch (api.definitionVersion) {
      case 'V2':
        return { label: api.definitionVersion };
      case 'V4':
        return { label: `${api.definitionVersion} - ${(api as ApiV4).type}` };
      case 'FEDERATED':
        return { label: `${api.definitionVersion}` };
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

  private getApiAccess(api: ApiV4 | ApiV2): string[] | null {
    if (api.definitionVersion === 'V4') {
      const tcpListenerHosts = api.listeners
        .filter((listener) => listener.type === 'TCP')
        .flatMap((listener: TcpListener) => listener.hosts);

      const httpListenerPaths = api.listeners
        .filter((listener) => listener.type === 'HTTP')
        .map((listener: HttpListener) => listener.paths.map((path) => `${path.host ?? ''}${path.path}`))
        .flat();

      return tcpListenerHosts.length > 0 ? tcpListenerHosts : httpListenerPaths.length > 0 ? httpListenerPaths : null;
    }

    return api.proxy.virtualHosts?.length > 0 ? api.proxy.virtualHosts.map((vh) => `${vh.host ?? ''}${vh.path}`) : [api.contextPath];
  }
}
