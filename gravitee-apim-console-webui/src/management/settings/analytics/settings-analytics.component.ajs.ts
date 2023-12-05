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
import * as _ from 'lodash';
import { Router } from '@angular/router';

import { Dashboard } from '../../../entities/dashboard';
import DashboardService from '../../../services/dashboard.service';
import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';
import UserService from '../../../services/user.service';

const SettingsAnalyticsComponentAjs: ng.IComponentOptions = {
  bindings: {
    dashboardsPlatform: '<',
    dashboardsApi: '<',
    dashboardsApplication: '<',
  },
  template: require('./settings-analytics.html'),
  controller: [
    'NotificationService',
    'PortalSettingsService',
    'Constants',
    '$mdDialog',
    'DashboardService',
    'UserService',
    'ngRouter',
    function (
      NotificationService: NotificationService,
      PortalSettingsService: PortalSettingsService,
      Constants: any,
      $mdDialog: angular.material.IDialogService,
      DashboardService: DashboardService,
      UserService: UserService,
      router: Router,
    ) {
      this.router = router;
      this.settings = _.cloneDeep(Constants.env.settings);
      this.providedConfigurationMessage = 'Configuration provided by the system';

      this.$onInit = () => {
        Promise.all([DashboardService.list('PLATFORM'), DashboardService.list('API'), DashboardService.list('APPLICATION')]).then(
          ([dashboardsPlatform, dashboardsApi, dashboardsApplication]) => {
            this.dashboardsByType = {
              Platform: dashboardsPlatform.data,
              API: dashboardsApi.data,
              Application: dashboardsApplication.data,
            };
          },
        );

        this.canUpdateSettings = UserService.isUserHasPermissions([
          'environment-settings-c',
          'environment-settings-u',
          'environment-settings-d',
        ]);
      };

      this.isDashboardsEmpty = () => {
        return _.flattenDeep(_.values(this.dashboardsByType)).length === 0;
      };

      this.save = () => {
        PortalSettingsService.save(this.settings).then((response) => {
          _.merge(Constants.env.settings, response.data);
          NotificationService.show('Configuration saved');
          this.formSettings.$setPristine();
        });
      };

      this.reset = () => {
        this.settings = _.cloneDeep(Constants.env.settings);
        this.formSettings.$setPristine();
      };

      this.delete = (dashboard: Dashboard) => {
        $mdDialog
          .show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
              title: `Are you sure you want to delete the dashboard '${dashboard.name}'?`,
              msg: '',
              confirmButton: 'Delete',
            },
          })
          .then((response) => {
            if (response) {
              DashboardService.delete(dashboard).then(() => {
                NotificationService.show("Dashboard '" + dashboard.name + "' has been deleted");
                this.$onInit();
              });
            }
          });
      };

      this.update = (dashboard: Dashboard) => {
        DashboardService.update(dashboard)
          .then(() => {
            NotificationService.show('Dashboard saved with success');
          })
          .finally(() => {
            this.$onInit();
          });
      };

      this.upward = (dashboard: Dashboard) => {
        dashboard.order--;
        this.update(dashboard);
      };

      this.downward = (dashboard: Dashboard) => {
        dashboard.order++;
        this.update(dashboard);
      };

      this.toggleEnable = (dashboard: Dashboard) => {
        dashboard.enabled = !dashboard.enabled;
        this.update(dashboard);
      };

      this.isReadonlySetting = (property: string): boolean => {
        return PortalSettingsService.isReadonly(this.settings, property);
      };

      this.navigateToDashboard = (type: string, dashboardId: string) => {
        this.router.navigate(['dashboard', type, dashboardId], { relativeTo: this.activatedRoute });
      };

      this.newDashboard = (type: string) => {
        this.router.navigate(['dashboard', type, 'new'], { relativeTo: this.activatedRoute });
      };
    },
  ],
};

export default SettingsAnalyticsComponentAjs;
