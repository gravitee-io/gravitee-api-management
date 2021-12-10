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
import NotificationService from '../../../../services/notification.service';
import NotifierService from '../../../../services/notifier.service';

const AlertNotificationComponent: ng.IComponentOptions = {
  bindings: {
    notification: '<',
    onNotificationRemove: '&',
    isReadonly: '<',
  },
  require: {
    parent: '^gvAlertNotifications',
  },
  template: require('./alert-notification.html'),
  controller: function (NotificationService: NotificationService, NotifierService: NotifierService) {
    'ngInject';

    this.notifierJsonSchemaForm = ['*'];

    this.$onInit = () => {
      if (this.notification.type) {
        this.reloadNotifierSchema();
      }
    };

    this.onNotifierChange = () => {
      this.notification.configuration = {};
      this.reloadNotifierSchema();
    };

    this.reloadNotifierSchema = () => {
      NotifierService.getSchema(this.notification.type).then(
        ({ data }) => {
          this.notifierJsonSchema = { ...data, readonly: this.isReadonly };
          return {
            schema: data,
          };
        },
        (response) => {
          if (response.status === 404) {
            this.notifierJsonSchema = {};
            return {
              schema: {},
            };
          } else {
            // todo manage errors
            NotificationService.showError('Unexpected error while loading notifier schema for ' + this.notifier.type);
          }
        },
      );
    };

    this.remove = () => {
      this.onNotificationRemove();
    };
  },
};

export default AlertNotificationComponent;
