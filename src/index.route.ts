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
import ApiService from "./services/api.service";
import DocumentationService from "./services/apiDocumentation.service";
import InstancesService from "./services/instances.service";
import UserService from './services/user.service';
import TenantService from './services/tenant.service';
import LoginController from "./login/login.controller";
import { User } from "./entities/user";

function routerConfig($stateProvider: ng.ui.IStateProvider, $urlRouterProvider: ng.ui.IUrlRouterProvider) {
  'ngInject';
  $stateProvider
    .state(
      'root',
      {
        abstract: true,
        template: "<div ui-view='sidenav' class='gravitee-sidenav'></div><md-content ui-view layout='column' flex></md-content>",
        resolve: {
          graviteeUser: (UserService: UserService) => UserService.current()
        }
      }
    )
    .state(
      'withSidenav',
      {
        parent: 'root',
        abstract: true,
        views: {
          'sidenav': {
            component: 'gvSidenav',
          },
          '': {
            template: '<div ui-view layout="column" flex></div>'
          }
        },
        resolve: {
          allMenuItems: ($state: ng.ui.IStateService) => $state.get(),
          menuItems: ($state: ng.ui.IStateService, graviteeUser: User, Constants: any) => {
            'ngInject';
            return $state.get()
                  .filter((state: any) => !state.abstract && state.data && state.data.menu)
                  .filter(routeMenuItem => {
                    let isMenuItem = routeMenuItem.data.menu.firstLevel && (!routeMenuItem.data.roles || graviteeUser.allowedTo(routeMenuItem.data.roles));
                    if (Constants.devMode) {
                      return isMenuItem && routeMenuItem.data.devMode;
                    }  else {
                      return isMenuItem;
                    }
                  });
          }
        }
      }
    )
    .state('home', {
      url: '/',
      redirectTo: 'apis.list',
      data: {
        devMode: true
      }
    })
    .state(
      'apis',
      {
        parent: 'withSidenav',
        abstract: true,
        url: '/apis',
        template: '<div ui-view layout="column" flex></div>'
      }
    )
    .state('apis.portal', {
      abstract: true,
      url: '/:apiId',
      template: require('./api/portal/apiPortal.html'),
      controller: 'ApiPortalController',
      controllerAs: 'apiCtrl',
      resolve: {
        api: ($stateParams: ng.ui.IStateParamsService, ApiService: ApiService) =>
          ApiService.get($stateParams['apiId']).then(response => response.data)
      }
    })
    .state('apis.portal.pages', {
      url: '/pages',
      component: 'apiPages',
      resolve: {
        pages: ($stateParams: ng.ui.IStateParamsService, DocumentationService: DocumentationService) =>
          DocumentationService.list($stateParams['apiId']).then(response => response.data)
      },
      data: {
        menu: {
          label: 'Documentation',
          icon: 'insert_drive_file'
        },
        devMode: true
      }
    })
    .state('apis.portal.pages.page', {
      url: '/:pageId',
      component: 'apiPage',
      resolve: {
        page: ($stateParams: ng.ui.IStateParamsService, DocumentationService: DocumentationService) =>
          DocumentationService.get($stateParams['apiId'], $stateParams['pageId']).then(response => response.data)
      },
      data: {
        menu: null,
        devMode: true
      },
      params: {
        pageId: {
          type: 'string',
          value: '',
          squash: true
        }
      }
    })
    .state('apis.portal.plans', {
      url: '/plans',
      template: require('./api/portal/plan/apiPlans.html'),
      controller: 'ApiPortalPlanController',
      controllerAs: 'apiPortalPlanCtrl',
      resolve: {
        resolvedPlans: function ($stateParams, ApiService) {
          return ApiService.listPlans($stateParams.apiId);
        }
      },
      data: {
        menu: {
          label: 'Plans',
          icon: 'view_week'
        }
      }
    })
    .state('apis.admin', {
      abstract: true,
      url: '/:apiId/settings',
      template: require('./api/admin/apiAdmin.html'),
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
      template: require('./api/admin/general/api.html'),
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl'
    })
    .state('apis.admin.general.main', {
      url: '/general',
      template: require('./api/admin/general/apiGeneral.html'),
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl',
      data: {
        menu: {
          label: 'General',
          icon: 'blur_on'
        }
      }
    })
    .state('apis.admin.general.gateway', {
      url: '/gateway',
      template: require('./api/admin/general/apiGateway.html'),
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
    .state('apis.admin.general.endpoint', {
      url: '/endpoint/:endpointName',
      template: require('./api/admin/endpoint/endpointConfiguration.html'),
      controller: 'ApiEndpointController',
      controllerAs: 'endpointCtrl',
      resolve: {
        resolvedTenants: (TenantService: TenantService) => TenantService.list()
      }
    })
    .state('apis.admin.plans', {
      url: '/plans?state',
      template: require('./api/admin/plans/apiPlans.html'),
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
          type: "string",
          dynamic: true,
        }
      }
    })
    .state('apis.admin.subscriptions', {
      url: '/subscriptions',
      template: require('./api/admin/subscriptions/subscriptions.html'),
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
    .state('apis.admin.resources', {
      url: '/resources',
      template: require('./api/admin/resources/resources.html'),
      controller: 'ApiResourcesController',
      controllerAs: 'apiResourcesCtrl',
      data: {
        menu: {
          label: 'Resources',
          icon: 'style'
        }
      }
    })
    .state('apis.admin.policies', {
      url: '/policies',
      template: require('./api/admin/policies/apiPolicies.html'),
      controller: 'ApiPoliciesController',
      controllerAs: 'apiPoliciesCtrl',
      data: {
        menu: {
          label: 'Policies',
          icon: 'share'
        }
      }
    })
    .state('apis.admin.members', {
      url: '/members',
      template: require('./api/admin/members/members.html'),
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
    .state('apis.admin.properties', {
      url: '/properties',
      template: require('./api/admin/properties/properties.html'),
      controller: 'ApiPropertiesController',
      controllerAs: 'apiPropertiesCtrl',
      data: {
        menu: {
          label: 'Properties',
          icon: 'assignment'
        }
      }
    })
    .state('apis.admin.analytics', {
      url: '/analytics?from&to',
      template: require('./api/admin/analytics/analytics.html'),
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
          type: "int",
          dynamic: true
        },
        to: {
          type: "int",
          dynamic: true
        }
      }
    })
    .state('apis.admin.documentation', {
      url: '/documentation',
      template: require('./api/admin/documentation/apiDocumentation.html'),
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl',
      resolve: {
        resolvedPages: function ($stateParams, DocumentationService: DocumentationService) {
          return DocumentationService.list($stateParams.apiId);
        }
      },
      data: {
        menu: {
          label: 'Documentation',
          icon: 'insert_drive_file'
        }
      }
    })
    .state('apis.admin.documentation.new', {
      url: '/new',
      template: require('./api/admin/documentation/page/apiPage.html'),
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
    .state('apis.admin.documentation.page', {
      url: '/:pageId',
      template: require('./api/admin/documentation/page/apiPage.html'),
      controller: 'PageController',
      controllerAs: 'pageCtrl',
      data: {menu: null}
    })
    .state('apis.admin.healthcheck', {
      url: '/healthcheck',
      template: require('./api/admin/healthcheck/healthcheck.html'),
      controller: 'ApiHealthCheckController',
      controllerAs: 'healthCheckCtrl',
      data: {
        menu: {
          label: 'Health-check',
          icon: 'favorite'
        }
      }
    })
    .state('apis.admin.history', {
      url: '/history',
      template: require('./api/admin/history/apiHistory.html'),
      controller: 'ApiHistoryController',
      controllerAs: 'apiHistoryCtrl',
      resolve: {
        resolvedEvents: function ($stateParams, ApiService) {
          var eventTypes = "PUBLISH_API";
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
    .state('apis.admin.events', {
      url: '/events',
      template: require('./api/admin/events/apiEvents.html'),
      controller: 'ApiEventsController',
      controllerAs: 'apiEventsCtrl',
      resolve: {
        resolvedEvents: function ($stateParams, ApiService) {
          var eventTypes = "START_API,STOP_API";
          return ApiService.getApiEvents($stateParams.apiId, eventTypes);
        }
      },
      data: {
        menu: {
          label: 'Events',
          icon: 'event_note'
        }
      }
    })
    .state('instances', {
      abstract: true,
      url: '/instances',
      template: '<div ui-view></div>',
      parent: 'withSidenav'
    })
    .state('instances.list', {
      url: '/',
      component: 'instances',
      resolve: {
        instances: (InstancesService: InstancesService) => InstancesService.list().then(response => response.data)
      },
      data: {
        menu: {
          label: 'Instances',
          icon: 'developer_dashboard',
          firstLevel: true,
          order: 30
        },
        roles: ['ADMIN']
      }
    })
    .state('instances.detail', {
      abstract: true,
      url: '/:instanceId',
      component: 'instance',
      resolve: {
        instance: ($stateParams: ng.ui.IStateParamsService, InstancesService: InstancesService) =>
          InstancesService.get($stateParams['instanceId']).then(response => response.data)
      }
    })
    .state('instances.detail.environment', {
      url: '/environment',
      component: 'instanceEnvironment',
      data: {
        menu: {
          label: 'Environment',
          icon: 'computer'
        }
      }
    })
    .state('instances.detail.monitoring', {
      url: '/monitoring',
      component: 'instanceMonitoring',
      data: {
        menu: {
          label: 'Monitoring',
          icon: 'graphic_eq'
        },
      },
      resolve: {
        monitoringData: ($stateParams: ng.ui.IStateParamsService, InstancesService: InstancesService, instance: any) =>
          InstancesService.getMonitoringData($stateParams['instanceId'], instance.id).then(response => response.data)
      }
    })
    .state('platform', {
      url: '/platform?from&to',
      template: require('./platform/dashboard/dashboard.html'),
      controller: 'DashboardController',
      controllerAs: 'dashboardCtrl',
      data: {
        menu: {
          label: 'Dashboard',
          icon: 'show_chart',
          firstLevel: true,
          order: 40
        },
        roles: ['ADMIN']
      },
      params: {
        from: {
          type: "int",
          dynamic: true
        },
        to: {
          type: "int",
          dynamic: true
        }
      },
      parent: 'withSidenav'
    })
    .state('user', {
      url: '/user',
      component: 'user',
      parent: 'withSidenav',
      resolve: {
        user: ( graviteeUser: User) => graviteeUser
      }
    })
    .state('login', {
      url: '/login',
      template: require('./login/login.html'),
      controller: LoginController,
      controllerAs: '$ctrl',
      data: {
        devMode: true
      }
    })
    .state('registration', {
      url: '/registration',
      template: require('./registration/registration.html'),
      controller: 'RegistrationController',
      controllerAs: 'registrationCtrl',
      data: {
        devMode: true
      }
    })
    .state('confirm', {
      url: '/registration/confirm/:token',
      template: require('./registration/confirm/confirm.html'),
      controller: 'ConfirmController',
      controllerAs: 'confirmCtrl',
      data: {
        devMode: true
      }
    })
    .state('logout', {
      controller: (UserService, $state: ng.ui.IStateService) => {
        UserService.logout().then(
          () => { $state.go('home'); }
        );
      }
    });

  $urlRouterProvider.otherwise('/');
}

export default routerConfig;
