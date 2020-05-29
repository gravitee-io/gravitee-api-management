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
import CategoryService from '../../services/category.service';
import TenantService from '../../services/tenant.service';
import TagService from '../../services/tag.service';
import MetadataService from '../../services/metadata.service';
import RoleService from '../../services/role.service';
import GroupService from '../../services/group.service';
import TopApiService from '../../services/top-api.service';
import UserService from '../../services/user.service';
import ApiService from '../../services/api.service';
import DictionaryService from '../../services/dictionary.service';
import ApiHeaderService from '../../services/apiHeader.service';
import IdentityProviderService from '../../services/identityProvider.service';
import DocumentationService, {DocumentationQuery} from '../../services/documentation.service';
import FetcherService from '../../services/fetcher.service';
import {StateParams} from '@uirouter/core';
import EntrypointService from '../../services/entrypoint.service';
import ClientRegistrationProviderService from '../../services/clientRegistrationProvider.service';
import _ = require('lodash');
import QualityRuleService from '../../services/qualityRule.service';
import DashboardService from '../../services/dashboard.service';

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
            // hack only read permissions is necessary but READ is also allowed for API_PUBLISHER
            'environment-category-r', 'environment-metadata-r', 'environment-top_apis-r', 'environment-group-r',
            'environment-tag-c', 'environment-tenant-c', 'environment-group-c', 'organization-role-c', 'environment-documentation-c',
            'environment-tag-u', 'environment-tenant-u', 'environment-group-u', 'organization-role-u', 'environment-documentation-u',
            'environment-tag-d', 'environment-tenant-d', 'environment-group-d', 'organization-role-d', 'environment-documentation-d',
            'environment-api_header-r'
          ]
        }
      }
    })
    .state('management.settings.categories', {
      url: '/categories',
      component: 'categories',
      resolve: {
        categories: (CategoryService: CategoryService) => CategoryService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-categories'
        },
        perms: {
          only: ['environment-category-r']
        }
      }
    })
    .state('management.settings.categorynew', {
      url: '/categories/new',
      component: 'category',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-categories'
        },
        perms: {
          only: ['environment-category-c']
        }
      }
    })
    .state('management.settings.category', {
      url: '/categories/:categoryId',
      component: 'category',
      resolve: {
        category: (CategoryService: CategoryService, $stateParams) => CategoryService.get($stateParams.categoryId).then(response => response.data),
        categoryApis: (ApiService: ApiService, $stateParams) => ApiService.list($stateParams.categoryId).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-categories'
        },
        perms: {
          only: ['environment-category-u', 'environment-category-d']
        }
      }
    })
    .state('management.settings.tags', {
      url: '/tags',
      component: 'tags',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data),
        entrypoints: (EntrypointService: EntrypointService) => EntrypointService.list().then(response => response.data),
        groups: (GroupService: GroupService) => GroupService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-sharding-tags'
        },
        perms: {
          only: ['environment-tag-r']
        }
      }
    })
    .state('management.settings.newEntrypoint', {
      url: '/tags/entrypoint/new',
      component: 'entrypoint',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-entrypoint'
        },
        perms: {
          only: ['environment-entrypoint-c']
        }
      }
    })
    .state('management.settings.entrypoint', {
      url: '/tags/entrypoint/:entrypointId',
      component: 'entrypoint',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-entrypoint'
        },
        perms: {
          only: ['environment-entrypoint-u']
        }
      }
    })
    .state('management.settings.tag', {
      url: '/tags/:tagId',
      component: 'tag',
      resolve: {
        groups: (GroupService: GroupService) => GroupService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-sharding-tag'
        },
        perms: {
          only: ['environment-tag-r', 'environment-tag-c', 'environment-tag-u']
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
          only: ['environment-tenant-r']
        }
      }
    })
    .state('management.settings.groups', {
      abstract: true,
      url: '/groups'
    })
    .state('management.settings.groups.list', {
      url: '/',
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
          only: ['environment-group-r']
        }
      }
    })
    .state('management.settings.groups.create', {
      url: '/new',
      component: 'group',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-group'
        },
        perms: {
          only: ['environment-group-r']
        }
      }
    })
    .state('management.settings.groups.group', {
      url: '/:groupId',
      component: 'group',
      resolve: {
        group: (GroupService: GroupService, $stateParams) =>
          GroupService.get($stateParams.groupId).then(response => response.data),
        members: (GroupService: GroupService, $stateParams) =>
          GroupService.getMembers($stateParams.groupId).then(response => response.data),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list('API').then( (roles) =>
            [{'scope': 'API', 'name': '', 'system': false}].concat(roles)
          ),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list('APPLICATION').then( (roles) =>
            [{'scope': 'APPLICATION', 'name': '', 'system': false}].concat(roles)
          ),
        invitations: (GroupService: GroupService, $stateParams) =>
          GroupService.getInvitations($stateParams.groupId).then(response => response.data),
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-group'
        },
        perms: {
          only: ['environment-group-r']
        }
      }
    })
    .state('management.settings.documentation', {
      url: '/pages?:parent',
      component: 'documentationManagement',
      resolve: {
        pages: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          if ($stateParams.parent && '' !== $stateParams.parent) {
            q.parent = $stateParams.parent;
          } else {
            q.root = true;
          }
          return DocumentationService.search(q)
            .then(response => response.data);
        },
        folders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q)
            .then(response => response.data);
        },
        systemFolders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q)
            .then(response => response.data);
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['environment-documentation-r']
        }
      },
      params: {
        parent: {
          type: 'string',
          value: '',
          squash: false
        }
      }
    })
    .state('management.settings.newdocumentation', {
      url: '/pages/new?type&:parent',
      component: 'newPage',
      resolve: {
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list().then(response => {
            return response.data;
          });
        },
        folders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q)
            .then(response => response.data);
        },
        systemFolders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q)
            .then(response => response.data);
        },
        pageResources: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            const q = new DocumentationQuery();
            return DocumentationService.search(q).then(response => response. data);
          }
        },
        categoryResources: (CategoryService: CategoryService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            return CategoryService.list().then(response => response.data);
          }
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['environment-documentation-c']
        }
      },
      params: {
        type: {
          type: 'string',
          value: '',
          squash: false
        },
        parent: {
          type: 'string',
          value: '',
          squash: false
        }
      }
    })
    .state('management.settings.importdocumentation', {
      url: '/pages/import',
      component: 'importPages',
      resolve: {
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list(true).then(response => {
            return response.data;
          });
        },
        resolvedRootPage: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'ROOT';
          return DocumentationService.search(q)
            .then(response => response.data && response.data.length > 0 ? response.data[0] : null);
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['environment-documentation-c']
        }
      }
    })
    .state('management.settings.editdocumentation', {
      url: '/pages/:pageId?:tab&type',
      component: 'editPage',
      resolve: {
        resolvedPage: (DocumentationService: DocumentationService, $stateParams: StateParams) =>
          DocumentationService.get(null, $stateParams.pageId).then(response => response.data),
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then(response => {
            return response.data;
          });
        },
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list().then(response => {
            return response.data;
          });
        },
        folders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q)
            .then(response => response.data);
        },
        systemFolders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q)
            .then(response => response.data);
        },
        pageResources: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            const q = new DocumentationQuery();
            return DocumentationService.search(q).then(response => response. data);
          }
        },
        categoryResources: (CategoryService: CategoryService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            return CategoryService.list().then(response => response.data);
          }
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['environment-documentation-u']
        }
      },
      params: {
        pageId: {
          type: 'string',
          value: '',
          squash: false
        }
      }
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
          only: ['environment-metadata-r']
        }
      }
    })
    .state('management.settings.roles', {
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
          page: 'management-configuration-roles'
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
    .state('management.settings.rolenew', {
      url: '/role/:roleScope/new',
      component: 'role',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['organization-role-c']
        }
      }
    })
    .state('management.settings.roleedit', {
      url: '/role/:roleScope/:role',
      component: 'role',
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-roles'
        },
        perms: {
          only: ['organization-role-u']
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
          only: ['organization-role-u']
        }
      },
      resolve: {
        members: (RoleService: RoleService, $stateParams) =>
          RoleService.listUsers($stateParams.roleScope, $stateParams.role).then( (response) => response
        )
      }
    })
    .state('management.settings.theme', {
      url: '/theme',
      component: 'theme',
      resolve: {
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-theme'
        },
        perms: {
          only: ['environment-theme-r']
        }
      },
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
          only: ['environment-top_apis-r']
        }
      }
    })
    .state('management.settings.users', {
      url: '/users?q&page',
      component: 'users',
      resolve: {
        usersPage: (UserService: UserService, $state, $stateParams) =>
          UserService.list($stateParams.q, $stateParams.page).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-users'
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
        organizationRoles: (RoleService: RoleService) =>
          RoleService.list('ORGANIZATION').then( (roles) =>
            roles
          ),
        environmentRoles: (RoleService: RoleService) =>
          RoleService.list('ENVIRONMENT').then( (roles) =>
            roles
          ),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list('API').then( (roles) =>
            [{'scope': 'API', 'name': '', 'system': false}].concat(roles)
          ),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list('APPLICATION').then( (roles) =>
            [{'scope': 'APPLICATION', 'name': '', 'system': false}].concat(roles)
          )
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-user'
        },
        perms: {
          only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d']
        }
      }
    })
    .state('management.settings.newuser', {
      url: '/users/new',
      component: 'newUser',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-create-user'
        },
        perms: {
          only: ['organization-user-c']
        }
      }
    })
    .state('management.settings.portal', {
      url: '/portal',
      component: 'portalSettings',
      resolve: {
        tags: (TagService: TagService) => TagService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal'
        },
        perms: {
          only: ['environment-settings-r']
        }
      }
    })
    .state('management.settings.dictionaries', {
      abstract: true,
      url: '/dictionaries'
    })
    .state('management.settings.dictionaries.list', {
      url: '/',
      component: 'dictionaries',
      resolve: {
        dictionaries: (DictionaryService: DictionaryService) =>
          DictionaryService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dictionaries'
        },
        perms: {
          only: ['environment-dictionary-r']
        }
      }
    })
    .state('management.settings.dictionaries.new', {
      url: '/new',
      component: 'dictionary',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dictionary'
        },
        perms: {
          only: ['environment-dictionary-c']
        }
      }
    })
    .state('management.settings.dictionaries.dictionary', {
      url: '/:dictionaryId',
      component: 'dictionary',
      resolve: {
        dictionary: (DictionaryService: DictionaryService, $stateParams) =>
          DictionaryService.get($stateParams.dictionaryId).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dictionary'
        },
        perms: {
          only: ['environment-dictionary-c', 'environment-dictionary-r', 'environment-dictionary-u', 'environment-dictionary-d']
        }
      }
    })
    .state('management.settings.analytics', {
      url: '/analytics',
      component: 'analyticsSettings',
      resolve: {
        dashboardsPlatform: (DashboardService: DashboardService) => DashboardService.list('PLATFORM').then(response => response.data),
        dashboardsApi: (DashboardService: DashboardService) => DashboardService.list('API').then(response => response.data),
        dashboardsApplication: (DashboardService: DashboardService) => DashboardService.list('APPLICATION').then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-analytics'
        },
        perms: {
          only: ['environment-settings-r']
        }
      }
    })
    .state('management.settings.dashboardnew', {
      url: '/analytics/dashboard/:type/new',
      component: 'dashboard',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dashboard'
        },
        perms: {
          only: ['environment-dashboard-c']
        }
      }
    })
    .state('management.settings.dashboard', {
      url: '/analytics/dashboard/:type/:dashboardId',
      component: 'dashboard',
      resolve: {
        dashboard: (DashboardService: DashboardService, $stateParams) => DashboardService.get($stateParams.dashboardId).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dashboard'
        },
        perms: {
          only: ['environment-dashboard-u']
        }
      }
    })
    .state('management.settings.apiPortalHeader', {
      url: '/apiportalheader',
      component: 'configApiPortalHeader',
      resolve: {
        apiPortalHeaders: (ApiHeaderService: ApiHeaderService) =>
          ApiHeaderService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiportalheader'
        },
        perms: {
          only: ['environment-api_header-r']
        }
      }
    })
    .state('management.settings.apiQuality', {
      url: '/apiquality',
      component: 'configApiQuality',
      resolve: {
        qualityRules: (QualityRuleService: QualityRuleService) => QualityRuleService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiquality'
        },
        perms: {
          only: ['environment-settings-r']
        }
      }
    })
    .state('management.settings.qualityRulenew', {
      url: '/apiquality/new',
      component: 'qualityRule',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiquality'
        },
        perms: {
          only: ['environment-quality_rule-c']
        }
      }
    })
    .state('management.settings.qualityRule', {
      url: '/apiquality/:qualityRuleId',
      component: 'qualityRule',
      resolve: {
        qualityRule: (QualityRuleService: QualityRuleService, $stateParams) => QualityRuleService.get($stateParams.qualityRuleId).then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiquality'
        },
        perms: {
          only: ['environment-quality_rule-u']
        }
      }
    })
    .state('management.settings.identityproviders', {
      abstract: true,
      url: '/identities'
    })
    .state('management.settings.identityproviders.list', {
      url: '/',
      component: 'identityProviders',
      resolve: {
        identityProviders: (IdentityProviderService: IdentityProviderService) =>
          IdentityProviderService.list().then(response => response)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-identityproviders'
        },
        perms: {
          only: ['environment-identity_provider-r']
        }
      }
    })
    .state('management.settings.identityproviders.new', {
      url: '/new?:type',
      component: 'identityProvider',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-identityprovider'
        },
        perms: {
          only: ['environment-identity_provider-c']
        }
      }
    })
    .state('management.settings.identityproviders.identityprovider', {
      url: '/:id',
      component: 'identityProvider',
      resolve: {
        identityProvider: (IdentityProviderService: IdentityProviderService, $stateParams) =>
          IdentityProviderService.get($stateParams.id).then(response => response),

        groups: (GroupService: GroupService) =>
          GroupService.list().then(response => response.data),

        environmentRoles: (RoleService: RoleService) =>
          RoleService.list('ENVIRONMENT').then( (roles) =>
            roles
          ),

        organizationRoles: (RoleService: RoleService) =>
          RoleService.list('ORGANIZATION').then( (roles) =>
            roles
          )
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-identityprovider'
        },
        perms: {
          only: ['environment-identity_provider-r', 'environment-identity_provider-u', 'environment-identity_provider-d']
        }
      }
    })
    .state('management.settings.api_logging', {
      url: '/api_logging',
      component: 'apiLogging',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apilogging'
        },
        perms: {
          only: ['environment-settings-r']
        }
      }
    })
    .state('management.settings.clientregistrationproviders', {
      abstract: true,
      url: '/client-registration'
    })
    .state('management.settings.clientregistrationproviders.list', {
      url: '/',
      component: 'clientRegistrationProviders',
      resolve: {
        clientRegistrationProviders: (ClientRegistrationProviderService: ClientRegistrationProviderService) =>
          ClientRegistrationProviderService.list().then(response => response)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-client-registration-providers'
        },
        perms: {
          only: ['environment-client_registration_provider-r']
        }
      }
    })
    .state('management.settings.clientregistrationproviders.create', {
      url: '/new',
      component: 'clientRegistrationProvider',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-client-registration-provider'
        },
        perms: {
          only: ['environment-client_registration_provider-c']
        }
      }
    })
    .state('management.settings.clientregistrationproviders.clientregistrationprovider', {
      url: '/:id',
      component: 'clientRegistrationProvider',
      resolve: {
        clientRegistrationProvider: (ClientRegistrationProviderService: ClientRegistrationProviderService, $stateParams) =>
          ClientRegistrationProviderService.get($stateParams.id).then(response => response),
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-client-registration-provider'
        },
        perms: {
          only: ['environment-client_registration_provider-r', 'environment-client_registration_provider-u', 'environment-client_registration_provider-d']
        }
      }
    });
}
