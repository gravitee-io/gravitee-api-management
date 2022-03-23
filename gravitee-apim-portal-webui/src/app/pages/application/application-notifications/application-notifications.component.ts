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
import '@gravitee/ui-components/wc/gv-switch';
import { ActivatedRoute } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

import { ApplicationService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-application-notifications',
  templateUrl: './application-notifications.component.html',
  styleUrls: ['./application-notifications.component.css'],
})
export class ApplicationNotificationsComponent implements OnInit {
  hooks: Array<string>;
  categories: Array<string>;
  hooksByCategory: any;

  constructor(
    private applicationService: ApplicationService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const applicationId = this.route.snapshot.params.applicationId;
    if (applicationId) {
      this.applicationService
        .getHooks()
        .toPromise()
        .then(hooks => {
          this.applicationService
            .getNotificationsByApplicationId({ applicationId })
            .toPromise()
            .then(applicationHooks => {
              this.hooks = applicationHooks;
              this.hooksByCategory = {};
              hooks.forEach(hook => {
                if (this.hooksByCategory[hook.category]) {
                  this.hooksByCategory[hook.category].push(hook);
                } else {
                  this.hooksByCategory[hook.category] = [hook];
                }
              });
              this.categories = Object.keys(this.hooksByCategory);
            });
        });
    }
  }

  onSwitch(hook, checked) {
    const applicationId = this.route.snapshot.params.applicationId;
    if (checked) {
      this.hooks.push(hook.id);
    } else {
      this.hooks.splice(this.hooks.indexOf(hook.id), 1);
    }
    this.applicationService
      .updateApplicationNotifications({ applicationId, notificationInput: { hooks: this.hooks } })
      .toPromise()
      .then(_ => {
        this.notificationService.success(i18n('application.notifications.saveSuccess'));
      });
  }
}
