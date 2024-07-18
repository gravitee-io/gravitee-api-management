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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, Page, PageType } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-default-page',
  templateUrl: './api-documentation-v4-default-page.component.html',
  styleUrls: ['./api-documentation-v4-default-page.component.scss'],
})
export class ApiDocumentationV4DefaultPageComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  api: Api;
  parentId: string;
  page: Page;
  isLoading = false;
  isReadOnly = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly matDialog: MatDialog,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.isLoading = true;

    this.apiV2Service
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap((api) => {
          this.api = api;
          this.isReadOnly = api.originContext?.origin === 'KUBERNETES';
        }),
        switchMap(() => this.activatedRoute.queryParams),
        switchMap((queryParams) => {
          this.parentId = queryParams.parentId || 'ROOT';
          return this.apiDocumentationV2Service.getApiPages(this.api.id, this.parentId);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((res) => {
        this.page = res.pages[0];
        this.isLoading = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  addPage(pageType: PageType) {
    this.router.navigate(['.', 'homepage', 'new'], {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.parentId, pageType },
    });
  }

  editPage(pageId: string) {
    this.router.navigate([pageId], {
      relativeTo: this.activatedRoute,
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
        switchMap((_) => this.apiDocumentationV2Service.publishDocumentationPage(this.api.id, pageId)),
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
        switchMap((_) => this.apiDocumentationV2Service.unpublishDocumentationPage(this.api.id, pageId)),
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
          title: 'Delete your page',
          content: 'Are you sure you want to delete this page? This action is irreversible.',
          confirmButton: 'Delete',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap((_) => this.apiDocumentationV2Service.deleteDocumentationPage(this.activatedRoute.snapshot.params.apiId, page.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success('Page deleted successfully');
          this.ngOnInit();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while deleting page');
        },
      });
  }
}
