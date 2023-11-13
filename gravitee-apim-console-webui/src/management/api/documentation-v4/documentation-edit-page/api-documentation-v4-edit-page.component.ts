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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StateParams } from '@uirouter/core';
import { StateService } from '@uirouter/angularjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { EMPTY, Observable, of, Subject } from 'rxjs';

import { CreateDocumentationMarkdown } from '../../../../entities/management-api-v2/documentation/createDocumentation';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import { Breadcrumb, Page } from '../../../../entities/management-api-v2/documentation/page';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

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
  step3Title: string;
  source: 'FILL' | 'IMPORT' | 'EXTERNAL' = 'FILL';
  breadcrumbs: Breadcrumb[];

  formUnchanged: boolean;
  page: Page;

  private loadPage$: Observable<Page>;
  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly formBuilder: FormBuilder,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.stepOneForm = this.formBuilder.group({
      name: this.formBuilder.control('', [Validators.required]),
      visibility: this.formBuilder.control('PUBLIC', [Validators.required]),
    });
    this.form = this.formBuilder.group({
      stepOne: this.stepOneForm,
      source: this.formBuilder.control(this.source, [Validators.required]),
      content: this.formBuilder.control('', [Validators.required]),
    });

    if (this.ajsStateParams.pageId) {
      this.mode = 'edit';
      this.step3Title = 'Edit content';
      this.loadPage$ = this.loadEditPage();

      this.form.valueChanges
        .pipe(
          tap((value) => {
            if (this.page) {
              this.formUnchanged =
                this.page.name === value.stepOne?.name &&
                this.page.visibility === value.stepOne?.visibility &&
                this.source === value.source &&
                this.page.content === value.content;
            }
          }),
        )
        .subscribe();
    } else {
      this.mode = 'create';
      this.step3Title = 'Add content';
      this.loadPage$ = of({});
    }

    this.loadPage$
      .pipe(
        switchMap((page) =>
          this.apiDocumentationService.getApiPages(this.ajsStateParams.apiId, this.ajsStateParams.parentId ?? page?.parentId ?? 'ROOT'),
        ),
      )
      .subscribe({
        next: (pagesResponse) => {
          this.breadcrumbs = pagesResponse.breadcrumb;
        },
      });

    this.stepOneForm.get('name').valueChanges.subscribe((value) => (this.pageTitle = value || 'Add new page'));
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  create() {
    this.createPage().subscribe(() => {
      this.ajsState.go('management.apis.documentationV4', this.ajsStateParams);
    });
  }

  createAndPublish() {
    this.createPage()
      .pipe(switchMap((page) => this.apiDocumentationService.publishDocumentationPage(this.ajsStateParams.apiId, page.id)))
      .subscribe({
        next: () => {
          this.ajsState.go('management.apis.documentationV4', this.ajsStateParams);
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot publish page');
        },
      });
  }

  update() {
    this.updatePage().subscribe({
      next: () => {
        this.ajsState.go('management.apis.documentationV4', this.ajsStateParams);
      },
    });
  }

  private updatePage(): Observable<Page> {
    const formValue = this.form.getRawValue();
    return this.apiDocumentationService
      .updateDocumentationPage(this.ajsStateParams.apiId, this.ajsStateParams.pageId, {
        name: formValue.stepOne.name,
        visibility: formValue.stepOne.visibility,
        content: formValue.content,
        type: this.page.type,
      })
      .pipe(
        catchError((err) => {
          this.snackBarService.error(err?.error?.message ?? 'Cannot update page');
          return EMPTY;
        }),
      );
  }

  private createPage(): Observable<Page> {
    const createPage: CreateDocumentationMarkdown = {
      type: 'MARKDOWN',
      name: this.form.getRawValue().stepOne.name,
      visibility: this.form.getRawValue().stepOne.visibility,
      content: this.form.getRawValue().content,
      parentId: this.ajsStateParams.parentId || 'ROOT',
    };
    return this.apiDocumentationService.createDocumentationPage(this.ajsStateParams.apiId, createPage).pipe(
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot save page');
        return EMPTY;
      }),
    );
  }

  private loadEditPage(): Observable<Page> {
    return this.apiDocumentationService.getApiPage(this.ajsStateParams.apiId, this.ajsStateParams.pageId).pipe(
      tap((page) => {
        this.pageTitle = page.name;
        this.page = page;

        this.stepOneForm.get('name').setValue(this.page.name);
        this.stepOneForm.get('visibility').setValue(this.page.visibility);

        this.form.get('content').setValue(this.page.content);
      }),
    );
  }

  exitWithoutSaving() {
    this.ajsState.go('management.apis.documentationV4', this.ajsStateParams);
  }
}
