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
import { EMPTY, Subject } from 'rxjs';
import { StateService } from '@uirouter/angular';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import * as _ from 'lodash';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../services-ngx/api.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { EndpointGroup, toEndpoints } from '../endpoint.adapter';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Api } from '../../../../../entities/api';

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
  public endpointTableDisplayedColumns = ['name', 'healthCheck', 'target', 'type', 'weight', 'actions'];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.initData(api);
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

  deleteGroup(groupName: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete Endpoint Group',
          content: `Are you sure you want to delete the Group <strong>${groupName}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEndpointGroupConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.apiId)),
        switchMap((api) => {
          _.remove(api.proxy.groups, (g) => g.name === groupName);
          return this.apiService.update(api);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap((api) => this.initData(api)),
        map(() => this.snackBarService.success(`Endpoint group ${groupName} successfully deleted!`)),
      )
      .subscribe();
  }

  deleteEndpoint(groupName: string, endpointName: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete Endpoint',
          content: `Are you sure you want to delete the Endpoint <strong>${endpointName}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEndpointConfirmDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.apiId)),
        switchMap((api) => {
          _.remove(_.find(api.proxy.groups, (g) => g.name === groupName).endpoints, (e) => e.name === endpointName);
          return this.apiService.update(api);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap((api) => this.initData(api)),
        map(() => this.snackBarService.success(`Endpoint ${endpointName} successfully deleted!`)),
      )
      .subscribe();
  }

  private initData(api: Api): void {
    this.apiId = api.id;
    this.endpointGroupsTableData = toEndpoints(api);
    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-r']) || api.definition_context?.origin === 'kubernetes';
  }
}
