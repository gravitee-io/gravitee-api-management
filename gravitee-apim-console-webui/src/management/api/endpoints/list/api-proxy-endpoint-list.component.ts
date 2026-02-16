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
import { Component, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { remove, find } from 'lodash';
import { ActivatedRoute } from '@angular/router';

import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { EndpointGroup, toEndpoints } from '../endpoint.adapter';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2, EndpointGroupV2, EndpointV2, UpdateApi } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-proxy-endpoint-list',
  templateUrl: './api-proxy-endpoint-list.component.html',
  styleUrls: ['./api-proxy-endpoint-list.component.scss'],
  standalone: false,
})
export class ApiProxyEndpointListComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public isReadOnly = false;
  public endpointGroupsTableData: EndpointGroup[];
  public endpointTableDisplayedColumns = ['name', 'healthCheck', 'target', 'type', 'weight', 'actions'];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((api: ApiV2) => {
        this.initData(api);
      });
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
        filter(confirm => confirm === true),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        switchMap((api: ApiV2) => {
          remove(api.proxy.groups, (g: EndpointGroupV2) => g.name === groupName);
          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap((api: ApiV2) => this.initData(api)),
        map(() => this.snackBarService.success(`Endpoint group ${groupName} successfully deleted!`)),
        takeUntil(this.unsubscribe$),
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
        filter(confirm => confirm === true),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        switchMap((api: ApiV2) => {
          remove(
            find(api.proxy.groups, (g: EndpointGroupV2) => g.name === groupName).endpoints,
            (e: EndpointV2) => e.name === endpointName,
          );
          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap((api: ApiV2) => this.initData(api)),
        map(() => this.snackBarService.success(`Endpoint ${endpointName} successfully deleted!`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initData(api: ApiV2): void {
    this.endpointGroupsTableData = toEndpoints(api);
    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-r']) || api.definitionContext?.origin === 'KUBERNETES';
  }
}
