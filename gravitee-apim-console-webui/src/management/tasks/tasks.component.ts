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
import { IController, IOnInit } from 'angular';

import { PagedResult } from '../../entities/pagedResult';
import { Task } from '../../entities/task/task';
import UserService from '../../services/user.service';

class TasksComponentController implements IController, IOnInit {
  private tasks: PagedResult<Task>;

  constructor(private readonly $state: StateService, private readonly UserService: UserService) {
    'ngInject';
  }

  $onInit(): void {
    this.tasks = this.UserService.currentUser.tasks;
  }

  taskMessage(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL': {
        const appName = this.tasks.metadata[task.data.application].name;
        const planName = this.tasks.metadata[task.data.plan].name;
        const apiId = this.tasks.metadata[task.data.plan].api;
        const apiName = this.tasks.metadata[apiId].name;
        return `The application <code>${appName}</code> requested a subscription for API <code>${apiName}</code> (plan: ${planName})`;
      }
      case 'IN_REVIEW':
        return `The API <code>${this.tasks.metadata[task.data.referenceId].name}</code> is ready to be reviewed`;
      case 'REQUEST_FOR_CHANGES': {
        let message = `The API <code>${
          this.tasks.metadata[task.data.referenceId].name
        }</code> has been reviewed and some changes are requested by the reviewer`;
        if (task.data.comment) {
          message += ': ' + task.data.comment;
        }
        return message;
      }
      case 'USER_REGISTRATION_APPROVAL':
        return `The registration of the user <strong>${task.data.displayName}</strong> has to be validated`;
      default:
        return 'Unknown task';
    }
  }

  title(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        return 'Subscription';
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES':
        return 'API Review';
      case 'USER_REGISTRATION_APPROVAL':
        return 'User Registration';
    }
  }

  go(task: Task): void {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        this.$state.go('management.apis.detail.portal.subscriptions.subscription', {
          apiId: task.data.api,
          subscriptionId: task.data.id,
        });
        break;
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES':
        this.$state.go('management.apis.detail.portal.general', { apiId: task.data.referenceId });
        break;
      case 'USER_REGISTRATION_APPROVAL':
        this.$state.go('organization.settings.ng-user', { userId: task.data.id });
        break;
    }
  }

  icon(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        return 'communication:vpn_key';
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES':
        return 'maps:rate_review';
      case 'USER_REGISTRATION_APPROVAL':
        return 'general:user';
      case 'PROMOTION_APPROVAL':
        return 'maps:rocket';
      default:
        return '';
    }
  }

  actionLabel(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        return 'Validate';
      case 'IN_REVIEW':
        return 'Review';
      case 'REQUEST_FOR_CHANGES':
        return 'Make changes';
      case 'USER_REGISTRATION_APPROVAL':
        return 'Validate';
      default:
        return 'Details';
    }
  }

  removeTask(taskToRemove: Task): void {
    this.tasks.data = this.tasks.data.filter((task) => task !== taskToRemove);
  }
}

export const TasksComponent: ng.IComponentOptions = {
  template: require('./tasks.html'),
  controller: TasksComponentController,
};
