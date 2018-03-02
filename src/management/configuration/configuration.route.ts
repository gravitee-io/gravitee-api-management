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
import SidenavService from '../../components/sidenav/sidenav.service';
import PortalPagesService from '../../services/portalPages.service';
import MetadataService from "../../services/metadata.service";
import RoleService from "../../services/role.service";
import GroupService from "../../services/group.service";
import {HookScope} from "../../entities/hookScope";
import NotificationSettingsService from "../../services/notificationSettings.service";
import TopApiService from "../../services/top-api.service";

export default configurationRouterConfig;

function configurationRouterConfig($stateProvider: ng.ui.IStateProvider) {
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
            'portal-view-r', 'portal-metadata-r', 'portal-top_apis-r',
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
      template: require('./groups/groups.html'),
      controller: 'GroupsController',
      controllerAs: 'groupsCtrl',
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
      url: '/roles?roleScope',
      component: 'roles',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
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
    .state('management.settings.role', {
      abstract: true,
      url: '/role?roleScope',
      controller: 'RoleSaveController',
      controllerAs: '$ctrl',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-r']
        }
      }
    })
    .state('management.settings.role.new', {
      url: '/new',
      template: require('./roles/role/save/role.save.html'),
      data: {
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-c']
        }
      }
    })
    .state('management.settings.role.edit', {
      url: '/edit?role',
      template: require('./roles/role/save/role.save.html'),
      data: {
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['management-role-u']
        }
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
    });
}
