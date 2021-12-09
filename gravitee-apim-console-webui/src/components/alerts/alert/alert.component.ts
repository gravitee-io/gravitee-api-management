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
import { IScope } from 'angular';
import _ = require('lodash');

import { Alert, Scope } from '../../../entities/alert';
import { Rule } from '../../../entities/alerts/rule.metrics';
import AlertService from '../../../services/alert.service';
import NotificationService from '../../../services/notification.service';
import UserService from '../../../services/user.service';

const AlertComponent: ng.IComponentOptions = {
  bindings: {
    alerts: '<',
    notifiers: '<',
    status: '<',
    mode: '<',
  },
  template: require('./alert.html'),
  controller: function (
    Constants: any,
    $scope: IScope,
    AlertService: AlertService,
    NotificationService: NotificationService,
    UserService: UserService,
    $state,
    $mdDialog,
  ) {
    'ngInject';

    this.$onInit = () => {
      this.tabs = ['general', 'notifications', 'history'];
      this.severities = ['INFO', 'WARNING', 'CRITICAL'];
      const indexOfTab = this.tabs.indexOf($state.params.tab);
      this.selectedTab = indexOfTab > -1 ? indexOfTab : 0;
      this.currentTab = this.tabs[this.selectedTab];

      let referenceId;
      let referenceType;

      if ($state.params.apiId) {
        referenceType = Scope.API;
        referenceId = $state.params.apiId;
        this.groups = ['API metrics', 'Health-check'];
        this.titlePrefix = ($scope.$parent as any).$resolve.resolvedApi.data.name;
      } else if ($state.params.applicationId) {
        referenceType = Scope.APPLICATION;
        referenceId = $state.params.applicationId;
        this.groups = ['Application'];
        this.titlePrefix = ($scope.$parent as any).$resolve.resolvedApplication.data.name;
      } else {
        referenceType = Scope.PLATFORM;
        this.groups = ['Node', 'API metrics', 'Health-check'];
        this.titlePrefix = 'Platform';
      }

      this.rules = Rule.findByScope(referenceType);
      this.updateMode = $state.params.alertId !== undefined;

      if (!this.updateMode) {
        this.alert = new Alert('New alert', 'INFO', undefined, undefined, undefined, referenceType, referenceId);
        this.alerts.push(this.alert);
      } else {
        this.alert = _.find(this.alerts, { id: $state.params.alertId }) || this.alerts[0];
        this.alert.type = (this.alert.source + '@' + this.alert.type).toUpperCase();
        this.alert.reference_type = referenceType;
      }

      this.template = this.alert.template || false;
      this.apiByDefault = this.alert.event_rules && this.alert.event_rules.findIndex((rule) => rule.event === 'API_CREATE') !== -1;
      this.initialAlert = _.cloneDeep(this.alert);
    };

    this.selectTab = (idx: number) => {
      this.selectedTab = idx;
      this.currentTab = this.tabs[this.selectedTab];
      // $state.transitionTo("^.alert", {alertId: this.alert.id, tab: this.currentTab}, {notify: false});
    };

    this.$onDestroy = () => {
      if (!this.updateMode) {
        this.alerts.pop();
      }
    };

    this.save = (alert: Alert) => {
      if (this.apiByDefault) {
        alert.event_rules = [{ event: 'API_CREATE' }];
      } else {
        delete alert.event_rules;
      }

      let service;
      alert.type = alert.type.split('@')[1];
      if (this.updateMode) {
        service = AlertService.update(alert);
      } else {
        service = AlertService.create(alert);
      }
      return service.then((response) => {
        this.formAlert.$setPristine();
        NotificationService.show('Alert has been saved successfully');
        const alert = response.data;
        $state.go('^.alert', { alertId: alert.id }, { reload: true });
        return alert;
      });
    };

    this.delete = () => {
      if (this.alert.id) {
        $mdDialog
          .show(
            $mdDialog.confirm({
              title: 'Warning',
              content: 'Are you sure you want to remove this alert?',
              ok: 'OK',
              cancel: 'Cancel',
            }),
          )
          .then(() => {
            AlertService.delete(this.alert).then(() => {
              NotificationService.show('Alert deleted with success');
              this.backToAlerts();
            });
          });
      } else {
        this.backToAlerts();
      }
    };

    this.reset = () => {
      this.alert = _.cloneDeep(this.initialAlert);
      this.formAlert.$setPristine();
    };

    this.associateToApis = () => {
      AlertService.associate(this.alert, 'api').then(() => {
        $state.reload();
        NotificationService.show("Alert '" + this.alert.name + "' has been associated to all APIs");
      });
    };

    this.onRuleChange = () => {
      const rule: Rule = _.find(this.rules, (rule) => rule.source + '@' + rule.type === this.alert.type);
      this.alert.source = rule.source;
      if (this.alert.filters) {
        this.alert.filters.length = 0;
      }
      this.alert.description = rule.description;
      // Template is a feature only available at platform level
      this.template = this.alert.reference_type === 2 && (rule.category === 'API metrics' || rule.category === 'Health-check');
    };

    this.backToAlerts = () => {
      if ($state.params.apiId) {
        $state.go('management.apis.detail.alerts.list', { apiId: $state.params.apiId });
      } else if ($state.params.applicationId) {
        $state.go('management.applications.application.alerts.list', { applicationId: $state.params.applicationId });
      } else {
        $state.go('management.settings.alerts.list');
      }
    };

    this.hasPermissionForCurrentScope = (permission: string): boolean => {
      let scope = 'environment';
      if ($state.params.apiId) {
        scope = 'api';
      } else if ($state.params.applicationId) {
        scope = 'application';
      }
      return UserService.isUserHasPermissions([`${scope}-${permission}`]);
    };

    this.isReadonly = (): boolean => {
      return this.mode === 'detail' && !this.hasPermissionForCurrentScope('alert-u');
    };
  },
};

export default AlertComponent;
