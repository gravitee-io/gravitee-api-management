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
import ViewService from "../services/view.service";
import ApiService from "../services/api.service";
import DocumentationService from "../services/apiDocumentation.service";
import PortalPagesService from "../services/portalPages.service";
import ApplicationService from "../services/applications.service";
import UserService from '../services/user.service';
import _ = require('lodash');

function portalRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';

  $stateProvider
    .state('portal', {
      abstract: true,
      template: require('./index.html'),
      controller: function (Build, $rootScope, Constants, resolvedDocumentation) {
        this.graviteeVersion = Build.version;
        this.companyName = Constants.company ? Constants.company.name : '';
        $rootScope.portalTitle = Constants.portal.title;
        this.pages = resolvedDocumentation;

        this.getLogo = () => Constants.theme.logo;
      },
      controllerAs: 'indexCtrl',
      resolve: {
        resolvedDocumentation: function (PortalPagesService: PortalPagesService) {
          return PortalPagesService.listPortalDocumentation().then(response => response.data)
        }
      }
    })
    .state('portal.home', {
      url: '/',
      template: require('./home/home.html'),
      controller: 'HomeController',
      controllerAs: 'homeCtrl',
      resolve: {
        resolvedApis: function (ApiService) {
          return ApiService.listTopAPIs().then(response => response.data);
        },
        resolvedHomepage: function (PortalPagesService: PortalPagesService) {
          return PortalPagesService.getHomepage().then(response => response.data);
        }
      }
    })
    .state('portal.apis', {
      abstract: true,
      url: '/apis',
      template: require('./api/apis.html')
    })
    .state('portal.apis.list', {
      url: '?view',
      template: require('./api/apisList.html'),
      controller: 'PortalApisController',
      controllerAs: 'apisCtrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService, ViewService: ViewService) {
          if ($stateParams.view) {
            return ApiService.list($stateParams.view);
          }
          return ViewService.getDefaultOrFirstOne().then(response => {
            if (response) {
              return ApiService.list(response.id);
            } else {
              return [];
            }
          });
        },
        resolvedViews: (ViewService: ViewService) => {
          return ViewService.list().then(response => {
            return response.data;
          });
        }
      },
      params: {
        view: undefined,
      }
    })
    .state('portal.api', {
      abstract: true,
      url: '/apis/:apiId',
      resolve: {
        api: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.get($stateParams['apiId']).then(response => response.data),
        apiRatingSummary: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) => {
          return ApiService.isRatingEnabled()
            ? ApiService.getApiRatingSummaryByApi($stateParams['apiId']).then(response => response.data)
            : null;
        },
        resolvedApiPermissions: (ApiService, $stateParams) => ApiService.getPermissions($stateParams.apiId),
        onEnter: function (UserService, resolvedApiPermissions) {
          if (!UserService.currentUser.userApiPermissions) {
            UserService.currentUser.userApiPermissions = [];
            _.forEach(_.keys(resolvedApiPermissions.data), function (permission) {
              _.forEach(resolvedApiPermissions.data[permission], function (right) {
                let permissionName = 'API-' + permission + '-' + right;
                UserService.currentUser.userApiPermissions.push(_.toLower(permissionName));
              });
            });
            UserService.reloadPermissions();
          }
        }
      },
      component: 'api'
    })
    .state('portal.api.detail', {
      url: '/detail',
      views: {
        'header': {component: 'apiPortalHeader'},
        'content': {component: 'apiHomepage'},
        'subContent': {component: 'apiPlans'}
      },
      resolve: {
        plans: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getPublishedApiPlans($stateParams['apiId']).then(response => response.data),
        homepage: ($stateParams: ng.ui.IStateParamsService, DocumentationService: DocumentationService) =>
          DocumentationService.getApiHomepage($stateParams['apiId']).then(response => response.data),
        isAuthenticated: ($stateParams: ng.ui.IStateParamsService, UserService: UserService) =>
          UserService.isAuthenticated()
      }
    })
    .state('portal.api.pages', {
      url: '/pages',
      views: {
        'header': {component: 'apiPortalHeader'},
        'content': {component: 'apiPages'}
      },
      resolve: {
        pages: ($stateParams: ng.ui.IStateParamsService, DocumentationService: DocumentationService) =>
          DocumentationService.listApiPages($stateParams['apiId']).then(response => response.data)
      },
    })
    .state('portal.api.pages.page', {
      url: '/:pageId',
      component: 'apiPage',
      resolve: {
        page: ($stateParams: ng.ui.IStateParamsService, DocumentationService: DocumentationService) =>
          DocumentationService.get($stateParams['apiId'], $stateParams['pageId'], true).then(response => response.data)
      },
      params: {
        pageId: {
          type: 'string',
          value: '',
          squash: true
        }
      }
    })
    .state('portal.api.support', {
      url: '/support',
      views: {
        'header': {component: 'apiPortalHeader'},
        'content': {component: 'apiSupport'}
      }
    })
    .state('portal.pages', {
      url: '/pages',
      component: 'pages',
      resolve: {
        pages: ($stateParams: ng.ui.IStateParamsService, PortalPagesService: PortalPagesService) =>
          PortalPagesService.listPortalDocumentation().then(response => response.data)
      },
    })
    .state('portal.pages.page', {
      url: '/:pageId',
      component: 'page',
      resolve: {
        page: ($stateParams: ng.ui.IStateParamsService, PortalPagesService: PortalPagesService) =>
          PortalPagesService.get($stateParams['pageId']).then(response => response.data)
      },
      params: {
        pageId: {
          type: 'string',
          value: '',
          squash: true
        }
      }
    })
    .state('portal.api.subscribe', {
      url: '/subscribe?planId',
      views: {
        'header': {component: 'apiPortalHeader'},
        'content': {component: 'apiSubscribe'}
      },
      resolve: {
        plans: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getPublishedApiPlans($stateParams['apiId']).then(response => response.data),
        applications: (ApplicationService: ApplicationService) =>
          ApplicationService.list().then(response => response.data),
        /*
        plan: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getApiPlan($stateParams['apiId'], $stateParams['planId']).then(response => response.data),

        subscriptions: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getPlanSubscriptions($stateParams['apiId'], $stateParams['planId']).then(response => response.data)
        */
      }
    })
    .state('portal.api.rating', {
      url: '/ratings?:pageNumber',
      views: {
        'header': {component: 'apiPortalHeader'},
        'content': {component: 'apiRatings'}
      },
      params: {
        pageNumber: {
          type: 'string',
          value: '1'
        }
      },
      resolve: {
        isAuthenticated: ($stateParams: ng.ui.IStateParamsService, UserService: UserService) =>
          UserService.isAuthenticated(),
        rating: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getApiRatingForConnectedUser($stateParams['apiId']).then(response => response.data),
        ratings: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getApiRatings($stateParams['apiId'], $stateParams['pageNumber']).then(response => response.data)
      }
    });
}

export default portalRouterConfig;
