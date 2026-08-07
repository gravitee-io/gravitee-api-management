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
import { ApiProductCardComponent } from '../../components/api-product-card/api-product-card.component';
import { BadgeComponent } from '../../components/badge/badge.component';
import { ButtonToggleGroupComponent } from '../../components/button-toggle-group/button-toggle-group.component';
import { ButtonToggleOptionComponent } from '../../components/button-toggle-group/button-toggle-option.component';
import { CardsGridComponent } from '../../components/cards-grid/cards-grid.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { OverflowLabelsComponent } from '../../components/overflow-labels/overflow-labels.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';

interface CatalogItemVM {
  id: string;
  type: 'API' | 'API_PRODUCT';
  title: string;
  version: string;
  content?: string;
  rootId: string;
  navItemId: string;
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
  pageSize = 20;
  pageSizeOptions = [8, 20, 40, 80];
  viewMode = signal<'grid' | 'list'>('grid');

  private readonly portalNavigationItemsService = inject(PortalNavigationItemsService);
  private readonly breakpointService = inject(ObservabilityBreakpointService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly isMobile = this.breakpointService.isMobile;

  private readonly page$ = new BehaviorSubject<number>(1);
  protected readonly query = toSignal(this.route.queryParams.pipe(map(p => p['query'] ?? '')), { initialValue: '' });
  protected readonly tableColumns = computed(() => (this.isMobile() ? ['name', 'version', 'mcp'] : ['name', 'labels', 'version', 'mcp']));
  protected catalogPaginator: Signal<CatalogPaginatorVM> = toSignal(this.loadCatalogItems$(), {
    initialValue: { data: [], page: 1, totalResults: 0 },
  });

  constructor() {
    effect(() => {
      if (this.query() || this.query() === '') {
        this.page$.next(1);
      }
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
      map(page => ({ page, pageSize: this.pageSize, query: this.query() })),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      tap(_ => this.loadingPage.set(true)),
      switchMap(({ page, pageSize, query }) =>
        this.portalNavigationItemsService
          .searchCatalogItems(page, query, pageSize)
          .pipe(catchError(_ => of({ data: [], metadata: undefined }))),
      ),
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
        };
      }),
      tap(_ => this.loadingPage.set(false)),
    );
  }
}
