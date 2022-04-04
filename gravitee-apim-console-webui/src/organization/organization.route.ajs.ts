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
import { Scope } from '../entities/alert';
import AlertService from '../services/alert.service';
import ConsoleSettingsService from '../services/consoleSettings.service';
import EntrypointService from '../services/entrypoint.service';
import EnvironmentService from '../services/environment.service';
import FlowService from '../services/flow.service';
import GroupService from '../services/group.service';
import IdentityProviderService from '../services/identityProvider.service';
import NotificationTemplatesService from '../services/notificationTemplates.service';
import OrganizationService from '../services/organization.service';
import PolicyService from '../services/policy.service';
import PortalSettingsService from '../services/portalSettings.service';
import RoleService from '../services/role.service';
import TagService from '../services/tag.service';
import TenantService from '../services/tenant.service';
import UserService from '../services/user.service';

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
    .state('organization.settings.ajs-cockpit', {
      url: '/ajs-cockpit',
      component: 'cockpit',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-cockpit',
        },
        perms: {
          only: ['organization-installation-r'],
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
    .state('organization.settings.ajs-console', {
      url: '/ajs-console',
      component: 'consoleSettings',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-console',
        },
        perms: {
          only: ['organization-settings-r'],
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
    .state('organization.settings.ajs-roles', {
      url: '/ajs-roles',
      component: 'roles',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes(),
        organizationRoles: (RoleService: RoleService) => RoleService.list('ORGANIZATION'),
        environmentRoles: (RoleService: RoleService) => RoleService.list('ENVIRONMENT'),
        apiRoles: (RoleService: RoleService) => RoleService.list('API'),
        applicationRoles: (RoleService: RoleService) => RoleService.list('APPLICATION'),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-r'],
        },
      },
      params: {
        roleScope: {
          type: 'string',
          value: 'ORGANIZATION',
          squash: false,
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
    .state('organization.settings.ajs-rolenew', {
      url: '/ajs-role/:roleScope/new',
      component: 'role',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes(),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-c'],
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
    .state('organization.settings.ajs-roleedit', {
      url: '/ajs-role/:roleScope/:role',
      component: 'role',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes(),
      },
      data: {
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
    .state('organization.settings.ajs-rolemembers', {
      url: '/ajs-role/:roleScope/:role/members',
      component: 'roleMembers',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles',
        },
        perms: {
          only: ['organization-role-u'],
        },
      },
      resolve: {
        members: (RoleService: RoleService, $stateParams) =>
          RoleService.listUsers($stateParams.roleScope, $stateParams.role).then((response) => response),
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
    .state('organization.settings.ajs-users', {
      url: '/ajs-users?q&page',
      component: 'users',
      resolve: {
        usersPage: (UserService: UserService, $state, $stateParams) =>
          UserService.list($stateParams.q, $stateParams.page).then((response) => response.data),
      },
      data: {
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
    .state('organization.settings.ajs-user', {
      url: '/ajs-users/:userId',
      component: 'userDetail',
      resolve: {
        selectedUser: (UserService: UserService, $stateParams) => UserService.get($stateParams.userId).then((response) => response),
        groups: (UserService: UserService, $stateParams) =>
          UserService.getUserGroups($stateParams.userId).then((response) => response.data),
        organizationRoles: (RoleService: RoleService) => RoleService.list('ORGANIZATION').then((roles) => roles),
        environments: (EnvironmentService: EnvironmentService) => EnvironmentService.list().then((response) => response.data),
        environmentRoles: (RoleService: RoleService) => RoleService.list('ENVIRONMENT').then((roles) => roles),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list('API').then((roles) => [{ scope: 'API', name: '', system: false }].concat(roles)),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list('APPLICATION').then((roles) => [{ scope: 'APPLICATION', name: '', system: false }].concat(roles)),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-user',
        },
        perms: {
          only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
        },
      },
    })
    .state('organization.settings.ajs-newuser', {
      url: '/ajs-users/new',
      component: 'newUser',
      resolve: {
        identityProviders: (IdentityProviderService: IdentityProviderService) => IdentityProviderService.list(),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-create-user',
        },
        perms: {
          only: ['organization-user-c'],
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
    .state('organization.settings.ajs-identityproviders', {
      abstract: true,
      url: '/ajs-identities',
    })
    .state('organization.settings.ajs-identityproviders.list', {
      url: '/',
      component: 'identityProviders',
      resolve: {
        target: () => 'ORGANIZATION',
        targetId: () => 'DEFAULT',
        identityProviders: (IdentityProviderService: IdentityProviderService) =>
          IdentityProviderService.list().then((response) => response),
        identities: (OrganizationService: OrganizationService) =>
          OrganizationService.listOrganizationIdentities().then((response) => response.data),
        settings: (ConsoleSettingsService: ConsoleSettingsService) => ConsoleSettingsService.get().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-identityproviders',
        },
        perms: {
          only: ['organization-identity_provider-r'],
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
    .state('organization.settings.ajs-identityproviders.new', {
      url: '/new?:type',
      component: 'identityProvider',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-identityprovider',
        },
        perms: {
          only: ['organization-identity_provider-c'],
        },
      },
    })
    .state('organization.settings.ajs-identityproviders.identityprovider', {
      url: '/:id',
      component: 'identityProvider',
      resolve: {
        identityProvider: (IdentityProviderService: IdentityProviderService, $stateParams) =>
          IdentityProviderService.get($stateParams.id).then((response) => response),

        groups: (GroupService: GroupService) => GroupService.list().then((response) => response.data),

        environmentRoles: (RoleService: RoleService) => RoleService.list('ENVIRONMENT').then((roles) => roles),

        organizationRoles: (RoleService: RoleService) => RoleService.list('ORGANIZATION').then((roles) => roles),

        environments: (EnvironmentService: EnvironmentService) => EnvironmentService.list().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-identityprovider',
        },
        perms: {
          only: ['organization-identity_provider-r', 'organization-identity_provider-u', 'organization-identity_provider-d'],
        },
      },
    })
    .state('organization.settings.ajs-tags', {
      url: '/ajs-tags',
      component: 'tags',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then((response) => response.data),
        entrypoints: (EntrypointService: EntrypointService) => EntrypointService.list().then((response) => response.data),
        groups: (GroupService: GroupService) => GroupService.listByOrganization().then((response) => response.data),
        settings: (PortalSettingsService: PortalSettingsService) => PortalSettingsService.get().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-sharding-tags',
        },
        perms: {
          only: ['organization-tag-r'],
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
    .state('organization.settings.ajs-policies', {
      url: '/ajs-policies',
      component: 'policies',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then((response) => response.data),
        settings: (PortalSettingsService: PortalSettingsService) => PortalSettingsService.get().then((response) => response.data),
        resolvedFlowSchema: (FlowService: FlowService) => FlowService.getPlatformFlowSchemaForm(),
        resolvedPolicies: (PolicyService: PolicyService) => PolicyService.list(true, true, true),
      },
      data: {
        docs: {
          page: 'management-configuration-policies',
        },
        perms: {
          only: ['organization-policies-r'],
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
    .state('organization.settings.ajs-newEntrypoint', {
      url: '/ajs-tags/entrypoint/new',
      component: 'entrypoint',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-entrypoint',
        },
        perms: {
          only: ['organization-entrypoint-c'],
        },
      },
    })
    .state('organization.settings.ajs-entrypoint', {
      url: '/ajs-tags/entrypoint/:entrypointId',
      component: 'entrypoint',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-entrypoint',
        },
        perms: {
          only: ['organization-entrypoint-u'],
        },
      },
    })
    .state('organization.settings.ajs-tag', {
      url: '/ajs-tags/:tagId',
      component: 'tag',
      resolve: {
        groups: (GroupService: GroupService) => GroupService.listByOrganization().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-sharding-tag',
        },
        perms: {
          only: ['organization-tag-r', 'organization-tag-c', 'organization-tag-u'],
        },
      },
    })
    .state('organization.settings.ajs-tenants', {
      url: '/ajs-tenants',
      component: 'tenants',
      resolve: {
        tenants: (TenantService: TenantService) => TenantService.list().then((response) => response.data),
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-tenants',
        },
        perms: {
          only: ['organization-tenant-r'],
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
    .state('organization.settings.ajs-notificationTemplates', {
      url: '/ajs-notification-templates',
      component: 'notificationTemplatesComponent',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-notification-templates',
        },
        perms: {
          only: ['organization-notification_templates-r'],
        },
      },
      resolve: {
        notificationTemplates: (NotificationTemplatesService: NotificationTemplatesService) =>
          NotificationTemplatesService.getNotificationTemplates().then((response) => response.data),
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
    .state('organization.settings.ajs-notificationTemplate', {
      url: '/ajs-notification-templates/:scope/:hook',
      component: 'notificationTemplateComponent',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-notification-template',
        },
        perms: {
          only: ['organization-notification_templates-r'],
        },
      },
      resolve: {
        notifTemplates: (NotificationTemplatesService: NotificationTemplatesService, $stateParams) => {
          if ($stateParams.scope.toUpperCase() === 'TEMPLATES_TO_INCLUDE') {
            return NotificationTemplatesService.getNotificationTemplates('', $stateParams.scope).then((response) => response.data);
          } else {
            return NotificationTemplatesService.getNotificationTemplates($stateParams.hook, $stateParams.scope).then(
              (response) => response.data,
            );
          }
        },
        alertingStatus: (AlertService: AlertService) => AlertService.getStatus(undefined, Scope.ENVIRONMENT).then((response) => response.data),
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
    });
}
