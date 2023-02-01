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

import { ApiService } from '../../../services/api.service';
import CategoryService from '../../../services/category.service';
import { DocumentationQuery, DocumentationService } from '../../../services/documentation.service';
import FetcherService from '../../../services/fetcher.service';
import GroupService from '../../../services/group.service';
import MetadataService from '../../../services/metadata.service';
import QualityRuleService from '../../../services/qualityRule.service';
import '@gravitee/ui-components/wc/gv-icon';

export default apisPortalRouterConfig;

/* @ngInject */
function apisPortalRouterConfig($stateProvider) {
  $stateProvider
    .state('management.apis.detail.portal', {
      resolve: {
        qualityRules: (QualityRuleService: QualityRuleService) => QualityRuleService.list().then((response) => response.data),
      },
    })
    .state('management.apis.detail.portal.general', {
      url: '/portal',
      component: 'ngApiPortalDetails',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'management-api',
        },
      },
    })
    .state('management.apis.detail.portal.plans', {
      url: '/plans?status',
      component: 'ngApiPortalPlanList',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-plan-r'],
        },
        docs: {
          page: 'management-api-plans',
        },
      },
      params: {
        status: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.portal.plan', {
      url: '/plan',
    })
    .state('management.apis.detail.portal.plan.new', {
      url: '/new',
      component: 'ngApiPortalPlanEdit',
      data: {
        perms: {
          only: ['api-plan-c'],
        },
        docs: {
          page: 'management-api-plans-wizard',
        },
      },
    })
    .state('management.apis.detail.portal.plan.edit', {
      url: '/:planId/edit',
      component: 'ngApiPortalPlanEdit',
      data: {
        perms: {
          only: ['api-plan-u'],
        },
        docs: {
          page: 'management-api-plans-wizard',
        },
      },
    })
    .state('management.apis.detail.portal.subscriptions', {
      abstract: true,
      url: '/subscriptions',
      template: '<div layout="column"><div ui-view></div></div>',
      resolve: {
        api: ($stateParams, ApiService: ApiService) => ApiService.get($stateParams.apiId).then((response) => response.data),
      },
    })
    .state('management.apis.detail.portal.subscriptions.list', {
      url: '?page&size&:application&:status&:plan&:api_key',
      component: 'apiSubscriptions',
      resolve: {
        subscriptions: ($stateParams, ApiService: ApiService) => {
          let query = '?page=' + $stateParams.page + '&size=' + $stateParams.size;

          if ($stateParams.status) {
            query += '&status=' + $stateParams.status;
          }

          if ($stateParams.application) {
            query += '&application=' + $stateParams.application;
          }

          if ($stateParams.plan) {
            query += '&plan=' + $stateParams.plan;
          }

          if ($stateParams.api_key) {
            query += '&api_key=' + $stateParams.api_key;
          }

          return ApiService.getSubscriptions($stateParams.apiId, query).then((response) => response.data);
        },

        plans: ($stateParams, ApiService: ApiService) => ApiService.getApiPlans($stateParams.apiId).then((response) => response.data),
      },
      data: {
        perms: {
          only: ['api-subscription-r'],
        },
        docs: {
          page: 'management-api-subscriptions',
        },
      },
      params: {
        status: {
          type: 'string',
          dynamic: true,
        },
        application: {
          type: 'string',
          dynamic: true,
        },
        plan: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          value: 1,
          dynamic: true,
        },
        size: {
          type: 'int',
          value: 10,
          dynamic: true,
        },
        api_key: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.portal.subscriptions.subscription', {
      url: '/:subscriptionId?:page&:size&:application&:status&:plan&:api_key',
      component: 'apiSubscription',
      resolve: {
        subscription: ($stateParams, ApiService: ApiService) =>
          ApiService.getSubscription($stateParams.apiId, $stateParams.subscriptionId).then((response) => response.data),
      },
      data: {
        perms: {
          only: ['api-subscription-r'],
        },
        docs: {
          page: 'management-api-subscriptions',
        },
      },
      params: {
        status: {
          type: 'string',
          dynamic: true,
        },
        application: {
          type: 'string',
          dynamic: true,
        },
        plan: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          value: 1,
          dynamic: true,
        },
        size: {
          type: 'int',
          value: 10,
          dynamic: true,
        },
        api_key: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.portal.members', {
      url: '/members',
      template: require('./userGroupAccess/members/members.html'),
      controller: 'ApiMembersController',
      controllerAs: 'apiMembersCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        },
      },
      data: {
        perms: {
          only: ['api-member-r'],
        },
        docs: {
          page: 'management-api-members',
        },
      },
    })
    .state('management.apis.detail.portal.ngmembers', {
      url: '/ng-members',
      component: 'ngApiMembers',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-member-r'],
        },
        docs: {
          page: 'management-api-members',
        },
      },
    })
    .state('management.apis.detail.portal.groups', {
      url: '/groups',
      component: 'ngApiPortalGroups',
      data: {
        perms: {
          only: ['api-member-r'],
        },
        docs: {
          page: 'management-api-members',
        },
        useAngularMaterial: true,
      },
    })
    .state('management.apis.detail.portal.transferownership', {
      url: '/transferownership',
      template: require('./userGroupAccess/transferOwnership/transferOwnership.html'),
      controller: 'ApiTransferOwnershipController',
      controllerAs: 'apiTransferOwnershipCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        },
      },
      data: {
        perms: {
          only: ['api-member-r'],
        },
        docs: {
          page: 'management-api-members',
        },
      },
    })
    .state('management.apis.detail.portal.metadata', {
      url: '/metadata',
      component: 'metadata',
      resolve: {
        metadataFormats: (MetadataService: MetadataService) => MetadataService.listFormats(),
        metadata: function ($stateParams, ApiService) {
          return ApiService.listApiMetadata($stateParams.apiId).then((response) => {
            return response.data;
          });
        },
      },
      data: {
        perms: {
          only: ['api-metadata-r'],
        },
        docs: {
          page: 'management-api-metadata',
        },
      },
    })
    .state('management.apis.detail.portal.documentation', {
      url: '/documentation?:parent',
      component: 'documentationManagement',
      resolve: {
        pages: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          if ($stateParams.parent && '' !== $stateParams.parent) {
            q.parent = $stateParams.parent;
          } else {
            q.root = true;
          }
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
        folders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
        systemFolders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
      data: {
        docs: {
          page: 'management-api-documentation',
        },
        perms: {
          only: ['api-documentation-r'],
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
    .state('management.apis.detail.portal.newdocumentation', {
      url: '/documentation/new?:type&:parent',
      component: 'newPage',
      resolve: {
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list().then((response) => {
            return response.data;
          });
        },
        folders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
        systemFolders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
        pageResources: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            const q = new DocumentationQuery();
            return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
          }
        },
        categoryResources: (CategoryService: CategoryService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            return CategoryService.list().then((response) => response.data);
          }
        },
        pagesToLink: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'MARKDOWN_TEMPLATE') {
            const q = new DocumentationQuery();
            q.homepage = false;
            q.published = true;
            return DocumentationService.search(q, $stateParams.apiId).then((response) =>
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
      },
      data: {
        docs: {
          page: 'management-api-documentation',
        },
        perms: {
          only: ['api-documentation-c'],
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
    .state('management.apis.detail.portal.importdocumentation', {
      url: '/documentation/import',
      component: 'importPages',
      resolve: {
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list(true).then((response) => {
            return response.data;
          });
        },
        resolvedRootPage: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'ROOT';
          return DocumentationService.search(q, $stateParams.apiId).then((response) =>
            response.data && response.data.length > 0 ? response.data[0] : null,
          );
        },
      },
      data: {
        docs: {
          page: 'management-api-documentation',
        },
        perms: {
          only: ['api-documentation-c'],
        },
      },
    })
    .state('management.apis.detail.portal.editdocumentation', {
      url: '/documentation/:pageId?:tab&type',
      component: 'editPage',
      resolve: {
        resolvedPage: (DocumentationService: DocumentationService, $stateParams: StateParams) =>
          DocumentationService.get($stateParams.apiId, $stateParams.pageId).then((response) => response.data),
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then((response) => {
            return response.data;
          });
        },
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list().then((response) => {
            return response.data;
          });
        },
        folders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
        systemFolders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
        pageResources: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            const q = new DocumentationQuery();
            return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
          }
        },
        categoryResources: (CategoryService: CategoryService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            return CategoryService.list().then((response) => response.data);
          }
        },
        pagesToLink: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'MARKDOWN_TEMPLATE') {
            const q = new DocumentationQuery();
            q.homepage = false;
            q.published = true;
            return DocumentationService.search(q, $stateParams.apiId).then((response) =>
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
        attachedResources: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'ASCIIDOC' || $stateParams.type === 'ASYNCAPI') {
            return DocumentationService.getMedia($stateParams.pageId, $stateParams.apiId).then((response) => response.data);
          }
        },
      },
      data: {
        docs: {
          page: 'management-api-documentation',
        },
        perms: {
          only: ['api-documentation-r'],
        },
      },
      params: {
        pageId: {
          type: 'string',
          value: '',
          squash: false,
        },
      },
    });
}
