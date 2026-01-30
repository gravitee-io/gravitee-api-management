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
import { AsyncPipe, NgClass } from '@angular/common';
import { Component, computed, effect, inject, input, InputSignal, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { isEqual } from 'lodash';
import { BehaviorSubject, catchError, distinctUntilChanged, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../../../components/api-card/api-card.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginationComponent } from '../../../../components/pagination/pagination.component';
import { SearchBarComponent } from '../../../../components/search-bar/search-bar.component';
import { ApisResponse } from '../../../../entities/api/apis-response';
import { Category } from '../../../../entities/categories/categories';
import { ApiService } from '../../../../services/api.service';
import { ObservabilityBreakpointService } from '../../../../services/observability-breakpoint.service';

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
  selector: 'app-apis-list',
  standalone: true,
  imports: [
    AsyncPipe,
    ApiCardComponent,
    LoaderComponent,
    SearchBarComponent,
    NgClass,
    PaginationComponent,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    RouterModule,
  ],
  templateUrl: './apis-list.component.html',
  styleUrl: './apis-list.component.scss',
})
export class ApisListComponent {
  query: InputSignal<string> = input('');
  currentCategory: InputSignal<Category> = input({});
  searchTerm = output<string>();

  categoryId = computed(() => this.currentCategory().id ?? 'all');

  apiPaginator$: Observable<ApiPaginatorVM> = of();
  loadingPage: boolean = true;
  pageSize = 20;
  pageSizeOptions = [8, 20, 40, 80];
  viewMode = signal<'grid' | 'list'>('grid');

  apiListContainerClasses = computed(() => ({
    'api-list__container--mobile': this.isMobile(),
    'api-list__container--list': this.viewMode() === 'list',
  }));
  protected readonly isMobile = inject(ObservabilityBreakpointService).isMobile;

  private readonly page$ = new BehaviorSubject<number>(1);
  private readonly pageSize$ = new BehaviorSubject<number>(20);

  constructor(private readonly apiService: ApiService) {
    effect(() => {
      if (this.categoryId()) {
        this.page$.next(1);
      }

      if (this.query() || this.query() === '') {
        this.page$.next(1);
      }
    });
    this.apiPaginator$ = this.loadApis$();
  }

  onPageChange(page: number) {
    this.page$.next(page);
  }

  onPageSizeChange(newPageSize: number) {
    this.pageSize = newPageSize;
    this.pageSize$.next(newPageSize);
    this.page$.next(1);
  }

  onSearchResults(searchInput: string) {
    if (searchInput !== this.query()) {
      this.searchTerm.emit(searchInput);
    }
  }

  toggleViewMode() {
    this.viewMode.set(this.viewMode() === 'grid' ? 'list' : 'grid');
  }

  private loadApis$(): Observable<ApiPaginatorVM> {
    return this.page$.pipe(
      switchMap(currentPage =>
        this.pageSize$.pipe(map(pageSize => ({ currentPage, pageSize, category: this.categoryId(), query: this.query() }))),
      ),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      tap(_ => (this.loadingPage = true)),
      switchMap(({ currentPage, pageSize, category, query }) => this.searchApis$(currentPage, pageSize, category, query)),
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

  private searchApis$(page: number, size: number, category: string, query?: string): Observable<ApisResponse> {
    return this.apiService.search(page, category, query ?? '', size).pipe(catchError(_ => of({ data: [], metadata: undefined })));
  }
}
