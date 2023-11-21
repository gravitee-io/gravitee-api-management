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
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import {
  ApiDocumentationV4EditFolderDialog,
  ApiDocumentationV4EditFolderDialogData,
} from './dialog/documentation-edit-folder-dialog/api-documentation-v4-edit-folder-dialog.component';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';
import { CreateDocumentationFolder } from '../../../entities/management-api-v2/documentation/createDocumentation';
import { Breadcrumb, Page } from '../../../entities/management-api-v2/documentation/page';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { EditDocumentationFolder } from '../../../entities/management-api-v2/documentation/editDocumentation';

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
    private readonly snackBarService: SnackBarService,
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
      .open<ApiDocumentationV4EditFolderDialog, ApiDocumentationV4EditFolderDialogData>(ApiDocumentationV4EditFolderDialog, {
        width: GIO_DIALOG_WIDTH.LARGE,
        data: {
          mode: 'create',
          existingNames: this.pages.filter((page) => page.type === 'FOLDER').map((page) => page.name.toLowerCase().trim()),
        },
      })
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
        switchMap((createdFolder) => this.apiDocumentationV2Service.publishDocumentationPage(this.ajsStateParams.apiId, createdFolder.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Folder created successfully');
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while creating folder');
        },
      });
  }

  addPage() {
    this.ajsState.go('management.apis.documentationV4-create', this.ajsStateParams);
  }

  navigateTo(folderId: string | null) {
    this.ajsState.go('management.apis.documentationV4', { parentId: folderId || 'ROOT' }, { reload: true });
  }

  editPage(pageId: string) {
    this.ajsState.go('management.apis.documentationV4-edit', { pageId }, { reload: true });
  }

  editFolder(folder: Page) {
    this.matDialog
      .open<ApiDocumentationV4EditFolderDialog, ApiDocumentationV4EditFolderDialogData>(ApiDocumentationV4EditFolderDialog, {
        width: GIO_DIALOG_WIDTH.LARGE,
        data: {
          mode: 'edit',
          name: folder.name,
          visibility: folder.visibility,
          existingNames: this.pages
            .filter((page) => page.type === 'FOLDER' && page.id !== folder.id)
            .map((page) => page.name.toLowerCase().trim()),
        },
      })
      .afterClosed()
      .pipe(
        filter((updateFolder) => !!updateFolder),
        switchMap((updateFolder: EditDocumentationFolder) =>
          this.apiDocumentationV2Service.updateDocumentationPage(this.ajsStateParams.apiId, folder.id, {
            ...folder,
            name: updateFolder.name,
            visibility: updateFolder.visibility,
          }),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Folder updated successfully');
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while updating folder');
        },
      });
  }

  publishPage(pageId: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Publish your page',
          content: 'Your page will be published to the Portal. Are you sure?',
          confirmButton: 'Publish',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap((_) => this.apiDocumentationV2Service.publishDocumentationPage(this.ajsStateParams.apiId, pageId)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success('Page published successfully');
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while publishing page');
        },
      });
  }

  unpublishPage(pageId: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Unpublish your page',
          content: 'Your page will be unpublished from Portal. Are you sure?',
          confirmButton: 'Unpublish',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap((_) => this.apiDocumentationV2Service.unpublishDocumentationPage(this.ajsStateParams.apiId, pageId)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success('Page unpublished successfully');
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while unpublishing page');
        },
      });
  }

  deletePage(page: Page) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: `Delete your ${page?.type === 'FOLDER' ? 'folder' : 'page'}`,
          content: `Are you sure you want to delete this ${
            page?.type === 'FOLDER' ? 'folder? Only empty folders can be deleted.' : 'page?'
          } This action is irreversible.`,
          confirmButton: 'Delete',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap((_) => this.apiDocumentationV2Service.deleteDocumentationPage(this.ajsStateParams.apiId, page?.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success(`${page?.type === 'FOLDER' ? 'Folder' : 'Page'} deleted successfully`);
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while deleting page');
        },
      });
  }

  moveUp(page: Page) {
    this.changeOrder(page, page.order - 1);
  }

  moveDown(page: Page) {
    this.changeOrder(page, page.order + 1);
  }

  private changeOrder(page: Page, order: number): void {
    this.apiDocumentationV2Service
      .updateDocumentationPage(this.ajsStateParams.apiId, page.id, {
        ...page,
        order,
      })
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: () => {
          this.snackBarService.success('Order updated successfully');
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while changing order');
        },
      });
  }
}
