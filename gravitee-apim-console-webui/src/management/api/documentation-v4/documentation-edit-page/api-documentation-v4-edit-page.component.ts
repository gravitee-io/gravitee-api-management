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
import { StateParams } from '@uirouter/core';
import { StateService } from '@uirouter/angularjs';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import { Breadcrumb, Page } from '../../../../entities/management-api-v2/documentation/page';
import { EditDocumentationMarkdown } from '../../../../entities/management-api-v2/documentation/editDocumentation';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-documentation-edit-page',
  template: require('./api-documentation-v4-edit-page.component.html'),
  styles: [require('./api-documentation-v4-edit-page.component.scss')],
})
export class ApiDocumentationV4EditPageComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  page: Page;
  content: string;
  breadcrumbs: Breadcrumb[];

  constructor(
    private readonly matDialog: MatDialog,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiDocumentationService
      .getApiPage(this.ajsStateParams.apiId, this.ajsStateParams.pageId)
      .pipe(
        tap((page) => {
          this.page = page;
          this.content = page.content;
        }),
        // get all pages from parent folder to build the breadcrumb. Will also be used to check page name is not a duplicate.
        switchMap((page) => this.apiDocumentationService.getApiPages(this.ajsStateParams.apiId, page.parentId ?? 'ROOT')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((res) => {
        this.breadcrumbs = res.breadcrumb;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    this.savePage().subscribe(() => {
      this.ajsState.go('management.apis.documentationV4', { ...this.ajsStateParams, parentId: this.page.parentId });
    });
  }

  saveAndPublish() {
    this.savePage()
      .pipe(switchMap(() => this.apiDocumentationService.publishDocumentationPage(this.ajsStateParams.apiId, this.ajsStateParams.pageId)))
      .subscribe({
        next: () => {
          this.ajsState.go('management.apis.documentationV4', { ...this.ajsStateParams, parentId: this.page.parentId });
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot save page');
        },
      });
  }

  private savePage() {
    return this.apiDocumentationService.getApiPage(this.ajsStateParams.apiId, this.ajsStateParams.pageId).pipe(
      switchMap((page) => {
        const editPage: EditDocumentationMarkdown = {
          ...page,
          type: 'MARKDOWN',
          content: this.content,
        };

        return this.apiDocumentationService.updateDocumentationPage(this.ajsStateParams.apiId, this.ajsStateParams.pageId, editPage);
      }),
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot save page');
        return EMPTY;
      }),
    );
  }
  exit() {
    // When no changes, exit without confirmation
    if (this.content === this.page.content) {
      this.exitWithoutSaving();
    } else {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
          width: GIO_DIALOG_WIDTH.SMALL,
          data: {
            title: 'Are you sure?',
            content: 'If you leave this page, you will lose your changes.',
            confirmButton: 'Discard changes',
            cancelButton: 'Keep creating',
          },
          role: 'alertdialog',
          id: 'exitWithoutSaving',
        })
        .afterClosed()
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe((shouldExit) => {
          if (shouldExit) this.exitWithoutSaving();
        });
    }
  }

  exitWithoutSaving() {
    this.ajsState.go('management.apis.documentationV4', { ...this.ajsStateParams, parentId: this.page.parentId });
  }
}
