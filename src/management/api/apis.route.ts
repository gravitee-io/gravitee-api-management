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
import ViewService from '../../services/view.service';
import ApisController from './apis.controller';
import DocumentationService from '../../services/apiDocumentation.service';
import TenantService from '../../services/tenant.service';
import ResourceService from '../../services/resource.service';
import TagService from '../../services/tag.service';
import ApiService from '../../services/api.service';
import MetadataService from '../../services/metadata.service';

export default apisRouterConfig;

function apisRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis', {
      abstract: true,
      url: '/apis'
    })
    .state('management.apis.detail', {
      abstract: true,
      url: '/:apiId/settings',
      template: require('./apiAdmin.html'),
      controller: 'ApiAdminController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApiState: function ($stateParams, ApiService) {
          return ApiService.isAPISynchronized($stateParams.apiId);
        },
        resolvedApi: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        },
        resolvedViews: (ViewService: ViewService) => {
          return ViewService.list().then(response => {
            return response.data;
          });
        },
        resolvedTags: (TagService: TagService) => {
          return TagService.list().then(response => {
            return response.data;
          });
        }
      }
    })
    .state('management.apis.new', {
      url: '/new',
      template: require('./creation/newApi.html'),
      controller: 'NewApiController',
      controllerAs: 'newApiCtrl',
      params: {
        api: null
      },
      data: {
        roles: ['ADMIN', 'API_PUBLISHER']
      }
    })
    .state('management.apis.create', {
      url: '/new/create',
      component: 'apiCreation',
      params: {
        api: null
      },
      data: {
        roles: ['ADMIN', 'API_PUBLISHER']

      }
    })
    .state('management.apis.list', {
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
    })
    .state('management.apis.detail.general', {
      template: require('./general/api.html'),
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl'
    })
    .state('management.apis.detail.general.main', {
      url: '/general',
      template: require('./general/apiGeneral.html'),
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl',
      data: {
        menu: {
          label: 'General',
          icon: 'blur_on'
        }
      }
    })
    .state('management.apis.detail.general.gateway', {
      url: '/gateway',
      template: require('./general/apiGateway.html'),
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl',
      resolve: {
        resolvedTenants: (TenantService: TenantService) => TenantService.list()
      },
      data: {
        menu: {
          label: 'Gateway',
          icon: 'device_hub'
        }
      }
    })
    .state('management.apis.detail.general.endpoint', {
      url: '/endpoint/:endpointName',
      template: require('./endpoint/endpointConfiguration.html'),
      controller: 'ApiEndpointController',
      controllerAs: 'endpointCtrl',
      resolve: {
        resolvedTenants: (TenantService: TenantService) => TenantService.list()
      }
    })
    .state('management.apis.detail.plans', {
      url: '/plans?state',
      template: require('./plans/apiPlans.html'),
      controller: 'ApiPlansController',
      controllerAs: 'apiPlansCtrl',
      resolve: {
        resolvedPlans: function ($stateParams, ApiService) {
          return ApiService.getApiPlans($stateParams.apiId);
        }
      },
      data: {
        menu: {
          label: 'Plans',
          icon: 'view_week'
        }
      },
      params: {
        state: {
          type: 'string',
          dynamic: true,
        }
      }
    })
    .state('management.apis.detail.subscriptions', {
      url: '/subscriptions',
      template: require('./subscriptions/subscriptions.html'),
      controller: 'SubscriptionsController',
      controllerAs: 'subscriptionsCtrl',
      resolve: {
        resolvedSubscriptions: function ($stateParams, ApiService) {
          return ApiService.getSubscriptions($stateParams.apiId);
        }
      },
      data: {
        menu: {
          label: 'Subscriptions',
          icon: 'vpn_key'
        }
      }
    })
    .state('management.apis.detail.resources', {
      url: '/resources',
      template: require('./resources/resources.html'),
      controller: 'ApiResourcesController',
      controllerAs: 'apiResourcesCtrl',
      resolve: {
        resolvedResources: (ResourceService: ResourceService) => ResourceService.list()
      },
      data: {
        menu: {
          label: 'Resources',
          icon: 'style'
        }
      }
    })
    .state('management.apis.detail.policies', {
      url: '/policies',
      template: require('./policies/apiPolicies.html'),
      controller: 'ApiPoliciesController',
      controllerAs: 'apiPoliciesCtrl',
      data: {
        menu: {
          label: 'Policies',
          icon: 'share'
        }
      }
    })
    .state('management.apis.detail.members', {
      url: '/members',
      template: require('./members/members.html'),
      controller: 'ApiMembersController',
      controllerAs: 'apiMembersCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        }
      },
      data: {
        menu: {
          label: 'Members',
          icon: 'group'
        }
      }
    })
    .state('management.apis.detail.properties', {
      url: '/properties',
      template: require('./properties/properties.html'),
      controller: 'ApiPropertiesController',
      controllerAs: 'apiPropertiesCtrl',
      data: {
        menu: {
          label: 'Properties',
          icon: 'assignment'
        }
      }
    })
    .state('management.apis.detail.metadata', {
      url: '/metadata',
      template: require('./metadata/apiMetadata.html'),
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
        menu: {
          label: 'Metadata',
          icon: 'description'
        }
      }
    })
    .state('management.apis.detail.analytics', {
      url: '/analytics?from&to&q',
      template: require('./analytics/analytics.html'),
      controller: 'ApiAnalyticsController',
      controllerAs: 'analyticsCtrl',
      data: {
        menu: {
          label: 'Analytics',
          icon: 'insert_chart'
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
    .state('management.apis.detail.logs', {
      url: '/logs?from&to&q',
      template: require('./logs/logs.html'),
      controller: 'ApiLogsController',
      controllerAs: 'logsCtrl',
      data: {
        menu: {
          label: 'Logs',
          icon: 'receipt'
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
    .state('management.apis.detail.log', {
      url: '/logs/:logId',
      component: 'log',
      resolve: {
        log: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getLog($stateParams['apiId'], $stateParams['logId']).then(response => response.data)
      },
    })
    .state('management.apis.detail.documentation', {
      url: '/documentation',
      template: require('./documentation/apiDocumentation.html'),
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl',
      data: {
        menu: {
          label: 'Documentation',
          icon: 'insert_drive_file'
        }
      }
    })
    .state('management.apis.detail.documentation.new', {
      url: '/new',
      template: require('./documentation/page/apiPage.html'),
      controller: 'PageController',
      controllerAs: 'pageCtrl',
      data: {menu: null},
      params: {
        type: {
          type: 'string',
          value: '',
          squash: false
        }
      }
    })
    .state('management.apis.detail.documentation.page', {
      url: '/:pageId',
      template: require('./documentation/page/apiPage.html'),
      controller: 'PageController',
      controllerAs: 'pageCtrl',
      data: {menu: null}
    })
    .state('management.apis.detail.healthcheck', {
      url: '/healthcheck',
      template: require('./healthcheck/healthcheck.html'),
      controller: 'ApiHealthCheckController',
      controllerAs: 'healthCheckCtrl',
      data: {
        menu: {
          label: 'Health-check',
          icon: 'favorite'
        }
      }
    })
    .state('management.apis.detail.history', {
      url: '/history',
      template: require('./history/apiHistory.html'),
      controller: 'ApiHistoryController',
      controllerAs: 'apiHistoryCtrl',
      resolve: {
        resolvedEvents: function ($stateParams, ApiService) {
          var eventTypes = 'PUBLISH_API';
          return ApiService.getApiEvents($stateParams.apiId, eventTypes);
        }
      },
      data: {
        menu: {
          label: 'History',
          icon: 'history'
        }
      }
    })
    .state('management.apis.detail.events', {
      url: '/events',
      template: require('./events/apiEvents.html'),
      controller: 'ApiEventsController',
      controllerAs: 'apiEventsCtrl',
      resolve: {
        resolvedEvents: function ($stateParams, ApiService) {
          const eventTypes = 'START_API,STOP_API';
          return ApiService.getApiEvents($stateParams.apiId, eventTypes);
        }
      },
      data: {
        menu: {
          label: 'Events',
          icon: 'event'
        }
      }
    });
}
