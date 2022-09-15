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
import { StateService } from '@uirouter/core';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../services-ngx/api.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ResponseTemplate, toResponseTemplates } from '../response-templates.adapter';

@Component({
  selector: 'api-proxy-response-templates-list',
  template: require('./api-proxy-response-templates-list.component.html'),
  styles: [require('./api-proxy-response-templates-list.component.scss')],
})
export class ApiProxyResponseTemplatesListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public responseTemplateTableDisplayedColumns = ['key', 'contentType', 'statusCode', 'body', 'actions'];
  public responseTemplateTableData: ResponseTemplate[] = [];
  public isReadOnly = false;
  public apiId: string;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.apiId = api.id;
          this.responseTemplateTableData = toResponseTemplates(api.response_templates);

          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-response_templates-u']);
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onAddResponseTemplateClicked() {
    this.ajsState.go('management.apis.detail.proxy.ng-responsetemplate-new', { apiId: this.apiId });
  }

  onEditResponseTemplateClicked(element: ResponseTemplate) {
    this.ajsState.go('management.apis.detail.proxy.ng-responsetemplate-edit', { apiId: this.apiId, responseTemplateId: element.id });
  }

  onDeleteResponseTemplateClicked() {
    // TODO in next commit
  }
}
