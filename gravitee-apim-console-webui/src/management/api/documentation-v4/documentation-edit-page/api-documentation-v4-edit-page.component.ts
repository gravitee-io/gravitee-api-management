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
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { StateParams } from '@uirouter/core';
import { StateService } from '@uirouter/angularjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Observable, of, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { CreateDocumentationMarkdown } from '../../../../entities/management-api-v2/documentation/createDocumentation';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import { Breadcrumb, Page } from '../../../../entities/management-api-v2/documentation/page';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-edit-page',
  template: require('./api-documentation-v4-edit-page.component.html'),
  styles: [require('./api-documentation-v4-edit-page.component.scss')],
})
export class ApiDocumentationV4EditPageComponent implements OnInit, OnDestroy {
  form: FormGroup;
  stepOneForm: FormGroup;
  mode: 'create' | 'edit';
  pageTitle = 'Add new page';
  step2Title: string;
  breadcrumbs: Breadcrumb[];

  api: Api;
  formUnchanged: boolean;
  page: Page;

  private existingNames: string[] = [];
  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly formBuilder: FormBuilder,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.stepOneForm = this.formBuilder.group({
      name: this.formBuilder.control('', [Validators.required, this.pageNameUniqueValidator()]),
      visibility: this.formBuilder.control('PUBLIC', [Validators.required]),
    });
    this.form = this.formBuilder.group({
      stepOne: this.stepOneForm,
      content: this.formBuilder.control('', [Validators.required]),
    });

    if (this.ajsStateParams.pageId) {
      this.mode = 'edit';
      this.step2Title = 'Edit content';

      this.form.valueChanges
        .pipe(
          tap((value) => {
            if (this.page) {
              this.formUnchanged =
                this.page.name === value.stepOne?.name &&
                this.page.visibility === value.stepOne?.visibility &&
                this.page.content === value.content;
            }
          }),
        )
        .subscribe();
    } else {
      this.mode = 'create';
      this.step2Title = 'Add content';
    }

    this.apiV2Service
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => {
          this.api = api;
          return this.mode === 'edit' ? this.loadEditPage() : of({});
        }),
        switchMap((_) => this.apiDocumentationService.getApiPages(this.api.id, this.getParentId())),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (pagesResponse) => {
          this.breadcrumbs = pagesResponse.breadcrumb;
          this.existingNames = pagesResponse.pages
            .filter((page) => page.id !== this.page?.id && page.type === 'MARKDOWN')
            .map((page) => page.name.toLowerCase().trim());
        },
      });

    this.stepOneForm.get('name').valueChanges.subscribe((value) => (this.pageTitle = value || 'Add new page'));
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  create() {
    this.createPage()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this.goBackToPageList();
      });
  }

  createAndPublish() {
    this.createPage()
      .pipe(
        switchMap((page) => this.apiDocumentationService.publishDocumentationPage(this.api.id, page.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot publish page');
        },
      });
  }

  update() {
    this.updatePage()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: () => {
          this.goBackToPageList();
        },
      });
  }

  private updatePage(): Observable<Page> {
    const formValue = this.form.getRawValue();
    return this.apiDocumentationService.getApiPage(this.api.id, this.ajsStateParams.pageId).pipe(
      switchMap((page) =>
        this.apiDocumentationService.updateDocumentationPage(this.api.id, this.ajsStateParams.pageId, {
          ...page,
          name: formValue.stepOne.name,
          visibility: formValue.stepOne.visibility,
          content: formValue.content,
        }),
      ),
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot update page');
        return EMPTY;
      }),
    );
  }

  goBackToPageList() {
    this.ajsState.go('management.apis.documentationV4', { apiId: this.api.id, parentId: this.getParentId() });
  }

  deletePage() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: `Delete your page`,
          content: `Are you sure you want to delete this page? This action is irreversible.`,
          confirmButton: 'Delete',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap((_) => this.apiDocumentationService.deleteDocumentationPage(this.ajsStateParams.apiId, this.page?.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success('Page deleted successfully');
          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while deleting page');
        },
      });
  }

  private createPage(): Observable<Page> {
    const createPage: CreateDocumentationMarkdown = {
      type: 'MARKDOWN',
      name: this.form.getRawValue().stepOne.name,
      visibility: this.form.getRawValue().stepOne.visibility,
      content: this.form.getRawValue().content,
      parentId: this.ajsStateParams.parentId || 'ROOT',
    };
    return this.apiDocumentationService.createDocumentationPage(this.api.id, createPage).pipe(
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot save page');
        return EMPTY;
      }),
    );
  }

  private loadEditPage(): Observable<Page> {
    return this.apiDocumentationService.getApiPage(this.api.id, this.ajsStateParams.pageId).pipe(
      tap((page) => {
        this.pageTitle = page.name;
        this.page = page;

        this.stepOneForm.get('name').setValue(this.page.name);
        this.stepOneForm.get('visibility').setValue(this.page.visibility);

        this.form.get('content').setValue(this.page.content);
      }),
    );
  }

  private pageNameUniqueValidator(): ValidatorFn {
    return (nameControl: AbstractControl): ValidationErrors | null =>
      this.existingNames.includes(nameControl.value?.toLowerCase().trim()) ? { unique: true } : null;
  }

  private getParentId(): string {
    return this.ajsStateParams.parentId ?? this.page?.parentId ?? 'ROOT';
  }
}
