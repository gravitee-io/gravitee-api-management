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
import { Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { ApplicationNotificationSettingsService } from '../../../../../../services-ngx/application-notification-settings.service';
import { NotificationSettings } from '../../../../../../entities/notification/notificationSettings';
import { NotificationSettingsDetailsServices } from '../../../../../../components/notification-settings/notification-settings-details/notification-settings-details.component';

@Component({
  selector: 'application-notification-settings-details',
  template: require('./application-notification-settings-details.component.html'),
  styles: [require('./application-notification-settings-details.component.scss')],
})
export class ApplicationNotificationSettingsDetailsComponent implements OnInit, OnDestroy {
  public notificationSettingsDetailsServices: NotificationSettingsDetailsServices;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationNotificationSettingsService: ApplicationNotificationSettingsService,
  ) {}

  public ngOnInit() {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;
    const notificationId = this.activatedRoute.snapshot.params.notificationId;

    this.notificationSettingsDetailsServices = {
      reference: { referenceType: 'APPLICATION', referenceId: applicationId },
      getHooks: () => this.applicationNotificationSettingsService.getHooks(),
      getSingleNotificationSetting: () =>
        this.applicationNotificationSettingsService.getSingleNotificationSetting(applicationId, notificationId),
      getNotifiers: () => this.applicationNotificationSettingsService.getNotifiers(applicationId),
      update: (notificationConfig: NotificationSettings) =>
        this.applicationNotificationSettingsService.update(applicationId, notificationId, notificationConfig),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
