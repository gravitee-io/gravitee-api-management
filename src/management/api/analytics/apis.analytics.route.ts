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
import { StateParams } from '@uirouter/core';
import SpelService from '../../../services/spel.service';
import TenantService from '../../../services/tenant.service';
import DashboardService from '../../../services/dashboard.service';

export default apisAnalyticsRouterConfig;

function apisAnalyticsRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.analytics', {
      template: require('./apis.analytics.route.html')
    })
    .state('management.apis.detail.analytics.overview', {
      url: '/analytics?from&to&q&dashboard',
      template: require('./overview/analytics.html'),
      controller: 'ApiAnalyticsController',
      controllerAs: 'analyticsCtrl',
      resolve: {
        dashboards: (DashboardService: DashboardService) => DashboardService.list('API').then(response => response.data)
      },
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
        },
        dashboard: {
          type: 'string',
          dynamic: true
        }
      }
    })
    .state('management.apis.detail.analytics.logs', {
      url: '/logs?from&to&q&page&size',
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
        },
        page: {
          type: 'int',
          dynamic: true
        },
        size: {
          type: 'int',
          dynamic: true
        }
      },
      resolve: {
        plans: ($stateParams: StateParams, ApiService: ApiService) =>
          ApiService.getApiPlans($stateParams.apiId),
        applications: ($stateParams: StateParams, ApiService: ApiService) =>
          ApiService.getSubscribers($stateParams.apiId),
        tenants: (TenantService: TenantService) => TenantService.list()
      }
    })
    .state('management.apis.detail.analytics.loggingconfigure', {
      url: '/logs/configure',
      template: require('./logs/logging-configuration.html'),
      controller: 'ApiLoggingConfigurationController',
      controllerAs: 'loggingCtrl',
      data: {
        menu: null,
        perms: {
          only: ['api-log-u']
        },
        docs: {
          page: 'management-api-logging-configuration'
        }
      },
      resolve: {
        spelGrammar: (SpelService: SpelService) => SpelService.getGrammar(),
      }
    })
    .state('management.apis.detail.analytics.log', {
      url: '/logs/:logId?timestamp&from&to&q&page&size',
      component: 'log',
      resolve: {
        log: ($stateParams: StateParams, ApiService: ApiService) =>
          ApiService.getLog($stateParams.apiId, $stateParams.logId, $stateParams.timestamp).then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-log-r']
        },
        docs: {
          page: 'management-api-log'
        }
      }
    })
    .state('management.apis.detail.analytics.pathMappings', {
      url: '/path-mappings',
      template: require('./pathMappings/pathMappings.html'),
      controller: 'ApiPathMappingsController',
      controllerAs: 'apiPathMappingCtrl',
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-pathMappings'
        }
      }
    });
}
