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

export default configurationRouterConfig;

function configurationRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management.configuration', {
      abstract: true,
      url: '/configuration'
    })
    .state('management.configuration.admin', {
      url: '/admin',
      controller: function ($state, SidenavService: SidenavService) {
        SidenavService.setCurrentResource('CONFIGURATION');
        if ('management.configuration.admin' === $state.current.name) {
          $state.go('management.configuration.admin.views');
        }
      },
      template: '<div ui-view></div>',
      data: {
        menu: {
          label: 'Configuration',
          icon: 'settings',
          firstLevel: true,
          order: 50
        },
        roles: ['ADMIN']
      }
    })
    .state('management.configuration.admin.views', {
      url: '/views',
      component: 'views',
      resolve: {
        views: (ViewService: ViewService) => ViewService.list().then(response => response.data)
      },
      data: {
        menu: {
          label: 'Views',
          icon: 'view_module'
        },
        roles: ['ADMIN']
      }
    })
    .state('management.configuration.admin.tags', {
      url: '/tags',
      component: 'tags',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: {
          label: 'Sharding tags',
          icon: 'label'
        },
        roles: ['ADMIN']
      }
    })
    .state('management.configuration.admin.tenants', {
      url: '/tenants',
      component: 'tenants',
      resolve: {
        tenants: (TenantService: TenantService) => TenantService.list().then(response => response.data)
      },
      data: {
        menu: {
          label: 'Tenants',
          icon: 'shuffle'
        },
        roles: ['ADMIN']
      }
    })
    .state('management.configuration.admin.groups', {
      url: '/groups',
      template: require('./groups/groups.html'),
      controller: 'GroupsController',
      controllerAs: 'groupsCtrl',
      data: {
        menu: {
          label: 'Groups',
          icon: 'group_work'
        },
        roles: ['ADMIN']
      }
    })
    .state('management.configuration.admin.pages', {
      url: '/pages',
      component: 'portalPages',
      resolve: {
        pages: (PortalPagesService: PortalPagesService) => PortalPagesService.list().then(response => response.data)
      },
      data: {
        menu: {
          label: 'Portal pages',
          icon: 'insert_drive_file'
        },
        roles: ['ADMIN']
      }
    })
    .state('management.configuration.admin.pages.new', {
      url: '/new',
      template: require('./pages/page/page.html'),
      controller: 'NewPageController',
      controllerAs: 'pageCtrl',
      data: {menu: null},
      params: {
        type: {
          type: 'string',
          value: '',
          squash: false
        }
      }
    })
    .state('management.configuration.admin.pages.page', {
      url: '/:pageId',
      template: require('./pages/page/page.html'),
      controller: 'NewPageController',
      controllerAs: 'pageCtrl',
      data: {menu: null}
    })
    .state('management.configuration.admin.metadata', {
      url: '/metadata',
      component: 'metadata',
      resolve: {
        metadata: (MetadataService: MetadataService) => MetadataService.list().then(response => response.data),
        metadataFormats: (MetadataService: MetadataService) => MetadataService.listFormats()
      },
      data: {
        menu: {
          label: 'Metadata',
          icon: 'description'
        },
        roles: ['ADMIN']
      }
    });
}
