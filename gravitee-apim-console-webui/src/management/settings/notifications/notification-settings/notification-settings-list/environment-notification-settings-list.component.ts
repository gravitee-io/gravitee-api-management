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
import { Subject } from 'rxjs';

import { EnvironmentNotificationSettingsService } from '../../../../../services-ngx/environment-notification-settings.service';
import { NotificationSettingsListServices } from '../../../../../components/notification-settings/notification-settings-list.component';

@Component({
  selector: 'environment-notification-settings-list-settings',
  template: require('./environment-notification-settings-list.component.html'),
})
export class EnvironmentNotificationSettingsListComponent implements OnInit {
  public notificationSettingsListServices: NotificationSettingsListServices;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(private readonly environmentNotificationSettingsService: EnvironmentNotificationSettingsService) {}

  public ngOnInit() {
    this.notificationSettingsListServices = {
      reference: { referenceType: 'PORTAL', referenceId: 'DEFAULT' },
      getList: () => this.environmentNotificationSettingsService.getAll(),
      getNotifiers: () => this.environmentNotificationSettingsService.getNotifiers(),
      create: (notificationSettings) => this.environmentNotificationSettingsService.create(notificationSettings),
      delete: (notificationId) => this.environmentNotificationSettingsService.delete(notificationId),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
