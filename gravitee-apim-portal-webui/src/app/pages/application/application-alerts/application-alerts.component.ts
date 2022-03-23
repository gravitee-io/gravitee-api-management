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
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import '@gravitee/ui-components/wc/gv-autocomplete';
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-confirm';
import '@gravitee/ui-components/wc/gv-icon';
import '@gravitee/ui-components/wc/gv-identity-picture';
import '@gravitee/ui-components/wc/gv-relative-time';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-select';
import '@gravitee/ui-components/wc/gv-table';

import {
  Alert,
  AlertStatusResponse,
  Application,
  ApplicationService,
  PermissionsService,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { HttpStatus } from '../../../utils/http-helpers';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-application-alerts',
  templateUrl: './application-alerts.component.html',
  styleUrls: ['./application-alerts.component.css'],
})
export class ApplicationAlertsComponent implements OnInit {
  private permissions: Array<string>;
  constructor(
    private applicationService: ApplicationService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private permissionsService: PermissionsService,
  ) {}

  status: AlertStatusResponse;
  alerts: Array<Alert> = [];
  application: Application;

  httpStatus: Array<HttpStatus>;

  get isAlertingEnabled() {
    return this.status?.available_plugins > 0 && this.status?.enabled;
  }

  ngOnInit(): void {
    this.application = this.route.snapshot.data.application;
    if (this.application) {
      this.permissionsService
        .getCurrentUserPermissions({ applicationId: this.application.id })
        .toPromise()
        .then(permissions => {
          this.permissions = permissions.ALERT;
          if (this.hasReadPermission()) {
            this.applicationService
              .getApplicationAlertStatus({
                applicationId: this.application.id,
              })
              .toPromise()
              .then(status => {
                this.status = status;
                if (!this.isAlertingEnabled) {
                  this.notificationService.warning(i18n('application.alerts.disabled'));
                } else {
                  this.resetAlerts();
                }
              });
          }
        });
    }
  }

  hasReadPermission() {
    return this.permissions?.find(p => p === 'R');
  }

  resetAlerts() {
    if (this.application && this.isAlertingEnabled) {
      this.applicationService
        .getAlertsByApplicationId({
          applicationId: this.application.id,
        })
        .toPromise()
        .then(alerts => {
          this.alerts = alerts;
        });
    }
  }

  onAddAlert(alert: Alert) {
    this.alerts.push(alert);
    if (this.alerts.length === 10) {
      this.notificationService.info(i18n('application.alerts.maximum'));
    }
  }

  onDeleteAlert(alert: Alert) {
    this.alerts = this.alerts.filter(a => a.id !== alert.id);
  }
}
