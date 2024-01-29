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

import { Component, ElementRef, Inject, Injector, Input, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { ActivatedRoute } from '@angular/router';
import { isEmpty } from 'lodash';

import { DocumentationService } from '../../services/documentation.service';

@Component({
  template: '',
  selector: 'ng-documentation-management',
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
export class DocumentationManagementComponent extends UpgradeComponent {
  @Input() pages;
  @Input() folders;
  @Input() systemFolders;
  apiId: string;
  parent: string;
  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    @Inject('ajsDocumentationService') private readonly ajsDocumentationService: DocumentationService,
  ) {
    super('documentationManagementAjs', elementRef, injector);
  }

  override ngOnInit() {
    const apiId = this.activatedRoute.snapshot.params.apiId;
    const parent = this.activatedRoute.snapshot.queryParams.parent ?? '';

    Promise.all([
      this.ajsDocumentationService.search(isEmpty(parent) ? { root: true } : { parent: parent }, apiId).then((response) => {
        return response.data;
      }),
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
    ])
      .then(([pages, folders, systemFolders]) => {
        this.pages = pages;
        this.folders = folders;
        this.systemFolders = systemFolders;
        this.parent = parent;
        this.apiId = apiId;
      })
      .then(() => {
        // Hack to Force the binding between Angular and AngularJS
        this.ngOnChanges({
          parent: new SimpleChange(null, this.parent, true),
          apiId: new SimpleChange(null, this.apiId, true),
          pages: new SimpleChange(null, this.pages, true),
          folders: new SimpleChange(null, this.folders, true),
          systemFolders: new SimpleChange(null, this.systemFolders, true),
          activatedRoute: new SimpleChange(null, this.activatedRoute, true),
        });

        super.ngOnInit();
      });
  }
}
