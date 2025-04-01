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

import { Component, ElementRef, Inject, Injector, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { ActivatedRoute } from '@angular/router';

import { DocumentationService } from '../../services/documentation.service';
import FetcherService from '../../services/fetcher.service';
import CategoryService from '../../services/category.service';
import { GroupService } from '../../services-ngx/group.service';
import { ApiService } from '../../services/api.service';

@Component({
  template: '',
  selector: 'documentation-edit-page',
  standalone: false,
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
export class DocumentationEditPageComponent extends UpgradeComponent {
  constructor(
    elementRef: ElementRef,
    injector: Injector,
    public readonly activatedRoute: ActivatedRoute,
    public readonly groupService: GroupService,
    @Inject('ajsDocumentationService') private readonly ajsDocumentationService: DocumentationService,
    @Inject('ajsFetcherService') private readonly ajsFetcherService: FetcherService,
    @Inject('ajsCategoryService') private readonly ajsCategoryService: CategoryService,
    @Inject('ajsApiService') private readonly ajsApiService: ApiService,
  ) {
    super('documentationEditPageAjs', elementRef, injector);
  }

  override ngOnInit() {
    const apiId = this.activatedRoute.snapshot.params.apiId;
    const pageId = this.activatedRoute.snapshot.params.pageId;
    const type = this.activatedRoute.snapshot.queryParams.type;

    Promise.all([
      this.ajsFetcherService.list().then((response) => {
        return response.data;
      }),
      this.ajsDocumentationService.get(apiId, pageId).then((response) => response.data),
      this.groupService.list().toPromise(),
      this.ajsDocumentationService
        .search(
          {
            type: 'FOLDER',
          },
          apiId,
        )
        .then((response) => response.data),
      this.ajsDocumentationService
        .search(
          {
            type: 'SYSTEM_FOLDER',
          },
          apiId,
        )
        .then((response) => response.data),
      type === 'LINK' ? this.ajsDocumentationService.search({}, apiId).then((response) => response.data) : Promise.resolve(null),
      type === 'LINK' ? this.ajsCategoryService.list().then((response) => response.data) : Promise.resolve(null),
      type === 'MARKDOWN' || type === 'MARKDOWN_TEMPLATE'
        ? this.ajsDocumentationService
            .search(
              {
                homepage: false,
                published: true,
              },
              apiId,
            )
            .then((response) =>
              response.data.filter(
                (page) =>
                  page.type.toUpperCase() === 'MARKDOWN' ||
                  page.type.toUpperCase() === 'SWAGGER' ||
                  page.type.toUpperCase() === 'ASCIIDOC' ||
                  page.type.toUpperCase() === 'ASYNCAPI',
              ),
            )
        : Promise.resolve(null),
      type === 'MARKDOWN' || type === 'ASCIIDOC' || type === 'ASYNCAPI'
        ? this.ajsDocumentationService.getMedia(pageId, apiId).then((response) => response.data)
        : Promise.resolve(null),
      apiId !== undefined
        ? this.ajsApiService.get(apiId).then((res) => res.data?.definition_context?.origin === 'kubernetes')
        : Promise.resolve(null),
    ]).then(
      ([
        resolvedFetchers,
        resolvedPage,
        resolvedGroups,
        folders,
        systemFolders,
        pageResources,
        categoryResources,
        pagesToLink,
        attachedResources,
        readOnly,
      ]) => {
        // Hack to Force the binding between Angular and AngularJS
        this.ngOnChanges({
          activatedRoute: new SimpleChange(null, this.activatedRoute, true),
          resolvedFetchers: new SimpleChange(null, resolvedFetchers, true),
          resolvedPage: new SimpleChange(null, resolvedPage, true),
          resolvedGroups: new SimpleChange(null, resolvedGroups, true),
          folders: new SimpleChange(null, folders, true),
          systemFolders: new SimpleChange(null, systemFolders, true),
          pageResources: new SimpleChange(null, pageResources, true),
          categoryResources: new SimpleChange(null, categoryResources, true),
          pagesToLink: new SimpleChange(null, pagesToLink, true),
          attachedResources: new SimpleChange(null, attachedResources, true),
          readOnly: new SimpleChange(null, readOnly, true),
        });

        super.ngOnInit();
      },
    );
  }
}
