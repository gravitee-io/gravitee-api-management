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
import TenantService from '../../../services/tenant.service';
import ApiService from '../../../services/api.service';

export default apisProxyRouterConfig;

function apisProxyRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.proxy', {
      template: require("./apis.proxy.route.html")
    })
    .state('management.apis.detail.proxy.general', {
      url: '/proxy',
      template: require('./general/apiProxyGeneral.html'),
      controller: 'ApiProxyController',
      controllerAs: 'apiProxyCtrl',
      resolve: {
        resolvedTenants: (TenantService: TenantService) => TenantService.list()
      },
      data: {
        menu: {
          label: 'Proxy',
          icon: 'device_hub'
        },
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy'
        }
      }
    })
    .state('management.apis.detail.proxy.cors', {
      url: '/cors',
      template: require('./general/apiProxyCORS.html'),
      controller: 'ApiProxyController',
      controllerAs: 'apiProxyCtrl',
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy'
        }
      }
    })
    .state('management.apis.detail.proxy.deployments', {
      url: '/deployments',
      template: require('./general/apiProxyDeployments.html'),
      controller: 'ApiProxyController',
      controllerAs: 'apiProxyCtrl',
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy'
        }
      }
    })
    .state('management.apis.detail.proxy.failover', {
      url: '/failover',
      template: require('./backend/failover/apiProxyFailover.html'),
      controller: 'ApiProxyController',
      controllerAs: 'apiProxyCtrl',
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy'
        }
      }
    })
    .state('management.apis.detail.proxy.endpoints', {
      url: '/endpoints',
      template: require('./backend/endpoint/apiEndpoints.html'),
      controller: 'ApiProxyController',
      controllerAs: 'apiProxyCtrl',
      resolve: {
        resolvedTenants: (TenantService: TenantService) => TenantService.list()
      },
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy-endpoints'
        }
      }
    })
    .state('management.apis.detail.proxy.endpoint', {
      url: '/groups/:groupName/endpoints/:endpointName',
      template: require('./backend/endpoint/endpointConfiguration.html'),
      controller: 'ApiEndpointController',
      controllerAs: 'endpointCtrl',
      resolve: {
        resolvedTenants: (TenantService: TenantService) => TenantService.list()
      },
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy-endpoints'
        }
      }
    })
    .state('management.apis.detail.proxy.group', {
      url: '/groups/:groupName',
      template: require('./backend/endpoint/group.html'),
      controller: 'ApiEndpointGroupController',
      controllerAs: 'groupCtrl',
      data: {
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-proxy-group'
        }
      }
    })
    .state('management.apis.detail.proxy.endpointhc', {
      url: '/groups/:groupName/endpoints/:endpointName/healthcheck',
      template: require('./backend/healthcheck/healthcheck-configure.html'),
      controller: 'ApiHealthCheckConfigureController',
      controllerAs: 'healthCheckCtrl',
      data: {
        menu: null,
        perms: {
          only: ['api-health-c']
        },
        docs: {
          page: 'management-api-health-check'
        }
      }
    })
    .state('management.apis.detail.proxy.healthcheck', {
      abstract: true,
      url: '/healthcheck',
      template: require('./backend/healthcheck/healthcheck.html')
    })
    .state('management.apis.detail.proxy.healthcheck.visualize', {
      url: '/',
      template: require('./backend/healthcheck/healthcheck-visualize.html'),
      controller: 'ApiHealthCheckController',
      controllerAs: 'healthCheckCtrl',
      data: {
        perms: {
          only: ['api-health-r']
        },
        docs: {
          page: 'management-api-health-check'
        }
      }
    })
    .state('management.apis.detail.proxy.healthcheck.configure', {
      url: '/configure',
      template: require('./backend/healthcheck/healthcheck-configure.html'),
      controller: 'ApiHealthCheckConfigureController',
      controllerAs: 'healthCheckCtrl',
      data: {
        menu: null,
        perms: {
          only: ['api-health-c']
        },
        docs: {
          page: 'management-api-health-check-configure'
        }
      }
    })
    .state('management.apis.detail.proxy.healthcheck.log', {
      url: '/logs/:log',
      template: require('./backend/healthcheck/healthcheck-log.html'),
      controller: 'ApiHealthCheckLogController',
      controllerAs: 'healthCheckLogCtrl',
      resolve: {
        resolvedLog: ($stateParams, ApiService: ApiService) =>
          ApiService.getHealthLog($stateParams['apiId'], $stateParams['log'])
      },
      data: {
        perms: {
          only: ['api-health-r']
        }
      }
    })
    .state('management.apis.detail.proxy.discovery', {
      url: '/discovery',
      template: require('./backend/discovery/discovery.html'),
      controller: 'ApiDiscoveryController',
      controllerAs: 'discoveryCtrl',
      data: {
        perms: {
          only: ['api-discovery-c']
        },
        docs: {
          page: 'management-api-discovery'
        }
      }
    })
}
