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
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { remove } from 'lodash';
import { EMPTY, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiV4, UpdateApi } from '../../../../../entities/management-api-v2';
import { EndpointGroup, toEndpoints } from './api-endpoints-groups.adapter';

@Component({
  selector: 'api-endpoints-groups',
  template: require('./api-endpoints-groups.component.html'),
  styles: [require('./api-endpoints-groups.component.scss')],
})
export class ApiEndpointsGroupsComponent implements OnInit, OnDestroy {
  @Input() public api: ApiV4;
  public endpointsDisplayedColumns = ['name', 'options', 'weight', 'actions'];
  public groupsTableData: EndpointGroup[];
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly matDialog: MatDialog,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit() {
    this.initData(this.api);
  }

  public initData(api: ApiV4) {
    this.groupsTableData = toEndpoints(api);
  }

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  public deleteGroup(groupName: string): void {
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
        switchMap(() => this.apiService.get(this.api.id)),
        switchMap((api: ApiV4) => {
          remove(api.endpointGroups, (g) => g.name === groupName);
          return this.apiService.update(api.id, { ...api } as UpdateApi);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap((api: ApiV4) => this.initData(api)),
        map(() => this.snackBarService.success(`Endpoint group ${groupName} successfully deleted!`)),
      )
      .subscribe();
  }
}
