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
import RoleService from '../services/role.service';
import UserService from '../services/user.service';
import EnvironmentService from '../services/environment.service';
import IdentityProviderService from '../services/identityProvider.service';
import GroupService from '../services/group.service';
import OrganizationService from '../services/organization.service';
import NotificationTemplatesService from '../services/notificationTemplates.service';
import ConsoleSettingsService from '../services/consoleSettings.service';

export default organizationRouterConfig;

function organizationRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('organization', {
      url: '/organization',
      redirectTo: 'organization.settings',
      parent: 'withoutSidenav'
    })
    .state('organization.settings', {
      url: '/settings',
      component: 'organizationSettings',
      resolve: {
        settings: (ConsoleSettingsService: ConsoleSettingsService) => ConsoleSettingsService.get().then(response => response.data)
      },
      data: {
        menu: null,
        perms: {
          only: [
            // hack only read permissions is necessary but READ is also allowed for API_PUBLISHER
            'organization-role-c', 'organization-role-u', 'organization-role-d', 'environment-documentation-d'
          ]
        }
      }
    })
    .state('organization.settings.cockpit', {
      url: '/cockpit',
      component: 'cockpit',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-cockpit'
        },
        perms: {
          only: ['organization-installation-r']
        }
      }
    })
    .state('organization.settings.console', {
      url: '/console',
      component: 'consoleSettings',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-console'
        },
        perms: {
          only: ['organization-settings-r']
        }
      }
    })
    .state('organization.settings.roles', {
      url: '/roles',
      component: 'roles',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes(),
        organizationRoles: (RoleService: RoleService) => RoleService.list('ORGANIZATION'),
        environmentRoles: (RoleService: RoleService) => RoleService.list('ENVIRONMENT'),
        apiRoles: (RoleService: RoleService) => RoleService.list('API'),
        applicationRoles: (RoleService: RoleService) => RoleService.list('APPLICATION')
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles'
        },
        perms: {
          only: ['organization-role-r']
        }
      },
      params: {
        roleScope: {
          type: 'string',
          value: 'ORGANIZATION',
          squash: false
        }
      }
    })
    .state('organization.settings.rolenew', {
      url: '/role/:roleScope/new',
      component: 'role',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles'
        },
        perms: {
          only: ['organization-role-c']
        }
      }
    })
    .state('organization.settings.roleedit', {
      url: '/role/:roleScope/:role',
      component: 'role',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles'
        },
        perms: {
          only: ['organization-role-u']
        }
      }
    })
    .state('organization.settings.rolemembers', {
      url: '/role/:roleScope/:role/members',
      component: 'roleMembers',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-roles'
        },
        perms: {
          only: ['organization-role-u']
        }
      },
      resolve: {
        members: (RoleService: RoleService, $stateParams) =>
          RoleService.listUsers($stateParams.roleScope, $stateParams.role).then((response) => response
          )
      }
    })
    .state('organization.settings.users', {
      url: '/users?q&page',
      component: 'users',
      resolve: {
        usersPage: (UserService: UserService, $state, $stateParams) =>
          UserService.list($stateParams.q, $stateParams.page).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-users'
        },
        perms: {
          only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d']
        }
      },
      params: {
        page: {
          value: '1',
          dynamic: true
        }
      }
    })
    .state('organization.settings.user', {
      url: '/users/:userId',
      component: 'userDetail',
      resolve: {
        selectedUser: (UserService: UserService, $stateParams) =>
          UserService.get($stateParams.userId).then(response =>
            response
          ),
        groups: (UserService: UserService, $stateParams) =>
          UserService.getUserGroups($stateParams.userId).then(response =>
            response.data
          ),
        organizationRoles: (RoleService: RoleService) =>
          RoleService.list('ORGANIZATION').then((roles) =>
            roles
          ),
        environments: (EnvironmentService: EnvironmentService) =>
          EnvironmentService.list().then(response => response.data
          ),
        environmentRoles: (RoleService: RoleService) =>
          RoleService.list('ENVIRONMENT').then((roles) =>
            roles
          ),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list('API').then((roles) =>
            [{ 'scope': 'API', 'name': '', 'system': false }].concat(roles)
          ),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list('APPLICATION').then((roles) =>
            [{ 'scope': 'APPLICATION', 'name': '', 'system': false }].concat(roles)
          )
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-user'
        },
        perms: {
          only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d']
        }
      }
    })
    .state('organization.settings.newuser', {
      url: '/users/new',
      component: 'newUser',
      resolve: {
        identityProviders: (IdentityProviderService: IdentityProviderService) => IdentityProviderService.list()
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-create-user'
        },
        perms: {
          only: ['organization-user-c']
        }
      }
    })
    .state('organization.settings.identityproviders', {
      abstract: true,
      url: '/identities'
    })
    .state('organization.settings.identityproviders.list', {
      url: '/',
      component: 'identityProviders',
      resolve: {
        target: () => 'ORGANIZATION',
        targetId: () => 'DEFAULT',
        identityProviders: (IdentityProviderService: IdentityProviderService) =>
          IdentityProviderService.list().then(response => response),
        identities: (OrganizationService: OrganizationService) =>
          OrganizationService.listOrganizationIdentities().then(response => response.data),
        settings: (ConsoleSettingsService: ConsoleSettingsService) =>
          ConsoleSettingsService.get().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-identityproviders'
        },
        perms: {
          only: ['organization-identity_provider-r']
        }
      }
    })
    .state('organization.settings.identityproviders.new', {
      url: '/new?:type',
      component: 'identityProvider',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-identityprovider'
        },
        perms: {
          only: ['organization-identity_provider-c']
        }
      }
    })
    .state('organization.settings.identityproviders.identityprovider', {
      url: '/:id',
      component: 'identityProvider',
      resolve: {
        identityProvider: (IdentityProviderService: IdentityProviderService, $stateParams) =>
          IdentityProviderService.get($stateParams.id).then(response => response),

        groups: (GroupService: GroupService) =>
          GroupService.list().then(response => response.data),

        environmentRoles: (RoleService: RoleService) =>
          RoleService.list('ENVIRONMENT').then((roles) =>
            roles
          ),

        organizationRoles: (RoleService: RoleService) =>
          RoleService.list('ORGANIZATION').then((roles) =>
            roles
          ),

        environments: (EnvironmentService: EnvironmentService) =>
          EnvironmentService.list().then((response) =>
            response.data
          ),
      },
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-identityprovider'
        },
        perms: {
          only: ['organization-identity_provider-r', 'organization-identity_provider-u', 'organization-identity_provider-d']
        }
      }
    })
    .state('organization.settings.notificationTemplates', {
      url: '/notification-templates',
      component: 'notificationTemplatesComponent',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-notification-templates'
        },
        perms: {
          only: ['organization-notification_templates-r']
        }
      },
      resolve: {
        notificationTemplates:
          (NotificationTemplatesService: NotificationTemplatesService) => NotificationTemplatesService.getNotificationTemplates()
            .then(response => response.data),
      }
    })
    .state('organization.settings.notificationTemplate', {
      url: '/notification-templates/:scope/:hook',
      component: 'notificationTemplateComponent',
      data: {
        menu: null,
        docs: {
          page: 'organization-configuration-notification-template'
        },
        perms: {
          only: ['organization-notification_templates-r']
        }
      },
      resolve: {
        notifTemplates:
          (NotificationTemplatesService: NotificationTemplatesService, $stateParams) => {
            if ($stateParams.scope.toUpperCase() === 'TEMPLATES_TO_INCLUDE') {
              return NotificationTemplatesService.getNotificationTemplates('', $stateParams.scope)
                .then(response => response.data);
            } else {
              return NotificationTemplatesService.getNotificationTemplates($stateParams.hook, $stateParams.scope)
                .then(response => response.data);
            }
          }
      }
    })
  ;
}
