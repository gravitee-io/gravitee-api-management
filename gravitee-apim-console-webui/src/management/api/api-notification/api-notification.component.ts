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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import {
  NotificationAddDialogComponent,
  NotificationAddDialogData,
  NotificationAddDialogResult,
  NotificationEditDialogComponent,
  NotificationEditDialogData,
  NotificationEditDialogResult,
  NotificationSummary,
} from '../../../components/notification';
import { ApiNotificationSettingsService } from '../../../services-ngx/api-notification-settings.service';
import { Notifier } from '../../../entities/notification/notifier';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { NotificationSettings } from '../../../entities/notification/notificationSettings';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Hooks } from '../../../entities/notification/hooks';

@Component({
  selector: 'api-notification',
  templateUrl: './api-notification.component.html',
  styleUrls: ['./api-notification.component.scss'],
  standalone: false,
})
export class ApiNotificationComponent implements OnInit, OnDestroy {
  private apiId = this.activatedRoute.snapshot.params.apiId;
  private notifications$: BehaviorSubject<NotificationSettings[]> = new BehaviorSubject(null);
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private hooks$ = this.notificationService.getHooks();

  protected loading = true;
  protected canAdd = this.permissionService.hasAnyMatching(['api-notification-c']);
  protected canDelete = this.permissionService.hasAnyMatching(['api-notification-d']);
  protected canUpdate = this.permissionService.hasAnyMatching(['api-notification-u']);
  protected notifiers: Notifier[];
  protected notificationsSummary$: Observable<NotificationSummary[]> = this.notifications$.asObservable().pipe(
    map((notifications) => {
      return notifications?.map((notification) => {
        return {
          id: notification.id ?? notification.config_type,
          name: notification.name,
          subscribedEvents: notification.hooks.length ?? 0,
          notifier: this.notifiers.find((n) => n.id === notification.notifier),
          isPortalNotification: notification.config_type === 'PORTAL',
        };
      });
    }),
    map((notifications) => {
      if (notifications != null) {
        this.loading = false;
        return notifications;
      }
      return [];
    }),
  );

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly notificationService: ApiNotificationSettingsService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  public ngOnInit(): void {
    combineLatest([this.notificationService.getNotifiers(this.apiId), this.notificationService.getAll(this.apiId)])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([notifiers, notifications]) => {
        this.notifiers = notifiers;
        this.notifications$.next(notifications);
      });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
    this.notifications$.unsubscribe();
  }

  protected addNotification() {
    this.matDialog
      .open<NotificationAddDialogComponent, NotificationAddDialogData, NotificationAddDialogResult>(NotificationAddDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          notifiers: this.notifiers,
        },
        role: 'dialog',
        id: 'addNotificationDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((newNotification) =>
          this.notificationService.create(this.apiId, {
            name: newNotification.name,
            notifier: newNotification.notifierId,
            referenceType: 'API',
            referenceId: this.apiId,
            config_type: 'GENERIC',
            hooks: [],
          }),
        ),
        tap(() => {
          this.snackBarService.success('Notification created successfully');
          this.refreshList();
        }),
        switchMap((created) => {
          return this.notificationService.getHooks().pipe(
            switchMap((hooks) => {
              return this.openEditDialog(hooks, created);
            }),
          );
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  protected deleteNotification(notification: NotificationSummary) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete notification',
          content: `Are you sure you want to delete the notification <strong>${notification.name}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteNotificationConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.notificationService.delete(this.apiId, notification.id)),
        tap(() => {
          this.snackBarService.success(`"${notification.name}" has been deleted`);
          this.refreshList();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  protected editNotification(id: string) {
    combineLatest([this.hooks$, this.notificationService.getSingleNotificationSetting(this.apiId, id)])
      .pipe(
        switchMap(([hooks, notification]) => {
          return this.openEditDialog(hooks, notification);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )

      .subscribe();
  }

  private openEditDialog(hooks: Hooks[], notification: NotificationSettings) {
    return this.matDialog
      .open<NotificationEditDialogComponent, NotificationEditDialogData, NotificationEditDialogResult>(NotificationEditDialogComponent, {
        width: GIO_DIALOG_WIDTH.LARGE,
        data: {
          hooks,
          notifier: this.notifiers.find((n) => n.id === notification.notifier),
          notification,
        },
        role: 'dialog',
        id: 'editNotificationDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((updated) => this.notificationService.update(this.apiId, notification.id ?? '', updated)),
        tap(() => {
          this.snackBarService.success('Notification saved successfully');
          this.refreshList();
        }),
      );
  }

  private refreshList() {
    this.loading = true;
    this.notificationService
      .getAll(this.apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((notifications) => {
        this.notifications$.next(notifications);
      });
  }
}
