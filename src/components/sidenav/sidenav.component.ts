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
import { IScope, IWindowService } from 'angular';
import { StateService } from '@uirouter/core';
import PortalConfigService from '../../services/portalConfig.service';
import UserService from '../../services/user.service';
import EnvironmentService from '../../services/environment.service';


export const SidenavComponent: ng.IComponentOptions = {
  template: require('./sidenav.html'),
  bindings: {
    graviteeUser: '<',
    menuItems: '<',
    allMenuItems: '<'
  },
  controller: function(
    Constants,
    $window: IWindowService,
    $scope: IScope,
    $state: StateService,
    $rootScope: IScope,
    PortalConfigService: PortalConfigService,
    EnvironmentService: EnvironmentService,
    UserService: UserService) {
    'ngInject';
    const lastEnvironmentLoaded = 'gv-last-environment-loaded';
    const reduceModeKey = 'gv-sidenav-reduce-mode';
    this.$window = $window;
    this.reducedMode = false;
    this.Constants = Constants;
    this.$state = $state;

    this.$onInit = () => {
      this.updateCurrentEnvSettings();
      if (this.$window.localStorage.getItem(reduceModeKey) !== null) {
        this.reducedMode = JSON.parse(this.$window.localStorage.getItem(reduceModeKey));
      }
    };

    this.updateCurrentEnvSettings = () => {
      PortalConfigService.get().then(response => {
        Constants.env.settings = response.data;
        $rootScope.$broadcast('graviteePortalUrlRefresh', Constants.env.settings.portal.url);
      });
    };

    this.toggleReducedMode = () => {
      this.reducedMode = !this.reducedMode;
      $window.localStorage.setItem(reduceModeKey, this.reducedMode);
      $rootScope.$broadcast('onWidgetResize');
    };

    this.isActive = function (menuItem) {
      let menuItemSplitted = menuItem.name.split('.');
      let currentStateSplitted = $state.current.name.split('.');
      return menuItemSplitted[0] === currentStateSplitted[0] &&
        menuItemSplitted[1] === currentStateSplitted[1];
    };

    $scope.$on('reduceSideNav', () => {
      if (!this.reducedMode) {
        this.toggleReducedMode();
      }
    });

    this.changeCurrentEnv = () => {
      this.updateCurrentEnvSettings();
      $window.localStorage.setItem(lastEnvironmentLoaded, Constants.org.currentEnv.id);

      UserService.refreshEnvironmentPermissions().then(() => {
        $state.go('management', { environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv) }, { reload: true });
      });
    };
  }
};
