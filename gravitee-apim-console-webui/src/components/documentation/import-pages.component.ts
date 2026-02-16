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

import { GroupService } from '../../services-ngx/group.service';
import { DocumentationService } from '../../services/documentation.service';
import FetcherService from '../../services/fetcher.service';

@Component({
  template: '',
  selector: 'documentation-import-pages',
  standalone: false,
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
export class DocumentationImportPagesComponent extends UpgradeComponent {
  constructor(
    elementRef: ElementRef,
    injector: Injector,
    public readonly activatedRoute: ActivatedRoute,
    public readonly groupService: GroupService,
    @Inject('ajsDocumentationService') private readonly ajsDocumentationService: DocumentationService,
    @Inject('ajsFetcherService') private readonly ajsFetcherService: FetcherService,
  ) {
    super('documentationImportPagesAjs', elementRef, injector);
  }

  override ngOnInit() {
    const apiId = this.activatedRoute.snapshot.params.apiId;

    Promise.all([
      this.ajsFetcherService.list().then(response => {
        return response.data;
      }),
      this.ajsDocumentationService
        .search(
          {
            type: 'ROOT',
          },
          apiId,
        )
        .then(response => (response.data && response.data.length > 0 ? response.data[0] : null)),
    ]).then(([resolvedFetchers, resolvedRootPage]) => {
      // Hack to Force the binding between Angular and AngularJS
      this.ngOnChanges({
        activatedRoute: new SimpleChange(null, this.activatedRoute, true),
        resolvedFetchers: new SimpleChange(null, resolvedFetchers, true),
        resolvedRootPage: new SimpleChange(null, resolvedRootPage, true),
      });

      super.ngOnInit();
    });
  }
}
