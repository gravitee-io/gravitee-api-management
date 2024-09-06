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
import { Observable, Subject, of, combineLatest, EMPTY, BehaviorSubject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { catchError, filter, map, switchMap, takeUntil } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiDocumentationPageResult, ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, Page, PageType } from '../../../../entities/management-api-v2';
@Component({
  selector: 'api-documentation-v4-main-pages-tab',
  templateUrl: './api-documentation-v4-main-pages-tab.component.html',
  styleUrls: ['./api-documentation-v4-main-pages-tab.component.scss'],
})
export class ApiDocumentationV4MainPagesTabComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  api: Api;
  parentId: string;
  pages: Page[];
  homepage: Page;
  isReadOnly = false;
  data$: Observable<{ homepage?: Page; hasCustomPages: boolean }> = of();

  private refreshPages = new BehaviorSubject(1);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly matDialog: MatDialog,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.data$ = this.activatedRoute.params.pipe(
      switchMap(({ apiId }) => (apiId ? combineLatest([this.apiV2Service.get(apiId), this.getApiPages(apiId)]) : EMPTY)),
      map(([api, pagesResponse]) => {
        this.api = api;
        this.isReadOnly = api.originContext?.origin === 'KUBERNETES';
        return {
          hasCustomPages: !!pagesResponse.pages.filter((p) => !p.homepage && p.type !== 'FOLDER').length,
          homepage: pagesResponse.pages.find((p) => p.homepage === true),
        };
      }),
      catchError(() => of({ hasCustomPages: false })),
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  addPage(pageType: PageType) {
    this.router.navigate(['.', 'homepage', 'new'], {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.parentId, pageType, homepage: true },
    });
  }

  editPage(pageId: string) {
    this.router.navigate(['homepage', pageId], {
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
          this.refreshPages.next(1);
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
          this.refreshPages.next(1);
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
          title: 'Delete Homepage',
          content: 'Are you sure you want to delete this page? This action is irreversible. Do you want to continue?',
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
          this.snackBarService.success('Homepage deleted successfully');
          this.refreshPages.next(1);
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error when deleting homepage');
        },
      });
  }

  chooseHomepage() {
    this.router.navigate(['.', 'homepage', 'choose'], {
      relativeTo: this.activatedRoute,
    });
  }

  private getApiPages(apiId: string): Observable<ApiDocumentationPageResult> {
    return this.refreshPages.pipe(switchMap((_) => this.apiDocumentationV2Service.getApiPages(apiId)));
  }
}
