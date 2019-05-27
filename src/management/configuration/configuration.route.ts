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
import MetadataService from "../../services/metadata.service";
import RoleService from "../../services/role.service";
import GroupService from "../../services/group.service";
import TopApiService from "../../services/top-api.service";
import UserService from "../../services/user.service";
import ApiService from "../../services/api.service";
import DictionaryService from "../../services/dictionary.service";
import ApiHeaderService from "../../services/apiHeader.service";
import IdentityProviderService from "../../services/identityProvider.service";
import DocumentationService, {DocumentationQuery} from "../../services/documentation.service";
import FetcherService from "../../services/fetcher.service";
import {StateParams} from '@uirouter/core';
import EntrypointService from "../../services/entrypoint.service";
import ClientRegistrationProviderService from "../../services/clientRegistrationProvider.service";
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
            'management-tag-d', 'management-tenant-d', 'management-group-d', 'management-role-d', 'portal-documentation-d',
            'portal-api_header-r'
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
          only: ['management-tag-r']
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
          only: ['management-entrypoint-c']
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
          only: ['management-entrypoint-u']
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
          only: ['management-tag-r', 'management-tag-c', 'management-tag-u']
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
          GroupService.get($stateParams.groupId).then(response => response.data),
        members: (GroupService: GroupService, $stateParams) =>
          GroupService.getMembers($stateParams.groupId).then(response => response.data),
        apiRoles: (RoleService: RoleService) =>
          RoleService.list("API").then( (roles) =>
            [{"scope":"API", "name": "", "system":false}].concat(roles)
          ),
        applicationRoles: (RoleService: RoleService) =>
          RoleService.list("APPLICATION").then( (roles) =>
            [{"scope":"APPLICATION", "name": "", "system":false}].concat(roles)
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
          only: ['management-group-r']
        }
      }
    })
    .state('management.settings.documentation', {
      url: '/pages?:parent',
      component: 'documentationManagement',
      resolve: {
        pages: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          if ($stateParams.parent && ""!==$stateParams.parent) {
            q.parent = $stateParams.parent;
          } else {
            q.root = true;
          }
          return DocumentationService.search(q)
            .then(response => response.data)
        },
        folders: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = "FOLDER";
          return DocumentationService.search(q)
            .then(response => response.data)
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['portal-documentation-r']
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
          })
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['portal-documentation-c']
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
          })
        },
        resolvedRootPage: (DocumentationService: DocumentationService) => {
          const q = new DocumentationQuery();
          q.type = "ROOT";
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
          only: ['portal-documentation-c']
        }
      }
    })
    .state('management.settings.editdocumentation', {
      url: '/pages/:pageId?:tab',
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
          })
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages'
        },
        perms: {
          only: ['portal-documentation-u']
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
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
      },
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
      resolve: {
        roleScopes: (RoleService: RoleService) => RoleService.listScopes()
      },
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
          RoleService.listUsers($stateParams.roleScope, $stateParams.role).then( (response) => response
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
      url: '/users?q',
      component: 'users',
      resolve: {
        usersPage: (UserService: UserService, $stateParams) =>
          UserService.list($stateParams.q).then(response => response.data)
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
    .state('management.settings.newuser', {
      url: '/users/new',
      component: 'newUser',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-create-user'
        },
        perms: {
          only: ['management-user-c']
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
          only: ['portal-settings-r']
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
          only: ['management-dictionary-r']
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
          only: ['management-dictionary-c']
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
          only: ['management-dictionary-c', 'management-dictionary-r', 'management-dictionary-u', 'management-dictionary-d']
        }
      }
    })
    .state('management.settings.analytics', {
      url: '/analytics',
      component: 'analyticsSettings',
      resolve: {
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-analytics'
        },
        perms: {
          only: ['portal-settings-r']
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
          only: ['portal-api_header-r']
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
          only: ['portal-identity_provider-r']
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
          only: ['portal-identity_provider-c']
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

        portalRoles: (RoleService: RoleService) =>
          RoleService.list('PORTAL').then( (roles) =>
            roles
          ),

        managementRoles: (RoleService: RoleService) =>
          RoleService.list('MANAGEMENT').then( (roles) =>
            roles
          )
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-identityprovider'
        },
        perms: {
          only: ['portal-identity_provider-r', 'portal-identity_provider-u', 'portal-identity_provider-d']
        }
      }
    })
    .state('management.settings.api_logging', {
      url: '/api_logging',
      component: 'apiLogging',
      resolve: {
        dictionaries: (DictionaryService: DictionaryService) =>
          DictionaryService.list().then(response => response.data)
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apilogging'
        },
        perms: {
          only: ['portal-settings-r']
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
          only: ['portal-client_registration_provider-r']
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
          only: ['portal-client_registration_provider-c']
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
          only: ['portal-client_registration_provider-r', 'portal-client_registration_provider-u', 'portal-client_registration_provider-d']
        }
      }
    })
}
