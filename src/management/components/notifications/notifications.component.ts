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
import {StateService} from '@uirouter/core';

const NotificationsComponent: ng.IComponentOptions = {
  bindings: {
    notificationSettings: '<',
    api: '<',
    application: '<',
    alerts: '<'
  },
  template: require('./notifications.html'),
  controller: function ($state: StateService) {
    'ngInject';

    this.$onInit = () => {
      $state.go('.notificationSetting');
      this.alertEnabled = this.alerts && this.alerts.length;
    };

    this.isNotificationActive = (notification) => {
      return $state.current.name.endsWith('.notifications.notificationSetting') && notification.id === $state.params.notificationId;
    };

    this.isAlertActive = (alert) => {
      return $state.current.name.endsWith('.notifications.alert') && alert.id === $state.params.alertId;
    };
  }
};

export default NotificationsComponent;
