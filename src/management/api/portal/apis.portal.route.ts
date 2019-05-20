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
import ApiService from '../../../services/api.service';
import MetadataService from '../../../services/metadata.service';
import GroupService from '../../../services/group.service';
import DocumentationService, {DocumentationQuery} from "../../../services/documentation.service";
import {StateParams} from '@uirouter/core';
import FetcherService from "../../../services/fetcher.service";
import PolicyService from "../../../services/policy.service";
import TagService from "../../../services/tag.service";
import UserService from "../../../services/user.service";

export default apisPortalRouterConfig;

function apisPortalRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.portal', {
      template:require('./apis.portal.route.html')
    })
    .state('management.apis.detail.portal.general', {
      url: '/portal',
      template: require('./general/apiPortal.html'),
      controller: 'ApiPortalController',
      controllerAs: 'portalCtrl',
      data: {
        menu: {
          label: 'Portal',
          icon: 'important_devices'
        },
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api'
        }
      }
    })
    .state('management.apis.detail.portal.plans', {
      abstract: true,
      url: '/plans',
      template: '<div layout="column"><div ui-view></div></div>',
      resolve: {
        api: ($stateParams, ApiService: ApiService) =>
          ApiService.get($stateParams.apiId).then(response => response.data)
      }
    })
    .state('management.apis.detail.portal.plans.list', {
      url: '?state',
      component: 'listPlans',
      resolve: {
        plans: ($stateParams, ApiService: ApiService) =>
          ApiService.getApiPlans($stateParams.apiId).then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-plan-r']
        },
        docs: {
          page: 'management-api-plans'
        }
      },
      params: {
        state: {
          type: 'string',
          dynamic: true
        }
      }
    })
    .state('management.apis.detail.portal.plans.new', {
      url: '/new',
      component: 'editPlan',
      resolve: {
        groups: (GroupService: GroupService) => GroupService.list().then(response => response.data),
        policies: (PolicyService: PolicyService) => PolicyService.list().then(response => response.data),
        tags: (TagService: TagService) => TagService.list().then(response => response.data),
        userTags: (UserService: UserService) => UserService.getCurrentUserTags().then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-plan-c']
        },
        docs: {
          page: 'management-api-plans-wizard'
        }
      }
    })
    .state('management.apis.detail.portal.plans.plan', {
      url: '/:planId/edit',
      component: 'editPlan',
      resolve: {
        plan: ($stateParams, ApiService: ApiService) =>
          ApiService.getApiPlan($stateParams.apiId, $stateParams.planId).then(response => response.data),
        groups: (GroupService: GroupService) => GroupService.list().then(response => response.data),
        policies: (PolicyService: PolicyService) => PolicyService.list().then(response => response.data),
        tags: (TagService: TagService) => TagService.list().then(response => response.data),
        userTags: (UserService: UserService) => UserService.getCurrentUserTags().then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-plan-u']
        },
        docs: {
          page: 'management-api-plans-wizard'
        }
      }
    })
    .state('management.apis.detail.portal.subscriptions', {
      abstract: true,
      url: '/subscriptions',
      template: '<div layout="column"><div ui-view></div></div>',
      resolve: {
        api: ($stateParams, ApiService: ApiService) =>
          ApiService.get($stateParams.apiId).then(response => response.data)
      }
    })
    .state('management.apis.detail.portal.subscriptions.list', {
      url: '',
      component: 'apiSubscriptions',
      resolve: {
        subscriptions: ($stateParams, ApiService: ApiService) =>
          ApiService.getSubscriptions($stateParams.apiId).then(response => response.data),

        subscribers: ($stateParams, ApiService: ApiService) =>
          ApiService.getSubscribers($stateParams.apiId).then(response => response.data),

        plans: ($stateParams, ApiService: ApiService) =>
          ApiService.getApiPlans($stateParams.apiId).then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-subscription-r']
        },
        docs: {
          page: 'management-api-subscriptions'
        }
      }
    })
    .state('management.apis.detail.portal.subscriptions.subscription', {
      url: '/:subscriptionId',
      component: 'apiSubscription',
      resolve: {
        subscription: ($stateParams, ApiService: ApiService) =>
          ApiService.getSubscription($stateParams.apiId, $stateParams.subscriptionId).then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-subscription-r']
        },
        docs: {
          page: 'management-api-subscriptions'
        }
      }
    })
    .state('management.apis.detail.portal.members', {
      url: '/members',
      template: require('./userGroupAccess/members/members.html'),
      controller: 'ApiMembersController',
      controllerAs: 'apiMembersCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        }
      },
      data: {
        perms: {
          only: ['api-member-r']
        },
        docs: {
          page: 'management-api-members'
        }
      }
    })
    .state('management.apis.detail.portal.groups', {
      url: '/groups',
      template: require('./userGroupAccess/groups/groups.html'),
      controller: 'ApiPortalController',
      controllerAs: 'portalCtrl',
      data: {
        perms: {
          only: ['api-member-r']
        },
        docs: {
          page: 'management-api-members'
        }
      }
    })
    .state('management.apis.detail.portal.transferownership', {
      url: '/transferownership',
      template: require('./userGroupAccess/transferOwnership/transferOwnership.html'),
      controller: 'ApiTransferOwnershipController',
      controllerAs: 'apiTransferOwnershipCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        }
      },
      data: {
        perms: {
          only: ['api-member-r']
        },
        docs: {
          page: 'management-api-members'
        }
      }
    })
    .state('management.apis.detail.portal.metadata', {
      url: '/metadata',
      template: require('./documentation/metadata/apiMetadata.html'),
      controller: 'ApiMetadataController',
      controllerAs: 'apiMetadataCtrl',
      resolve: {
        metadataFormats: (MetadataService: MetadataService) => MetadataService.listFormats(),
        metadata: function ($stateParams, ApiService) {
          return ApiService.listApiMetadata($stateParams.apiId).then(function (response) {
            return response.data;
          });
        }
      },
      data: {
        perms: {
          only: ['api-metadata-r']
        },
        docs: {
          page: 'management-api-metadata'
        }
      }
    })
    .state('management.apis.detail.portal.documentation', {
      url: '/documentation?:parent',
      component: 'documentationManagement',
      resolve: {
        pages: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          if ($stateParams.parent && ""!==$stateParams.parent) {
            q.parent = $stateParams.parent;
          } else {
            q.root = true;
          }
          return DocumentationService.search(q, $stateParams.apiId)
            .then(response => response.data)
        },
        folders: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = "FOLDER";
          return DocumentationService.search(q, $stateParams.apiId)
            .then(response => response.data)
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-api-documentation'
        },
        perms: {
          only: ['api-documentation-r']
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
    .state('management.apis.detail.portal.newdocumentation', {
      url: '/documentation/new?:type&:parent',
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
          page: 'management-api-documentation'
        },
        perms: {
          only: ['api-documentation-c']
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
    .state('management.apis.detail.portal.importdocumentation', {
      url: '/documentation/import',
      component: 'importPages',
      resolve: {
        resolvedFetchers: (FetcherService: FetcherService) => {
          return FetcherService.list(true).then(response => {
            return response.data;
          })
        },
        resolvedRootPage: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = "ROOT";
          return DocumentationService.search(q, $stateParams.apiId)
            .then(response => response.data && response.data.length > 0 ? response.data[0] : null);
        }
      },
      data: {
        menu: null,
        docs: {
          page: 'management-api-documentation'
        },
        perms: {
          only: ['api-documentation-c']
        }
      }
    })
    .state('management.apis.detail.portal.editdocumentation', {
      url: '/documentation/:pageId?:tab',
      component: 'editPage',
      resolve: {
        resolvedPage: (DocumentationService: DocumentationService, $stateParams: StateParams) =>
          DocumentationService.get($stateParams.apiId, $stateParams.pageId).then(response => response.data),
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
          page: 'management-api-documentation'
        },
        perms: {
          only: ['api-documentation-u']
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
}
