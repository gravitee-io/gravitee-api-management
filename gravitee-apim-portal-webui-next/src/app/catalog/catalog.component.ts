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

import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { BehaviorSubject, catchError, map, Observable, scan, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { ApiFilterComponent } from '../../components/api-filter/api-filter.component';
import { BannerComponent } from '../../components/banner/banner.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Category } from '../../entities/categories/categories';
import { ApiService } from '../../services/api.service';
import { CategoriesService } from '../../services/categories.service';

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
  selector: 'app-catalog',
  standalone: true,
  imports: [
    BannerComponent,
    MatCard,
    MatCardContent,
    ApiCardComponent,
    AsyncPipe,
    InfiniteScrollModule,
    LoaderComponent,
    ApiFilterComponent,
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent {
  apiPaginator$: Observable<ApiPaginatorVM>;
  filterList$: Observable<Category[]> = of([]);
  loadingPage$ = new BehaviorSubject(true);

  // TODO: Get banner title + subtitle from configuration
  bannerTitle: string = 'Welcome to Gravitee Developer Portal!';
  bannerSubtitle: string = 'Discover powerful APIs to supercharge your projects.';
  selectedFilter: string = 'all';

  private apiService = inject(ApiService);
  private categoriesService = inject(CategoriesService);
  private page$ = new BehaviorSubject(1);

  constructor() {
    this.apiPaginator$ = this.loadApis$();
    this.filterList$ = this.loadCategories$();
  }

  loadMoreApis(paginator: ApiPaginatorVM) {
    if (!paginator.hasNextPage) {
      return;
    }

    this.page$.next(paginator.page + 1);
  }

  public onFilterSelection(event: string) {
    this.selectedFilter = event;
    this.page$.next(1);
  }

  private loadApis$(): Observable<ApiPaginatorVM> {
    return this.page$.pipe(
      tap(_ => this.loadingPage$.next(true)),
      switchMap(currentPage => this.apiService.list(currentPage, this.selectedFilter)),
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
      tap(_ => this.loadingPage$.next(false)),
    );
  }

  private loadCategories$(): Observable<Category[]> {
    return this.categoriesService.categories().pipe(
      map(response => {
        if (response) {
          return response.data.sort((a, b) => a.name!.localeCompare(b.name!));
        } else {
          return [];
        }
      }),
      catchError(_ => of([])),
    );
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
