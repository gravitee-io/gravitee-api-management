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
import { Component, Signal, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { isEqual } from 'lodash';
import { BehaviorSubject, catchError, distinctUntilChanged, filter, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { ApiProductCardComponent } from '../../components/api-product-card/api-product-card.component';
import { BadgeComponent } from '../../components/badge/badge.component';
import { ButtonToggleGroupComponent } from '../../components/button-toggle-group/button-toggle-group.component';
import { ButtonToggleOptionComponent } from '../../components/button-toggle-group/button-toggle-option.component';
import { CardsGridComponent } from '../../components/cards-grid/cards-grid.component';
import { DropdownSearchComponent } from '../../components/dropdown-search/dropdown-search.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { OverflowLabelsComponent } from '../../components/overflow-labels/overflow-labels.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { PortalCategory } from '../../entities/categories/portal-category';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { PortalCategoriesService } from '../../services/portal-categories.service';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';

interface CatalogItemVM {
  id: string;
  type: 'API' | 'API_PRODUCT';
  title: string;
  version: string;
  content?: string;
  rootId: string;
  navItemId: string;
  categoryIds?: string[];
}

interface CatalogApiVM extends CatalogItemVM {
  type: 'API';
  isEnabledMcpServer: boolean;
  picture?: string;
  labels?: string[];
}

interface CatalogApiProductVM extends CatalogItemVM {
  type: 'API_PRODUCT';
  apiNames: string[];
}

interface CatalogPaginatorVM {
  data: (CatalogApiVM | CatalogApiProductVM)[];
  page: number;
  totalResults: number;
  error: boolean;
}

interface CategoriesState {
  categories: PortalCategory[];
  status: 'loading' | 'loaded' | 'error';
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
    CardsGridComponent,
    DropdownSearchComponent,
    LoaderComponent,
    MobileClassDirective,
    OverflowLabelsComponent,
    PaginationComponent,
    ReactiveFormsModule,
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
  pageSize = 20;
  pageSizeOptions = [8, 20, 40, 80];
  viewMode = signal<'grid' | 'list'>('grid');

  private readonly portalNavigationItemsService = inject(PortalNavigationItemsService);
  private readonly portalCategoriesService = inject(PortalCategoriesService);
  private readonly breakpointService = inject(ObservabilityBreakpointService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly isMobile = this.breakpointService.isMobile;

  private readonly page$ = new BehaviorSubject<number>(1);
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
  protected readonly categoryOptions = computed(() => this.categories().map(category => ({ value: category.id, label: category.title })));
  protected readonly categoryFilter = new FormControl<string[] | null>(null);
  protected readonly unknownCategory = computed(() => {
    const categoryId = this.categoryId();
    const { categories, status } = this.categoriesState();
    return !!categoryId && status === 'loaded' && !categories.some(category => category.id === categoryId);
  });
  protected readonly categoryNameById = computed(() => new Map(this.categories().map(category => [category.id, category.title])));
  protected readonly tableColumns = computed(() =>
    this.isMobile() ? ['name', 'version', 'mcp'] : ['name', 'labels', 'category', 'version', 'mcp'],
  );
  protected catalogPaginator: Signal<CatalogPaginatorVM> = toSignal(this.loadCatalogItems$(), {
    initialValue: { data: [], page: 1, totalResults: 0, error: false },
  });

  constructor() {
    effect(() => {
      this.query();
      this.categoryId();
      this.categoriesState();
      this.page$.next(1);
    });

    effect(() => {
      const categoryId = this.categoryId();
      this.categoryFilter.setValue(categoryId ? [categoryId] : [], { emitEvent: false });
    });

    this.categoryFilter.valueChanges.pipe(takeUntilDestroyed()).subscribe(values => {
      this.onCategorySelect(values?.[0] ?? null);
    });
  }

  onPageChange(page: number) {
    this.page$.next(page);
  }

  onPageSizeChange(newPageSize: number) {
    this.pageSize = newPageSize;
    this.page$.next(1);
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
        queryParams: { category: categoryId, query: null },
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

  private loadCatalogItems$(): Observable<CatalogPaginatorVM> {
    return this.page$.pipe(
      map(page => ({
        page,
        pageSize: this.pageSize,
        query: this.query(),
        categoryId: this.categoryId(),
        status: this.categoriesState().status,
        unknownCategory: this.unknownCategory(),
      })),
      filter(({ categoryId, status }) => !categoryId || status !== 'loading'),
      map(({ status: _status, ...rest }) => rest),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      switchMap(({ page, pageSize, query, categoryId, unknownCategory }) => {
        if (unknownCategory) {
          return of({ data: [], metadata: undefined, error: true });
        }
        this.loadingPage.set(true);
        return this.portalNavigationItemsService.searchCatalogItems(page, query, pageSize, categoryId ?? undefined).pipe(
          map(resp => ({ ...resp, error: false })),
          catchError(_ => of({ data: [], metadata: undefined, error: true })),
        );
      }),
      map(resp => {
        const data = resp.data.map(item => {
          if (item.type === 'API') {
            return {
              id: item.id,
              type: item.type,
              content: item.description,
              version: item.version,
              title: item.name,
              picture: item._links?.picture,
              isEnabledMcpServer: !!item.mcp,
              labels: item.labels,
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
            apiNames: item.apis.map(api => api.name),
            rootId: item.rootId,
            navItemId: item.navItemId,
          } satisfies CatalogApiProductVM;
        });

        const page = resp.metadata?.pagination?.current_page ?? 1;
        const totalResults = resp.metadata?.pagination?.total ?? 0;
        return {
          data,
          page,
          totalResults,
          error: resp.error,
        };
      }),
      tap(_ => this.loadingPage.set(false)),
    );
  }
}
