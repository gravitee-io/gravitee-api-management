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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { StateService } from '@uirouter/angularjs';
import { filter, switchMap, takeUntil } from 'rxjs/operators';
import { StateParams } from '@uirouter/core';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';

import { ApiDocumentationV4AddFolderDialog } from './documentation-add-folder-dialog/api-documentation-v4-add-folder-dialog.component';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';
import { CreateDocumentationFolder } from '../../../entities/management-api-v2/documentation/createDocumentation';
import { Breadcrumb, Page } from '../../../entities/management-api-v2/documentation/page';

@Component({
  selector: 'api-documentation-v4',
  template: require('./api-documentation-v4.component.html'),
  styles: [require('./api-documentation-v4.component.scss')],
})
export class ApiDocumentationV4Component implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  parentId: string;
  pages: Page[];
  breadcrumbs: Breadcrumb[];

  constructor(
    private readonly matDialog: MatDialog,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
  ) {}

  ngOnInit() {
    this.parentId = this.ajsStateParams.parentId || 'ROOT';
    this.apiDocumentationV2Service
      .getApiPages(this.ajsStateParams.apiId, this.parentId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((res) => {
        this.pages = res.pages;
        this.breadcrumbs = res.breadcrumb;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  addFolder() {
    this.matDialog
      .open(ApiDocumentationV4AddFolderDialog, { width: GIO_DIALOG_WIDTH.MEDIUM })
      .afterClosed()
      .pipe(
        filter((createFolder) => !!createFolder),
        switchMap((createFolder: CreateDocumentationFolder) =>
          this.apiDocumentationV2Service.createDocumentationPage(this.ajsStateParams.apiId, {
            type: 'FOLDER',
            name: createFolder.name,
            visibility: createFolder.visibility,
            parentId: this.parentId,
          }),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.ngOnInit();
      });
  }

  addPage() {
    this.ajsState.go('management.apis.documentationV4-create', this.ajsStateParams);
  }

  navigateTo(folderId: string | null) {
    this.ajsState.go('management.apis.documentationV4', { parentId: folderId || 'ROOT' }, { reload: true });
  }
}
