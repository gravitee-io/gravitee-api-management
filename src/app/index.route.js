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
function routerConfig($stateProvider, $urlRouterProvider) {
  'ngInject';
  $stateProvider
    .state('home', {
      url: '/?:view',
      templateUrl: 'app/main/main.html',
      controller: 'ApisController',
      controllerAs: 'apisCtrl',
      resolve: {
        resolvedApis: function (ApiService) {
          return ApiService.list();
        }
      },
      menu: {
        label: 'Home',
        icon: 'home',
        firstLevel: true
      },
      devMode: true
    })
    .state('apis', {
      abstract: true,
      url: '/apis',
      templateUrl: 'app/api/apis.html'
    })
    .state('apis.new', {
      url: '/new',
      templateUrl: 'app/api/admin/creation/newApi.html',
      controller: 'NewApiController',
      controllerAs: 'newApiCtrl',
      params: {
        api: null
      }
    })
    .state('apis.list', {
      abstract: true,
      url: '/',
      templateUrl: 'app/api/apisList.html',
      controller: 'ApisController',
      controllerAs: 'apisCtrl',
      resolve: {
        resolvedApis: function ($stateParams, ApiService) {
          return ApiService.list();
        }
      }
    })
    .state('apis.list.table', {
      url: 'table',
      templateUrl: 'app/api/apisTableMode.html',
      devMode: true
    })
    .state('apis.list.thumb', {
      url: 'thumb?:view',
      templateUrl: 'app/api/apisThumbMode.html',
      menu: {
        label: 'APIs',
        icon: 'dashboard',
        firstLevel: true
      },
      devMode: true
    })
    .state('apis.portal', {
      url: '/:apiId',
      templateUrl: 'app/api/portal/apiPortal.html',
      controller: 'ApiPortalController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApi: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        },
        resolvedPages: function ($stateParams, DocumentationService) {
          return DocumentationService.list($stateParams.apiId);
        }
      },
      menu: {
        label: 'Documentation',
        icon: 'insert_drive_file'
      },
      devMode: true
    })
    .state('apis.portal.page', {
      url: '/pages/:pageId',
      templateUrl: 'app/api/portal/apiPage.html',
      controller: 'ApiPortalPageController',
      controllerAs: 'apiPortalPageCtrl',
      resolve: {
        resolvedPage: function ($stateParams, DocumentationService) {
          return DocumentationService.get($stateParams.apiId, $stateParams.pageId);
        }
      },
      devMode: true
    })
    .state('apis.admin', {
      abstract: true,
      url: '/:apiId/settings',
      templateUrl: 'app/api/admin/apiAdmin.html',
      controller: 'ApiAdminController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApiState: function ($stateParams, ApiService) {
          return ApiService.isAPISynchronized($stateParams.apiId);
        },
        resolvedApi: function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        }
      }
    })
    .state('apis.admin.general', {
      abtract: true,
      url: '',
      templateUrl: 'app/api/admin/general/api.html',
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl'
    })
    .state('apis.admin.general.main', {
      url: '/general',
      templateUrl: 'app/api/admin/general/apiGeneral.html',
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl',
      menu: {
        label: 'Global settings',
        icon: 'blur_on'
      }
    })
    .state('apis.admin.general.gateway', {
      url: '/gateway',
      templateUrl: 'app/api/admin/general/apiGateway.html',
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl'
    })
    .state('apis.admin.endpoint', {
      url: '/endpoint/:endpointName',
      templateUrl: 'app/api/admin/endpoint/endpointConfiguration.html',
      controller: 'ApiEndpointController',
      controllerAs: 'endpointCtrl'
    })
    .state('apis.admin.apikeys', {
      url: '/apikeys',
      templateUrl: 'app/api/admin/apikeys/apikeys.html',
      controller: 'ApiKeysController',
      controllerAs: 'apiKeysCtrl',
      resolve: {
        resolvedApiKeys: function ($stateParams, ApiService) {
          return ApiService.getApiKeys($stateParams.apiId);
        }
      },
      menu: {
        label: 'Api keys',
        icon: 'vpn_key'
      }
    })
    .state('apis.admin.resources', {
      url: '/resources',
      templateUrl: 'app/api/admin/resources/resources.html',
      controller: 'ApiResourcesController',
      controllerAs: 'apiResourcesCtrl',
      menu: {
        label: 'Resources',
        icon: 'style'
      }
    })
    .state('apis.admin.policies', {
      url: '/policies',
      templateUrl: 'app/api/admin/policies/apiPolicies.html',
      controller: 'ApiPoliciesController',
      controllerAs: 'apiPoliciesCtrl',
      menu: {
        label: 'Policies',
        icon: 'share'
      }
    })
    .state('apis.admin.documentation', {
      url: '/documentation',
      templateUrl: 'app/api/admin/documentation/apiDocumentation.html',
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl',
      menu: {
        label: 'Documentation',
        icon: 'insert_drive_file'
      }
    })
    .state('apis.admin.members', {
      url: '/members',
      templateUrl: 'app/api/admin/members/members.html',
      controller: 'ApiMembersController',
      controllerAs: 'apiMembersCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        }
      },
      menu: {
        label: 'Members',
        icon: 'group'
      }
    })
    .state('apis.admin.properties', {
      url: '/properties',
      templateUrl: 'app/api/admin/properties/properties.html',
      controller: 'ApiPropertiesController',
      controllerAs: 'apiPropertiesCtrl',
      menu: {
        label: 'Properties',
        icon: 'assignment'
      }
    })
    .state('apis.admin.analytics', {
      url: '/analytics?timeframe&timestamp',
      templateUrl: 'app/api/admin/analytics/analytics.html',
      controller: 'ApiAnalyticsController',
      controllerAs: 'analyticsCtrl',
      menu: {
        label: 'Analytics',
        icon: 'insert_chart'
      }
    })
    .state('apis.admin.documentation.new', {
      url: '/new?type',
      templateUrl: 'app/api/admin/documentation/page/apiPage.html',
      controller: 'PageController',
      controllerAs: 'pageCtrl'
    })
    .state('apis.admin.documentation.page', {
      url: '/:pageId',
      templateUrl: 'app/api/admin/documentation/page/apiPage.html',
      controller: 'PageController',
      controllerAs: 'pageCtrl'
    })
    .state('apis.admin.healthcheck', {
      url: '/healthcheck',
      templateUrl: 'app/api/admin/healthcheck/healthcheck.html',
      controller: 'ApiHealthCheckController',
      controllerAs: 'healthCheckCtrl',
      menu: {
        label: 'Health-check',
        icon: 'favorite'
      }
    })
    .state('apis.admin.history', {
      url: '/history',
      templateUrl: 'app/api/admin/history/apiHistory.html',
      controller: 'ApiHistoryController',
      controllerAs: 'apiHistoryCtrl',
      resolve: {
        resolvedEvents: function ($stateParams, ApiService) {
          var eventTypes = "PUBLISH_API";
          return ApiService.getApiEvents($stateParams.apiId, eventTypes);
        }
      },
      menu: {
        label: 'History',
        icon: 'history'
      }
    })
    .state('apis.admin.events', {
      url: '/events',
      templateUrl: 'app/api/admin/events/apiEvents.html',
      controller: 'ApiEventsController',
      controllerAs: 'apiEventsCtrl',
      resolve: {
        resolvedEvents: function ($stateParams, ApiService) {
          var eventTypes = "START_API,STOP_API";
          return ApiService.getApiEvents($stateParams.apiId, eventTypes);
        }
      },
      menu: {
        label: 'Events',
        icon: 'event_note'
      }
    })
    .state('applications', {
      abstract: true,
      url: '/applications',
      templateUrl: 'app/application/applications.html',
      controller: 'ApplicationsController',
      controllerAs: 'applicationsCtrl',
      resolve: {
        resolvedApplications: function (ApplicationService) {
          return ApplicationService.list();
        }
      }
    })
    .state('applications.list', {
      abstract: true,
      url: '/',
      templateUrl: 'app/application/applicationsList.html'
    })
    .state('applications.list.table', {
      url: 'table',
      templateUrl: 'app/application/applicationsTableMode.html',
      devMode: true
    })
    .state('applications.list.thumb', {
      url: 'thumb',
      templateUrl: 'app/application/applicationsThumbMode.html',
      menu: {
        label: 'Applications',
        icon: 'list',
        firstLevel: true
      },
      devMode: true
    })
    .state('applications.portal', {
      abstract: true,
      url: '/:applicationId',
      templateUrl: 'app/application/details/applicationDetail.html',
      controller: 'ApplicationController',
      controllerAs: 'applicationCtrl',
      resolve: {
        resolvedApplication: function ($stateParams, ApplicationService) {
          return ApplicationService.get($stateParams.applicationId);
        }
      }
    })
    .state('applications.portal.general', {
      url: '/general',
      templateUrl: 'app/application/details/general/applicationGeneral.html',
      controller: 'ApplicationGeneralController',
      controllerAs: 'applicationGeneralCtrl',
      menu: {
        label: 'Global settings',
        icon: 'blur_on'
      },
      devMode: true
    })
    .state('applications.portal.apikeys', {
      url: '/apikeys',
      templateUrl: 'app/application/details/apikeys/applicationAPIKeys.html',
      controller: 'ApplicationAPIKeysController',
      controllerAs: 'applicationAPIKeysCtrl',
      resolve: {
        resolvedAPIKeys: function ($stateParams, ApplicationService) {
          return ApplicationService.getAPIKeys($stateParams.applicationId);
        }
      },
      menu: {
        label: 'Api keys',
        icon: 'vpn_key'
      },
      devMode: true
    })
    .state('applications.portal.members', {
      url: '/members',
      templateUrl: 'app/application/details/members/applicationMembers.html',
      controller: 'ApplicationMembersController',
      controllerAs: 'applicationMembersCtrl',
      resolve: {
        resolvedMembers: function ($stateParams, ApplicationService) {
          return ApplicationService.getMembers($stateParams.applicationId);
        }
      },
      menu: {
        label: 'Members',
        icon: 'group'
      },
      devMode: true
    })
    .state('applications.portal.analytics', {
      url: '/analytics?timeframe&timestamp',
      templateUrl: 'app/application/details/analytics/analytics.html',
      controller: 'ApplicationAnalyticsController',
      controllerAs: 'analyticsCtrl',
      menu: {
        label: 'Analytics',
        icon: 'insert_chart'
      },
      devMode: true
    })
    .state('instances', {
      abstract: true,
      url: '/instances',
      templateUrl: 'app/instances/instancesList.html'
    })
    .state('instances.list', {
      url: '/',
      templateUrl: 'app/instances/instances.html',
      controller: 'InstancesController',
      controllerAs: 'instancesCtrl',
      resolve: {
        resolvedInstances: function (InstancesService) {
          return InstancesService.list();
        }
      },
      menu: {
        label: 'Instances',
        icon: 'developer_dashboard',
        firstLevel: true
      },
      roles: ['ADMIN']
    })
    .state('instances.detail', {
      abstract: true,
      url: '/:id',
      templateUrl: 'app/instances/details/instance.html',
      controller: 'InstanceController',
      controllerAs: 'instanceCtrl',
      resolve: {
        resolvedInstance: function ($stateParams, InstancesService) {
          return InstancesService.get($stateParams.id);
        }
      }
    })
    .state('instances.detail.environment', {
      url: '/environment',
      templateUrl: 'app/instances/details/environment/instanceEnvironment.html',
      controller: 'InstanceEnvironmentController',
      controllerAs: 'instanceEnvironmentCtrl',
      menu: {
        label: 'Environment',
        icon: 'computer'
      }
    })
    .state('instances.detail.monitoring', {
      url: '/monitoring',
      templateUrl: 'app/instances/details/monitoring/instanceMonitoring.html',
      controller: 'InstanceMonitoringController',
      controllerAs: 'instanceMonitoringCtrl',
      menu: {
        label: 'Monitoring',
        icon: 'graphic_eq'
      },
      resolve: {
        resolvedMonitoringData: function ($stateParams, InstancesService, resolvedInstance) {
          return InstancesService.getMonitoringData($stateParams.id, resolvedInstance.data.id);
        }
      }
    })
    .state('platform', {
      url: '/platform?timeframe&from&to',
      templateUrl: 'app/platform/dashboard/dashboard.html',
      controller: 'DashboardController',
      controllerAs: 'dashboardCtrl',
      menu: {
        label: 'Dashboard',
        icon: 'show_chart',
        firstLevel: true
      },
      roles: ['ADMIN']
    })
    .state('configuration', {
      abstract: true,
      template: '<div ui-view></div>',
      url: '/configuration'
    })
    .state('configuration.admin', {
      url: '/admin',
      controller: function ($state, $rootScope) {
        $rootScope.currentResource = 'CONFIGURATION';
        if ('configuration.admin' === $state.current.name) {
          $state.go('configuration.admin.views');
        }
      },
      template: '<div ui-view></div>',
      menu: {
        label: 'Configuration',
        icon: 'settings',
        firstLevel: true
      },
      roles: ['ADMIN']
    })
    .state('configuration.admin.views', {
      url: '/views',
      templateUrl: 'app/configuration/admin/views/views.html',
      controller: 'ViewsController',
      controllerAs: 'viewsCtrl',
      menu: {
        label: 'Views',
        icon: 'view_module'
      },
      roles: ['ADMIN']
    })
    .state('configuration.admin.groups', {
      url: '/groups',
      templateUrl: 'app/configuration/admin/groups/groups.html',
      controller: 'GroupsController',
      controllerAs: 'groupsCtrl',
      menu: {
        label: 'Groups',
        icon: 'group_work'
      },
      roles: ['ADMIN']
    })
    .state('user', {
      url: '/user',
      templateUrl: 'app/user/user.html',
      controller: 'UserController',
      controllerAs: 'userCtrl'
    })
    .state('login', {
      url: '/login',
      templateUrl: 'app/login/login.html',
      controller: 'LoginController',
      controllerAs: 'loginCtrl'
    })
    .state('registration', {
      url: '/registration',
      templateUrl: 'app/registration/registration.html',
      controller: 'RegistrationController',
      controllerAs: 'registrationCtrl'
    })
    .state('confirm', {
      url: '/registration/confirm/:token',
      templateUrl: 'app/registration/confirm/confirm.html',
      controller: 'ConfirmController',
      controllerAs: 'confirmCtrl'
    });

  $urlRouterProvider.otherwise('/');
}

export default routerConfig;
