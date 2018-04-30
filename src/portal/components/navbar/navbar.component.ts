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
export const NavbarComponent: ng.IComponentOptions = {
  template: require('./navbar.html'),
  controller: function(
    UserService: UserService,
    TaskService: TaskService,
    UserNotificationService: UserNotificationService,
    $scope: IScope,
    Constants,
    $rootScope: IScope,
    $state: ng.ui.IStateService,
    $transitions,
    $interval: IIntervalService,
    AuthenticationService: AuthenticationService
  ) {
    'ngInject';

    const vm = this;
    vm.tasksScheduler = null;
    vm.$rootScope = $rootScope;
    vm.displayContextualDocumentationButton = false;
    vm.visible = true;
    vm.providers = AuthenticationService.getProviders();
    vm.localLoginDisabled = (!Constants.authentication.localLogin.enabled) || false;

    $scope.$on('graviteeUserRefresh', function () {
      UserService.current().then(function (user) {
        vm.graviteeUser = user;
        if (user && user.username) {
          let that = vm;
          // schedule an automatic refresh of the user tasks
          if (!that.tasksScheduler) {
            that.refreshUserTasks();
            that.tasksScheduler = $interval(() => {
              that.refreshUserTasks();
            }, TaskService.getTaskSchedulerInSeconds() * 1000);
          }
        }
      }).catch(function () {
        delete vm.graviteeUser;
        $state.go('portal.home');
      });

      vm.supportEnabled = Constants.portal.support.enabled;
    });

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
      $scope.$emit('graviteeUserRefresh');
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
      TaskService.getTasks().then((response) => {
        const result = new PagedResult();
        result.populate(response.data);
        TaskService.fillUserTasks(vm.graviteeUser, result);
      });
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
          UserService.current().then( () => {
            vm.$rootScope.$broadcast('graviteeUserRefresh');
          });
        })
        .catch( () => {});
    };

    vm.isOnlyOAuth = vm.localLoginDisabled && vm.providers.length == 1;

  }
};
