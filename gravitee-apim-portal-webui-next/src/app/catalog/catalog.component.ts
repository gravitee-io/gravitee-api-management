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
import { Component, LOCALE_ID, Signal, computed, effect, inject, signal, viewChild } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import type { LocalizeFn } from '@angular/localize/init';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { isEqual } from 'lodash';
import { catchError, distinctUntilChanged, filter, from, map, mergeMap, Observable, scan, startWith, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { CatalogFilterSelection, buildFilterFields, matchesFilters, pruneSelection } from './catalog-filters';
import { CatalogFiltersComponent } from './catalog-filters.component';
import { CatalogKind, accessLabel, catalogAccess, isAgent, protocolLabel, publishedLabel } from './catalog-item';
import { ApiCardAccess, ApiCardComponent, ApiCardSkill } from '../../components/api-card/api-card.component';
import { ApiProductCardComponent } from '../../components/api-product-card/api-product-card.component';
import { BadgeComponent } from '../../components/badge/badge.component';
import { ButtonToggleGroupComponent } from '../../components/button-toggle-group/button-toggle-group.component';
import { ButtonToggleOptionComponent } from '../../components/button-toggle-group/button-toggle-option.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { OverflowLabelsComponent } from '../../components/overflow-labels/overflow-labels.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { PortalCategory } from '../../entities/categories/portal-category';
import { getPlanSecurityTypeLabel, Plan } from '../../entities/plan/plan';
import { CurrentUserService } from '../../services/current-user.service';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { PlanService } from '../../services/plan.service';
import { PortalCategoriesService } from '../../services/portal-categories.service';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';
import { SubscriptionService } from '../../services/subscription.service';

interface CatalogItemVM {
  id: string;
  type: 'API' | 'API_PRODUCT' | 'AGENT';
  title: string;
  version: string;
  content?: string;
  rootId: string;
  navItemId: string;
  categoryIds?: string[];
}

interface CatalogApiVM extends CatalogItemVM {
  type: 'API' | 'AGENT';
  isEnabledMcpServer: boolean;
  picture?: string;
  labels?: string[];
  isAgent: boolean;
  capabilities: string[];
  publishedAt?: Date;
  updatedAt?: Date;
  publisher?: string;
  skills: ApiCardSkill[];
  endpoint?: string;
  protocol: string;
  published?: string;
}

interface CatalogAccessVM {
  access?: ApiCardAccess;
  accessLabel?: string;
  planSummary?: string;
}

type CatalogCardVM = (CatalogApiVM | CatalogApiProductVM) & CatalogAccessVM;

interface CatalogApiProductVM extends CatalogItemVM {
  type: 'API_PRODUCT';
  apiNames: string[];
}

interface CatalogVM {
  data: (CatalogApiVM | CatalogApiProductVM)[];
  error: boolean;
}

interface CategoriesState {
  categories: PortalCategory[];
  status: 'loading' | 'loaded' | 'error';
}

declare const $localize: LocalizeFn;

export type CatalogSort = 'name' | 'newest' | 'updated';

function buildComparator(sort: CatalogSort): (left: CatalogCardVM, right: CatalogCardVM) => number {
  if (sort === 'name') {
    return (left, right) => left.title.localeCompare(right.title);
  }
  const field = sort === 'newest' ? 'publishedAt' : 'updatedAt';
  return (left, right) => timestampOf(right, field) - timestampOf(left, field);
}

function timestampOf(item: CatalogCardVM, field: 'publishedAt' | 'updatedAt'): number {
  const value = item.type === 'API' ? item[field] : undefined;
  return value ? new Date(value).getTime() : 0;
}

const ALL_ITEMS = -1;
const PLAN_REQUEST_CONCURRENCY = 6;

const ACCESS_ORDER: ApiCardAccess[] = ['SUBSCRIBED', 'NO_KEY', 'CREDENTIALS', 'APPROVAL'];

function toAccessVM(plans: Plan[], subscribed: boolean): CatalogAccessVM {
  const securities = [...new Set(plans.map(plan => getPlanSecurityTypeLabel(plan.security)))].filter(label => !!label);
  const access = catalogAccess(plans, subscribed);
  return { access, accessLabel: access ? accessLabel(access) : undefined, planSummary: securities.join(' · ') || undefined };
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [
    ApiCardComponent,
    ApiProductCardComponent,
    BadgeComponent,
    ButtonToggleGroupComponent,
    ButtonToggleOptionComponent,
    CatalogFiltersComponent,
    LoaderComponent,
    MobileClassDirective,
    OverflowLabelsComponent,
    PaginationComponent,
    SearchBarComponent,
    MatChipsModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent {
  readonly loadingPage = signal<boolean>(true);
  protected readonly page = signal(1);
  protected readonly pageSize = signal(20);
  pageSizeOptions = [8, 20, 40, 80];
  viewMode = signal<'grid' | 'list'>('grid');

  private readonly portalNavigationItemsService = inject(PortalNavigationItemsService);
  private readonly planService = inject(PlanService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly portalCategoriesService = inject(PortalCategoriesService);
  private readonly breakpointService = inject(ObservabilityBreakpointService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly locale = inject(LOCALE_ID);
  protected readonly isMobile = this.breakpointService.isMobile;

  protected readonly agentTypeLabel = $localize`:@@catalogAgentType:AGENT`;
  protected readonly apiTypeLabel = $localize`:@@catalogApiType:API`;
  private readonly kindInUrl = toSignal(
    this.route.queryParams.pipe(map(params => (params['kind'] === 'apis' ? 'apis' : 'agents') as CatalogKind)),
    { initialValue: 'agents' as CatalogKind },
  );
  protected readonly kind = signal<CatalogKind>('agents');
  protected readonly sort = signal<CatalogSort>('name');
  protected readonly filtersOpen = signal(false);
  protected readonly filters = signal<CatalogFilterSelection>({});

  protected readonly query = toSignal(this.route.queryParams.pipe(map(p => p['query'] ?? '')), { initialValue: '' });
  protected readonly categoryId = toSignal(this.route.queryParams.pipe(map(p => p['category'] ?? null)), {
    initialValue: null as string | null,
  });
  private readonly categoriesState = toSignal<CategoriesState, CategoriesState>(
    this.portalCategoriesService.getCategories().pipe(
      map((categories): CategoriesState => ({ categories, status: 'loaded' })),
      catchError((): Observable<CategoriesState> => of({ categories: [], status: 'error' })),
    ),
    { initialValue: { categories: [] as PortalCategory[], status: 'loading' as const } },
  );
  protected readonly categories = computed(() => this.categoriesState().categories);
  protected readonly unknownCategory = computed(() => {
    const categoryId = this.categoryId();
    const { categories, status } = this.categoriesState();
    return !!categoryId && status === 'loaded' && !categories.some(category => category.id === categoryId);
  });
  protected readonly categoryNameById = computed(() => new Map(this.categories().map(category => [category.id, category.title])));
  protected readonly tableColumns = computed(() =>
    this.isMobile() ? ['name', 'version', 'mcp'] : ['name', 'labels', 'category', 'version', 'mcp'],
  );
  private readonly searchInputs = computed(() => ({
    query: this.query(),
    status: this.categoriesState().status,
    unknownCategory: this.unknownCategory(),
  }));
  protected catalog: Signal<CatalogVM> = toSignal(this.loadCatalogItems$(), { initialValue: { data: [], error: false } });

  private readonly filtersPanel = viewChild(CatalogFiltersComponent);
  private readonly accessInputs = computed(() => ({
    apiIds: this.catalog()
      .data.filter(item => item.type !== 'API_PRODUCT')
      .map(item => item.id),
    authenticated: this.currentUserService.isUserAuthenticated(),
  }));
  private readonly accessByApiId: Signal<Map<string, CatalogAccessVM>> = toSignal(
    toObservable(this.accessInputs).pipe(
      distinctUntilChanged(isEqual),
      switchMap(({ apiIds, authenticated }) => this.loadAccess$(apiIds, authenticated)),
    ),
    { initialValue: new Map<string, CatalogAccessVM>() },
  );

  private readonly itemsOfSelectedKind = computed(() => {
    const kind = this.kind();
    return this.catalog().data.filter(item => (kind === 'agents') === this.isAgentItem(item));
  });
  private readonly candidates = computed<CatalogCardVM[]>(() => {
    const accessByApiId = this.accessByApiId();
    return this.itemsOfSelectedKind().map(item => ({ ...item, ...(accessByApiId.get(item.id) ?? {}) }));
  });
  protected readonly candidatesCount = computed(() => this.candidates().length);
  protected readonly filterFields = computed(() =>
    buildFilterFields(
      this.candidates(),
      { category: this.categoryNameById(), access: new Map(ACCESS_ORDER.map(access => [access, accessLabel(access)])) },
      this.filters(),
    ),
  );
  protected readonly appliedFilters = computed(() => pruneSelection(this.filters(), this.filterFields()));
  protected readonly visibleItems = computed<CatalogCardVM[]>(() =>
    [...this.candidates().filter(item => matchesFilters(item, this.appliedFilters()))].sort(buildComparator(this.sort())),
  );
  protected readonly lastPage = computed(() => Math.max(1, Math.ceil(this.visibleItems().length / this.pageSize())));
  protected readonly currentPage = computed(() => Math.min(this.page(), this.lastPage()));
  protected readonly pagedItems = computed<CatalogCardVM[]>(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.visibleItems().slice(start, start + this.pageSize());
  });
  protected readonly kindCounts = computed(() => {
    const data = this.catalog().data;
    const agents = data.filter(item => this.isAgentItem(item)).length;
    return { agents, apis: data.length - agents };
  });
  protected readonly tally = computed(() => {
    const shown = this.visibleItems().length;
    if (this.kind() === 'agents') {
      return shown === 1 ? $localize`:@@catalogOneAgent:1 Agent` : $localize`:@@catalogManyAgents:${shown}:count: Agents`;
    }
    return shown === 1 ? $localize`:@@catalogOneApi:1 API` : $localize`:@@catalogManyApis:${shown}:count: APIs`;
  });
  protected readonly hasClearableFilters = computed(() =>
    Boolean(this.query() || Object.values(this.appliedFilters()).some(values => values?.length)),
  );

  constructor() {
    effect(() => {
      this.query();
      this.categoryId();
      this.categoriesState();
      this.kind();
      this.filters();
      this.page.set(1);
    });

    effect(() => {
      this.kind.set(this.kindInUrl());
    });

    effect(() => {
      const categoryId = this.categoryId();
      const selected = this.filters().category ?? [];
      if (categoryId && !selected.includes(categoryId)) {
        this.filters.set({ ...this.filters(), category: [categoryId] });
      } else if (!categoryId && selected.length) {
        this.filters.set({ ...this.filters(), category: [] });
      }
    });
  }

  selectKind(kind: CatalogKind) {
    if (kind !== this.kind()) {
      this.kind.set(kind);
      this.router.navigate([], { relativeTo: this.route, queryParams: { kind }, queryParamsHandling: 'merge' });
    }
  }

  toggleFilters() {
    this.filtersOpen.update(open => !open);
  }

  selectSort(event: Event) {
    this.sort.set((event.target as HTMLSelectElement).value as CatalogSort);
  }

  onFiltersChange(selection: CatalogFilterSelection) {
    this.filters.set(selection);
    const category = selection.category?.[0] ?? null;
    if (category !== this.categoryId()) {
      this.onCategorySelect(category);
    }
  }

  clearFilters() {
    this.filters.set({});
    this.filtersPanel()?.focusFirstField();
    if (this.categoryId() || this.query()) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { category: null, query: null },
        queryParamsHandling: 'merge',
      });
    }
  }

  navigateToSubscribe(apiId: string) {
    this.router.navigate(['api', apiId, 'subscribe'], { relativeTo: this.route });
  }

  trackById(_index: number, item: CatalogCardVM) {
    return item.id;
  }

  onPageChange(page: number) {
    this.page.set(page);
  }

  onPageSizeChange(newPageSize: number) {
    this.pageSize.set(newPageSize);
    this.page.set(1);
  }

  onSearchResults(searchInput: string) {
    if (searchInput !== this.query()) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { query: searchInput },
        queryParamsHandling: 'merge',
      });
    }
  }

  onCategorySelect(categoryId: string | null) {
    if (categoryId !== this.categoryId()) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { category: categoryId },
        queryParamsHandling: 'merge',
      });
    }
  }

  toggleViewMode() {
    this.viewMode.set(this.viewMode() === 'grid' ? 'list' : 'grid');
  }

  navigateToDocumentation(item: CatalogApiVM | CatalogApiProductVM) {
    this.router.navigate(['/documentation', item.rootId], { queryParams: { selectedId: item.navItemId } });
  }

  private isAgentItem(item: CatalogApiVM | CatalogApiProductVM): boolean {
    return item.type !== 'API_PRODUCT' && item.isAgent;
  }

  private loadAccess$(apiIds: string[], authenticated: boolean): Observable<Map<string, CatalogAccessVM>> {
    if (apiIds.length === 0) {
      return of(new Map<string, CatalogAccessVM>());
    }
    const subscribedApiIds$ = authenticated
      ? this.subscriptionService.list({ statuses: ['ACCEPTED'], size: -1 }).pipe(
          map(response => new Set(response.data.map(subscription => subscription.api))),
          catchError(() => of(new Set<string | undefined>())),
        )
      : of(new Set<string | undefined>());

    return subscribedApiIds$.pipe(
      switchMap(subscribedApiIds =>
        from(apiIds).pipe(
          mergeMap(
            apiId =>
              this.planService.list(apiId).pipe(
                map(response => [apiId, toAccessVM(response.data ?? [], subscribedApiIds.has(apiId))] as const),
                catchError(() => of([apiId, {} as CatalogAccessVM] as const)),
              ),
            PLAN_REQUEST_CONCURRENCY,
          ),
          scan((accessByApiId, [apiId, access]) => new Map(accessByApiId).set(apiId, access), new Map<string, CatalogAccessVM>()),
          startWith(new Map<string, CatalogAccessVM>()),
        ),
      ),
    );
  }

  private loadCatalogItems$(): Observable<CatalogVM> {
    return toObservable(this.searchInputs).pipe(
      filter(({ status }) => !this.categoryId() || status !== 'loading'),
      map(({ status: _status, ...rest }) => rest),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      switchMap(({ query, unknownCategory }) => {
        if (unknownCategory) {
          return of({ data: [], metadata: undefined, error: true });
        }
        this.loadingPage.set(true);
        return this.portalNavigationItemsService.searchCatalogItems(1, query, ALL_ITEMS).pipe(
          map(resp => ({ ...resp, error: false })),
          catchError(_ => of({ data: [], metadata: undefined, error: true })),
        );
      }),
      map(resp => {
        const data = resp.data.map(item => {
          if (item.type === 'API' || item.type === 'AGENT') {
            const skills = (item.mcp?.tools ?? []).flatMap(tool =>
              tool?.toolDefinition?.name ? [{ name: tool.toolDefinition.name, description: tool.toolDefinition.description }] : [],
            );
            return {
              id: item.id,
              type: item.type,
              content: item.description,
              version: item.version,
              title: item.name,
              picture: item._links?.picture,
              isEnabledMcpServer: !!item.mcp,
              labels: item.labels,
              isAgent: item.type === 'AGENT' || isAgent(item.apiType, !!item.mcp),
              capabilities: skills.length ? skills.map(skill => skill.name) : (item.labels ?? []),
              skills,
              endpoint: item.entrypoints?.[0],
              protocol: protocolLabel(item.apiType, !!item.mcp),
              published: publishedLabel(item.createdAt, this.locale),
              publishedAt: item.createdAt,
              updatedAt: item.updatedAt,
              publisher: item.publisher,
              rootId: item.rootId,
              navItemId: item.navItemId,
              categoryIds: item.categoryIds,
            } satisfies CatalogApiVM;
          }

          return {
            id: item.id,
            type: item.type,
            content: item.description,
            version: item.version,
            title: item.name,
            apiNames: (item.apis ?? []).map(api => api.name),
            rootId: item.rootId,
            navItemId: item.navItemId,
            categoryIds: item.categoryIds,
          } satisfies CatalogApiProductVM;
        });

        const total = resp.metadata?.pagination?.total;
        const truncated = total !== undefined && data.length < total;
        return truncated ? { data: [], error: true } : { data, error: resp.error };
      }),
      catchError(() => of({ data: [] as (CatalogApiVM | CatalogApiProductVM)[], error: true })),
      tap(_ => this.loadingPage.set(false)),
    );
  }
}
