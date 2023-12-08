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

import { Component, Input, OnInit } from '@angular/core';
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

import {
  NotificationSettingsAddDialogComponent,
  NotificationSettingsAddDialogData,
  NotificationSettingsAddDialogResult,
} from './notifications-settings-add-dialog/notification-settings-add-dialog.component';

import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { NotificationSettings } from '../../entities/notification/notificationSettings';
import { NewNotificationSettings } from '../../entities/notification/newNotificationSettings';
import { Notifier } from '../../entities/notification/notifier';

export interface NotificationSettingsListServices {
  reference: {
    referenceType: 'API' | 'APPLICATION' | 'PORTAL';
    referenceId: string;
  };
  getList: () => Observable<NotificationSettings[]>;
  getNotifiers: () => Observable<Notifier[]>;
  create: (notificationSettings: NewNotificationSettings) => Observable<NewNotificationSettings>;
  delete: (notificationId: string) => Observable<NotificationSettings[]>;
}

export interface NotificationSettingsTable {
  name: string;
  id: string;
  configType: string;
  notifierName?: string;
}

@Component({
  selector: 'notification-settings-list',
  templateUrl: './notification-settings-list.component.html',
  styleUrls: ['./notification-settings-list.component.scss'],
})
export class NotificationSettingsListComponent implements OnInit {
  public displayedColumns = ['name', 'notifier', 'actions'];
  public notificationUnpaginatedLength = 0;
  public filteredNotificationsSettingsTable = [];
  public notificationSettingsListTable: NotificationSettingsTable[] = [];
  public notifiersGroup: Notifier[];
  public notificationsList: NotificationSettings[];
  public isLoadingData = true;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public initialFilters: GioTableWrapperFilters = {
    pagination: {
      size: 10,
      index: 1,
    },
    searchTerm: '',
  };

  @Input() notificationSettingsListServices: NotificationSettingsListServices;

  constructor(private readonly matDialog: MatDialog, private readonly snackBarService: SnackBarService) {}

  public ngOnInit() {
    this.isLoadingData = true;
    combineLatest([this.notificationSettingsListServices.getList(), this.notificationSettingsListServices.getNotifiers()])
      .pipe(
        tap(([notificationsList, notifiers]) => {
          this.notifiersGroup = notifiers;
          this.notificationsList = notificationsList;
          this.filteredNotificationsSettingsTable = [];

          this.notificationSettingsListTable = this.notificationsList.map((notificationSettings) => {
            return {
              id: notificationSettings.id,
              configType: notificationSettings.config_type,
              name: notificationSettings.name,
              notifier: notificationSettings.notifier || 'none',
              notifierName: this.setNotifierName(notificationSettings),
            };
          });
          this.onPropertiesFiltersChanged(this.initialFilters);
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
        switchMap(() => this.notificationSettingsListServices.delete(id)),
        tap(() => {
          this.snackBarService.success(`“${name}” has been deleted”`);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  addNotification() {
    this.matDialog
      .open<NotificationSettingsAddDialogComponent, NotificationSettingsAddDialogData, NotificationSettingsAddDialogResult>(
        NotificationSettingsAddDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: {
            notifier: this.notifiersGroup,
            reference: this.notificationSettingsListServices.reference,
          },
          role: 'dialog',
          id: 'addNotificationDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((newNotificationSettings) => this.notificationSettingsListServices.create(newNotificationSettings)),
        tap(() => {
          this.snackBarService.success('Notification created successfully');
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  setNotifierName(element) {
    if (element.id) {
      return this.notifiersGroup?.find((i) => i.id === element.notifier)?.name ?? element.notifier;
    }
  }

  onPropertiesFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.notificationSettingsListTable, filters);
    this.filteredNotificationsSettingsTable = filtered.filteredCollection;
    this.notificationUnpaginatedLength = filtered.unpaginatedLength;
  }
}
