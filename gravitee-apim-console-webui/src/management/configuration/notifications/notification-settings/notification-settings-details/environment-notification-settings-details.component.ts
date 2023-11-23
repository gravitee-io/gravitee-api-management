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

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { EnvironmentNotificationSettingsService } from '../../../../../services-ngx/environment-notification-settings.service';
import { NotificationSettings } from '../../../../../entities/notification/notificationSettings';
import { NotificationSettingsDetailsServices } from '../../../../../components/notifications/notification-settings/notification-settings-details/notification-settings-details.component';

@Component({
  selector: 'environment-notification-settings-details',
  template: require('./environment-notification-settings-details.component.html'),
})
export class EnvironmentNotificationSettingsDetailsComponent implements OnInit {
  public notificationSettingsDetailsServices: NotificationSettingsDetailsServices;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly environmentNotificationSettingsService: EnvironmentNotificationSettingsService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  public ngOnInit() {
    this.notificationSettingsDetailsServices = {
      reference: { referenceType: 'PORTAL', referenceId: this.ajsStateParams.applicationId },
      update: (notificationConfig: NotificationSettings) =>
        this.environmentNotificationSettingsService.update(this.ajsStateParams.notificationId, notificationConfig),
      getHooks: () => this.environmentNotificationSettingsService.getHooks(),
      getSingleNotificationSetting: () =>
        this.environmentNotificationSettingsService.getSingleNotificationSetting(this.ajsStateParams.notificationId),
      getNotifiers: () => this.environmentNotificationSettingsService.getNotifiers(),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
