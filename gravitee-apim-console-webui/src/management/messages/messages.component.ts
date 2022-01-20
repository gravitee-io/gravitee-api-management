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
import * as _ from 'lodash';

import { ApiService } from '../../services/api.service';
import MessageService from '../../services/message.service';
import NotificationService from '../../services/notification.service';

const MessagesComponent: ng.IComponentOptions = {
  bindings: {
    resolvedScope: '<',
    resolvedRoles: '<',
    resolvedApiId: '<',
  },
  template: require('./messages.html'),
  controller: function (
    $state: StateService,
    NotificationService: NotificationService,
    MessageService: MessageService,
    ApiService: ApiService,
  ) {
    'ngInject';

    this.$onInit = () => {
      const roles = _.sortBy(this.resolvedRoles, ['name']);
      this.recipients = [];
      if (this.resolvedApiId != null) {
        this.recipients.push({ name: 'API_SUBSCRIBERS', displayName: 'API subscribers' });
      }

      roles.forEach((role) => {
        if (this.resolvedScope === 'APPLICATION') {
          role.displayName = `Members with the ${role.name} role on subscribing applications`;
        } else {
          role.displayName = `Members with the ${role.name} role on ${this.resolvedScope} scope`;
        }
        this.recipients.push(role);
      });

      this.channels = [
        { id: 'PORTAL', name: 'Portal Notifications' },
        { id: 'MAIL', name: 'Email' },
        { id: 'HTTP', name: 'POST HTTP message' },
      ];
      this.channel = 'PORTAL';
      this.defaultHttpHeaders = ApiService.defaultHttpHeaders();
      this.httpHeaders = [];
      this.recipientValues = [];
      this.newHttpHeader();
    };

    this.send = () => {
      if (this.resolvedApiId) {
        MessageService.sendFromApi(
          this.resolvedApiId,
          this.title,
          this.text,
          this.channel,
          this.resolvedScope,
          this.recipientValues,
          this.url,
          this.useSystemProxy,
          this.httpHeaders,
        ).then((response) => {
          NotificationService.show(response.data + ' messages has been sent.');
          this.resetForm();
        });
      } else {
        MessageService.sendFromPortal(
          this.title,
          this.text,
          this.channel,
          this.resolvedScope,
          this.recipientValues,
          this.url,
          this.useSystemProxy,
          this.httpHeaders,
        ).then((response) => {
          NotificationService.show(response.data + ' messages has been sent.');
          this.resetForm();
        });
      }
    };

    this.resetForm = () => {
      this.title = '';
      this.url = '';
      this.text = '';
      this.recipientValues = [];
      this.httpHeaders = [];
      this.newHttpHeader();
      this.formMsg.$setPristine();
      this.formMsg.$setUntouched();
    };

    this.newHttpHeader = () => {
      this.httpHeaders.push({
        key: '',
        value: '',
      });
    };

    this.deleteHttpHeader = (idx) => {
      this.httpHeaders.splice(idx, 1);
    };
  },
};

export default MessagesComponent;
