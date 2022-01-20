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
import { IScope } from 'angular';
import * as _ from 'lodash';

import UserService from '../../services/user.service';

// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-state');

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
        goTo: 'organization.settings.ng-identityproviders',
      },
      consoleSettings: {
        perm: UserService.isUserHasPermissions(['organization-settings-r']),
        goTo: 'organization.settings.ng-console',
      },

      // USER MANAGEMENT
      users: {
        perm: UserService.isUserHasPermissions(['organization-user-r']),
        goTo: 'organization.settings.ng-users',
      },
      roles: {
        perm: UserService.isUserHasPermissions(['organization-role-c', 'organization-role-u', 'organization-role-d']),
        goTo: 'organization.settings.ng-roles',
      },

      tags: {
        perm: UserService.isUserHasPermissions(['organization-tag-c', 'organization-tag-u', 'organization-tag-d']),
        goTo: 'organization.settings.ng-tags',
      },
      tenants: {
        perm: UserService.isUserHasPermissions(['organization-tenant-c', 'organization-tenant-u', 'organization-tenant-d']),
        goTo: 'organization.settings.ng-tenants',
      },
      policies: {
        perm: UserService.isUserHasPermissions(['organization-policies-c', 'organization-policies-u', 'organization-policies-d']),
        goTo: 'organization.settings.ng-policies',
      },

      // ALERT
      notificationTemplates: {
        perm: UserService.isUserHasPermissions(['organization-notification_templates-r']),
        goTo: 'organization.settings.ng-notificationTemplates',
      },

      // COCKPIT
      cockpit: {
        perm: UserService.isUserHasPermissions(['organization-installation-r']),
        goTo: 'organization.settings.ng-cockpit',
      },
    };

    const getDefaultSettingsMenu = (): string => {
      for (const entry of _.keys(this.settingsMenu)) {
        if (this.settingsMenu[entry].perm) {
          return this.settingsMenu[entry].goTo;
        }
      }
    };

    $transitions.onBefore({}, (trans) => {
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
