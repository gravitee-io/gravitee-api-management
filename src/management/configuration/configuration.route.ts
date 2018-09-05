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
import ViewService from '../../services/view.service';
import TenantService from '../../services/tenant.service';
import TagService from '../../services/tag.service';
import PortalPagesService from '../../services/portalPages.service';
import MetadataService from "../../services/metadata.service";
import RoleService from "../../services/role.service";
import GroupService from "../../services/group.service";
import {HookScope} from "../../entities/hookScope";
import NotificationSettingsService from "../../services/notificationSettings.service";
import TopApiService from "../../services/top-api.service";
import UserService from "../../services/user.service";
import ApiService from "../../services/api.service";
import _ = require('lodash');

export default configurationRouterConfig;

function configurationRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.settings', {
      url: '/settings',
      component: 'settings',
      data: {
        menu: {
          label: 'Settings',
          icon: 'settings',
          firstLevel: true,
          order: 50
        },
        perms: {
          only: [
            //hack only read permissions is necessary but READ is also allowed for API_PUBLISHER
            'portal-view-r', 'portal-metadata-r', 'portal-top_apis-r', 'management-group-r',
            'management-tag-c', 'management-tenant-c', 'management-group-c', 'management-role-c', 'portal-documentation-c',
            'management-tag-u', 'management-tenant-u', 'management-group-u', 'management-role-u', 'portal-documentation-u',
            'management-tag-d', 'management-tenant-d', 'management-group-d', 'management-role-d', 'portal-documentation-d'
          ]
        }
      }
    })
    .state('management.settings.views', {
      url: '/views',
      component: 'views',
      resolve: {
        views: (ViewService: ViewService) => ViewService.list(true).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-views'
        },
        perms: {
          only: ['portal-view-r']
        }
      }
    })
    .state('management.settings.viewnew', {
      url: '/views/new',
      component: 'view',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-views'
        },
        perms: {
          only: ['portal-view-c']
        }
      }
    })
    .state('management.settings.view', {
      url: '/views/:viewId',
      component: 'view',
      resolve: {
        view: (ViewService: ViewService, $stateParams) => ViewService.get($stateParams.viewId).then(response => response.data),
        viewApis: (ApiService: ApiService, $stateParams) => ApiService.list($stateParams.viewId).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-views'
        },
        perms: {
          only: ['portal-view-u', 'portal-view-d']
        }
      }
    })
    .state('management.settings.tags', {
      url: '/tags',
      component: 'tags',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-sharding-tags'
        },
        perms: {
          only: ['management-tag-r']
        }
      }
    })
    .state('management.settings.tenants', {
      url: '/tenants',
      component: 'tenants',
      resolve: {
        tenants: (TenantService: TenantService) => TenantService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-tenants'
        },
        perms: {
          only: ['management-tenant-r']
        }
      }
    })
    .state('management.settings.groups', {
      url: '/groups',
      component: 'groups',
      resolve: {
        groups: (GroupService: GroupService) =>
          GroupService.list().then(response =>
            _.filter(response.data, 'manageable'))
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-groups'
        },
        perms: {
          only: ['management-group-r']
        }
      }
    })
    .state('management.settings.group', {
      url: '/groups/:groupId',
      component: 'group',
      resolve: {
        group: (GroupService: GroupService, $stateParams) =>
          GroupService.get($stateParams.groupId).then(response =>
            response.data
          ),
        members: (GroupService: GroupService, $stateParams) =>
          GroupService.getMembers($stateParams.groupId).then(response =>
            response.data
          ),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list("API").then( (roles) =>
            [{"scope":"API", "name": "", "system":false}].concat(roles)
          ),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list("APPLICATION").then( (roles) =>
            [{"scope":"APPLICATION", "name": "", "system":false}].concat(roles)
          )
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-group'
        },
        perms: {
          only: ['management-group-r']
        }
      }
    })
    .state('management.settings.pages', {
      url: '/pages',
      component: 'portalPages',
      resolve: {
        pages: (PortalPagesService: PortalPagesService) => PortalPagesService.list().then(response => response.data),
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then(response => {
            return response.data;
          });
        },
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['portal-documentation-r']
        }
      }
    })
    .state('management.settings.pages.new', {
      url: '/new',
      template: require('./pages/page/page.html'),
      controller: 'NewPageController',
      controllerAs: 'pageCtrl',
      data: {
        menu: null,
        perms: {
          only: ['portal-documentation-c']
        }},
      params: {
        type: {
          type: 'string',
          value: '',
          squash: false
        }
      }
    })
    .state('management.settings.pages.page', {
      url: '/:pageId',
      template: require('./pages/page/page.html'),
      controller: 'NewPageController',
      controllerAs: 'pageCtrl',
      data: {menu: null}
    })
    .state('management.settings.metadata', {
      url: '/metadata',
      component: 'metadata',
      resolve: {
        metadata: (MetadataService: MetadataService) => MetadataService.list().then(response => response.data),
        metadataFormats: (MetadataService: MetadataService) => MetadataService.listFormats()
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-metadata'
        },
        perms: {
          only: ['portal-metadata-r']
        }
      }
    })
    .state('management.settings.roles', {
      url: '/roles',
      component: 'roles',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes(),
        managementRoles: (RoleService: RoleService) => RoleService.list("MANAGEMENT"),
        portalRoles: (RoleService: RoleService) => RoleService.list("PORTAL"),
        apiRoles: (RoleService: RoleService) => RoleService.list("API"),
        applicationRoles: (RoleService: RoleService) => RoleService.list("APPLICATION")
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-r']
        }
      },
      params: {
        roleScope: {
          type: 'string',
          value: 'MANAGEMENT',
          squash: false
        }
      }
    })
    .state('management.settings.rolenew', {
      url: '/role/:roleScope/new',
      component: 'role',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-c']
        }
      }
    })
    .state('management.settings.roleedit', {
      url: '/role/:roleScope/:role',
      component: 'role',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-u']
        }
      }
    })
    .state('management.settings.rolemembers', {
      url: '/role/:roleScope/:role/members',
      component: 'roleMembers',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-u']
        }
      },
      resolve: {
        members: (RoleService: RoleService, $stateParams) =>
          RoleService.listUsers($stateParams.roleScope, $stateParams.role).then( (response) =>
            response
        )
      }
    })
    .state('management.settings.notifications', {
      url: '/notifications',
      component: 'notificationSettingsComponent',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-notifications'
        },
        perms: {
          only: ['management-notification-r']
        }
      },
      resolve: {
        resolvedHookScope: () => HookScope.PORTAL,
        resolvedHooks:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getHooks(HookScope.PORTAL).then( (response) =>
              response.data
            ),
        resolvedNotifiers:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getNotifiers(HookScope.PORTAL, null).then( (response) =>
              response.data
            ),
        resolvedNotificationSettings:
          (NotificationSettingsService: NotificationSettingsService) =>
            NotificationSettingsService.getNotificationSettings(HookScope.PORTAL, null).then( (response) =>
              response.data
            )
      }
     })
    .state('management.settings.top-apis', {
      url: '/top-apis',
      component: 'topApis',
      resolve: {
        topApis: (TopApiService: TopApiService) => TopApiService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-top_apis'
        },
        perms: {
          only: ['portal-top_apis-r']
        }
      }
    })
    .state('management.settings.users', {
      url: '/users',
      component: 'users',
      resolve: {
        usersPage: (UserService: UserService) => UserService.list(undefined).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-users'
        },
        perms: {
          only: ['management-user-c', 'management-user-r', 'management-user-u', 'management-user-d']
        }
      }
    })
    .state('management.settings.user', {
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
        managementRoles: (RoleService: RoleService) =>
          RoleService.list("MANAGEMENT").then( (roles) =>
            roles
          ),
        portalRoles: (RoleService: RoleService) =>
          RoleService.list("PORTAL").then( (roles) =>
            roles
          ),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list("API").then( (roles) =>
            [{"scope":"API", "name": "", "system":false}].concat(roles)
          ),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list("APPLICATION").then( (roles) =>
            [{"scope":"APPLICATION", "name": "", "system":false}].concat(roles)
          )
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-user'
        },
        perms: {
          only: ['management-user-c', 'management-user-r', 'management-user-u', 'management-user-d']
        }
      }
    })
    .state('management.settings.portal', {
      url: '/portal',
      component: 'portalSettings',
      resolve: {
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal'
        },
        perms: {
          only: ['portal-settings-r']
        }
      }
    });
}
