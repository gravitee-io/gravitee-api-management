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

export default apisAnalyticsRouterConfig;

function apisAnalyticsRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.analytics', {
      template: require("./apis.analytics.route.html")
    })
    .state('management.apis.detail.analytics.overview', {
      url: '/analytics?from&to&q',
      template: require('./overview/analytics.html'),
      controller: 'ApiAnalyticsController',
      controllerAs: 'analyticsCtrl',
      data: {
        menu: {
          label: 'Analytics',
          icon: 'insert_chart'
        },
        perms: {
          only: ['api-analytics-r']
        },
        docs: {
          page: 'management-api-analytics'
        }
      },
      params: {
        from: {
          type: 'int',
          dynamic: true
        },
        to: {
          type: 'int',
          dynamic: true
        },
        q: {
          type: 'string',
          dynamic: true
        }
      }
    })
    .state('management.apis.detail.analytics.logs', {
      url: '/logs?from&to&q',
      template: require('./logs/logs.html'),
      controller: 'ApiLogsController',
      controllerAs: 'logsCtrl',
      data: {
        perms: {
          only: ['api-log-r']
        },
        docs: {
          page: 'management-api-logs'
        }
      },
      params: {
        from: {
          type: 'int',
          dynamic: true
        },
        to: {
          type: 'int',
          dynamic: true
        },
        q: {
          type: 'string',
          dynamic: true
        }
      }
    })
    .state('management.apis.detail.analytics.log', {
      url: '/logs/:logId?timestamp',
      component: 'log',
      resolve: {
        log: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getLog($stateParams['apiId'], $stateParams['logId'], $stateParams['timestamp']).then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-log-r']
        }
      }
    })
}
