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
function portalRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';

  $stateProvider
    .state('portal', {
      abstract: true,
      template: require('./index.html'),
      controller: function (Build) {
        this.graviteeVersion = Build.version;
      },
      controllerAs: 'indexCtrl'
    })
    .state('portal.home', {
      url: '/',
      template: require('./home/home.html'),
      controller: 'HomeController',
      controllerAs: 'homeCtrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService) {
          if ($stateParams.view && $stateParams.view !== 'all') {
            return ApiService.list($stateParams.view);
          }
          return ApiService.list();
        }
      }
    })
    .state('portal.apis', {
      abstract: true,
      url: '/apis',
      template: require('./api/apis.html')
    })
    .state('portal.apis.list', {
      url: '',
      template: require('./api/apisList.html'),
      controller: 'PortalApisController',
      controllerAs: 'apisCtrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService) {
          if ($stateParams.view && $stateParams.view !== 'all') {
            return ApiService.list($stateParams.view);
          }
          return ApiService.list();
        },
        resolvedViews: (ViewService: ViewService, $translate) => {
          return ViewService.list().then(response => {
            let views = response.data;
            $translate('apis.all').then(function (all) {
              views.unshift({id: 'all', name: all});
            });
            return views;
          });
        }
      }
    })
    .state('portal.apis.detail', {
      abstract: true,
      url: '/:apiId',
      template: require('./api/portal/api.html'),
      resolve: {
        resolvedApi: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        }
      }
    })
    .state('portal.apis.detail.general', {
      url: '',
      template: require('./api/portal/general/apiGeneral.html'),
      controller: 'ApiController',
      controllerAs: 'apiCtrl'
    })
    .state('portal.apis.detail.docs', {
      url: '/docs',
      template: require('./api/portal/docs/apiDocs.html'),
      controller: 'ApiDocsController',
      controllerAs: 'apiDocsCtrl'
    });
}

export default portalRouterConfig;
