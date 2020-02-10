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
import SidenavService from '../../components/sidenav/sidenav.service';
import {IScope} from 'angular';
import UserService from '../../services/user.service';
import _ = require('lodash');
import { StateService } from '@uirouter/core';

const SettingsComponent: ng.IComponentOptions = {

  template: require('./settings.html'),
  controller: function (
    $rootScope: IScope,
    SidenavService: SidenavService,
    $state: StateService,
    UserService: UserService,
    Constants
  ) {
    'ngInject';
    this.$state = $state;
    this.settingsMenu = {
      // PORTAL
      analytics: {
        perm: UserService.isUserHasPermissions(
          ['environment-settings-r']),
        goTo: 'management.settings.analytics'
      },
      apiPortalHeader: {
        perm: UserService.isUserHasPermissions(
          ['environment-api_header-r']),
        goTo: 'management.settings.apiPortalHeader'
      },
      apiQuality: {
        perm: UserService.isUserHasPermissions(['environment-settings-r']),
        goTo: 'management.settings.apiQuality'
      },
      clientRegistration: {
        perm: UserService.isUserHasPermissions(
          ['environment-client_registration_provider-r']),
        goTo: 'management.settings.clientregistrationproviders.list'
      },
      identityProviders: {
        perm: UserService.isUserHasPermissions(
          ['environment-identity_provider-r']),
        goTo: 'management.settings.identityproviders.list'
      },
      documentations: {
        perm: UserService.isUserHasPermissions(
          ['environment-documentation-c', 'environment-documentation-u', 'environment-documentation-d']),
        goTo: 'management.settings.documentation'
      },
      metadata: {
        perm: UserService.isUserHasPermissions(
          ['environment-metadata-r']),
        goTo: 'management.settings.metadata'
      },
      portalSettings: {
        perm: UserService.isUserHasPermissions(
          ['environment-settings-r']),
        goTo: 'management.settings.portal'
      },
      theme: {
        perm: UserService.isUserHasPermissions(
          ['portal-theme-r']),
        goTo: 'management.settings.theme'
      },
      topApis: {
        perm: UserService.isUserHasPermissions(
          ['environment-top_apis-r']),
        goTo: 'management.settings.top-apis'
      },
      views: {
        perm: UserService.isUserHasPermissions(
          ['environment-view-r']),
        goTo: 'management.settings.views'
      },

      // MANAGEMENT
      managementSettings: {
        perm: UserService.isUserHasPermissions(
          ['environment-settings-r']),
        goTo: 'management.settings.management'
      },

      // GATEWAYS
      api_logging: {
        perm: UserService.isUserHasPermissions(
          ['environment-settings-r']),
        goTo: 'management.settings.api_logging'
      },
      dictionaries: {
        perm: UserService.isUserHasPermissions(
          ['environment-dictionary-r']),
        goTo: 'management.settings.dictionaries.list'
      },
      tags: {
        perm: UserService.isUserHasPermissions(
          ['environment-tag-c', 'environment-tag-u', 'environment-tag-d']),
        goTo: 'management.settings.tags'
      },
      tenants: {
        perm: UserService.isUserHasPermissions(
          ['environment-tenant-c', 'environment-tenant-u', 'environment-tenant-d']),
        goTo: 'management.settings.tenants'
      },

      // USER MANAGEMENT
      users: {
        perm: UserService.isUserHasPermissions(
          ['organization-user-r']),
        goTo: 'management.settings.users'
      },
      groups: {
        perm: UserService.isUserHasPermissions(
          ['environment-group-r']),
        goTo: 'management.settings.groups'
      },
      roles: {
        perm: UserService.isUserHasPermissions(
          ['organization-role-c', 'organization-role-u', 'organization-role-d']),
        goTo: 'management.settings.roles'
      },

      // ALERT
      notifications: {
        perm: UserService.isUserHasPermissions(['environment-notification-r']),
        goTo: 'management.settings.notifications'
      },
      alerts: {
        perm: UserService.isUserHasPermissions(['environment-alert-r']) && Constants.alert && Constants.alert.enabled,
        goTo: 'management.settings.alerts'
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
