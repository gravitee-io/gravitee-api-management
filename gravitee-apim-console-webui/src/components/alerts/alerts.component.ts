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
import { StateService } from '@uirouter/core';

import { Alert, Scope } from '../../entities/alert';
import AlertService from '../../services/alert.service';
import NotificationService from '../../services/notification.service';
import UserService from '../../services/user.service';

const AlertsComponent: ng.IComponentOptions = {
  bindings: {
    alerts: '<',
    api: '<',
    application: '<',
  },
  template: require('./alerts.html'),
  controller: function (
    $stateParams,
    $state: StateService,
    AlertService: AlertService,
    NotificationService: NotificationService,
    UserService: UserService,
    $mdDialog,
  ) {
    'ngInject';
    this.goTo = (suffixState: string, alertId: string) => {
      if ($stateParams.apiId) {
        $state.go('management.apis.detail.alerts.' + suffixState, { apiId: $stateParams.apiId, alertId: alertId });
      } else if ($stateParams.applicationId) {
        $state.go('management.applications.application.alerts.' + suffixState, {
          applicationId: $stateParams.applicationId,
          alertId: alertId,
        });
      } else {
        $state.go('management.settings.alerts.' + suffixState, { alertId: alertId });
      }
    };

    this.delete = (alert: Alert) => {
      this.enhanceAlert(alert);
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: `Are you sure you want to delete the alert '${alert.name}'?`,
            msg: '',
            confirmButton: 'Delete',
          },
        })
        .then((response) => {
          if (response) {
            AlertService.delete(alert).then(() => {
              NotificationService.show("Alert '" + alert.name + "' has been deleted");
              $state.go($state.current, {}, { reload: true });
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
          $state.go($state.current, {}, { reload: true });
        });
    };

    this.toggleEnable = (alert: Alert) => {
      alert.enabled = !alert.enabled;
      this.update(alert);
    };

    this.enhanceAlert = (alert: Alert) => {
      if ($stateParams.apiId) {
        alert.reference_type = Scope.API;
      } else if ($stateParams.applicationId) {
        alert.reference_type = Scope.APPLICATION;
      } else {
        alert.reference_type = Scope.PLATFORM;
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
      if ($stateParams.apiId) {
        scope = 'api';
      } else if ($stateParams.applicationId) {
        scope = 'application';
      }
      return UserService.isUserHasPermissions([`${scope}-${permission}`]);
    };
  },
};

export default AlertsComponent;
