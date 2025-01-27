/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe } from '@angular/common';
import { Component, computed, input, InputSignal, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, Router } from '@angular/router';
import { InfiniteScrollDirective } from 'ngx-infinite-scroll';
import { catchError, combineLatestWith, EMPTY, map, Observable, scan, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../../components/api-card/api-card.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { SearchBarComponent } from '../../../components/search-bar/search-bar.component';
import { ApisResponse } from '../../../entities/api/apis-response';
import { Category } from '../../../entities/categories/categories';
import { ApiService } from '../../../services/api.service';

export interface ApiVM {
  id: string;
  title: string;
  version: string;
  content: string;
  picture?: string;
}

export interface ApiPaginatorVM {
  data: ApiVM[];
  page: number;
  hasNextPage: boolean;
}

@Component({
  selector: 'app-tabs-view',
  standalone: true,
  imports: [AsyncPipe, MatTabsModule, SearchBarComponent, ApiCardComponent, MatCardModule, LoaderComponent, InfiniteScrollDirective],
  templateUrl: './tabs-view.component.html',
  styleUrl: './tabs-view.component.scss',
})
export class TabsViewComponent {
  categories: InputSignal<Category[]> = input<Category[]>([]);

  apiPaginator$: Observable<ApiPaginatorVM> = of();
  loadingPage: boolean = true;

  query: string = '';
  filter = signal('');
  filterAsCategory = computed(() => this.categories().find(cat => cat.id === this.filter()));
  selectedFilterIndex = computed(() => {
    const foundCategory = this.filterAsCategory();
    return foundCategory ? this.categories().indexOf(foundCategory) + 1 : 0;
  });

  private page = signal(1);
  private page$ = toObservable(this.page);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private apiService: ApiService,
  ) {
    this.apiPaginator$ = this.loadApis$();
  }

  loadMoreApis(paginator: ApiPaginatorVM) {
    if (!paginator.hasNextPage) {
      return;
    }

    this.page.set(paginator.page + 1);
  }

  public onFilterSelection($event: MatTabChangeEvent) {
    const categoryId = this.categories().find(cat => cat.name === $event.tab.textLabel)?.id ?? '';

    this.router.navigate([''], {
      relativeTo: this.route,
      queryParams: {
        filter: categoryId,
        query: this.query,
      },
    });
  }

  public onSearchResults(searchInput: string) {
    this.router.navigate([''], {
      relativeTo: this.route,
      queryParams: {
        filter: this.filter(),
        query: searchInput,
      },
    });
  }

  private loadApis$(): Observable<ApiPaginatorVM> {
    return this.route.queryParams.pipe(
      tap(_ => {
        this.page.set(1);
      }),
      combineLatestWith(this.page$),
      tap(_ => (this.loadingPage = true)),
      switchMap(([queryParams, currentPage]) => {
        const category = queryParams['filter'];
        const query = queryParams['query'];

        this.filter.set(category ?? '');
        this.query = query;

        if (currentPage === 1) {
          return of({ page: currentPage, size: 18, category, query });
        } else if (currentPage === 2) {
          this.page.set(3);
          return EMPTY;
        } else {
          return of({ page: currentPage, size: 9, category, query });
        }
      }),
      switchMap(({ page, size, category, query }) => this.searchApis$(page, size, category, query)),
      map(resp => {
        const data = resp.data
          ? resp.data.map(api => ({
              id: api.id,
              content: api.description,
              version: api.version,
              title: api.name,
              picture: api._links?.picture,
            }))
          : [];

        const page = resp.metadata?.pagination?.current_page ?? 1;
        const hasNextPage = resp.metadata?.pagination?.total_pages ? page < resp.metadata.pagination.total_pages : false;
        return {
          data,
          page,
          hasNextPage,
        };
      }),
      scan(this.updatePaginator, { data: [], page: 1, hasNextPage: true }),
      tap(_ => (this.loadingPage = false)),
    );
  }

  private searchApis$(page: number, size: number, category: string, query?: string): Observable<ApisResponse> {
    return this.apiService.search(page, category, query ?? '', size).pipe(catchError(_ => of({ data: [], metadata: undefined })));
  }

  private updatePaginator(accumulator: ApiPaginatorVM, value: ApiPaginatorVM): ApiPaginatorVM {
    if (value.page === 1) {
      return value;
    }

    accumulator.data.push(...value.data);
    accumulator.page = value.page;
    accumulator.hasNextPage = value.hasNextPage;

    return accumulator;
  }
}
