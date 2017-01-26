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
import ApisController from "./apis.controller";

export default apisConfig;

function apisConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider.state('apis.new', {
      url: "/new",
      template: require('./admin/creation/newApi.html'),
      controller: 'NewApiController',
      controllerAs: 'newApiCtrl',
      params: {
        api: null
      },
      data: {
        roles: ['ADMIN', 'API_PUBLISHER']
      }
    })
    .state('apis.create', {
      url: '/new/create',
      component: 'apiCreation',
      params: {
        api: null
      },
      data: {
        roles: ['ADMIN', 'API_PUBLISHER']

      }
    })
    .state('apis.list', {
      url: '/?view',
      template: require('./apis.html'),
      controller: ApisController,
      controllerAs: '$ctrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService) {
          if ($stateParams.view && $stateParams.view !== 'all') {
            return ApiService.list($stateParams.view);
          }
          return ApiService.list();
        },
        resolvedViews: (ViewService: ViewService) => {
          return ViewService.list().then(response => {
            let views = response.data;
            views.unshift({id: 'all', name: 'All APIs'});
            return views;
          });
        }
      },
      data: {
        menu: {
          label: 'APIs',
          icon: 'dashboard',
          firstLevel: true,
          order: 10
        },
        devMode: true
      },
      params: {
        view: {
          type: 'string',
          value: 'all',
          squash: true
        }
      }
    });
}
