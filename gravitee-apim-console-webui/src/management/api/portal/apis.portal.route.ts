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

import CategoryService from '../../../services/category.service';
import { DocumentationQuery, DocumentationService } from '../../../services/documentation.service';
import FetcherService from '../../../services/fetcher.service';
import GroupService from '../../../services/group.service';
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
      url: '/new?selectedPlanMenuItem',
      component: 'ngApiPortalPlanEdit',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-plan-c'],
        },
        docs: {
          page: 'management-api-plans-wizard',
        },
      },
      params: {
        securityType: {
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.portal.plan.edit', {
      url: '/:planId/edit',
      component: 'ngApiPortalPlanEdit',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-plan-u'],
        },
        docs: {
          page: 'management-api-plans-wizard',
        },
      },
    })
    .state('management.apis.detail.portal.subscriptions', {
      url: '/subscriptions?page&size&:application&:status&:plan&:apiKey',
      component: 'ngApiPortalSubscriptionList',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'management-api-subscriptions',
        },
        perms: {
          only: ['api-subscription-r'],
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
        apiKey: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.portal.subscription', {
      url: '/subscription',
    })
    .state('management.apis.detail.portal.subscription.edit', {
      url: '/:subscriptionId',
      component: 'ngApiPortalSubscriptionEdit',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'management-api-subscriptions',
        },
        perms: {
          only: ['api-subscription-r'],
        },
      },
    })
    .state('management.apis.detail.portal.members', {
      url: '/members',
      component: 'ngApiMembers',
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
      component: 'ngApiTransferOwnership',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-member-u'],
        },
        docs: {
          page: 'management-api-members',
        },
      },
    })
    .state('management.apis.detail.portal.metadata', {
      url: '/metadata',
      component: 'ngApiPortalDocumentationMetadata',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-metadata-r'],
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
