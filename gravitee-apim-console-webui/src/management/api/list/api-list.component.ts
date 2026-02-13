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
import { BehaviorSubject, merge, Observable, of, Subject } from 'rxjs';
import { castArray, isEqual } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';
import { TitleCasePipe } from '@angular/common';

import { TagService } from '../../../services-ngx/tag.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApiService } from '../../../services-ngx/api.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Constants } from '../../../entities/Constants';
import {
  Api,
  ApiLifecycleState,
  ApiSearchQuery,
  apiSortByParamFromString,
  ApisResponse,
  ApiState,
  ApiV2,
  ApiV4,
  HttpListener,
  KafkaListener,
  Listener,
  ListenerType,
  Origin,
  PagedResult,
  TcpListener,
} from '../../../entities/management-api-v2';
import { CategoryService } from '../../../services-ngx/category.service';
import { ConsoleExtensionEventsService } from '../../../services-ngx/console-extension-events.service';

export enum FilterType {
  API_TYPE,
  STATUS,
  TAGS,
  CATEGORIES,
  PUBLISHED,
  PORTAL_VISIBILITY,
}

const availableDisplayedColumns = [
  'picture',
  'name',
  'apiType',
  'states',
  'access',
  'tags',
  'categories',
  'owner',
  'portalStatus',
  'visibility',
  'actions',
];

export type ApisTableDS = {
  id: string;
  name: string;
  version: string;
  access: string[];
  tags: string[];
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
  provider?: string;
  readonly: boolean;
  definitionVersion: { label: string; icon?: string };
  categories: string[];
}[];

interface ApiListTableWrapperFilters extends GioTableWrapperFilters {
  apiTypes?: string[];
  statuses?: string[];
  tags?: string[];
  categories?: string[];
  published?: string[];
  portalVisibilities?: string[];
}

@Component({
  selector: 'api-list',
  templateUrl: './api-list.component.html',
  styleUrls: ['./api-list.component.scss'],
  providers: [TitleCasePipe],
  standalone: false,
})
export class ApiListComponent implements OnInit, OnDestroy {
  FilterType = FilterType;
  displayedColumns = [...availableDisplayedColumns];
  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS = [];
  filters: ApiListTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
  };
  isQualityDisplayed: boolean;
  searchLabel = 'Search';
  isLoadingData = true;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<ApiListTableWrapperFilters>(this.filters);
  private refreshTrigger$ = new Subject<void>();
  private visibilitiesIcons = {
    PUBLIC: 'public',
    PRIVATE: 'lock',
  };
  public categoriesNames = new Map<string, string>();
  tags: string[] = [];
  checkedApiTypes: string[];
  checkedStatuses: string[];
  checkedTags: string[];
  checkedCategories: string[];
  checkedPublished: string[];
  checkedPortalVisibilities: string[];

  checkedVisibleColumns = {
    apiType: true,
    states: true,
    access: true,
    qualityScore: false,
    tags: true,
    categories: true,
    owner: true,
    portalStatus: true,
    visibility: true,
  };

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    @Inject(Constants) private readonly constants: Constants,
    private readonly apiService: ApiService,
    private readonly apiServiceV2: ApiV2Service,
    private readonly tagService: TagService,
    private readonly titleCasePipe: TitleCasePipe,
    private readonly categoryService: CategoryService,
    private readonly consoleExtensionEventsService: ConsoleExtensionEventsService,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit(): void {
    this.isQualityDisplayed = this.constants.env.settings.apiQualityMetrics && this.constants.env.settings.apiQualityMetrics.enabled;
    if (this.isQualityDisplayed) {
      this.displayedColumns.splice(5, 0, 'qualityScore');
      this.checkedVisibleColumns.qualityScore = true;
    }

    if (localStorage.getItem(`${this.constants.org.currentEnv.id}-api-list-visible-columns`)) {
      let storedColumns = JSON.parse(localStorage.getItem(`${this.constants.org.currentEnv.id}-api-list-visible-columns`));
      if (!this.isQualityDisplayed && storedColumns.includes('qualityScore')) {
        storedColumns = storedColumns.filter((item) => item !== 'qualityScore');
      }
      this.displayedColumns = storedColumns;
      this.checkedVisibleColumns = {
        apiType: this.displayedColumns.includes('apiType'),
        states: this.displayedColumns.includes('states'),
        access: this.displayedColumns.includes('access'),
        tags: this.displayedColumns.includes('tags'),
        qualityScore: this.displayedColumns.includes('qualityScore'),
        categories: this.displayedColumns.includes('categories'),
        owner: this.displayedColumns.includes('owner'),
        portalStatus: this.displayedColumns.includes('portalStatus'),
        visibility: this.displayedColumns.includes('visibility'),
      };
    }

    this.initFilters();

    this.tagService
      .list()
      .pipe(
        map((tags) => (this.tags = tags.map((tag) => tag.id))),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.categoryService
      .list()
      .pipe(
        map((cats) => cats.forEach((cat) => this.categoriesNames.set(cat.key, cat.name))),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    merge(
      this.filters$.pipe(debounceTime(100), distinctUntilChanged(isEqual)),
      this.refreshTrigger$.pipe(map(() => this.filters$.value)),
    )
      .pipe(
        map((filters: ApiListTableWrapperFilters) => {
          let order: string;
          if (filters.sort?.direction) {
            order = toOrder(filters.sort);
          } else if (!filters.searchTerm && !filters.sort?.direction) {
            order = 'name';
          } else if (filters.searchTerm && !filters.sort?.direction) {
            order = undefined;
          } else {
            order = toOrder(filters.sort);
          }
          return { filters, order };
        }),
        tap(({ filters, order }) => {
          // Change url params
          this.router.navigate([], {
            relativeTo: this.activatedRoute,
            queryParams: {
              q: filters.searchTerm,
              page: filters.pagination.index,
              size: filters.pagination.size,
              status,
              order,
              apiTypes: filters.apiTypes,
              statuses: filters.statuses,
              tags: filters.tags,
              categories: filters.categories,
              published: filters.published,
              portalVisibilities: filters.portalVisibilities,
            },
            queryParamsHandling: 'merge',
          });
        }),
        switchMap(({ filters, order }) => {
          const body: ApiSearchQuery = {
            query: filters.searchTerm,
            apiTypes: this.filters.apiTypes,
            statuses: this.filters.statuses,
            tags: this.filters.tags,
            categories: this.filters.categories,
            published: this.filters.published,
            visibilities: this.filters.portalVisibilities,
          };
          return this.apiServiceV2
            .search(body, apiSortByParamFromString(order), filters.pagination.index, filters.pagination.size)
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

    this.consoleExtensionEventsService
      .on('API')
      .pipe(debounceTime(500), takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this.refreshTrigger$.next();
      });
  }

  onAdditionalFiltersChanged(type: FilterType) {
    this.filters.pagination = {
      ...this.filters.pagination,
      index: 1,
    };
    switch (type) {
      case FilterType.API_TYPE:
        this.filters = { ...this.filters, apiTypes: this.checkedApiTypes };
        break;
      case FilterType.STATUS:
        this.filters = { ...this.filters, statuses: this.checkedStatuses };
        break;
      case FilterType.TAGS:
        this.filters = { ...this.filters, tags: this.checkedTags };
        break;
      case FilterType.CATEGORIES:
        this.filters = { ...this.filters, categories: this.checkedCategories };
        break;
      case FilterType.PUBLISHED:
        this.filters = { ...this.filters, published: this.checkedPublished };
        break;
      case FilterType.PORTAL_VISIBILITY:
        this.filters = { ...this.filters, portalVisibilities: this.checkedPortalVisibilities };
        break;
    }
    this.filters$.next(this.filters);
  }

  updateVisibleColumns() {
    const checkedColumns = Object.entries(this.checkedVisibleColumns)
      .filter(([_k, v]) => v)
      .map(([k]) => k);
    this.displayedColumns = ['picture', 'name', ...checkedColumns, 'actions'];
    localStorage.setItem(`${this.constants.org.currentEnv.id}-api-list-visible-columns`, JSON.stringify(this.displayedColumns));
  }

  onFiltersChanged(filters: ApiListTableWrapperFilters) {
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
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.apiTypes) {
      this.filters.apiTypes = castArray(queryParams.apiTypes);
      this.checkedApiTypes = this.filters.apiTypes;
    }
    if (queryParams.statuses) {
      this.filters.statuses = castArray(queryParams.statuses);
      this.checkedStatuses = this.filters.statuses;
    }
    if (queryParams.tags) {
      this.filters.tags = castArray(queryParams.tags);
      this.checkedTags = this.filters.tags;
    }
    if (queryParams.categories) {
      this.filters.categories = castArray(queryParams.categories);
      this.checkedCategories = this.filters.categories;
    }
    if (queryParams.published) {
      this.filters.published = castArray(queryParams.published);
      this.checkedPublished = this.filters.published;
    }
    if (queryParams.portalVisibilities) {
      this.filters.portalVisibilities = castArray(queryParams.portalVisibilities);
      this.checkedPortalVisibilities = this.filters.portalVisibilities;
    }
    this.filters$.next(this.filters);
  }

  private toApisTableDS(apisResponse: ApisResponse): ApisTableDS {
    return apisResponse?.pagination?.totalCount > 0
      ? apisResponse.data.map((api) => {
          const tableDS = {
            id: api.id,
            name: api.name,
            version: api.apiVersion,
            tags: api.tags ?? [],
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
            categories: (api.categories ?? []).map((cat) => this.categoriesNames.get(cat)),
          };
          if (api.definitionVersion === 'V4') {
            return {
              ...tableDS,
              access: this.getApiAccess(api),
              isNotSynced$: undefined,
              qualityScore$: null,
            };
          } else if (api.definitionVersion === 'FEDERATED' || api.definitionVersion === 'FEDERATED_AGENT') {
            return {
              ...tableDS,
              access: [],
              isNotSynced$: undefined,
              qualityScore$: null,
              provider: api.originContext.provider,
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
        return { label: 'V2 HTTP Proxy' };
      case 'V4':
        return { label: this.getLabelType(api) };
      case 'FEDERATED':
        return { label: 'Federated API' };
      case 'FEDERATED_AGENT':
        return { label: 'Federated A2A Agent' };
      default:
        return { icon: 'gio:alert-circle', label: 'V1' };
    }
  }

  private getLabelType(api: ApiV4): string {
    if (api.type === 'MESSAGE') {
      return 'Message';
    }
    if (api.type === 'NATIVE') {
      return api.listeners.map((listener: Listener): ListenerType => listener.type).includes('KAFKA') ? 'Kafka Native' : 'Native';
    }
    if (api.type === 'MCP_PROXY') {
      return 'MCP Proxy';
    }
    if (api.type === 'LLM_PROXY') {
      return 'LLM Proxy';
    }

    return api.listeners.map((listener: Listener): ListenerType => listener.type).includes('TCP') ? 'TCP Proxy' : 'HTTP Proxy';
  }

  displayFirstTag(element) {
    if (this.filters.sort?.active === 'tags' && this.filters.sort?.direction === 'desc') {
      return element.tags?.sort()?.reverse()[0];
    }
    return element.tags?.sort()?.[0];
  }

  displayFirstCategory(element) {
    if (this.filters.sort?.active === 'categories' && this.filters.sort?.direction === 'desc') {
      return element.categories?.sort()?.reverse()[0];
    }
    return element.categories?.sort()?.[0];
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
      if (api.type === 'NATIVE') {
        const kafkaListenerHosts = api.listeners
          .filter((listener) => listener.type === 'KAFKA')
          .map((kafkaListener: KafkaListener) => {
            const host = kafkaListener.host ?? '';
            const port = kafkaListener.port ? `:${kafkaListener.port}` : '';
            return `${host}${port}`;
          });

        return kafkaListenerHosts.length > 0 ? kafkaListenerHosts : null;
      }

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
