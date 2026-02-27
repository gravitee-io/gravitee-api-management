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
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
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
import { LoaderComponent } from '../../components/loader/loader.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { ApisResponse } from '../../entities/api/apis-response';
import { ApiService } from '../../services/api.service';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { CardsGridComponent } from 'src/components/list-page/cards-grid.component';

interface ApiVM {
  id: string;
  title: string;
  version: string;
  content: string;
  isEnabledMcpServer: boolean;
  picture?: string;
  labels?: string[];
}

interface ApiPaginatorVM {
  data: ApiVM[];
  page: number;
  totalResults: number;
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
    LoaderComponent,
    PaginationComponent,
    SearchBarComponent,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent {
  loadingPage: boolean = true;
  pageSize = 20;
  pageSizeOptions = [8, 20, 40, 80];
  viewMode = signal<'grid' | 'list'>('grid');

  private readonly apiService = inject(ApiService);
  private readonly breakpointService = inject(ObservabilityBreakpointService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly isMobile = this.breakpointService.isMobile;
  protected readonly isNarrow = this.breakpointService.isNarrow;

  private readonly page$ = new BehaviorSubject<number>(1);
  protected readonly query = toSignal(this.route.queryParams.pipe(map(p => p['query'] ?? '')), { initialValue: '' });
  protected readonly tableColumns = computed(() => (this.isMobile() ? ['name', 'version', 'mcp'] : ['name', 'labels', 'version', 'mcp']));
  protected apiPaginator: ReturnType<typeof toSignal<ApiPaginatorVM, ApiPaginatorVM>>;

  constructor() {
    effect(() => {
      if (this.query() || this.query() === '') {
        this.page$.next(1);
      }
    });
    this.apiPaginator = toSignal(this.loadApis$(), { initialValue: { data: [], page: 1, totalResults: 0 } });
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

  navigateToApi(id: string) {
    this.router.navigate(['api', id], { relativeTo: this.route });
  }


  private loadApis$(): Observable<ApiPaginatorVM> {
    return this.page$.pipe(
      map(currentPage => ({ currentPage, pageSize: this.pageSize, query: this.query() })),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      tap(_ => (this.loadingPage = true)),
      switchMap(({ currentPage, pageSize, query }) => this.searchApis$(currentPage, pageSize, query)),
      map(resp => {
        const data = resp.data
          ? resp.data.map(api => ({
              id: api.id,
              content: api.description,
              version: api.version,
              title: api.name,
              picture: api._links?.picture,
              isEnabledMcpServer: !!api.mcp,
              labels: api.labels,
            }))
          : [];

        const page = resp.metadata?.pagination?.current_page ?? 1;
        const totalResults = resp.metadata?.pagination?.total ?? 0;
        return {
          data,
          page,
          totalResults,
        };
      }),
      tap(_ => (this.loadingPage = false)),
    );
  }

  private searchApis$(page: number, size: number, query?: string): Observable<ApisResponse> {
    return this.apiService.search(page, 'all', query ?? '', size).pipe(catchError(_ => of({ data: [], metadata: undefined })));
  }
}
