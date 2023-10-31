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
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { EMPTY, Observable, Subject } from 'rxjs';

import { CreateDocumentationMarkdown } from '../../../../entities/management-api-v2/documentation/createDocumentation';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import { Breadcrumb, Page } from '../../../../entities/management-api-v2/documentation/page';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-documentation-new-page',
  template: require('./api-documentation-v4-new-page.component.html'),
  styles: [require('./api-documentation-v4-new-page.component.scss')],
})
export class ApiDocumentationV4NewPageComponent implements OnInit, OnDestroy {
  form: FormGroup;
  pageTitle = 'Add new page';
  source: 'FILL' | 'IMPORT' | 'EXTERNAL' = 'FILL';
  content = '';
  private unsubscribe$: Subject<void> = new Subject<void>();
  breadcrumbs: Breadcrumb[];

  constructor(
    private readonly formBuilder: FormBuilder,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiDocumentationService
      .getApiPages(this.ajsStateParams.apiId, this.ajsStateParams.parentId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((res) => {
        this.breadcrumbs = res.breadcrumb;
      });
    this.form = this.formBuilder.group({
      name: this.formBuilder.control('', [Validators.required]),
      visibility: this.formBuilder.control('PUBLIC', [Validators.required]),
    });

    this.form.controls['name'].valueChanges.subscribe((value) => (this.pageTitle = value || 'Add new page'));
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    this.savePage().subscribe(() => {
      this.ajsState.go('management.apis.documentationV4', this.ajsStateParams);
    });
  }

  saveAndPublish() {
    this.savePage()
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

  private savePage(): Observable<Page> {
    const createPage: CreateDocumentationMarkdown = {
      type: 'MARKDOWN',
      name: this.form.getRawValue().name,
      visibility: this.form.getRawValue().visibility,
      content: this.content,
      parentId: this.ajsStateParams.parentId || 'ROOT',
    };
    return this.apiDocumentationService.createDocumentationPage(this.ajsStateParams.apiId, createPage).pipe(
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot save page');
        return EMPTY;
      }),
    );
  }

  exitWithoutSaving() {
    this.ajsState.go('management.apis.documentationV4', this.ajsStateParams);
  }
}
