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

import ApplicationService from '../../services/application.service';
import NotificationService from '../../services/notification.service';
import UserService from '../../services/user.service';

class ApplicationsController {
  private applications: any;
  private applicationsToDisplay: any;
  private selectedApplications: any;
  private subMessage: string;
  private selectedTabIdx: number;
  private currentTab: string;
  private tabs: string[];
  private searchApplications: string;
  private loading = false;

  constructor(
    private UserService: UserService,
    private $filter,
    private $state: StateService,
    private ApplicationService: ApplicationService,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
  ) {
    'ngInject';
    this.selectedApplications = [];

    UserService.current().then((user) => {
      if (UserService.isUserHasPermissions(['environment-application-c'])) {
        this.subMessage = 'Start creating an application';
      } else if (!user.username) {
        this.subMessage = 'Login to get access to your applications';
      } else {
        this.subMessage = '';
      }
    });
    this.tabs = ['active', 'archived'];
    this.selectedTabIdx = 0;
    this.currentTab = this.tabs[this.selectedTabIdx];
  }

  isAdmin = () => this.UserService.currentUser.roles.some((role) => role.scope === 'ENVIRONMENT' && role.name === 'ADMIN');

  loadMore = function (order, searchApplications, showNext) {
    const doNotLoad =
      showNext && (this.applications && this.applications.length) === (this.applicationsToDisplay && this.applicationsToDisplay.length);
    if (!doNotLoad && this.applications && this.applications.length) {
      let applications = _.clone(this.applications);
      if (searchApplications) {
        applications = this.$filter('filter')(applications, searchApplications);
      }
      applications = _.sortBy(applications, _.replace(order, '-', ''));
      if (_.startsWith(order, '-')) {
        applications.reverse();
      }
      const applicationsLength = this.applicationsToDisplay ? this.applicationsToDisplay.length : 0;
      this.applicationsToDisplay = _.take(applications, 20 + applicationsLength);
    }
  };

  selectTab(idx: number) {
    this.loading = true;
    this.selectedTabIdx = idx;
    this.currentTab = this.tabs[this.selectedTabIdx];
    this.selectedApplications = [];
    this.applications = [];
    this.applicationsToDisplay = [];
    this.ApplicationService.list(this.currentTab)
      .then((applications) => {
        this.applications = applications.data;
        this.searchApplications = '';
      })
      .finally(() => (this.loading = false));
  }

  showRestoreConfirm(ev, applicationId: string, applicationName: string) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to restore the application "' + applicationName + '"?',
          confirmButton: 'Restore',
          msg: 'Every subscription belonging to this application will be restored in PENDING status. Subscriptions can be reactivated as per requirements.',
        },
      })
      .then((response) => {
        if (response) {
          this.restoreApplication(applicationId);
        }
      });
  }

  restoreApplication(application: string) {
    this.ApplicationService.restore(application).then((response) => {
      this.applications = this.applications.filter((app) => app.id !== application);
      this.applicationsToDisplay = this.applicationsToDisplay.filter((app) => app.id !== application);
      this.NotificationService.show('Application ' + response.data.name + ' has been restored');
      this.$state.go('management.applications.application.subscriptions.list', { applicationId: application }, { reload: true });
    });
  }
}

export default ApplicationsController;
