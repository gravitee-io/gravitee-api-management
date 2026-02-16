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
import { isEmpty, isEqual } from 'lodash';
import { distinctUntilChanged, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, from, Subject } from 'rxjs';

import { DocumentationService, Page } from '../../services/documentation.service';
import { ApiService } from '../../services/api.service';

@Component({
  template: '',
  selector: 'ng-documentation-management',
  standalone: false,
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
export class DocumentationManagementComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  pages: Page[];
  folders: Page[];
  systemFolders: Page[];
  apiId: string;
  parent: string;
  firstChange: boolean = true;
  readOnly = false;

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    @Inject('ajsDocumentationService') private readonly ajsDocumentationService: DocumentationService,
    @Inject('ajsApiService') private readonly ajsApiService: ApiService,
  ) {
    super('documentationManagementAjs', elementRef, injector);
  }

  override ngOnInit() {
    this.activatedRoute.params
      .pipe(
        distinctUntilChanged(isEqual),
        tap(params => {
          if (params.apiId) {
            this.ajsApiService.get(params.apiId).then(res => {
              this.readOnly = res.data?.definition_context?.origin === 'kubernetes';
            });
          }
        }),
        switchMap(params => {
          this.firstChange = true;
          this.apiId = params.apiId;

          return combineLatest([
            from(this.ajsDocumentationService.search({ type: 'FOLDER' }, this.apiId).then(response => response.data)),
            from(this.ajsDocumentationService.search({ type: 'SYSTEM_FOLDER' }, this.apiId).then(response => response.data)),
          ]);
        }),
        switchMap(([folders, systemFolders]) => {
          this.folders = folders;
          this.systemFolders = systemFolders;

          return this.activatedRoute.queryParams;
        }),
        distinctUntilChanged(isEqual),
        switchMap(queryParams => {
          this.parent = queryParams.parent;

          return from(
            this.ajsDocumentationService
              .search(isEmpty(this.parent) ? { root: true } : { parent: this.parent }, this.apiId)
              .then(response => {
                return response.data;
              }),
          );
        }),
        tap(pages => {
          this.pages = pages;

          // Hack to Force the binding between Angular and AngularJS
          this.ngOnChanges({
            parent: new SimpleChange(null, this.parent, this.firstChange),
            apiId: new SimpleChange(null, this.apiId, this.firstChange),
            pages: new SimpleChange(null, this.pages, this.firstChange),
            readOnly: new SimpleChange(null, this.readOnly, this.firstChange),
            folders: new SimpleChange(null, this.folders, this.firstChange),
            systemFolders: new SimpleChange(null, this.systemFolders, this.firstChange),
            activatedRoute: new SimpleChange(null, this.activatedRoute, this.firstChange),
          });

          if (this.firstChange) {
            super.ngOnInit();
            this.firstChange = false;
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
