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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';

import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApplicationNotificationSettingsService } from '../../../../../../services-ngx/application-notification-settings.service';
import { NotificationSettings } from '../../../../../../entities/notification/notificationSettings';
import { NotificationSettingsDetailsServices } from '../../../../../../components/notifications/notifications-list/notification-details/notification-settings-details.component';

@Component({
  selector: 'application-notification-settings-details',
  template: require('./application-notification-settings-details.component.html'),
  styles: [require('./application-notification-settings-details.component.scss')],
})
export class ApplicationNotificationSettingsDetailsComponent implements OnInit, OnDestroy {
  public notificationSettingsDetailsServices: NotificationSettingsDetailsServices;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly applicationNotificationSettingsService: ApplicationNotificationSettingsService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  public ngOnInit() {
    this.notificationSettingsDetailsServices = {
      reference: { referenceType: 'APPLICATION', referenceId: this.ajsStateParams.applicationId },
      getHooks: () => this.applicationNotificationSettingsService.getHooks(),
      getSingleNotificationSetting: () =>
        this.applicationNotificationSettingsService.getSingleNotificationSetting(
          this.ajsStateParams.applicationId,
          this.ajsStateParams.notificationId,
        ),
      getNotifiers: () => this.applicationNotificationSettingsService.getNotifiers(this.ajsStateParams.applicationId),
      update: (notificationConfig: NotificationSettings) =>
        this.applicationNotificationSettingsService.update(
          this.ajsStateParams.applicationId,
          this.ajsStateParams.notificationId,
          notificationConfig,
        ),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
