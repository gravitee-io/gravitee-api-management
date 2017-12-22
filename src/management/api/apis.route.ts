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
import TenantService from '../../services/tenant.service';
import ResourceService from '../../services/resource.service';
import TagService from '../../services/tag.service';
import ApiService from '../../services/api.service';
import MetadataService from '../../services/metadata.service';
import GroupService from '../../services/group.service';
import * as _ from 'lodash';
import AuditService from "../../services/audit.service";

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
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then(response => {
            return response.data;
          });
        },
        resolvedTags: (TagService: TagService) => {
          return TagService.list().then(response => {
            return response.data;
          });
        },
        resolvedTenants: () => [],
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
        perms: {
          only: ['management-api-c']
        },
        docs: {
          page: 'management-apis-create'
        }
      }
    })
    .state('management.apis.create', {
      url: '/new/create',
      component: 'apiCreation',
      params: {
        api: null
      },
      data: {
        perms: {
          only: ['management-api-c']
        },
        docs: {
          page: 'management-apis-create-steps'
        }
      }
    })
    .state('management.apis.list', {
      url: '/?view',
      template: require('./apis.html'),
      controller: ApisController,
      controllerAs: '$ctrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService) {
          return ApiService.list();
        }
      },
      data: {
        menu: {
          label: 'APIs',
          icon: 'dashboard',
          firstLevel: true,
          order: 10
        },
        docs: {
          page: 'management-apis'
        },
        devMode: true
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
        },
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api'
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
    .state('management.apis.detail.general.endpoint', {
      url: '/endpoint/:endpointName',
      template: require('./endpoint/endpointConfiguration.html'),
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
    .state('management.apis.detail.general.endpointhc', {
      url: '/endpoint/:endpointName/healthcheck',
      template: require('./healthcheck/healthcheck-configure.html'),
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
    .state('management.apis.detail.discovery', {
      url: '/audit',
      template: require('./discovery/discovery.html'),
      controller: 'ApiDiscoveryController',
      controllerAs: 'discoveryCtrl',
      data: {
        menu: {
          label: 'Discovery',
          icon: 'settings_input_antenna'
        },
        perms: {
          only: ['api-discovery-c']
        },
        docs: {
          page: 'management-api-discovery'
        }
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
        },
        perms: {
          only: ['api-plan-r']
        },
        docs: {
          page: 'management-api-plans'
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
        },
        perms: {
          only: ['api-subscription-r']
        },
        docs: {
          page: 'management-api-subscriptions'
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
        },
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-resources'
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
        },
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-policies'
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
        },
        perms: {
          only: ['api-member-r']
        },
        docs: {
          page: 'management-api-members'
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
        },
        perms: {
          only: ['api-definition-r']
        },
        docs: {
          page: 'management-api-properties'
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
        },
        perms: {
          only: ['api-metadata-r']
        },
        docs: {
          page: 'management-api-metadata'
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
    .state('management.apis.detail.logs', {
      url: '/logs?from&to&q',
      template: require('./logs/logs.html'),
      controller: 'ApiLogsController',
      controllerAs: 'logsCtrl',
      data: {
        menu: {
          label: 'Logs',
          icon: 'receipt'
        },
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
    .state('management.apis.detail.log', {
      url: '/logs/:logId',
      component: 'log',
      resolve: {
        log: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getLog($stateParams['apiId'], $stateParams['logId']).then(response => response.data)
      },
      data: {
        perms: {
          only: ['api-log-r']
        }
      }
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
        },
        perms: {
          only: ['api-documentation-r']
        },
        docs: {
          page: 'management-api-documentation'
        }
      }
    })
    .state('management.apis.detail.documentation.new', {
      url: '/new',
      template: require('./documentation/page/apiPage.html'),
      controller: 'PageController',
      controllerAs: 'pageCtrl',
      data: {
        menu: null,
        perms: {
          only: ['api-documentation-c']
        }
      },
      params: {
        type: {
          type: 'string',
          value: '',
          squash: false
        },
        fallbackPageId: {
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
      data: {
        menu: null,
        perms: {
          only: ['api-documentation-r']
        }
      }
    })
    .state('management.apis.detail.healthcheck', {
      abstract: true,
      url: '/healthcheck',
      template: require('./healthcheck/healthcheck.html')
    })
    .state('management.apis.detail.healthcheck.visualize', {
      url: '/',
      template: require('./healthcheck/healthcheck-visualize.html'),
      controller: 'ApiHealthCheckController',
      controllerAs: 'healthCheckCtrl',
      data: {
        menu: {
          label: 'Health-check',
          icon: 'favorite'
        },
        perms: {
          only: ['api-health-r']
        },
        docs: {
          page: 'management-api-health-check'
        }
      }
    })
    .state('management.apis.detail.healthcheck.configure', {
      url: '/configure',
      template: require('./healthcheck/healthcheck-configure.html'),
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
    .state('management.apis.detail.healthcheck.log', {
      url: '/logs/:log',
      template: require('./healthcheck/healthcheck-log.html'),
      controller: 'ApiHealthCheckLogController',
      controllerAs: 'healthCheckLogCtrl',
      resolve: {
        resolvedLog: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.getHealthLog($stateParams['apiId'], $stateParams['log'])
      },
      data: {
        perms: {
          only: ['api-health-r']
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
        },
        perms: {
          only: ['api-event-r']
        },
        docs: {
          page: 'management-api-history'
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
        },
        perms: {
          only: ['api-event-r']
        },
        docs: {
          page: 'management-api-events'
        }
      }
    })
    .state('management.apis.detail.audit', {
      url: '/audit',
      template: require('./audit/audit.html'),
      controller: 'ApiAuditController',
      controllerAs: 'auditCtrl',
      data: {
        menu: {
          label: 'Audit',
          icon: 'visibility',
        },
        perms: {
          only: ['api-audit-r']
        },
        docs: {
          page: 'management-api-audit'
        }
      },
      resolve: {
        resolvedEvents:
          (AuditService: AuditService, $stateParams) => AuditService.listEvents($stateParams.apiId).then(response => response.data)
      }
    });
}
