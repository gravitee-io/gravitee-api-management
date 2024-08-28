/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, OnInit } from '@angular/core';
import { MatCard } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { combineLatest, EMPTY, Observable, of, switchMap } from 'rxjs';
import { AsyncPipe } from '@angular/common';
import { map } from 'rxjs/operators';

import { Api, Page, PageType } from '../../../../entities/management-api-v2';
import { DocumentationEditPageComponent } from '../components/documentation-edit-page/documentation-edit-page.component';
import { ApiDocumentationV4Module } from '../api-documentation-v4.module';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiDocumentationV4PageTitleComponent } from '../components/api-documentation-v4-page-title/api-documentation-v4-page-title.component';
import { DocumentationNewPageComponent } from '../components/documentation-new-page/documentation-new-page.component';
import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';

@Component({
  selector: 'documentation-edit-custom-page',
  standalone: true,
  templateUrl: './documentation-edit-custom-page.component.html',
  imports: [
    DocumentationEditPageComponent,
    MatCard,
    AsyncPipe,
    ApiDocumentationV4Module,
    ApiDocumentationV4PageTitleComponent,
    DocumentationNewPageComponent,
  ],
  styleUrl: './documentation-edit-custom-page.component.scss',
})
export class DocumentationEditCustomPageComponent implements OnInit {
  data$: Observable<{ api: Api; edit?: { page: Page }; new?: { pageType: PageType; parentId: string } }> = of();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
  ) {}

  ngOnInit() {
    this.data$ = combineLatest([this.activatedRoute.params, this.activatedRoute.queryParams]).pipe(
      switchMap(([params, queryParams]) => {
        const { apiId, pageId } = params;
        const { pageType, parentId } = queryParams;

        if (!apiId) {
          return EMPTY;
        }
        return combineLatest([
          this.apiV2Service.get(apiId),
          pageId ? this.apiDocumentationService.getApiPage(apiId, pageId) : of(undefined),
          of(pageType ?? 'MARKDOWN'),
          of(parentId ?? 'ROOT'),
        ]);
      }),
      map(([api, page, pageType, parentId]) => ({ api, edit: { page }, new: { pageType, parentId } })),
    );
  }
}
