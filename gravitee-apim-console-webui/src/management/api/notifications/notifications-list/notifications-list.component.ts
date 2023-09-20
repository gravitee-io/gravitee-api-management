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
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { NotificationSettingsService } from '../../../../services-ngx/notification-settings.service';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

type NotificationSettingsTable = {
  name: string;
  id: string;
  configType: string;
};

@Component({
  selector: 'notifications-list',
  template: require('./notifications-list.component.html'),
  styles: [require('./notifications-list.component.scss')],
})
export class NotificationsListComponent implements OnInit {
  public notificationsSettingsTable: NotificationSettingsTable[] = [];
  public isLoadingData = true;
  public displayedColumns = ['name', 'actions'];
  public notificationUnpaginatedLength = 0;
  public filteredNotificationsSettingsTable = [];

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly notificationSettingsService: NotificationSettingsService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    this.filteredNotificationsSettingsTable = [];
    this.notificationSettingsService
      .getNotificationSettings(this.ajsStateParams.apiId)
      .pipe(
        tap((settings) => {
          this.notificationsSettingsTable = settings.map((notificationSettings) => {
            return {
              id: notificationSettings.id,
              configType: notificationSettings.config_type,
              name: notificationSettings.name,
            };
          });
          this.filteredNotificationsSettingsTable = this.notificationsSettingsTable;
          this.notificationUnpaginatedLength = this.filteredNotificationsSettingsTable.length;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onPropertiesFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.notificationsSettingsTable, filters);
    this.filteredNotificationsSettingsTable = filtered.filteredCollection;
    this.notificationUnpaginatedLength = filtered.unpaginatedLength;
  }

  deleteNotification(name: string, id: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete notification',
          content: `Are you sure you want to delete the notification <strong>${name}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteNotificationConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.notificationSettingsService.delete(this.ajsStateParams.apiId, id)),
        tap(() => this.snackBarService.success(`“${name}” has been deleted”`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
