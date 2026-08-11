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
import { toSignal } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { isEqual } from 'lodash';
import { BehaviorSubject, catchError, distinctUntilChanged, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { BadgeComponent } from '../../components/badge/badge.component';
import { ButtonToggleGroupComponent } from '../../components/button-toggle-group/button-toggle-group.component';
import { ButtonToggleOptionComponent } from '../../components/button-toggle-group/button-toggle-option.component';
import { CardsGridComponent } from '../../components/cards-grid/cards-grid.component';
import { CategorySelectComponent } from '../../components/category-select/category-select.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { PortalCategory } from '../../entities/categories/portal-category';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { PortalCategoriesService } from '../../services/portal-categories.service';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';

interface ApiVM {
  id: string;
  title: string;
  version: string;
  content: string;
  isEnabledMcpServer: boolean;
  picture?: string;
  labels?: string[];
  categoryIds?: string[];
  rootId: string;
  navItemId: string;
}

interface ApiPaginatorVM {
  data: ApiVM[];
  page: number;
  totalResults: number;
  error: boolean;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [
    ApiCardComponent,
    BadgeComponent,
    ButtonToggleGroupComponent,
    ButtonToggleOptionComponent,
    CardsGridComponent,
    CategorySelectComponent,
    LoaderComponent,
    MobileClassDirective,
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
  private readonly categoriesState = toSignal(
    this.portalCategoriesService.getCategories().pipe(
      map(categories => ({ categories, loaded: true })),
      catchError(_ => of({ categories: [] as PortalCategory[], loaded: true })),
    ),
    { initialValue: { categories: [] as PortalCategory[], loaded: false } },
  );
  protected readonly categories = computed(() => this.categoriesState().categories);
  protected readonly unknownCategory = computed(() => {
    const categoryId = this.categoryId();
    const { categories, loaded } = this.categoriesState();
    return !!categoryId && loaded && !categories.some(category => category.id === categoryId);
  });
  protected readonly categoryNameById = computed(() => new Map(this.categories().map(category => [category.id, category.title])));
  protected readonly tableColumns = computed(() =>
    this.isMobile() ? ['name', 'version', 'mcp'] : ['name', 'labels', 'category', 'version', 'mcp'],
  );
  protected apiPaginator: Signal<ApiPaginatorVM> = toSignal(this.loadNavigationItemsWithApis$(), {
    initialValue: { data: [], page: 1, totalResults: 0, error: false },
  });

  constructor() {
    effect(() => {
      this.query();
      this.categoryId();
      this.categoriesState();
      this.page$.next(1);
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

  navigateToApi(id: string) {
    const api = this.apiPaginator().data.find(api => api.id === id);
    if (api) {
      this.router.navigate(['/documentation', api.rootId], { queryParams: { selectedId: api.navItemId } });
    } else {
      this.router.navigate(['/404']);
    }
  }

  private loadNavigationItemsWithApis$(): Observable<ApiPaginatorVM> {
    return this.page$.pipe(
      map(page => ({
        page,
        pageSize: this.pageSize,
        query: this.query(),
        categoryId: this.categoryId(),
        unknownCategory: this.unknownCategory(),
      })),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      switchMap(({ page, pageSize, query, categoryId, unknownCategory }) => {
        if (unknownCategory) {
          return of({ data: [], metadata: undefined, error: true });
        }
        this.loadingPage.set(true);
        return this.portalNavigationItemsService.searchNavigationItemsWithApis(page, query, pageSize, categoryId ?? undefined).pipe(
          map(resp => ({ ...resp, error: false })),
          catchError(_ => of({ data: [], metadata: undefined, error: true })),
        );
      }),
      map(resp => {
        const data = resp.data.map(item => ({
          id: item.id,
          content: item.description,
          version: item.version,
          title: item.name,
          picture: item._links?.picture,
          isEnabledMcpServer: !!item.mcp,
          labels: item.labels,
          categoryIds: item.categoryIds,
          rootId: item.rootId,
          navItemId: item.navItemId,
        }));

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
