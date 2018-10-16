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
import _ = require('lodash');
import AlertService from "../../../../services/alert.service";
import NotificationService from "../../../../services/notification.service";
import {Alert} from "./alert";
const AlertComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    alerts: '<',
    alertMetrics: '<',
    plans: '<',
    subscriptions: '<'
  },
  template: require('./alert.html'),
  controller: function(AlertService: AlertService, NotificationService: NotificationService, $state, $mdDialog) {
    'ngInject';
    this.$onInit = () => {
      let referenceId;
      let referenceType;
      if ($state.params.apiId) {
        referenceType = 'api';
        referenceId = $state.params.apiId;
      } else if ($state.params.applicationId) {
        referenceType = 'application';
        referenceId = $state.params.applicationId;
      }
      if ($state.params.alertId === 'new') {
        const newAlert = new Alert('', '', 'request', referenceType, referenceId);
        this.alerts.push(newAlert);
        this.selectedAlert = newAlert;
      } else {
        this.selectedAlert = _.find(this.alerts, {id: $state.params.alertId}) || this.alerts[0];
      }
    };

    this.save = (alert) => {
      let service;
      if (alert.id) {
        service = AlertService.update(alert);
      } else {
        service = AlertService.create(alert);
      }
      return service.then((response) => {
        this.formAlert.$setPristine();
        NotificationService.show('Alert saved with success');
        let alert = response.data;
        $state.go('.', {alertId: alert.id}, {reload: true});
        return alert;
      });
    };

    this.delete = () => {
      if (this.selectedAlert.id) {
        $mdDialog
          .show($mdDialog.confirm({
            title: 'Warning',
            content: 'Are you sure you want to remove this alert?',
            ok: 'OK',
            cancel: 'Cancel'
          }))
          .then( () => {
            AlertService.delete(this.selectedAlert).then(() => {
              NotificationService.show('Alert deleted with success');
              $state.go('.', {}, {reload: true});
            })
          });
      } else {
        $state.go('.', {alertId: this.alerts[0].id});
      }
    };

    this.new = () => {
      $state.go('.', {alertId: 'new'});
    };

    this.getThresholdsByMetric = (metric) => {
      if (metric) {
        return (_.find(this.alertMetrics, {key: metric}) as any).thresholds;
      }
    };
  }
};

export default AlertComponent;
