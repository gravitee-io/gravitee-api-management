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
import { Component, computed, effect, input, InputSignal, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { isEqual } from 'lodash';
import { InfiniteScrollDirective } from 'ngx-infinite-scroll';
import { BehaviorSubject, catchError, distinctUntilChanged, EMPTY, map, Observable, scan, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../../../components/api-card/api-card.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { SearchBarComponent } from '../../../../components/search-bar/search-bar.component';
import { ApisResponse } from '../../../../entities/api/apis-response';
import { Category } from '../../../../entities/categories/categories';
import { ApiService } from '../../../../services/api.service';

interface ApiVM {
  id: string;
  title: string;
  version: string;
  content: string;
  picture?: string;
}

interface ApiPaginatorVM {
  data: ApiVM[];
  page: number;
  hasNextPage: boolean;
}

@Component({
  selector: 'app-apis-list',
  standalone: true,
  imports: [AsyncPipe, MatCardModule, ApiCardComponent, InfiniteScrollDirective, LoaderComponent, SearchBarComponent],
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

  private readonly page$ = new BehaviorSubject<number>(1);

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

  loadMoreApis(paginator: ApiPaginatorVM) {
    if (!paginator.hasNextPage) {
      return;
    }

    this.page$.next(paginator.page + 1);
  }

  onSearchResults(searchInput: string) {
    if (searchInput !== this.query()) {
      this.searchTerm.emit(searchInput);
    }
  }

  private loadApis$(): Observable<ApiPaginatorVM> {
    return this.page$.pipe(
      map(currentPage => ({ currentPage, category: this.categoryId(), query: this.query() })),
      distinctUntilChanged((previous, current) => isEqual(previous, current)),
      tap(_ => (this.loadingPage = true)),
      switchMap(({ currentPage, category, query }) => {
        if (currentPage === 1) {
          return of({ page: currentPage, size: 18, category, query });
        } else if (currentPage === 2) {
          this.page$.next(3);
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
