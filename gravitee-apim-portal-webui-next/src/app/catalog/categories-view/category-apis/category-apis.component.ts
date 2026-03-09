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
import { Component, computed, effect, input, InputSignal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';
import { BreadcrumbService } from 'xng-breadcrumb';

import { BreadcrumbNavigationComponent } from '../../../../components/breadcrumb-navigation/breadcrumb-navigation.component';
import { Category } from '../../../../entities/categories/categories';
import { ConfigService } from '../../../../services/config.service';
import { ApisListComponent } from '../../components/apis-list/apis-list.component';
import { CatalogBannerComponent } from '../../components/catalog-banner/catalog-banner.component';

@Component({
  selector: 'app-category-apis',
  standalone: true,
  imports: [BreadcrumbNavigationComponent, ApisListComponent, AsyncPipe, CatalogBannerComponent],
  templateUrl: './category-apis.component.html',
  styleUrl: './category-apis.component.scss',
})
export class CategoryApisComponent implements OnInit {
  categoryId: InputSignal<string> = input.required<string>();
  categories: InputSignal<Category[]> = input.required<Category[]>();

  currentCategory = computed(() => this.categories()?.find(cat => cat.id === this.categoryId()));
  categoryName = computed(() => {
    if (this.currentCategory()) {
      return this.currentCategory()?.name ?? '';
    }
    return 'All APIs';
  });

  query$: Observable<{ query: string }> = of();

  showBanner: boolean;

  constructor(
    private readonly breadcrumbService: BreadcrumbService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly configService: ConfigService,
  ) {
    this.showBanner = !!this.configService.configuration?.portalNext?.banner?.enabled;
    effect(() => {
      this.breadcrumbService.set('@categoryName', this.categoryName());
    });
  }

  ngOnInit() {
    this.query$ = this.activatedRoute.queryParams.pipe(map(queryParams => ({ query: queryParams['query'] ?? '' })));
  }

  onSearch(query: string) {
    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        query,
      },
    });
  }
}
