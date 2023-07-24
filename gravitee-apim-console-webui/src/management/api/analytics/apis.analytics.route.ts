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
import { StateParams, StateService } from '@uirouter/core';

import { Scope } from '../../../entities/alert';
import AlertService from '../../../services/alert.service';
import { ApiService } from '../../../services/api.service';
import DashboardService from '../../../services/dashboard.service';
import TenantService from '../../../services/tenant.service';
import EnvironmentService from '../../../services/environment.service';

export default apisAnalyticsRouterConfig;

/* @ngInject */
function apisAnalyticsRouterConfig($stateProvider) {
  $stateProvider
    .state('management.apis.detail.analytics', {
      abstract: true,
    })
    .state('management.apis.detail.analytics.overview', {
      url: '/analytics?from&to&q&dashboard',
      template: require('./overview/analytics-overview.html'),
      controller: 'ApiAnalyticsController',
      controllerAs: 'analyticsCtrl',
      resolve: {
        dashboards: (DashboardService: DashboardService) => DashboardService.list('API').then((response) => response.data),
        resolvedApi: function (
          $stateParams: StateParams,
          ApiService: ApiService,
          $state: StateService,
          Constants: any,
          EnvironmentService: EnvironmentService,
        ) {
          return ApiService.get($stateParams.apiId).catch((err) => {
            if (err && err.interceptorFuture) {
              $state.go('management.apis.list', { environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv.id) });
            }
          });
        },
      },
      data: {
        perms: {
          only: ['api-analytics-r'],
        },
        docs: {
          page: 'management-api-analytics',
        },
      },
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        dashboard: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.analytics.logs', {
      abstract: true,
      url: '/logs',
    })
    .state('management.apis.detail.analytics.logs.list', {
      url: '?from&to&q&page&size',
      template: require('./logs/analytics-logs.html'),
      controller: 'ApiLogsController',
      controllerAs: 'logsCtrl',
      data: {
        perms: {
          only: ['api-log-r'],
        },
        docs: {
          page: 'management-api-logs',
        },
      },
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          dynamic: true,
        },
        size: {
          type: 'int',
          dynamic: true,
        },
      },
      resolve: {
        plans: ($stateParams: StateParams, ApiService: ApiService) => ApiService.getApiPlans($stateParams.apiId),
        applications: ($stateParams: StateParams, ApiService: ApiService) =>
          ApiService.getSubscribers($stateParams.apiId, null, null, null, ['owner']),
        tenants: (TenantService: TenantService) => TenantService.list(),
        resolvedApi: function (
          $stateParams: StateParams,
          ApiService: ApiService,
          $state: StateService,
          Constants: any,
          EnvironmentService: EnvironmentService,
        ) {
          return ApiService.get($stateParams.apiId).catch((err) => {
            if (err && err.interceptorFuture) {
              $state.go('management.apis.list', { environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv.id) });
            }
          });
        },
      },
    })
    .state('management.apis.detail.analytics.logs.configuration', {
      url: '/configure',
      component: 'ngApiLogsConfiguration',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-log-u'],
        },
        docs: {
          page: 'management-api-logging-configuration',
        },
      },
    })
    .state('management.apis.detail.analytics.logs.log', {
      url: '/:logId?timestamp&from&to&q&page&size',
      component: 'log',
      resolve: {
        log: ($stateParams: StateParams, ApiService: ApiService) =>
          ApiService.getLog($stateParams.apiId, $stateParams.logId, $stateParams.timestamp).then((response) => response.data),
      },
      data: {
        perms: {
          only: ['api-log-r'],
        },
        docs: {
          page: 'management-api-log',
        },
      },
    })
    .state('management.apis.detail.analytics.pathMappings', {
      url: '/path-mappings',
      useAngularMaterial: true,
      component: 'ngApiPathMappings',
      data: {
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-pathMappings',
        },
      },
    })
    .state('management.apis.detail.analytics.alerts', {
      url: '/analytics/alerts',
      template: require('./alerts/api-alerts-dashboard.html'),
      controller: 'ApiAlertsDashboardController',
      controllerAs: '$ctrl',
      resolve: {
        configuredAlerts: (AlertService: AlertService, $stateParams) =>
          AlertService.listAlerts(Scope.API, false, $stateParams.apiId).then((response) => response.data),
        alertingStatus: (AlertService: AlertService, $stateParams) =>
          AlertService.getStatus(Scope.API, $stateParams.apiId).then((response) => response.data),
      },
      data: {
        perms: {
          only: ['api-alert-r'],
        },
        docs: {
          page: 'management-api-alerts',
        },
      },
    });
}
