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
import _ = require('lodash');
import { StateService } from '@uirouter/core';

const SettingsComponent: ng.IComponentOptions = {

  template: require("./settings.html"),
  controller: function (
    $rootScope: IScope,
    SidenavService: SidenavService,
    $state: StateService,
    UserService: UserService
  ) {
    'ngInject';
    this.$state = $state;
    this.settingsMenu = {
      // PORTAL
      documentations: {
        perm: UserService.isUserHasPermissions(
          ['portal-documentation-c', 'portal-documentation-u', 'portal-documentation-d']),
        goTo: 'management.settings.pages'
      },
      metadata: {
        perm: UserService.isUserHasPermissions(
          ['portal-metadata-c', 'portal-metadata-u', 'portal-metadata-d']),
        goTo: 'management.settings.metadata'
      },
      views: {
        perm: UserService.isUserHasPermissions(
          ['portal-view-c', 'portal-view-u', 'portal-view-d']),
        goTo: 'management.settings.views'
      },
      topApis: {
        perm: UserService.isUserHasPermissions(
          ['portal-top_apis-c', 'portal-top_apis-u', 'portal-top_apis-d']),
        goTo: 'management.settings.top-apis'
      },
      portalSettings: {
        perm: UserService.isUserHasPermissions(
          ['portal-settings-r']),
        goTo: 'management.settings.portal'
      },

      // GATEWAYS
      tags: {
        perm: UserService.isUserHasPermissions(
          ['management-tag-c', 'management-tag-u', 'management-tag-d']),
        goTo: 'management.settings.tags'
      },
      tenants: {
        perm: UserService.isUserHasPermissions(
          ['management-tenant-c', 'management-tenant-u', 'management-tenant-d']),
        goTo: 'management.settings.tenants'
      },

      // USER MANAGEMENT
      users: {
        perm: UserService.isUserHasPermissions(
          ['management-user-c', 'management-user-u', 'management-user-d']),
        goTo: 'management.settings.users'
      },
      groups: {
        perm: UserService.isUserHasPermissions(
          ['management-group-c', 'management-group-r', 'management-group-u', 'management-group-d']),
        goTo: 'management.settings.groups'
      },
      roles: {
        perm: UserService.isUserHasPermissions(
          ['management-role-c', 'management-role-u', 'management-role-d']),
        goTo: 'management.settings.roles'
      },

      // ALERT
      notifications: {
        perm: UserService.isUserHasPermissions(
          ['management-notification-c', 'management-notification-u', 'management-notification-d']),
        goTo: 'management.settings.notifications'
      }};

    this.$onInit = () => {
      if ($state.current.name === 'management.settings') {
        $rootScope.$broadcast('reduceSideNav');
        SidenavService.setCurrentResource('SETTINGS');

        for ( let entry of _.keys(this.settingsMenu)) {
          if (this.settingsMenu[entry].perm) {
            $state.go(this.settingsMenu[entry].goTo);
            break;
          }
        }
      }
    };
  }
};

export default SettingsComponent;
