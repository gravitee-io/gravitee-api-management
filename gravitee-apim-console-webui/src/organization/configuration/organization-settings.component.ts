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
import { IScope } from 'angular';
import { StateService } from '@uirouter/core';
import UserService from '../../services/user.service';
import _ = require('lodash');

const OrganizationSettingsComponent: ng.IComponentOptions = {
  template: require('./organization-settings.html'),
  controller: function ($rootScope: IScope, $state: StateService, UserService: UserService, Constants, $transitions) {
    'ngInject';
    this.$state = $state;
    this.Constants = Constants;
    this.settingsMenu = {
      // MANAGEMENT
      organizationIdentityProviders: {
        perm: UserService.isUserHasAllPermissions(['organization-identity_provider-r', 'organization-identity_provider_activation-r']),
        goTo: 'organization.settings.identityproviders.list',
      },
      consoleSettings: {
        perm: UserService.isUserHasPermissions(['organization-settings-r']),
        goTo: 'organization.settings.console',
      },

      // USER MANAGEMENT
      users: {
        perm: UserService.isUserHasPermissions(['organization-user-r']),
        goTo: 'organization.settings.users',
      },
      roles: {
        perm: UserService.isUserHasPermissions(['organization-role-c', 'organization-role-u', 'organization-role-d']),
        goTo: 'organization.settings.roles',
      },

      // ALERT
      notificationTemplates: {
        perm: UserService.isUserHasPermissions(['organization-notification_templates-r']),
        goTo: 'organization.settings.notificationTemplates',
      },
    };

    let that = this;
    function getDefaultSettingsMenu(): string {
      for (let entry of _.keys(that.settingsMenu)) {
        if (that.settingsMenu[entry].perm) {
          return that.settingsMenu[entry].goTo;
        }
      }
    }

    $transitions.onBefore({}, function (trans) {
      if (trans.to().name === 'organization.settings') {
        return trans.router.stateService.target(getDefaultSettingsMenu());
      }
    });

    this.$onInit = () => {
      if ($state.current.name === 'organization.settings') {
        $state.go(getDefaultSettingsMenu());
      }
    };
  },
};

export default OrganizationSettingsComponent;
