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
import UserService from '../../services/user.service';
import TaskService from '../../services/task.service';
import { IIntervalService, IScope } from 'angular';
import { PagedResult } from '../../entities/pagedResult';
import UserNotificationService from '../../services/userNotification.service';
import { StateService } from '@uirouter/core';
import OrganizationService from '../../services/organization.service';
import AuthenticationService from '../../services/authentication.service';

export const NavbarComponent: ng.IComponentOptions = {
  template: require('./navbar.html'),
  controller: function (
    UserService: UserService,
    TaskService: TaskService,
    UserNotificationService: UserNotificationService,
    OrganizationService: OrganizationService,
    $scope: IScope,
    Constants,
    $rootScope: IScope,
    $state: StateService,
    $transitions,
    $interval: IIntervalService,
    AuthenticationService: AuthenticationService,
    $window
  ) {
    'ngInject';

    const vm = this;

    vm.refreshUser = (user) => {
      vm.profileConfirmed = (user && !user.firstLogin) || $window.localStorage.getItem('profileConfirmed');
    };

    vm.$state = $state;
    vm.tasksScheduler = null;
    vm.$rootScope = $rootScope;
    vm.displayContextualDocumentationButton = false;
    vm.visible = true;
    vm.localLoginDisabled = (!Constants.authentication.localLogin.enabled) || false;
    vm.refreshUser(UserService.currentUser);

    $scope.$on('graviteeUserRefresh', (event, { user, refresh }) => {

      if (refresh) {
        UserService.current()
          .then((user) => {
            vm.startTasks(user);
            vm.refreshUser(user);
          })
          .catch(() => delete vm.graviteeUser);
      } else if (user && user.authenticated) {
        vm.startTasks(user);
        vm.refreshUser(user);
      } else {
        delete vm.graviteeUser;
      }
    });

    vm.startTasks = function (user) {
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

    $scope.$on('graviteeUserTaskRefresh', function () {
      vm.refreshUserTasks();
    });

    $scope.$on('graviteeUserCancelScheduledServices', function () {
      vm.cancelRefreshUserTasks();
    });

    $transitions.onFinish({}, function (trans) {
      vm.displayContextualDocumentationButton =
        !trans.to().name.startsWith('portal') &&
        !trans.to().name.startsWith('support') &&
        !trans.to().name.startsWith('login') &&
        !trans.to().name.startsWith('registration') &&
        !trans.to().name.startsWith('confirm') &&
        !trans.to().name.startsWith('user');

      vm.visible = (trans.to().name !== 'login' &&
        trans.to().name !== 'registration' && trans.to().name !== 'confirm');
    });

    vm.$onInit = function () {
      vm.supportEnabled = Constants.portal.support.enabled;
      $scope.$emit('graviteeUserRefresh', { user: undefined, refresh: true });
      vm.portalURL = Constants.portal.url;
    };

    vm.userShortName = function () {
      if (vm.graviteeUser.firstname && vm.graviteeUser.lastname) {
        const capitalizedFirstName = vm.graviteeUser.firstname[0].toUpperCase() + vm.graviteeUser.firstname.slice(1);
        const shotLastName = vm.graviteeUser.lastname[0].toUpperCase();
        return `${capitalizedFirstName} ${shotLastName}.`;
      } else {
        return vm.graviteeUser.displayName;
      }
    };

    vm.isUserManagement = function () {
      return vm.graviteeUser.isAdmin();
    };

    vm.isAppManagement = function () {
      return vm.graviteeUser.allowedTo(['environment-application-r']);
    };

    vm.getLogo = function () {
      return Constants.theme.logo;
    };

    vm.getUserPicture = function () {
      return UserService.currentUserPicture();
    };

    vm.openContextualDocumentation = function () {
      vm.$rootScope.$broadcast('openContextualDocumentation');
    };

    vm.hasAlert = function () {
      return this.getUserTaskCount() > 0;
    };

    vm.getUserTaskCount = function () {
      if (vm.graviteeUser.tasks) {
        return vm.graviteeUser.tasks.page.total_elements;
      }
      return 0;
    };

    vm.refreshUserTasks = function () {
      if (vm.$rootScope.isWindowFocused) {
        TaskService.getTasks().then((response) => {
          const result = new PagedResult();
          result.populate(response.data);
          vm.graviteeUser = TaskService.fillUserTasks(result);
        });
      }
    };

    vm.cancelRefreshUserTasks = function () {
      if (vm.tasksScheduler) {
        $interval.cancel(vm.tasksScheduler);
        vm.tasksScheduler = undefined;
      }
    };

    vm.authenticate = function () {
      OrganizationService.listSocialIdentityProviders().then((response) => {
        let providers = response.data;
        if (vm.localLoginDisabled && providers.length === 1) {
          AuthenticationService.authenticate(providers[0]);
        } else {
          this.$state.go('login');
        }
      });
    };
  }
};
