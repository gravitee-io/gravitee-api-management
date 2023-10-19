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

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiNotificationSettingsService } from '../../../../services-ngx/api-notification-settings.service';
import { NotificationSettings } from '../../../../entities/notification/notificationSettings';

@Component({
  selector: 'api-notification-settings-details',
  template: require('./api-notification-settings-details.component.html'),
})
export class ApiNotificationSettingsDetailsComponent implements OnInit, OnDestroy {
  public notificationsDetailsServices: any;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly notificationSettingsService: ApiNotificationSettingsService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  public ngOnInit() {
    this.notificationsDetailsServices = {
      reference: { referenceType: 'API', referenceId: this.ajsStateParams.applicationId },
      update: (notificationConfig: NotificationSettings) =>
        this.notificationSettingsService.update(this.ajsStateParams.apiId, this.ajsStateParams.notificationId, notificationConfig),
      getHooks: () => this.notificationSettingsService.getHooks(),
      getSingleNotificationSetting: () =>
        this.notificationSettingsService.getSingleNotificationSetting(this.ajsStateParams.apiId, this.ajsStateParams.notificationId),
      getNotifiers: () => this.notificationSettingsService.getNotifiers(this.ajsStateParams.apiId),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
