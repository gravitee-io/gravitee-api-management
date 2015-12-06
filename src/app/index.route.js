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
function routerConfig ($stateProvider, $urlRouterProvider) {
  'ngInject';
  $stateProvider
    .state('home', {
      url: '/',
      templateUrl: 'app/main/main.html',
      controller: 'ApisController',
      controllerAs: 'apisCtrl',
			ncyBreadcrumb: {
    		label: 'Home'
  		}
    })
    .state('apis', {
      abstract: true,
      url: '/apis',
      templateUrl: 'app/api/apis.html',
      controller: 'ApisController',
      controllerAs: 'apisCtrl'
    })
    .state('apis.list', {
      abstract: true,
      url: '/',
      templateUrl: 'app/api/apisList.html'
    })
    .state('apis.list.table', {
      url: 'table',
      templateUrl: 'app/api/apisTableMode.html',
			ncyBreadcrumb: {
				label: 'APIs',
				parent: 'home'
			}
    })
    .state('apis.list.thumb', {
      url: 'thumb',
      templateUrl: 'app/api/apisThumbMode.html',
			ncyBreadcrumb: {
				label: 'APIs',
				parent: 'home'
			}
    })
    .state('apis.portal', {
      url: '/:apiId',
      templateUrl: 'app/api/portal/apiPortal.html',
      controller: 'ApiPortalController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApi:function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        },
        resolvedPage:function ($stateParams, DocumentationService) {
          return DocumentationService.list($stateParams.apiId);
        }
      },
			ncyBreadcrumb: {
				label: '{{apiCtrl.api.name}}',
				parent: 'apis.list.thumb'
			}
    })
    .state('apis.admin', {
      url: '/:apiId/settings',
      templateUrl: 'app/api/admin/apiAdmin.html',
      controller: 'ApiAdminController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApi:function ($stateParams, ApiService) {
          return ApiService.get($stateParams.apiId);
        }
      },
			ncyBreadcrumb: {
				label: '{{apiCtrl.api.name}}',
				parent: 'apis.list.thumb'
			}
    })
    .state('apis.admin.analytics', {
      url: '/analytics',
      templateUrl: 'app/api/admin/analytics.html',
      controller: 'ApiAnalyticsController',
      controllerAs: 'analyticsCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.policies', {
      url: '/policies',
      templateUrl: 'app/api/admin/policies/apiPolicies.html',
      controller: 'ApiPoliciesController',
      controllerAs: 'apiPoliciesCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.documentation', {
      url: '/documentation',
      templateUrl: 'app/api/admin/documentation/apiDocumentation.html',
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.documentation.page', {
      url: '/:pageId',
      templateUrl: 'app/api/admin/documentation/page/apiPage.html',
      controller: 'PageController',
      controllerAs: 'pageCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.general', {
      url: '/general',
      templateUrl: 'app/api/admin/general/apiGeneral.html',
      controller: 'ApiGeneralController',
      controllerAs: 'generalCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.members', {
      url: '/members',
      templateUrl: 'app/api/admin/members.html',
      controller: 'ApiMembersController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedMembers:function ($stateParams, ApiService) {
          return ApiService.getMembers($stateParams.apiId);
        }
      },
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.monitoring', {
      url: '/monitoring',
      templateUrl: 'app/api/admin/apiMonitoring.html',
      controller: 'ApiMonitoringController',
      controllerAs: 'monitoringCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('apis.admin.properties', {
      url: '/properties',
      templateUrl: 'app/api/admin/properties/properties.html',
      controller: 'ApiPropertiesController',
      controllerAs: 'apiPropertiesCtrl',
      ncyBreadcrumb: {
        skip: true
      }
    })
    .state('apis.admin.apikeys', {
      url: '/apikeys',
      templateUrl: 'app/api/admin/apikeys/apikeys.html',
      controller: 'ApiKeysController',
      controllerAs: 'apiKeysCtrl',
      resolve: {
        resolvedApiKeys:function ($stateParams, ApiService) {
          return ApiService.getApiKeys($stateParams.apiId);
        }
      },
      ncyBreadcrumb: {
        skip: true
      }
    })
    .state('apis.admin.descriptor', {
      url: '/descriptor',
      templateUrl: 'app/api/admin/descriptor.html',
      controller: 'ApiDescriptorController',
      controllerAs: 'apiDescriptorCtrl',
      ncyBreadcrumb: {
        skip: true
      }
    })
    .state('documentation', {
      url: '/documentation',
      templateUrl: 'app/documentation/documentation.html',
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl'
    })
    .state('profile', {
      url: '/profile',
      templateUrl: 'app/profile/profile_stub.html',
      controller: 'ProfileController',
      controllerAs: 'profileCtrl'
    })
    .state('applications', {
			abstract: true,
      url: '/applications',
      templateUrl: 'app/application/applications.html',
      controller: 'ApplicationsController',
      controllerAs: 'applicationsCtrl'
    })
		.state('applications.table', {
      url: '/table',
      templateUrl: 'app/application/applicationsTableMode.html',
			ncyBreadcrumb: {
				label: 'Applications',
				parent: 'home'
			}
    })
    .state('applications.thumb', {
      url: '/thumb',
      templateUrl: 'app/application/applicationsThumbMode.html',
			ncyBreadcrumb: {
				label: 'Applications',
				parent: 'home'
			}
    })
    .state('application', {
      url: '/applications/:applicationId',
      templateUrl: 'app/application/details/application.html',
      controller: 'ApplicationController',
      controllerAs: 'applicationCtrl',
      resolve: {
        resolvedApplication:function ($stateParams, ApplicationService) {
          return ApplicationService.get($stateParams.applicationId);
        }
      },
			ncyBreadcrumb: {
				label: '{{applicationCtrl.application.name}}',
				parent: 'applications.thumb'
			}
    })
    .state('application.general', {
      url: '/general',
      templateUrl: 'app/application/details/general/applicationGeneral.html',
      controller: 'ApplicationGeneralController',
      controllerAs: 'applicationGeneralCtrl',
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('application.apikeys', {
      url: '/apikeys',
      templateUrl: 'app/application/details/apikeys/applicationAPIKeys.html',
      controller: 'ApplicationAPIKeysController',
      controllerAs: 'applicationAPIKeysCtrl',
      resolve: {
        resolvedAPIKeys:function ($stateParams, ApplicationService) {
          return ApplicationService.getAPIKeys($stateParams.applicationId);
        }
      },
			ncyBreadcrumb: {
				skip: true
			}
    })
    .state('application.members', {
      url: '/members',
      templateUrl: 'app/application/details/members/applicationMembers.html',
      controller: 'ApplicationMembersController',
      controllerAs: 'applicationMembersCtrl',
      resolve: {
        resolvedMembers:function ($stateParams, ApplicationService) {
          return ApplicationService.getMembers($stateParams.applicationId);
        }
      },
			ncyBreadcrumb: {
				skip: true
			}
    });

  $urlRouterProvider.otherwise('/');
}

export default routerConfig;
