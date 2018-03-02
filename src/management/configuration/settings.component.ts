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

import SidenavService from "../../components/sidenav/sidenav.service";
import {IScope} from "angular";
import UserService from "../../services/user.service";

const SettingsComponent: ng.IComponentOptions = {

  template: require("./settings.html"),
  controller: function (
    $rootScope: IScope,
    SidenavService: SidenavService,
    $state: ng.ui.IStateService,
    UserService: UserService
  ) {
    'ngInject';
    this.$state = $state;

    this.$onInit = () => {

      $rootScope.$broadcast('reduceSideNav');
      SidenavService.setCurrentResource('SETTINGS');

      if (UserService.isUserHasPermissions(['portal-documentation-r'])) {
        $state.go('management.settings.pages');
      } else if (UserService.isUserHasPermissions(['portal-metadata-r'])) {
        $state.go("management.settings.metadata");
      } else if (UserService.isUserHasPermissions(['portal-view-r'])) {
        $state.go("management.settings.views");
      } else if (UserService.isUserHasPermissions(['portal-top_apis-r'])) {
        $state.go("management.settings.top-apis");
      } else if (UserService.isUserHasPermissions(['management-tag-r'])) {
        $state.go("management.settings.tags");
      } else if (UserService.isUserHasPermissions(['management-tenant-r'])) {
        $state.go("management.settings.tenants");
      } else if (UserService.isUserHasPermissions(['management-group-r'])) {
        $state.go("management.settings.groups");
      } else if (UserService.isUserHasPermissions(['management-role-r'])) {
        $state.go("management.settings.roles");
      } else if (UserService.isUserHasPermissions(['management-notification-r'])) {
        $state.go("management.settings.notifications");
      }
    }
  }
};

export default SettingsComponent;