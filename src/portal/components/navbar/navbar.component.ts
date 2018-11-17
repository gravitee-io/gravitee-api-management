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
import UserService from "../../../services/user.service";
import TaskService from "../../../services/task.service";
import {IIntervalService, IScope} from "angular";
import {PagedResult} from "../../../entities/pagedResult";
import UserNotificationService from "../../../services/userNotification.service";
import AuthenticationService from '../../../services/authentication.service';
import { StateService } from '@uirouter/core';

export const NavbarComponent: ng.IComponentOptions = {
  template: require('./navbar.html'),
  controller: function(
    UserService: UserService,
    TaskService: TaskService,
    UserNotificationService: UserNotificationService,
    $scope: IScope,
    Constants,
    $rootScope: IScope,
    $state: StateService,
    $transitions,
    $interval: IIntervalService,
    AuthenticationService: AuthenticationService
  ) {
    'ngInject';

    const vm = this;
    vm.$state = $state;
    vm.tasksScheduler = null;
    vm.$rootScope = $rootScope;
    vm.displayContextualDocumentationButton = false;
    vm.visible = true;
    vm.providers = AuthenticationService.getProviders();
    vm.localLoginDisabled = (!Constants.authentication.localLogin.enabled) || false;

    $scope.$on('graviteeUserRefresh', (event, {user, refresh}) => {
      if (refresh) {
        UserService.current()
          .then((user) => vm.startTasks(user))
          .catch(() => delete vm.graviteeUser);
      } else if (user && user.authenticated) {
        vm.startTasks(user);
      } else {
        delete vm.graviteeUser;
      }
    });

    vm.startTasks = function(user) {
      if (user.authenticated) {
        vm.graviteeUser = user;
        // schedule an automatic refresh of the user tasks
        if (!vm.tasksScheduler) {
          vm.refreshUserTasks();
          vm.tasksScheduler = $interval(() => {
            vm.refreshUserTasks();
          }, TaskService.getTaskSchedulerInSeconds() * 1000);
        }
      }
    };

    $scope.$on("graviteeUserTaskRefresh", function () {
      vm.refreshUserTasks();
    });

    $scope.$on("graviteeUserCancelScheduledServices", function () {
      vm.cancelRefreshUserTasks();
    });

    $transitions.onFinish({}, function (trans) {
      vm.displayContextualDocumentationButton =
        !trans.to().name.startsWith('portal') &&
        !trans.to().name.startsWith('support') &&
        !trans.to().name.startsWith('login') &&
        !trans.to().name.startsWith('registration') &&
        !trans.to().name.startsWith('user');

      let forceLogin = Constants.authentication.forceLogin.enabled || false;
      vm.visible = ! forceLogin || (forceLogin && !trans.to().name.startsWith('login') &&
        !trans.to().name.startsWith('registration'));
    });

    vm.$onInit = function () {
      vm.supportEnabled = Constants.portal.support.enabled;
      $scope.$emit('graviteeUserRefresh', {user: undefined, refresh: true});

    };

    vm.isUserManagement = function () {
      return vm.graviteeUser.isAdmin();
    };

    vm.getLogo = function() {
      return Constants.theme.logo;
    };

    vm.getUserPicture = function() {
      return UserService.currentUserPicture();
    };

    vm.openContextualDocumentation = function() {
      vm.$rootScope.$broadcast('openContextualDocumentation');
    };

    vm.hasAlert = function() {
      return this.getUserTaskCount() > 0;
    };

    vm.getUserTaskCount = function() {
      if (vm.graviteeUser.tasks) {
        return vm.graviteeUser.tasks.page.total_elements;
      }
      return 0;
    };

    vm.refreshUserTasks = function() {
      if(vm.$rootScope.isWindowFocused) {
        TaskService.getTasks().then((response) => {
          const result = new PagedResult();
          result.populate(response.data);
          TaskService.fillUserTasks(vm.graviteeUser, result);
        });
      }
    };

    vm.cancelRefreshUserTasks = function() {
      if (vm.tasksScheduler) {
        $interval.cancel(vm.tasksScheduler);
        vm.tasksScheduler = undefined;
      }
    };

    vm.authenticate = function(provider: string) {
      AuthenticationService.authenticate(provider)
        .then( () => {
          UserService.current().then( (user) => {
            vm.$rootScope.$broadcast('graviteeUserRefresh', {user: user});
          });
        })
        .catch( () => {});
    };

    vm.isOnlyOAuth = vm.localLoginDisabled && vm.providers.length == 1;

  }
};
