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
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { MatCard } from '@angular/material/card';
import { catchError, map, Observable, of, switchMap } from 'rxjs';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PageComponent } from '../../../../components/page/page.component';
import { MarkdownDescriptionPipe } from '../../../../components/pipe/markdown-description.pipe';
import { Api } from '../../../../entities/api/api';
import { ApiInformation } from '../../../../entities/api/api-information';
import { Page } from '../../../../entities/page/page';
import { CategoriesService } from '../../../../services/categories.service';
import { PageService } from '../../../../services/page.service';
import { PortalService } from '../../../../services/portal.service';

interface HomepageData {
  result?: Page;
  error?: unknown;
}

@Component({
  selector: 'app-api-tab-details',
  standalone: true,
  imports: [MarkdownDescriptionPipe, AsyncPipe, DatePipe, PageComponent, LoaderComponent, MatCard],
  templateUrl: './api-tab-details.component.html',
  styleUrl: './api-tab-details.component.scss',
})
export class ApiTabDetailsComponent implements OnInit {
  @Input()
  api!: Api;
  @Input()
  pages!: Page[];
  homepageData$: Observable<HomepageData> = of();
  categories$: Observable<string> = of('');
  apiInformation$: Observable<ApiInformation[]> = of([]);

  constructor(
    private pageService: PageService,
    private categoriesService: CategoriesService,
    private portalService: PortalService,
  ) {}

  ngOnInit(): void {
    this.homepageData$ = this.pageService.listByApiId(this.api.id, true).pipe(
      switchMap(pageResponse => {
        if (pageResponse.data?.length) {
          return this.pageService.getByApiIdAndId(this.api.id, pageResponse.data[0].id, true).pipe(
            map(result => ({ result })),
            catchError(error => of({ error })),
          );
        } else {
          return of({});
        }
      }),
    );

    if (this.api.categories?.length) {
      this.categories$ = this.categoriesService.categories().pipe(
        map(({ data }) => {
          if (!data.length || !this.api.categories) {
            return '';
          }
          return this.api.categories
            .map(c => data.find(category => category.id === c)?.name)
            .filter(c => !!c)
            .join(', ');
        }),
      );
    }

    this.apiInformation$ = this.portalService.getApiInformations(this.api.id).pipe(catchError(_ => of([])));
  }
}
