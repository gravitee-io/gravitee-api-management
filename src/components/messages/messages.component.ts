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
import MessageService from "../../services/message.service";
import NotificationService from "../../services/notification.service";
import _ = require('lodash');

const MessagesComponent: ng.IComponentOptions = {
  bindings: {
    resolvedScope: '<',
    resolvedRoles: '<',
    resolvedApiId: '<'
  },
  template: require('./messages.html'),
  controller: function(
    $state: StateService,
    NotificationService: NotificationService,
    MessageService: MessageService
  ) {
    'ngInject';

    this.$onInit = () => {
      this.roles = _.sortBy(this.resolvedRoles, ["name"]);
      this.channels = [
        {id: "PORTAL", name: "Portal Notifications"},
        {id: "MAIL", name: "Email"}
      ];
      this.channel = "PORTAL";
    };

    this.send = () => {
      const title = this.title;
      const text = this.text;
      const channel = this.channel;
      const roleScope = this.resolvedScope;
      const roleValues = [this.role];
      if (this.resolvedApiId) {
        MessageService
          .sendFromApi(this.resolvedApiId, title, text, channel, roleScope, roleValues)
          .then( (response) => {
            NotificationService.show(response.data + ' messages has been sent.');
          });
      } else {
        MessageService
          .sendFromPortal(title, text, channel, roleScope, roleValues)
          .then( (response) => {
            NotificationService.show(response.data + ' messages has been sent.');
          });
      }
      this.title = "";
      this.text = "";
      this.formMsg.$setPristine();
    }
  }
};

export default MessagesComponent;
