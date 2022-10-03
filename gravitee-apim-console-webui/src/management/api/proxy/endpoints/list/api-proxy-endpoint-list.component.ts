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
import { Component, Inject, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { StateService } from '@uirouter/angular';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../services-ngx/api.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { EndpointGroup, toEndpoints } from '../endpoint.adapter';

@Component({
  selector: 'api-proxy-endpoint-list',
  template: require('./api-proxy-endpoint-list.component.html'),
  styles: [require('./api-proxy-endpoint-list.component.scss')],
})
export class ApiProxyEndpointListComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public isReadOnly = false;
  public apiId: string;
  public endpointGroupsTableData: EndpointGroup[];
  public endpointTableDisplayedColumns = ['name', 'target', 'type', 'weight', 'actions'];

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
          this.endpointGroupsTableData = toEndpoints(api.proxy.groups);

          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-r']) || api.definition_context?.origin === 'kubernetes';
        }),
      )
      .subscribe();
  }

  navigateToGroup(groupName: string): void {
    this.ajsState.go('management.apis.detail.proxy.group', { groupName });
  }

  navigateToEndpoint(groupName: string, endpointName: string): void {
    this.ajsState.go('management.apis.detail.proxy.endpoint', { endpointName, groupName });
  }
}
