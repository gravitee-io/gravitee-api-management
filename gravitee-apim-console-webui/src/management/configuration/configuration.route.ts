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
import { StateParams } from '@uirouter/core';
import * as _ from 'lodash';
import { StateProvider } from '@uirouter/angularjs';

import { ApiService } from '../../services/api.service';
import ApiHeaderService from '../../services/apiHeader.service';
import CategoryService from '../../services/category.service';
import CustomUserFieldsService from '../../services/custom-user-fields.service';
import DashboardService from '../../services/dashboard.service';
import DictionaryService from '../../services/dictionary.service';
import { DocumentationQuery, DocumentationService } from '../../services/documentation.service';
import EnvironmentService from '../../services/environment.service';
import FetcherService from '../../services/fetcher.service';
import GroupService from '../../services/group.service';
import IdentityProviderService from '../../services/identityProvider.service';
import PortalSettingsService from '../../services/portalSettings.service';
import QualityRuleService from '../../services/qualityRule.service';
import RoleService from '../../services/role.service';
import TagService from '../../services/tag.service';
import TopApiService from '../../services/top-api.service';

export default configurationRouterConfig;

function configurationRouterConfig($stateProvider: StateProvider) {
  $stateProvider
    .state('management.settings', {
      abstract: true,
      url: '/settings',
      template: require('./settings.html'),
    })
    // Portal
    .state('management.settings.analytics', {
      abstract: true,
      url: '/analytics',
    })
    .state('management.settings.analytics.list', {
      url: '',
      component: 'analyticsSettings',
      resolve: {
        dashboardsPlatform: [
          'DashboardService',
          (DashboardService: DashboardService) => DashboardService.list('PLATFORM').then((response) => response.data),
        ],
        dashboardsApi: [
          'DashboardService',
          (DashboardService: DashboardService) => DashboardService.list('API').then((response) => response.data),
        ],
        dashboardsApplication: [
          'DashboardService',
          (DashboardService: DashboardService) => DashboardService.list('APPLICATION').then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-analytics',
        },
        perms: {
          only: ['environment-dashboard-r'],
          unauthorizedFallbackTo: 'management.settings.apiPortalHeader',
        },
      },
    })
    .state('management.settings.analytics.dashboardnew', {
      url: '/dashboard/:type/new',
      component: 'dashboard',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dashboard',
        },
        perms: {
          only: ['environment-dashboard-c'],
        },
      },
    })
    .state('management.settings.analytics.dashboard', {
      url: '/dashboard/:type/:dashboardId',
      component: 'dashboard',
      resolve: {
        dashboard: [
          'DashboardService',
          '$stateParams',
          (DashboardService: DashboardService, $stateParams) =>
            DashboardService.get($stateParams.dashboardId).then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dashboard',
        },
        perms: {
          only: ['environment-dashboard-u'],
        },
      },
    })
    .state('management.settings.apiPortalHeader', {
      url: '/apiportalheader',
      component: 'configApiPortalHeader',
      resolve: {
        apiPortalHeaders: [
          'ApiHeaderService',
          (ApiHeaderService: ApiHeaderService) => ApiHeaderService.list().then((response) => response.data),
        ],
        settings: [
          'PortalSettingsService',
          (PortalSettingsService: PortalSettingsService) => PortalSettingsService.get().then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiportalheader',
        },
        perms: {
          only: ['environment-api_header-r'],
          unauthorizedFallbackTo: 'management.settings.apiQuality.list',
        },
      },
    })
    .state('management.settings.apiQuality', {
      abstract: true,
      url: '/apiquality',
    })
    .state('management.settings.apiQuality.list', {
      url: '',
      component: 'configApiQuality',
      resolve: {
        qualityRules: [
          'QualityRuleService',
          (QualityRuleService: QualityRuleService) => QualityRuleService.list().then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiquality',
        },
        perms: {
          only: ['environment-quality_rule-r'],
          unauthorizedFallbackTo: 'management.settings.environment.identityproviders',
        },
      },
    })
    .state('management.settings.apiQuality.qualityRulenew', {
      url: '/new',
      component: 'qualityRule',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiquality',
        },
        perms: {
          only: ['environment-quality_rule-c'],
        },
      },
    })
    .state('management.settings.apiQuality.qualityRule', {
      url: '/:qualityRuleId',
      component: 'qualityRule',
      resolve: {
        qualityRule: [
          'QualityRuleService',
          '$stateParams',
          (QualityRuleService: QualityRuleService, $stateParams) =>
            QualityRuleService.get($stateParams.qualityRuleId).then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apiquality',
        },
        perms: {
          only: ['environment-quality_rule-u'],
        },
      },
    })
    .state('management.settings.environment', {
      abstract: true,
      url: '/environment',
    })
    .state('management.settings.environment.identityproviders', {
      url: '/identity-providers',
      component: 'identityProviders',
      resolve: {
        target: () => 'ENVIRONMENT',
        targetId: ['Constants', (Constants) => Constants.org.currentEnv.id],
        identityProviders: [
          'IdentityProviderService',
          (IdentityProviderService: IdentityProviderService) => IdentityProviderService.list().then((response) => response),
        ],
        identities: [
          'EnvironmentService',
          'Constants',
          (EnvironmentService: EnvironmentService, Constants) =>
            EnvironmentService.listEnvironmentIdentities(Constants.org.currentEnv.id).then((response) => response.data),
        ],
        settings: [
          'PortalSettingsService',
          (PortalSettingsService: PortalSettingsService) => PortalSettingsService.get().then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-identityproviders',
        },
        perms: {
          only: ['environment-identity_provider_activation-r'],
          unauthorizedFallbackTo: 'management.settings.categories.list',
        },
      },
    })
    .state('management.settings.categories', {
      abstract: true,
      url: '/categories',
    })
    .state('management.settings.categories.list', {
      url: '',
      component: 'categories',
      resolve: {
        categories: [
          'CategoryService',
          (CategoryService: CategoryService) => CategoryService.list(['total-apis']).then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-categories',
        },
        perms: {
          only: ['environment-category-r'],
          unauthorizedFallbackTo: 'management.settings.clientregistrationproviders.list',
        },
      },
    })
    .state('management.settings.categories.create', {
      url: '/new',
      component: 'category',
      resolve: {
        pages: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'MARKDOWN';
            q.published = true;
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-categories',
        },
        perms: {
          only: ['environment-category-c'],
        },
      },
    })
    .state('management.settings.categories.category', {
      url: '/:categoryId',
      component: 'category',
      resolve: {
        category: [
          'CategoryService',
          '$stateParams',
          (CategoryService: CategoryService, $stateParams) =>
            CategoryService.get($stateParams.categoryId).then((response) => response.data),
        ],
        categoryApis: [
          'ApiService',
          '$stateParams',
          (ApiService: ApiService, $stateParams) => ApiService.list($stateParams.categoryId).then((response) => response.data),
        ],
        pages: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'MARKDOWN';
            q.published = true;
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-categories',
        },
        perms: {
          only: ['environment-category-u', 'environment-category-d'],
        },
      },
    })
    .state('management.settings.clientregistrationproviders', {
      abstract: true,
      url: '/client-registration',
    })
    .state('management.settings.clientregistrationproviders.list', {
      url: '/',
      component: 'ngClientRegistrationProviders',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-client-registration-providers',
        },
        perms: {
          only: ['environment-client_registration_provider-r'],
          unauthorizedFallbackTo: 'management.settings.documentation.list',
        },
      },
    })
    .state('management.settings.clientregistrationproviders.create', {
      url: '/new',
      component: 'ngClientRegistrationProvider',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-client-registration-provider',
        },
        perms: {
          only: ['environment-client_registration_provider-c'],
        },
      },
    })
    .state('management.settings.clientregistrationproviders.clientregistrationprovider', {
      url: '/:id',
      component: 'ngClientRegistrationProvider',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-client-registration-provider',
        },
        perms: {
          only: [
            'environment-client_registration_provider-r',
            'environment-client_registration_provider-u',
            'environment-client_registration_provider-d',
          ],
        },
      },
    })
    .state('management.settings.documentation', {
      abstract: true,
      url: '/pages',
    })
    .state('management.settings.documentation.list', {
      url: '?:parent',
      component: 'documentationManagementAjs',
      resolve: {
        pages: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) => {
            const q = new DocumentationQuery();
            if ($stateParams.parent && '' !== $stateParams.parent) {
              q.parent = $stateParams.parent;
            } else {
              q.root = true;
            }
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
        folders: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'FOLDER';
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
        systemFolders: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'SYSTEM_FOLDER';
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages',
        },
        perms: {
          only: ['environment-documentation-c', 'environment-documentation-u', 'environment-documentation-d'],
          unauthorizedFallbackTo: 'management.settings.metadata',
        },
      },
      params: {
        parent: {
          type: 'string',
          value: '',
          squash: false,
        },
      },
    })
    .state('management.settings.documentation.new', {
      url: '/new?type&:parent',
      component: 'documentationNewPageAjs',
      resolve: {
        resolvedFetchers: [
          'FetcherService',
          (FetcherService: FetcherService) => {
            return FetcherService.list().then((response) => {
              return response.data;
            });
          },
        ],
        pagesToLink: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) => {
            if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'MARKDOWN_TEMPLATE') {
              const q = new DocumentationQuery();
              q.homepage = false;
              q.published = true;
              return DocumentationService.search(q).then((response) =>
                response.data.filter(
                  (page) =>
                    page.type.toUpperCase() === 'MARKDOWN' ||
                    page.type.toUpperCase() === 'SWAGGER' ||
                    page.type.toUpperCase() === 'ASCIIDOC' ||
                    page.type.toUpperCase() === 'ASYNCAPI',
                ),
              );
            }
          },
        ],
        folders: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'FOLDER';
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
        systemFolders: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'SYSTEM_FOLDER';
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
        pageResources: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) => {
            if ($stateParams.type === 'LINK') {
              const q = new DocumentationQuery();
              return DocumentationService.search(q).then((response) => response.data);
            }
          },
        ],
        categoryResources: [
          'CategoryService',
          '$stateParams',
          (CategoryService: CategoryService, $stateParams: StateParams) => {
            if ($stateParams.type === 'LINK') {
              return CategoryService.list().then((response) => response.data);
            }
          },
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages',
        },
        perms: {
          only: ['environment-documentation-c'],
        },
      },
      params: {
        type: {
          type: 'string',
          value: '',
          squash: false,
        },
        parent: {
          type: 'string',
          value: '',
          squash: false,
        },
      },
    })
    .state('management.settings.documentation.import', {
      url: '/import',
      component: 'documentationImportPagesAjs',
      resolve: {
        resolvedFetchers: [
          'FetcherService',
          (FetcherService: FetcherService) => {
            return FetcherService.list(true).then((response) => {
              return response.data;
            });
          },
        ],
        resolvedRootPage: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'ROOT';
            return DocumentationService.search(q).then((response) => (response.data && response.data.length > 0 ? response.data[0] : null));
          },
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages',
        },
        perms: {
          only: ['environment-documentation-c'],
        },
      },
    })
    .state('management.settings.documentation.edit', {
      url: '/:pageId?:tab&type',
      component: 'documentationEditPageAjs',
      resolve: {
        resolvedPage: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) =>
            DocumentationService.get(null, $stateParams.pageId).then((response) => response.data),
        ],
        resolvedGroups: [
          'GroupService',
          (GroupService: GroupService) => {
            return GroupService.list().then((response) => {
              return response.data;
            });
          },
        ],
        resolvedFetchers: [
          'FetcherService',
          (FetcherService: FetcherService) => {
            return FetcherService.list().then((response) => {
              return response.data;
            });
          },
        ],
        pagesToLink: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) => {
            if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'MARKDOWN_TEMPLATE') {
              const q = new DocumentationQuery();
              q.homepage = false;
              q.published = true;
              return DocumentationService.search(q).then((response) =>
                response.data.filter(
                  (page) =>
                    (page.type.toUpperCase() === 'MARKDOWN' ||
                      page.type.toUpperCase() === 'SWAGGER' ||
                      page.type.toUpperCase() === 'ASCIIDOC' ||
                      page.type.toUpperCase() === 'ASYNCAPI') &&
                    page.id !== $stateParams.pageId,
                ),
              );
            }
          },
        ],
        folders: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'FOLDER';
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
        systemFolders: [
          'DocumentationService',
          (DocumentationService: DocumentationService) => {
            const q = new DocumentationQuery();
            q.type = 'SYSTEM_FOLDER';
            return DocumentationService.search(q).then((response) => response.data);
          },
        ],
        pageResources: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) => {
            if ($stateParams.type === 'LINK') {
              const q = new DocumentationQuery();
              return DocumentationService.search(q).then((response) => response.data);
            }
          },
        ],
        categoryResources: [
          'CategoryService',
          '$stateParams',
          (CategoryService: CategoryService, $stateParams: StateParams) => {
            if ($stateParams.type === 'LINK') {
              return CategoryService.list().then((response) => response.data);
            }
          },
        ],
        attachedResources: [
          'DocumentationService',
          '$stateParams',
          (DocumentationService: DocumentationService, $stateParams: StateParams) => {
            if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'ASCIIDOC' || $stateParams.type === 'ASYNCAPI') {
              return DocumentationService.getMedia($stateParams.pageId, null).then((response) => response.data);
            }
          },
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-pages',
        },
        perms: {
          only: ['environment-documentation-u'],
        },
      },
      params: {
        pageId: {
          type: 'string',
          value: '',
          squash: false,
        },
      },
    })
    .state('management.settings.metadata', {
      url: '/metadata',
      component: 'ngEnvironmentMetadata',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-metadata',
        },
        perms: {
          only: ['environment-metadata-r'],
          unauthorizedFallbackTo: 'management.settings.portal',
        },
        useAngularMaterial: true,
      },
    })
    .state('management.settings.portal', {
      url: '/portal',
      component: 'portalSettings',
      resolve: {
        tags: ['TagService', (TagService: TagService) => TagService.list().then((response) => response.data)],
        settings: [
          'PortalSettingsService',
          (PortalSettingsService: PortalSettingsService) => PortalSettingsService.get().then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal',
        },
        perms: {
          only: ['environment-settings-r'],
          unauthorizedFallbackTo: 'management.settings.theme',
        },
      },
    })
    .state('management.settings.theme', {
      url: '/theme',
      component: 'theme',
      resolve: {},
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-portal-theme',
        },
        perms: {
          only: ['environment-theme-r'],
          unauthorizedFallbackTo: 'management.settings.top-apis',
        },
      },
    })
    .state('management.settings.top-apis', {
      url: '/top-apis',
      component: 'topApis',
      resolve: {
        topApis: ['TopApiService', (TopApiService: TopApiService) => TopApiService.list().then((response) => response.data)],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-top_apis',
        },
        perms: {
          only: ['environment-top_apis-r'],
          unauthorizedFallbackTo: 'management.settings.api_logging',
        },
      },
    })
    // Gateway
    .state('management.settings.api-logging', {
      url: '/api-logging',
      component: 'ngApiLogging',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-apilogging',
        },
        perms: {
          only: ['organization-settings-r'],
          unauthorizedFallbackTo: 'management.settings.dictionaries.list',
        },
      },
    })
    .state('management.settings.dictionaries', {
      abstract: true,
      url: '/dictionaries',
    })
    .state('management.settings.dictionaries.list', {
      url: '/',
      component: 'dictionaries',
      resolve: {
        dictionaries: [
          'DictionaryService',
          (DictionaryService: DictionaryService) => DictionaryService.list().then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dictionaries',
        },
        perms: {
          only: ['environment-dictionary-r'],
          unauthorizedFallbackTo: 'management.settings.customUserFields',
        },
      },
    })
    .state('management.settings.dictionaries.new', {
      url: '/new',
      component: 'dictionary',
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dictionary',
        },
        perms: {
          only: ['environment-dictionary-c'],
        },
      },
    })
    .state('management.settings.dictionaries.dictionary', {
      url: '/:dictionaryId',
      component: 'dictionary',
      resolve: {
        dictionary: [
          'DictionaryService',
          '$stateParams',
          (DictionaryService: DictionaryService, $stateParams) =>
            DictionaryService.get($stateParams.dictionaryId).then((response) => response.data),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-dictionary',
        },
        perms: {
          only: ['environment-dictionary-c', 'environment-dictionary-r', 'environment-dictionary-u', 'environment-dictionary-d'],
        },
      },
    })
    // User management
    .state('management.settings.customUserFields', {
      url: '/custom-user-fields',
      component: 'customUserFields',
      resolve: {
        fields: [
          'CustomUserFieldsService',
          (CustomUserFieldsService: CustomUserFieldsService) => CustomUserFieldsService.list().then((response) => response.data),
        ],
        fieldFormats: [
          'CustomUserFieldsService',
          (CustomUserFieldsService: CustomUserFieldsService) => CustomUserFieldsService.listFormats(),
        ],
        predefinedKeys: [
          'CustomUserFieldsService',
          (CustomUserFieldsService: CustomUserFieldsService) => CustomUserFieldsService.listPredefinedKeys(),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-custom-user-fields',
        },
        perms: {
          only: ['organization-custom_user_fields-r'],
          unauthorizedFallbackTo: 'management.settings.groups.list',
        },
      },
    })
    .state('management.settings.notification-settings', {
      url: '/notification-settings',
      component: 'environmentNotificationSettingsList',
      data: {
        docs: {
          page: 'management-configuration-notifications',
        },
        perms: {
          unauthorizedFallbackTo: 'management.home',
        },
      },
    })
    .state('management.settings.notification-settings-details', {
      url: '/notification-settings/:notificationId',
      component: 'environmentNotificationSettingsDetails',
      data: {
        docs: {
          page: 'management-configuration-notifications',
        },
      },
    })
    .state('management.settings.groups', {
      abstract: true,
      url: '/groups',
    })
    .state('management.settings.groups.list', {
      url: '/',
      component: 'groups',
      resolve: {
        groups: [
          'GroupService',
          (GroupService: GroupService) => GroupService.list().then((response) => _.filter(response.data, 'manageable')),
        ],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-groups',
        },
        perms: {
          only: ['environment-group-r'],
          unauthorizedFallbackTo: 'management.settings.notification-settings',
        },
      },
    })
    .state('management.settings.groups.create', {
      url: '/new',
      component: 'group',
      resolve: {
        tags: ['TagService', (TagService: TagService) => TagService.list().then((response) => response.data)],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-group',
        },
        perms: {
          only: ['environment-group-r'],
        },
      },
    })
    .state('management.settings.groups.group', {
      url: '/:groupId',
      component: 'group',
      resolve: {
        group: [
          'GroupService',
          '$stateParams',
          (GroupService: GroupService, $stateParams) => GroupService.get($stateParams.groupId).then((response) => response.data),
        ],
        apiRoles: [
          'RoleService',
          (RoleService: RoleService) => RoleService.list('API').then((roles) => [{ scope: 'API', name: '', system: false }].concat(roles)),
        ],
        applicationRoles: [
          'RoleService',
          (RoleService: RoleService) =>
            RoleService.list('APPLICATION').then((roles) => [{ scope: 'APPLICATION', name: '', system: false }].concat(roles)),
        ],
        invitations: [
          'GroupService',
          '$stateParams',
          (GroupService: GroupService, $stateParams) => GroupService.getInvitations($stateParams.groupId).then((response) => response.data),
        ],
        tags: ['TagService', (TagService: TagService) => TagService.list().then((response) => response.data)],
      },
      data: {
        menu: null,
        docs: {
          page: 'management-configuration-group',
        },
        perms: {
          only: ['environment-group-r'],
        },
      },
    });
}
configurationRouterConfig.$inject = ['$stateProvider'];