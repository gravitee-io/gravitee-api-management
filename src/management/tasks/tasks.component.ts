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
import UserService from "../../services/user.service";
import { StateService } from '@uirouter/core';

const TasksComponent: ng.IComponentOptions = {
  template: require('./tasks.html'),
  controller: function($state: StateService, UserService: UserService) {
    'ngInject';

    this.tasks = UserService.currentUser.tasks;

    this.taskMessage = (task) => {
      switch (task.type) {
        case 'subscription_approval':
          const appName = this.tasks.metadata[task.data.application].name;
          const planName = this.tasks.metadata[task.data.plan].name;
          const apiId = this.tasks.metadata[task.data.plan].api;
          const apiName = this.tasks.metadata[apiId].name;
          return 'The application "' + appName + '" requests a subscription for API: ' + apiName + ' (plan: ' + planName + ')';
        case 'in_review':
          return 'The API "' + this.tasks.metadata[task.data.referenceId].name + '" is ready to be reviewed';
        case 'request_for_changes':
          let message = 'The API "' + this.tasks.metadata[task.data.referenceId].name + '" need changes';
          if (task.data.comment) {
            message += ': ' + task.data.comment;
          }
          return message;
        default:
          return 'Unknown task';
      }
    };

    this.title = (task) => {
      return _.startCase(task.type);
    };

    this.go = (task) => {
      switch (task.type) {
        case 'subscription_approval':
          $state.go('management.apis.detail.portal.subscriptions.subscription',
            {
              apiId: task.data.api,
              subscriptionId: task.data.id
            });
          break;
        case 'in_review':
        case 'request_for_changes':
          $state.go('management.apis.detail.portal.general', {apiId: task.data.referenceId});
          break;
      }
    }

    this.icon = (task) => {
      switch (task.type) {
        case 'subscription_approval':
          return 'vpn_key';
        case 'in_review':
        case 'request_for_changes':
          return 'rate_review';
        default:
          return '';
      }
    }
  }
};

export default TasksComponent;
