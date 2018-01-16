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

const TasksComponent: ng.IComponentOptions = {
  bindings: {
    tasks: '<'
  },
  template: require('./tasks.html'),
  controller: function($state: ng.ui.IStateService, UserService: UserService) {
    'ngInject';
    const vm = this;

    vm.$onInit = function() {
      //in case of refresh
      if (!this.tasks) {
        this.tasks = UserService.currentUser.tasks;
      }
    };

    vm.taskMessage = function (task) {
      const appName = vm.tasks.metadata[task.data.application].name;
      const planName = vm.tasks.metadata[task.data.plan].name;
      const apiId = vm.tasks.metadata[task.data.plan].api;
      const apiName = vm.tasks.metadata[apiId].name;
      return 'The application "' + appName + '" requests a subscription for API: ' + apiName + ' (plan: ' + planName + ')';
    };

    vm.title = function (task) {
      return _.startCase(task.type);
    };

    vm.go = function(task) {
      $state.go("management.apis.detail.subscriptions.subscription",
        {
          apiId: task.data.api,
          subscriptionId: task.data.id
        });
    }
  }
};

export default TasksComponent;
