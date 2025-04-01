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
import { EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UserNotificationService } from '../../services-ngx/user-notification.service';
import { UserNotification } from '../../entities/user-notification/userNotification';
import { SnackBarService } from '../../services-ngx/snack-bar.service';

@Component({
  selector: 'gio-notification-menu',
  templateUrl: './gio-notification-menu.component.html',
  styleUrls: ['./gio-notification-menu.component.scss'],
  standalone: false,
})
export class GioNotificationMenuComponent implements OnInit, OnDestroy {
  private startNotificationFetch$ = new Subject<void>();
  private unsubscribe$ = new Subject();
  public hasNotifications = false;
  public userNotificationsCount = 0;
  public userNotifications: UserNotification[];
  public isOpen: boolean;

  constructor(
    public readonly userNotificationService: UserNotificationService,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.startNotificationFetch$
      .pipe(
        switchMap(() => this.userNotificationService.getNotificationsAutoFetch()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((notificationPagedResult) => {
        this.userNotificationsCount = notificationPagedResult.page.total_elements;
        this.hasNotifications = this.userNotificationsCount > 0;
        this.userNotifications = notificationPagedResult.data;
      });
    this.startNotificationFetch$.next();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public deleteAll() {
    this.userNotificationService
      .deleteAll()
      .pipe(
        tap(() => {
          this.snackBarService.success('Notifications successfully deleted!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.startNotificationFetch$.next());
  }

  public delete(userNotification: UserNotification) {
    this.userNotificationService
      .delete(userNotification.id)
      .pipe(
        tap(() => {
          this.snackBarService.success('Notification successfully deleted!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.startNotificationFetch$.next());
  }

  onOutsideClick() {
    this.isOpen = false;
  }
}
