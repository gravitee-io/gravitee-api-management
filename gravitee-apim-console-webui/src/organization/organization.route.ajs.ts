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
import ConsoleSettingsService from '../services/consoleSettings.service';

export default organizationRouterConfig;

function organizationRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('organization', {
      url: '/organization',
      redirectTo: 'organization.settings',
      parent: 'withoutSidenav',
    })
    .state('organization.settings', {
      url: '/settings',
      component: 'organizationSettings',
      resolve: {
        settings: (ConsoleSettingsService: ConsoleSettingsService) => ConsoleSettingsService.get().then((response) => response.data),
      },
      data: {
        menu: null,
        perms: {
          only: [
            // hack only read permissions is necessary but READ is also allowed for API_PUBLISHER
            'organization-role-c',
            'organization-role-u',
            'organization-role-d',
            'environment-documentation-d',
          ],
        },
      },
    })
    .state('organization.settings.ng-cockpit', {
      url: '/cockpit',
      component: 'ngCockpit',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-cockpit',
        },
        perms: {
          only: ['organization-installation-r'],
        },
      },
    })
    .state('organization.settings.ng-console', {
      url: '/console',
      component: 'ngConsoleSettings',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-console',
        },
        perms: {
          only: ['organization-settings-r'],
        },
      },
    })
    .state('organization.settings.ng-roles', {
      url: '/roles',
      component: 'ngRoles',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-r'],
        },
      },
    })
    .state('organization.settings.ng-rolenew', {
      url: '/role/:roleScope/',
      component: 'ngOrgSettingsRole',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-u'],
        },
      },
    })
    .state('organization.settings.ng-roleedit', {
      url: '/role/:roleScope/:role',
      component: 'ngOrgSettingsRole',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-u'],
        },
      },
    })
    .state('organization.settings.ng-rolemembers', {
      url: '/role/:roleScope/:role/members',
      component: 'ngRoleMembers',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-u'],
        },
      },
    })
    .state('organization.settings.ng-users', {
      url: '/users?q&page',
      component: 'ngOrgSettingsUsers',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-users',
        },
        perms: {
          only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
        },
      },
      params: {
        page: {
          value: '1',
          dynamic: true,
        },
        q: {
          dynamic: true,
        },
      },
    })
    .state('organization.settings.ng-newuser', {
      url: '/users/new',
      component: 'ngOrgSettingsNewUser',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-create-user',
        },
        perms: {
          only: ['organization-user-c'],
        },
      },
    })
    .state('organization.settings.ng-user', {
      url: '/users/:userId',
      component: 'ngOrgSettingsUserDetail',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-user',
        },
        perms: {
          only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
        },
      },
    })
    .state('organization.settings.ng-identityproviders', {
      url: '/identities',
      component: 'ngOrgSettingsIdentityProviders',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-identityproviders',
        },
        perms: {
          only: ['organization-identity_provider-r'],
        },
      },
    })
    .state('organization.settings.ng-identityprovider-edit', {
      url: '/identities/:id',
      component: 'ngOrgSettingsIdentityProvider',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-identityproviders',
        },
        perms: {
          only: ['organization-identity_provider-r', 'organization-identity_provider-u', 'organization-identity_provider-d'],
        },
      },
    })
    .state('organization.settings.ng-identityprovider-new', {
      url: '/identities/',
      component: 'ngOrgSettingsIdentityProvider',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-identityproviders',
        },
        perms: {
          only: ['organization-identity_provider-c'],
        },
      },
    })
    .state('organization.settings.ng-tags', {
      url: '/tags',
      component: 'ngOrgSettingsTags',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'management-configuration-sharding-tags',
        },
        perms: {
          only: ['organization-tag-r'],
        },
      },
    })
    .state('organization.settings.ng-policies', {
      url: '/policies',
      component: 'ngPlatformPolicies',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'management-configuration-policies',
        },
        perms: {
          only: ['organization-policies-r'],
        },
      },
    })
    .state('organization.settings.ng-tenants', {
      url: '/tenants',
      component: 'ngTenants',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'management-configuration-tenants',
        },
        perms: {
          only: ['organization-tenant-r'],
        },
      },
    })
    .state('organization.settings.ng-notificationTemplates', {
      url: '/notification-templates',
      component: 'ngNotificationTemplatesComponent',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-notification-templates',
        },
        perms: {
          only: ['organization-notification_templates-r'],
        },
      },
    })
    .state('organization.settings.ng-notificationTemplate', {
      url: '/notification-templates/:scope/:hook',
      component: 'ngNotificationTemplateComponent',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'organization-configuration-notification-template',
        },
        perms: {
          only: ['organization-notification_templates-r'],
        },
      },
    })
    .state('organization.settings.ng-audit', {
      url: '/audit',
      component: 'ngOrgSettingsAudit',
      data: {
        useAngularMaterial: true,
        menu: null,
        docs: {
          page: 'management-audit',
        },
        perms: {
          only: ['organization-audit-r'],
        },
      },
    });
}
