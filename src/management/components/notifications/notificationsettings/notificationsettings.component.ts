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
import {Hook} from '../../../../entities/hook';
import {NotificationConfig} from '../../../../entities/notificationConfig';
import NotificationSettingsService from '../../../../services/notificationSettings.service';
import NotificationService from '../../../../services/notification.service';
import {Scope} from '../../../../entities/scope';
import { StateService } from '@uirouter/core';
import {IScope} from 'angular';

const NotificationSettingsComponent: ng.IComponentOptions = {
  bindings: {
    resolvedHookScope: '<',
    resolvedHooks: '<',
    resolvedNotifiers: '<',
    notificationSettings: '<'
  },
  template: require('./notificationsettings.html'),
  controller: function(
    $state: StateService,
    NotificationSettingsService: NotificationSettingsService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $timeout: ng.ITimeoutService,
    $rootScope: IScope
  ) {
    'ngInject';
    const vm = this;
    this.$rootScope = $rootScope;
    vm.$mdDialog = $mdDialog;

    vm.$onInit = () => {
      vm.hooksByCategory = _.groupBy(vm.resolvedHooks, 'category');
      vm.hooksCategories = _.keysIn(vm.hooksByCategory);

      vm.selectNotificationSetting(_.find(vm.notificationSettings, {id: $state.params.notificationId})
        || vm.notificationSettings[0]);

      if ($state.params.apiId) {
        vm.currentReferenceId = $state.params.apiId;
      } else if ($state.params.applicationId) {
        vm.currentReferenceId = $state.params.applicationId;
      } else {
        vm.currentReferenceId = 'DEFAULT';
      }
    };

    vm.selectNotificationSetting = (n) => {
      vm.selectedNotificationSetting = n;
      vm.hookStatus = {};
      _.forEach(vm.resolvedHooks,  (hook: Hook) => {
        vm.hookStatus[hook.id] = vm.selectedNotificationSetting.hooks.indexOf(hook.id) >= 0;
      });
      if (vm.selectedNotificationSetting.notifier) {
        vm.selectedNotifier = _.filter(vm.resolvedNotifiers, {
          'id' : vm.selectedNotificationSetting.notifier
        })[0];
      } else {
        vm.selectedNotifier = undefined;
      }
      $timeout(function () {
        $state.params.notificationId = vm.selectedNotificationSetting.id;
        $state.transitionTo($state.current, $state.params);
      });
    };

    vm.save = () => {
      let cfg = new NotificationConfig();
      cfg.id = vm.selectedNotificationSetting.id;
      cfg.name = vm.selectedNotificationSetting.name;
      cfg.config_type = vm.selectedNotificationSetting.config_type;
      cfg.referenceType = vm.selectedNotificationSetting.referenceType;
      cfg.referenceId = vm.selectedNotificationSetting.referenceId;
      if (vm.selectedNotifier) {
        cfg.notifier = vm.selectedNotifier.id;
      }
      cfg.config = vm.selectedNotificationSetting.config;
      cfg.useSystemProxy = vm.selectedNotificationSetting.useSystemProxy;
      cfg.hooks = [];
      _.forEach(vm.hookStatus, (k,v)  => {
        if (k) {
          cfg.hooks.push(v);
        }
      });
      NotificationSettingsService.update(cfg).then( (response) => {
        const idx = _.findIndex(vm.notificationSettings, {'id':response.data.id});
        // portal notification has no id
        if (idx < 0) {
          vm.notificationSettings[0] = response.data;
        } else {
          vm.notificationSettings[idx] = response.data;
        }
        NotificationService.show('Notifications saved with success');
        vm.formNotification.$setPristine();
      });
    };

    vm.delete = () => {
      let alert = this.$mdDialog.confirm({
        title: 'Warning',
        content: 'Are you sure you want to remove this notification?',
        ok: 'OK',
        cancel: 'Cancel'
      });
      this.$mdDialog
        .show(alert)
        .then( () => {
          NotificationSettingsService.delete(vm.resolvedHookScope, vm.selectedNotificationSetting.referenceId, vm.selectedNotificationSetting.id)
            .then((response) => {
              NotificationService.show('Notification deleted with success');
              vm.notificationSettings = _.filter(vm.notificationSettings, (n: any) => {
                return vm.selectedNotificationSetting.id !== n.id;
              });
              vm.selectNotificationSetting(vm.notificationSettings[0]);
          })
        });
    };

    vm.addDialog = () => {
      vm.$mdDialog.show({
        controller: 'DialogAddNotificationSettingsController',
        controllerAs: 'dialogAddNotificationSettingsController',
        template: require('./addnotificationsettings.dialog.html'),
        clickOutsideToClose: true,
        notifiers: vm.resolvedNotifiers
      }).then(function (newConfig) {
        let cfg = new NotificationConfig();
        cfg.name = newConfig.name;
        cfg.config_type = 'GENERIC';
        cfg.referenceId = vm.currentReferenceId;
        cfg.notifier = newConfig.notifierId;
        cfg.hooks = [];
        switch (vm.resolvedHookScope) {
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
        NotificationSettingsService.create(cfg).then( (response) => {
          vm.notificationSettings.push(response.data);
          vm.selectNotificationSetting(response.data);
        });
      });
    };

    vm.validate = () => {
      return vm.selectedNotificationSetting.config_type === 'portal' ||
        (vm.selectedNotificationSetting.config && vm.selectedNotificationSetting.config !== '');
    };
  }
};

export default NotificationSettingsComponent;
