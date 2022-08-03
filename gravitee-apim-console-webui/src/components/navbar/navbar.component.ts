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
import { Constants } from '../../entities/Constants';

export const NavbarComponent: ng.IComponentOptions = {
  template: require('./navbar.html'),
  controller: function (
    UserService: UserService,
    TaskService: TaskService,
    UserNotificationService: UserNotificationService,
    OrganizationService: OrganizationService,
    $scope: IScope,
    Constants: Constants,
    $rootScope: IScope,
    $state: StateService,
    $transitions,
    $interval: IIntervalService,
    AuthenticationService: AuthenticationService,
    $window,
  ) {
    'ngInject';

    this.refreshUser = (user) => {
      this.newsletterProposed =
        (user && !user.firstLogin) || !!$window.localStorage.getItem('newsletterProposed') || !Constants.org.settings.newsletter.enabled;
    };

    this.$state = $state;
    this.tasksScheduler = null;
    this.$rootScope = $rootScope;
    this.displayContextualDocumentationButton = false;
    this.visible = true;
    this.localLoginDisabled = !Constants.org.settings.authentication.localLogin.enabled || false;
    this.refreshUser(UserService.currentUser);

    $scope.$on('graviteePortalUrlRefresh', (event, portalURL) => {
      this.portalURL = portalURL;
    });

    $scope.$on('graviteeUserRefresh', (event, { user, refresh }) => {
      if (refresh) {
        UserService.current()
          .then((user) => {
            this.startTasks(user);
            this.refreshUser(user);
          })
          .catch(() => delete this.graviteeUser);
      } else if (user && user.authenticated) {
        this.startTasks(user);
        this.refreshUser(user);
      } else {
        delete this.graviteeUser;
      }
    });

    this.startTasks = (user) => {
      if (user.authenticated) {
        this.graviteeUser = user;
        // schedule an automatic refresh of the user tasks
        if (!this.tasksScheduler) {
          this.refreshUserTasks();
          this.tasksScheduler = $interval(() => {
            this.refreshUserTasks();
          }, TaskService.getTaskSchedulerInSeconds() * 1000);
        }
      }
    };

    $scope.$on('graviteeUserTaskRefresh', () => {
      this.refreshUserTasks();
    });

    $scope.$on('graviteeUserCancelScheduledServices', () => {
      this.cancelRefreshUserTasks();
    });

    $transitions.onFinish({}, (trans) => {
      this.displayContextualDocumentationButton =
        !trans.to().name.startsWith('portal') &&
        !trans.to().name.startsWith('support') &&
        !trans.to().name.startsWith('login') &&
        !trans.to().name.startsWith('registration') &&
        !trans.to().name.startsWith('confirm') &&
        !trans.to().name.startsWith('user');

      this.visible = trans.to().name !== 'login' && trans.to().name !== 'registration' && trans.to().name !== 'confirm';
    });

    this.$onInit = () => {
      this.supportEnabled = Constants.org.settings.management.support.enabled;
      $scope.$emit('graviteeUserRefresh', { user: undefined, refresh: true });
      this.portalURL = Constants.env.settings ? Constants.env.settings.portal.url : undefined;
    };

    this.userShortName = () => {
      if (this.graviteeUser.firstname && this.graviteeUser.lastname) {
        const capitalizedFirstName = this.graviteeUser.firstname[0].toUpperCase() + this.graviteeUser.firstname.slice(1);
        const shotLastName = this.graviteeUser.lastname[0].toUpperCase();
        return `${capitalizedFirstName} ${shotLastName}.`;
      } else {
        return this.graviteeUser.displayName;
      }
    };

    this.isUserManagement = () => this.graviteeUser.isAdmin();

    this.isAppManagement = () => this.graviteeUser.allowedTo(['environment-application-r']);

    this.getLogo = () => Constants.org.settings.theme.logo;

    this.getUserPicture = () => UserService.currentUserPicture();

    this.openContextualDocumentation = () => {
      this.$rootScope.$broadcast('openContextualDocumentation');
    };

    this.hasAlert = () => this.getUserTaskCount() > 0;

    this.getUserTaskCount = () => {
      if (this.graviteeUser.tasks) {
        return this.graviteeUser.tasks.page.total_elements;
      }
      return 0;
    };

    this.refreshUserTasks = () => {
      if (this.$rootScope.isWindowFocused) {
        TaskService.getTasks().then((response) => {
          const result = new PagedResult();
          result.populate(response.data);
          this.graviteeUser = TaskService.fillUserTasks(result);
        });
      }
    };

    this.cancelRefreshUserTasks = () => {
      if (this.tasksScheduler) {
        $interval.cancel(this.tasksScheduler);
        this.tasksScheduler = undefined;
      }
    };

    this.authenticate = () => {
      OrganizationService.listSocialIdentityProviders().then((response) => {
        const providers = response.data;
        if (this.localLoginDisabled && providers.length === 1) {
          AuthenticationService.authenticate(providers[0]);
        } else {
          this.$state.go('login');
        }
      });
    };

    this.goToMyAccount = () => {
      this.$state.go('user', { ...this.$state.params, environmentId: this.$state.params.environmentId ?? Constants.org.currentEnv.id });
    };
  },
};
