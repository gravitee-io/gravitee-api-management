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

import { ApiNotificationSettingsService } from '../../../../services-ngx/api-notification-settings.service';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { NotificationSettingsListServices } from '../../../../components/notifications/notifications-list/notification-settings-list.component';

@Component({
  selector: 'api-notification-settings-list',
  template: require('./api-notification-settings-list.component.html'),
})
export class ApiNotificationSettingsListComponent implements OnInit {
  public notificationsServices: NotificationSettingsListServices;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly notificationSettingsService: ApiNotificationSettingsService,
  ) {}

  public ngOnInit() {
    this.notificationsServices = {
      reference: { referenceType: 'API', referenceId: this.ajsStateParams.apiId },
      getList: () => this.notificationSettingsService.getAll(this.ajsStateParams.apiId),
      getNotifiers: () => this.notificationSettingsService.getNotifiers(this.ajsStateParams.apiId),
      create: (notificationConfig) => this.notificationSettingsService.create(this.ajsStateParams.apiId, notificationConfig),
      delete: (notificationId) => this.notificationSettingsService.delete(this.ajsStateParams.applicationId, notificationId),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
