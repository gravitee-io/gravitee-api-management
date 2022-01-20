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
import { IScope } from 'angular';
import * as _ from 'lodash';

import { Hook } from '../../../entities/hook';
import { NotificationConfig } from '../../../entities/notificationConfig';
import { Scope } from '../../../entities/scope';
import NotificationService from '../../../services/notification.service';
import NotificationSettingsService from '../../../services/notificationSettings.service';

const NotificationSettingsComponent: ng.IComponentOptions = {
  bindings: {
    resolvedHookScope: '<',
    resolvedHooks: '<',
    resolvedNotifiers: '<',
    notificationSettings: '=',
  },
  template: require('./notificationsettings.html'),
  controller: function (
    $state: StateService,
    NotificationSettingsService: NotificationSettingsService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $timeout: ng.ITimeoutService,
    $rootScope: IScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$mdDialog = $mdDialog;

    this.$onInit = () => {
      this.hooksByCategory = _.groupBy(this.resolvedHooks, 'category');
      this.hooksCategories = _.keysIn(this.hooksByCategory);

      this.selectNotificationSetting(
        _.find(this.notificationSettings, { id: $state.params.notificationId }) || this.notificationSettings[0],
      );

      if ($state.params.apiId) {
        this.currentReferenceId = $state.params.apiId;
      } else if ($state.params.applicationId) {
        this.currentReferenceId = $state.params.applicationId;
      } else {
        this.currentReferenceId = 'DEFAULT';
      }
    };

    this.selectNotificationSetting = (n, reload) => {
      this.selectedNotificationSetting = n;
      this.hookStatus = {};
      _.forEach(this.resolvedHooks, (hook: Hook) => {
        this.hookStatus[hook.id] = this.selectedNotificationSetting.hooks.indexOf(hook.id) >= 0;
      });
      if (this.selectedNotificationSetting.notifier) {
        this.selectedNotifier = _.filter(this.resolvedNotifiers, {
          id: this.selectedNotificationSetting.notifier,
        })[0];
      } else {
        this.selectedNotifier = undefined;
      }
      $timeout(() => {
        $state.params.notificationId = this.selectedNotificationSetting.id || 'portal';
        $state.transitionTo($state.current, $state.params, { reload: reload });
      });
    };

    this.save = () => {
      const cfg = new NotificationConfig();
      cfg.id = this.selectedNotificationSetting.id;
      cfg.name = this.selectedNotificationSetting.name;
      cfg.config_type = this.selectedNotificationSetting.config_type;
      cfg.referenceType = this.selectedNotificationSetting.referenceType;
      cfg.referenceId = this.selectedNotificationSetting.referenceId;
      if (this.selectedNotifier) {
        cfg.notifier = this.selectedNotifier.id;
      }
      cfg.config = this.selectedNotificationSetting.config;
      cfg.useSystemProxy = this.selectedNotificationSetting.useSystemProxy;
      cfg.hooks = [];
      _.forEach(this.hookStatus, (k, v) => {
        if (k) {
          cfg.hooks.push(v);
        }
      });
      NotificationSettingsService.update(cfg).then((response) => {
        const idx = _.findIndex(this.notificationSettings, { id: response.data.id });
        // portal notification has no id
        if (idx < 0) {
          this.notificationSettings[0] = response.data;
        } else {
          this.notificationSettings[idx] = response.data;
        }
        NotificationService.show('Notifications saved with success');
        this.formNotification.$setPristine();
      });
    };

    this.delete = () => {
      const alert = this.$mdDialog.confirm({
        title: 'Warning',
        content: 'Are you sure you want to remove this notification?',
        ok: 'OK',
        cancel: 'Cancel',
      });
      this.$mdDialog.show(alert).then(() => {
        NotificationSettingsService.delete(
          this.resolvedHookScope,
          this.selectedNotificationSetting.referenceId,
          this.selectedNotificationSetting.id,
        ).then(() => {
          NotificationService.show('Notification deleted with success');
          this.notificationSettings = _.filter(this.notificationSettings, (n: any) => {
            return this.selectedNotificationSetting.id !== n.id;
          });
          this.selectNotificationSetting(this.notificationSettings[0], true);
        });
      });
    };

    this.addDialog = () => {
      this.$mdDialog
        .show({
          controller: 'DialogAddNotificationSettingsController',
          controllerAs: 'dialogAddNotificationSettingsController',
          template: require('./addnotificationsettings.dialog.html'),
          clickOutsideToClose: true,
          notifiers: this.resolvedNotifiers,
        })
        .then((newConfig) => {
          const cfg = new NotificationConfig();
          cfg.name = newConfig.name;
          cfg.config_type = 'GENERIC';
          cfg.referenceId = this.currentReferenceId;
          cfg.notifier = newConfig.notifierId;
          cfg.hooks = [];
          switch (this.resolvedHookScope) {
            case Scope.APPLICATION:
              cfg.referenceType = 'APPLICATION';
              break;
            case Scope.API:
              cfg.referenceType = 'API';
              break;
            case Scope.PORTAL:
              cfg.referenceType = 'PORTAL';
              break;
            default:
              break;
          }
          NotificationSettingsService.create(cfg).then((response) => {
            this.notificationSettings.push(response.data);
            this.selectNotificationSetting(response.data);
          });
        });
    };

    this.validate = () => {
      return (
        this.selectedNotificationSetting.config_type === 'PORTAL' ||
        (this.selectedNotificationSetting.config && this.selectedNotificationSetting.config !== '')
      );
    };
  },
};

export default NotificationSettingsComponent;
