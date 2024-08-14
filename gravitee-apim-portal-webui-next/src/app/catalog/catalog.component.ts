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
import { ChangeDetectionStrategy, Component, inject, Input } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { BehaviorSubject, catchError, EMPTY, map, Observable, scan, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { ApiFilterComponent } from '../../components/api-filter/api-filter.component';
import { BannerComponent } from '../../components/banner/banner.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { Category } from '../../entities/categories/categories';
import { ApiService } from '../../services/api.service';
import { CategoriesService } from '../../services/categories.service';
import { ConfigService } from '../../services/config.service';

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
    SearchBarComponent,
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogComponent {
  @Input() query!: string;
  @Input() filter: string = 'all';

  apiPaginator$: Observable<ApiPaginatorVM>;
  filterList$: Observable<Category[]> = of([]);
  loadingPage$ = new BehaviorSubject(true);

  showBanner: boolean;
  bannerTitle: string;
  bannerSubtitle: string;
  selectedCategory: Category | undefined;

  private apiService = inject(ApiService);
  private categoriesService = inject(CategoriesService);
  private page$ = new BehaviorSubject(1);
  private initialLoad: boolean = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private configService: ConfigService,
  ) {
    this.showBanner = this.configService.configuration?.portalNext?.banner?.enabled ?? false;
    this.bannerTitle = this.configService.configuration?.portalNext?.banner?.title ?? '';
    this.bannerSubtitle = this.configService.configuration?.portalNext?.banner?.subtitle ?? '';
    this.apiPaginator$ = this.loadApis$();
    this.filterList$ = this.loadCategories$();
  }

  loadMoreApis(paginator: ApiPaginatorVM) {
    if (!paginator.hasNextPage) {
      return;
    }

    this.page$.next(paginator.page + 1);
  }

  public onFilterSelection(event: string, categories: Category[]) {
    this.filter = event;
    this.selectedCategory = this.filter === 'all' ? undefined : categories.find(cat => cat.id === event);
    this.router.navigate([''], {
      relativeTo: this.route,
      queryParams: {
        filter: this.filter === 'all' ? '' : this.filter,
        query: this.query,
      },
    });
    this.page$.next(1);
  }

  public onSearchResults(searchInput: string) {
    this.query = searchInput;
    this.router.navigate([''], {
      relativeTo: this.route,
      queryParams: {
        filter: this.filter,
        query: this.query,
      },
    });
    this.page$.next(1);
  }

  private loadApis$(): Observable<ApiPaginatorVM> {
    return this.page$.pipe(
      tap(_ => this.loadingPage$.next(true)),
      switchMap(currentPage => {
        if (this.initialLoad) {
          this.initialLoad = false;
          return of({ page: currentPage, size: 18 });
        } else if (currentPage === 2) {
          this.page$.next(3);
          return EMPTY;
        } else {
          return of({ page: currentPage, size: 9 });
        }
      }),
      switchMap(({ page, size }) => this.apiService.search(page, this.filter, this.query ?? '', size)),
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
      map(response => response.data ?? []),
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
