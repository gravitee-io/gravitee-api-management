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
import { flatMap } from 'lodash';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../services-ngx/api.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

export type ResponseTemplatesDS = {
  key: string;
  contentType: string;
  statusCode?: number;
  body?: string;
}[];

@Component({
  selector: 'api-proxy-response-templates',
  template: require('./api-proxy-response-templates.component.html'),
  styles: [require('./api-proxy-response-templates.component.scss')],
})
export class ApiProxyResponseTemplatesComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public responseTemplateTableDisplayedColumns = ['key', 'contentType', 'statusCode', 'body', 'actions'];
  public responseTemplateTableData: ResponseTemplatesDS = [];
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
          this.responseTemplateTableData = flatMap(Object.entries(api.response_templates), ([key, responseTemplates]) => {
            return [
              ...Object.entries(responseTemplates).map(([contentType, responseTemplate]) => ({
                key: key,
                contentType,
                statusCode: responseTemplate.status,
                body: responseTemplate.body,
              })),
            ];
          });

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
    this.ajsState.go('management.apis.detail.proxy.responsetemplates.new', { apiId: this.apiId });
  }

  onEditResponseTemplateClicked(element: ResponseTemplatesDS[number]) {
    this.ajsState.go('management.apis.detail.proxy.responsetemplates.edit', { apiId: this.apiId, key: element.key });
  }

  onDeleteResponseTemplateClicked() {
    // TODO in next commit
  }
}
