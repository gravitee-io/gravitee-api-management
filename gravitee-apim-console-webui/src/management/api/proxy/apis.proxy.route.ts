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
import { ApiService } from '../../../services/api.service';
import EnvironmentService from '../../../services/environment.service';
import SpelService from '../../../services/spel.service';

export default apisProxyRouterConfig;

function apisProxyRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.proxy', {
      resolve: {
        resolvedCurrentEnvironment: (EnvironmentService: EnvironmentService) => EnvironmentService.getCurrent(),
      },
    })
    .state('management.apis.detail.proxy.ng-entrypoints', {
      url: '/proxy',
      component: 'ngApiProxyEntrypoints',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r', 'api-health-r'],
        },
        docs: {
          page: 'management-api-proxy',
        },
      },
    })
    .state('management.apis.detail.proxy.ng-cors', {
      url: '/cors',
      component: 'ngApiProxyCors',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-proxy',
        },
      },
    })
    .state('management.apis.detail.proxy.ng-deployments', {
      url: '/deployments',
      component: 'ngApiProxyDeployments',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-proxy',
        },
      },
    })
    .state('management.apis.detail.proxy.failover', {
      url: '/failover',
      component: 'ngApiProxyFailover',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-proxy-failover',
        },
      },
    })
    .state('management.apis.detail.proxy.endpoints', {
      url: '/endpoints',
      component: 'ngApiProxyEndpointList',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-proxy-endpoint',
        },
      },
    })
    .state('management.apis.detail.proxy.endpoint', {
      url: '/ng-groups/:groupName/ng-endpoints/:endpointName',
      component: 'ngApiProxyGroupEndpointEdit',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-proxy-endpoints',
        },
      },
    })
    .state('management.apis.detail.proxy.group', {
      url: '/ng-groups/:groupName',
      component: 'ngApiProxyGroupEdit',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-definition-r'],
        },
        docs: {
          page: 'management-api-proxy-group',
        },
      },
    })
    .state('management.apis.detail.proxy.endpointhc', {
      url: '/groups/:groupName/endpoints/:endpointName/healthcheck',
      template: require('./backend/healthcheck/healthcheck-configure.html'),
      controller: 'ApiHealthCheckConfigureController',
      controllerAs: 'healthCheckCtrl',
      data: {
        perms: {
          only: ['api-health-c'],
        },
        docs: {
          page: 'management-api-health-check',
        },
      },
      resolve: {
        resolvedSpelGrammar: (SpelService: SpelService) => SpelService.getGrammar(),
      },
    })
    .state('management.apis.detail.proxy.healthcheck', {
      abstract: true,
      url: '/healthcheck',
      template: require('./backend/healthcheck/healthcheck.html'),
    })
    .state('management.apis.detail.proxy.healthcheck.visualize', {
      url: '?from&to&page&size',
      template: require('./backend/healthcheck/healthcheck-visualize.html'),
      controller: 'ApiHealthCheckController',
      controllerAs: 'healthCheckCtrl',
      data: {
        perms: {
          only: ['api-health-r'],
        },
        docs: {
          page: 'management-api-health-check',
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
        page: {
          type: 'int',
          dynamic: true,
        },
        size: {
          type: 'int',
          dynamic: true,
        },
      },
    })
    .state('management.apis.detail.proxy.healthcheck.configure', {
      url: '/configure',
      template: require('./backend/healthcheck/healthcheck-configure.html'),
      controller: 'ApiHealthCheckConfigureController',
      controllerAs: 'healthCheckCtrl',
      data: {
        perms: {
          only: ['api-health-c'],
        },
        docs: {
          page: 'management-api-health-check-configure',
        },
      },
      resolve: {
        resolvedSpelGrammar: (SpelService: SpelService) => SpelService.getGrammar(),
      },
    })
    .state('management.apis.detail.proxy.healthcheck.log', {
      url: '/logs/:log',
      template: require('./backend/healthcheck/healthcheck-log.html'),
      controller: 'ApiHealthCheckLogController',
      controllerAs: 'healthCheckLogCtrl',
      resolve: {
        resolvedLog: ($stateParams, ApiService: ApiService) => ApiService.getHealthLog($stateParams.apiId, $stateParams.log),
      },
      data: {
        perms: {
          only: ['api-health-r'],
        },
      },
    })
    .state('management.apis.detail.proxy.discovery', {
      url: '/discovery',
      template: require('./backend/discovery/discovery.html'),
      controller: 'ApiDiscoveryController',
      controllerAs: 'discoveryCtrl',
      data: {
        perms: {
          only: ['api-discovery-c'],
        },
        docs: {
          page: 'management-api-discovery',
        },
      },
    })
    .state('management.apis.detail.proxy.ng-responsetemplates', {
      abstract: true,
      url: '/responsetemplates',
    })
    .state('management.apis.detail.proxy.ng-responsetemplates.list', {
      url: '',
      component: 'ngApiProxyResponseTemplatesList',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-response_templates-r'],
        },
        docs: {
          page: 'management-api-proxy-response-templates',
        },
      },
    })
    .state('management.apis.detail.proxy.ng-responsetemplates.new', {
      url: '/',
      component: 'ngApiProxyResponseTemplatesEdit',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
        },
        docs: {
          page: 'management-api-proxy-response-template',
        },
      },
    })
    .state('management.apis.detail.proxy.ng-responsetemplates.edit', {
      url: '/:responseTemplateId',
      component: 'ngApiProxyResponseTemplatesEdit',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
        },
        docs: {
          page: 'management-api-proxy-response-template',
        },
      },
    });
}
