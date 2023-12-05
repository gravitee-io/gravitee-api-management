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
import { ActivatedRoute } from '@angular/router';

import { ApplicationNotificationSettingsService } from '../../../../../../services-ngx/application-notification-settings.service';
import { NotificationSettingsListServices } from '../../../../../../components/notification-settings/notification-settings-list.component';

@Component({
  selector: 'application-notification-settings-list',
  template: require('./application-notification-settings-list.component.html'),
})
export class ApplicationNotificationSettingsListComponent implements OnInit {
  public notificationSettingsListServices: NotificationSettingsListServices;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationNotificationSettingsService: ApplicationNotificationSettingsService,
  ) {}

  public ngOnInit() {
    this.notificationSettingsListServices = {
      reference: { referenceType: 'APPLICATION', referenceId: this.activatedRoute.snapshot.params.applicationId },
      getList: () => this.applicationNotificationSettingsService.getAll(this.activatedRoute.snapshot.params.applicationId),
      getNotifiers: () => this.applicationNotificationSettingsService.getNotifiers(this.activatedRoute.snapshot.params.applicationId),
      create: (notificationSettings) =>
        this.applicationNotificationSettingsService.create(this.activatedRoute.snapshot.params.applicationId, notificationSettings),
      delete: (notificationId) =>
        this.applicationNotificationSettingsService.delete(this.activatedRoute.snapshot.params.applicationId, notificationId),
    };
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
