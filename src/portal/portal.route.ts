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
import ApplicationService from "../services/application.service";
import UserService from '../services/user.service';
import _ = require('lodash');
import PortalService from "../services/portal.service";
import EntrypointService from "../services/entrypoint.service";
import DocumentationService, {DocumentationQuery} from "../services/documentation.service";

function portalRouterConfig($stateProvider) {
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
        resolvedDocumentation: function (DocumentationService: DocumentationService) {
          let q = new DocumentationQuery();
          q.homepage = false;
          q.root = true;
          return DocumentationService
            .search(q)
            .then(response => _.filter(response.data, (p) => { return p.type!=='FOLDER'; }));
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
        resolvedHomepage: function (DocumentationService: DocumentationService) {
          let q = new DocumentationQuery();
          q.homepage = true;
          return DocumentationService.search(q).then(response => response.data && response.data.length > 0 ? response.data[0] : null);
        }
      }
    })
    .state('portal.apis', {
      controller: 'PortalApisController'
    })
    .state('portal.apilist', {
      url: '/apis?view&q',
      template: require('./api/api-list.html'),
      controller: 'PortalApiListController',
      controllerAs: 'apisCtrl',
      resolve: {
        resolvedApis: function ($stateParams, PortalService: PortalService, ApiService, ViewService: ViewService) {
          if ($stateParams.q) {
            return PortalService.searchApis($stateParams.q);
          }
          if ($stateParams.view) {
            return ApiService.list($stateParams.view, true);
          }
          return ViewService.getDefaultOrFirstOne().then(response => {
            if (response) {
              return ApiService.list(response.id, true);
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
        q: {
          dynamic: true
        }
      }
    })
    .state('portal.views', {
      url: '/views',
      template: require('./views/views.html'),
      controller: 'PortalViewsController',
      controllerAs: 'viewsCtrl',
      resolve: {
        resolvedViews: (ViewService: ViewService) => ViewService.list()
      }
    })
    .state('portal.view', {
      url: '/views/:viewId',
      template: require('./views/view/view.html'),
      controller: 'PortalViewController',
      controllerAs: 'viewCtrl',
      resolve: {
        resolvedView: ($stateParams, ViewService: ViewService) => ViewService.get($stateParams.viewId),
        resolvedApis: ($stateParams, ApiService:ApiService) => ApiService.list($stateParams.viewId, true)
      }
    })
    .state('portal.api', {
      abstract: true,
      url: '/apis/:apiId',
      resolve: {
        api: ($stateParams, ApiService: ApiService) =>
          ApiService.get($stateParams['apiId']).then(response => response.data),
        apiRatingSummary: ($stateParams, ApiService: ApiService) => {
          return ApiService.isRatingEnabled()
            ? ApiService.getApiRatingSummaryByApi($stateParams['apiId']).then(response => response.data)
            : null;
        },
        apiPortalHeaders: ($stateParams, ApiService: ApiService) =>
          ApiService.getPortalHeaders($stateParams['apiId']).then(response => response.data),
        resolvedApiPermissions: (ApiService, $stateParams) => ApiService.getPermissions($stateParams.apiId),
        onEnter: function (UserService, resolvedApiPermissions) {
          if (UserService.currentUser && !UserService.currentUser.userApiPermissions) {
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
        plans: ($stateParams, ApiService: ApiService) =>
          ApiService.getPublishedApiPlans($stateParams['apiId']).then(response => response.data),
        homepage: ($stateParams, DocumentationService: DocumentationService) =>
          DocumentationService.getApiHomepage($stateParams['apiId']).then(response => response.data),
        isAuthenticated: ($stateParams, UserService: UserService) =>
          UserService.isAuthenticated(),
        entrypoints: (EntrypointService: EntrypointService) => EntrypointService.listForPortal().then(response => response.data)
      }
    })
    .state('portal.api.pages', {
      url: '/pages',
      views: {
        'header': {component: 'apiPortalHeader'},
        'content': {component: 'portalPages'}
      },
      resolve: {
        resolvedPages: ($stateParams, DocumentationService: DocumentationService) => {
          let q = new DocumentationQuery();
          q.homepage = false;
          return DocumentationService
            .search(q, $stateParams['apiId'])
            .then(response => response.data);
        }
      },
    })
    .state('portal.api.pages.page', {
      url: '/:pageId',
      component: 'portalPage',
      resolve: {
        page: ($stateParams, DocumentationService: DocumentationService) =>
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
      component: 'portalPages',
      resolve: {
        resolvedPages: (DocumentationService: DocumentationService) => {
          let q = new DocumentationQuery();
          q.homepage = false;
          return DocumentationService
            .search(q)
            .then(response => response.data);
        }
      }
    })
    .state('portal.pages.page', {
      url: '/:pageId',
      component: 'portalPage',
      resolve: {
        page: ($stateParams, DocumentationService: DocumentationService) =>
          DocumentationService.get(null, $stateParams['pageId'], true).then(response => response.data)
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
        plans: ($stateParams, ApiService: ApiService) =>
          ApiService.getPublishedApiPlans($stateParams['apiId']).then(response => response.data),
        applications: (ApplicationService: ApplicationService) =>
          ApplicationService.list().then(response => response.data),
        entrypoints: (EntrypointService: EntrypointService) => EntrypointService.listForPortal().then(response => response.data)
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
        isAuthenticated: ($stateParams, UserService: UserService) =>
          UserService.isAuthenticated(),
        rating: ($stateParams, ApiService: ApiService) =>
          ApiService.getApiRatingForConnectedUser($stateParams['apiId']).then(response => response.data),
        ratings: ($stateParams, ApiService: ApiService) =>
          ApiService.getApiRatings($stateParams['apiId'], $stateParams['pageNumber']).then(response => response.data)
      }
    });
}

export default portalRouterConfig;
