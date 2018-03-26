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

export default apisPortalRouterConfig;

function apisPortalRouterConfig($stateProvider: ng.ui.IStateProvider) {
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
        api: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.get($stateParams.apiId).then(response => response.data)
      }
    })
    .state('management.apis.detail.portal.plans.list', {
      url: '?state',
      component: 'listPlans',
      resolve: {
        plans: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
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
        groups: (GroupService: GroupService) => GroupService.list().then(response => response.data)
      }
    })
    .state('management.apis.detail.portal.plans.plan', {
      url: '/:planId/edit',
      component: 'editPlan',
      resolve: {
        plan: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getApiPlan($stateParams.apiId, $stateParams.planId).then(response => response.data),
        groups: (GroupService: GroupService) => GroupService.list().then(response => response.data)
      }
    })
    .state('management.apis.detail.portal.subscriptions', {
      abstract: true,
      url: '/subscriptions',
      template: '<div layout="column"><div ui-view></div></div>',
      resolve: {
        api: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.get($stateParams.apiId).then(response => response.data)
      }
    })
    .state('management.apis.detail.portal.subscriptions.list', {
      url: '',
      component: 'apiSubscriptions',
      resolve: {
        subscriptions: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getSubscriptions($stateParams.apiId).then(response => response.data),

        subscribers: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getSubscribers($stateParams.apiId).then(response => response.data),

        plans: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
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
        subscription: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
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
      url: '/documentation',
      template: require('./documentation/pages/apiDocumentation.html'),
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl',
      data: {
        perms: {
          only: ['api-documentation-r']
        },
        docs: {
          page: 'management-api-documentation'
        }
      }
    })
    .state('management.apis.detail.portal.documentation.new', {
      url: '/new',
      template: require('./documentation/pages/page/apiPage.html'),
      controller: 'PageController',
      controllerAs: 'pageCtrl',
      data: {
        menu: null,
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
        fallbackPageId: {
          type: 'string',
          value: '',
          squash: false
        }
      }
    })
    .state('management.apis.detail.portal.documentation.page', {
      url: '/:pageId',
      template: require('./documentation/pages/page/apiPage.html'),
      controller: 'PageController',
      controllerAs: 'pageCtrl',
      data: {
        menu: null,
        perms: {
          only: ['api-documentation-r']
        }
      }
    })
}
