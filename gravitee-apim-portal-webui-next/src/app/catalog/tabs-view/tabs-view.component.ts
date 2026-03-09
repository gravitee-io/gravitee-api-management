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
import { Component, computed, input, InputSignal, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { Category } from '../../../entities/categories/categories';
import { ConfigService } from '../../../services/config.service';
import { ApisListComponent } from '../components/apis-list/apis-list.component';
import { CatalogBannerComponent } from '../components/catalog-banner/catalog-banner.component';

@Component({
  selector: 'app-tabs-view',
  standalone: true,
  imports: [AsyncPipe, MatTabsModule, MatCardModule, ApisListComponent, CatalogBannerComponent],
  templateUrl: './tabs-view.component.html',
  styleUrl: './tabs-view.component.scss',
})
export class TabsViewComponent implements OnInit {
  categories: InputSignal<Category[]> = input.required<Category[]>();
  showBanner: boolean;

  query: string = '';
  filter = signal('');
  filterAsCategory = computed(() => this.categories().find(cat => cat.id === this.filter()));
  selectedFilterIndex = computed(() => {
    const foundCategory = this.filterAsCategory();
    return foundCategory ? this.categories().indexOf(foundCategory) + 1 : 0;
  });

  filterAndQuery$: Observable<{ filter: string; query: string }> = of();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly configService: ConfigService,
  ) {
    this.showBanner = this.configService.configuration?.portalNext?.banner?.enabled ?? false;
  }

  ngOnInit() {
    this.filterAndQuery$ = this.route.queryParams.pipe(
      map(queryParams => ({
        query: queryParams['query'] ?? '',
        filter: queryParams['filter'] ?? '',
      })),
      tap(({ query, filter }) => {
        this.filter.set(filter);
        this.query = query;
      }),
    );
  }

  onFilterSelection($event: MatTabChangeEvent) {
    const categoryId = this.categories().find(cat => cat.name === $event.tab.textLabel)?.id ?? '';

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        filter: categoryId,
        query: this.query,
      },
    });
  }

  onSearchResults(searchInput: string) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        filter: this.filter(),
        query: searchInput,
      },
    });
  }
}
