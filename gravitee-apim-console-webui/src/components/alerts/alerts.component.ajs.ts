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
import { Router } from '@angular/router';

import { Alert, Scope } from '../../entities/alert';
import AlertService from '../../services/alert.service';
import NotificationService from '../../services/notification.service';
import UserService from '../../services/user.service';

const AlertsComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
    alerts: '<',
    reload: '&',
  },
  template: require('html-loader!./alerts.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: [
    'ngRouter',
    'AlertService',
    'NotificationService',
    'UserService',
    '$mdDialog',
    function (ngRouter: Router, AlertService: AlertService, NotificationService: NotificationService, UserService: UserService, $mdDialog) {
      this.alerts = this.alerts ?? [];
      this.goTo = (urlSegment: string) => {
        ngRouter.navigate(['./', urlSegment], { relativeTo: this.activatedRoute });
      };

      this.delete = (alert: Alert) => {
        this.enhanceAlert(alert);
        $mdDialog
          .show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('html-loader!../../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
            clickOutsideToClose: true,
            locals: {
              title: `Are you sure you want to delete the alert '${alert.name}'?`,
              msg: '',
              confirmButton: 'Delete',
            },
          })
          .then(response => {
            if (response) {
              AlertService.delete(alert).then(() => {
                NotificationService.show("Alert '" + alert.name + "' has been deleted");
                this.reload();
              });
            }
          });
      };

      this.update = (alert: Alert) => {
        this.enhanceAlert(alert);
        AlertService.update(alert)
          .then(() => {
            NotificationService.show('Alert saved with success');
          })
          .finally(() => {
            this.reload();
          });
      };

      this.toggleEnable = (alert: Alert) => {
        alert.enabled = !alert.enabled;
        this.update(alert);
      };

      this.enhanceAlert = (alert: Alert) => {
        if (this.activatedRoute.snapshot.params.apiId) {
          alert.reference_type = Scope.API;
        } else if (this.activatedRoute.snapshot.params.applicationId) {
          alert.reference_type = Scope.APPLICATION;
        } else {
          alert.reference_type = Scope.ENVIRONMENT;
        }
      };

      this.getSeverityColor = (alert: Alert) => {
        switch (alert.severity) {
          case 'INFO':
            return '#54a3ff';
          case 'WARNING':
            return '#FF950D';
          case 'CRITICAL':
            return '#d73a49';
        }
      };

      this.hasPermissionForCurrentScope = (permission: string): boolean => {
        let scope = 'environment';
        if (this.activatedRoute.snapshot.params.apiId) {
          scope = 'api';
        } else if (this.activatedRoute.snapshot.params.applicationId) {
          scope = 'application';
        }
        return UserService.isUserHasPermissions([`${scope}-${permission}`]);
      };
    },
  ],
};

export default AlertsComponentAjs;
