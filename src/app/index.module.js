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
import config from './index.config';
import routerConfig from './index.route';
import interceptorConfig from './index.interceptor';
import delegatorConfig from './index.delegator';
import runBlock from './index.run';

import FormDirective from './components/form/form.directive';
import AutofocusDirective from './components/autofocus/autofocus.directive';
import ApiService from './services/api.service';
import ApisController from './api/apis.controller';
import ApiPortalController from './api/portal/apiPortal.controller';
import ApiPortalPagesController from './api/portal/documentation/apiPages.controller';
import ApiPortalPageController from './api/portal/documentation/apiPage.controller';
import ApiPortalPlanController from './api/portal/plan/apiPlan.controller';
import ApiGeneralController from './api/admin/general/apiGeneral.controller';
import ApiAdminController from './api/admin/apiAdmin.controller';
import ApiAnalyticsController from './api/admin/analytics/analytics.controller';
import ApiMembersController from './api/admin/members/members.controller';
import ApiPoliciesController from './api/admin/policies/policies.controller';
import ApiEndpointController from './api/admin/endpoint/endpointConfiguration.controller';
import AddPoliciesPathController from './api/admin/policies/addPoliciesPath.controller';
import ApiHealthCheckController from './api/admin/healthcheck/healthcheck.controller';
import ApiResourcesController from './api/admin/resources/resources.controller';
import NewApiController from './api/admin/creation/newApi.controller';
import DialogApiSwaggerImportController from './api/admin/creation/swagger/importSwaggerDialog.controller';
import DialogAssertionInformationController from './api/admin/healthcheck/healthcheck-assertion-dialog.controller';
import DialogApiPermissionsHelpController from './api/admin/members/api-permissions-dialog.controller';
import DialogApplicationPermissionsHelpController from './application/details/members/application-permissions-dialog.controller';
import ApiPropertiesController from './api/admin/properties/properties.controller';
import SubscriptionsController from './api/admin/subscriptions/subscriptions.controller';
import ApiEventsController from './api/admin/events/apiEvents.controller';
import ApiHistoryController from './api/admin/history/apiHistory.controller';
import DialogAddPropertyController from './api/admin/properties/add-property.dialog.controller';
import DialogAddMemberApiController from './api/admin/members/addMemberDialog.controller';
import DialogTransferApiController from './api/admin/members/transferAPIDialog.controller';
import DialogApiKeyExpirationController from './api/admin/subscriptions/apikey.expiration.dialog.controller';
import DialogEditPolicyController from './api/admin/policies/dialog/policyDialog.controller';
import UserService from './services/user.service';
import UserController from './user/user.controller';
import NotificationService from './services/notification.service';
import DocumentationDirective from './api/admin/documentation/apiDocumentation.directive';
import DocumentationController from './api/admin/documentation/apiDocumentation.controller';
import DocumentationService from './services/apiDocumentation.service';
import ApplicationsController from './application/applications.controller';
import ApplicationController from './application/details/applications.controller';
import ApplicationGeneralController from './application/details/general/applicationGeneral.controller';
import ApplicationMembersController from './application/details/members/applicationMembers.controller';
import ApplicationSubscriptionsController from './application/details/subscriptions/applicationSubscriptions.controller';
import ApplicationAnalyticsController from './application/details/analytics/analytics.controller';
import DialogApplicationController from './application/dialog/applicationDialog.controller';
import DialogSubscribeApiController from './application/dialog/subscribeApiDialog.controller';
import DialogAddMemberController from './application/dialog/addMemberDialog.controller';
import ApplicationService from './services/applications.service';
import SideNavDirective from './components/sidenav/sidenav.directive';
import PageController from './api/admin/documentation/page/apiPage.controller';
import PolicyService from './services/policy.service';
import ResourceService from './services/resource.service';
import FetcherService from './services/fetcher.service';
import PageDirective from './components/documentation/page.directive';
import LoginController from './login/login.controller';
import RoleDirective from './components/role/role.directive';
import DiffDirective from './components/diff/diff.directive';
import DialogApiDefinitionController from './api/admin/general/dialog/apiDefinitionDialog.controller';
import InstancesService from './services/instances.service';
import InstancesController from './instances/instances.controller';
import InstanceController from './instances/details/instance.controller';
import InstanceEnvironmentController from './instances/details/environment/instanceEnvironment.controller';
import InstanceMonitoringController from './instances/details/monitoring/instanceMonitoring.controller';
import ImageDirective from './components/image/image.directive';
import EventsService from './services/events.service';
import AnalyticsService from './services/analytics.service';
import DashboardController from './platform/dashboard/dashboard.controller';
import PageSwaggerConfigurationService from './services/pageSwaggerConfiguration.service';
import PageSwaggerHttpClientService from './services/pageSwaggerHttpClient.service';
import DashboardModelDirective from './platform/dashboard/dashboardModel.directive';
import ViewsController from './configuration/admin/views/views.controller';
import ViewService from './services/view.service';
import DeleteViewDialogController from './configuration/admin/views/deleteview.dialog.controller';
import AnalyticsAPIModelDirective from './application/details/analytics/analyticsAPIModel.directive';
import AnalyticsApplicationModelDirective from './api/admin/analytics/analyticsApplicationModel.directive';
import GroupsController from './configuration/admin/groups/groups.controller';
import GroupService from './services/group.service';
import DialogAddGroupController from './configuration/admin/groups/dialog/add-group.dialog.controller';
import DialogAddGroupMemberController from './configuration/admin/groups/dialog/addMemberDialog.controller';
import RegistrationController from './registration/registration.controller';
import ConfirmController from './registration/confirm/confirm.controller';
import DialogSubscribePlanController from './api/portal/plan/subscribePlanDialog.controller';
import SubscriptionService from './services/subscription.service';
import ApiPlansController from './api/admin/plans/apiPlans.controller';
import DialogSubscriptionRejectController from './api/admin/subscriptions/subscription.reject.dialog.controller';
import DialogSubscriptionAcceptController from './api/admin/subscriptions/subscription.accept.dialog.controller';
import DialogSubscriptionCreateController from './api/admin/subscriptions/subscription.create.dialog.controller';
import EmptyStateDirective from './components/emptystate/emptystate.directive';
import DialogClosePlanController from './api/admin/plans/closePlanDialog.controller';
import DialogPublishPlanController from './api/admin/plans/publishPlanDialog.controller';

angular.module('gravitee', ['ui.router', 'ngMaterial', 'ramlConsoleApp', 'ng-showdown', 'swaggerUi',
  'ngMdIcons', 'ui.codemirror', 'md.data.table', 'ngCookies', 'dragularModule', 'readMore',
  'ngMessages', 'vAccordion', 'schemaForm', 'ngclipboard', 'ui.validate', 'gvConstants', 'angular-timeline',
  'ab-base64',  'ngFileUpload', 'n3-pie-chart', 'tc.chartjs', 'md-steppers', 'ui.tree', 'angular-jwt'])
  .config(config)
  .config(routerConfig)
  .config(interceptorConfig)
  .config(delegatorConfig)
  .config(function ($mdThemingProvider) {
    $mdThemingProvider.theme('default')
      .primaryPalette('blue-grey')
      .accentPalette('blue');

    $mdThemingProvider.theme('sidenav')
      .backgroundPalette('grey', {
        'default': '50'
      });

    $mdThemingProvider.theme('toast-success');
    $mdThemingProvider.theme('toast-error');
  })
  .config(function ($showdownProvider) {
    $showdownProvider.setOption('tables', true);
    $showdownProvider.loadExtension('prettify');
  })
  .run(runBlock)
  .controller('ApisController', ApisController)
  .controller('ApiAdminController', ApiAdminController)
  .controller('ApiAnalyticsController', ApiAnalyticsController)
  .controller('ApiPoliciesController', ApiPoliciesController)
  .controller('AddPoliciesPathController', AddPoliciesPathController)
  .controller('ApiMembersController', ApiMembersController)
  .controller('ApiPortalController', ApiPortalController)
  .controller('ApiPortalPagesController', ApiPortalPagesController)
  .controller('ApiPortalPageController', ApiPortalPageController)
  .controller('ApiGeneralController', ApiGeneralController)
  .controller('ApiHealthCheckController', ApiHealthCheckController)
  .controller('ApiEndpointController', ApiEndpointController)
  .controller('DialogAssertionInformationController', DialogAssertionInformationController)
  .controller('DialogApiPermissionsHelpController', DialogApiPermissionsHelpController)
  .controller('DialogApplicationPermissionsHelpController', DialogApplicationPermissionsHelpController)
  .controller('ApiPropertiesController', ApiPropertiesController)
  .controller('SubscriptionsController', SubscriptionsController)
  .controller('ApiEventsController', ApiEventsController)
  .controller('ApiHistoryController', ApiHistoryController)
  .controller('ApiResourcesController', ApiResourcesController)
  .controller('NewApiController', NewApiController)
  .controller('DialogApiSwaggerImportController', DialogApiSwaggerImportController)
  .controller('DialogAddPropertyController', DialogAddPropertyController)
  .controller('DialogAddMemberApiController', DialogAddMemberApiController)
  .controller('DialogTransferApiController', DialogTransferApiController)
  .controller('DialogApiKeyExpirationController', DialogApiKeyExpirationController)
  .controller('UserController', UserController)
  .controller('DocumentationController', DocumentationController)
  .controller('ApplicationsController', ApplicationsController)
  .controller('ApplicationController', ApplicationController)
  .controller('ApplicationGeneralController', ApplicationGeneralController)
  .controller('ApplicationMembersController', ApplicationMembersController)
  .controller('ApplicationSubscriptionsController', ApplicationSubscriptionsController)
  .controller('ApplicationAnalyticsController', ApplicationAnalyticsController)
  .controller('DialogApplicationController', DialogApplicationController)
  .controller('DialogSubscribeApiController', DialogSubscribeApiController)
  .controller('DialogAddMemberController', DialogAddMemberController)
  .controller('DialogApiDefinitionController', DialogApiDefinitionController)
  .controller('DialogEditPolicyController', DialogEditPolicyController)
  .controller('PageController', PageController)
  .controller('LoginController', LoginController)
  .controller('InstancesController', InstancesController)
  .controller('InstanceController', InstanceController)
  .controller('InstanceEnvironmentController', InstanceEnvironmentController)
  .controller('InstanceMonitoringController', InstanceMonitoringController)
  .controller('DashboardController', DashboardController)
  .controller('ViewsController', ViewsController)
  .controller('DeleteViewDialogController', DeleteViewDialogController)
  .controller('GroupsController', GroupsController)
  .controller('DialogAddGroupController', DialogAddGroupController)
  .controller('DialogAddGroupMemberController', DialogAddGroupMemberController)
  .controller('RegistrationController', RegistrationController)
  .controller('ConfirmController', ConfirmController)
  .controller('ApiPortalPlanController', ApiPortalPlanController)
  .controller('DialogSubscribePlanController', DialogSubscribePlanController)
  .controller('ApiPlansController', ApiPlansController)
  .controller('DialogSubscriptionRejectController', DialogSubscriptionRejectController)
  .controller('DialogSubscriptionAcceptController', DialogSubscriptionAcceptController)
  .controller('DialogSubscriptionCreateController', DialogSubscriptionCreateController)
  .controller('DialogClosePlanController', DialogClosePlanController)
  .controller('DialogPublishPlanController', DialogPublishPlanController)
  .service('ApplicationService', ApplicationService)
  .service('ApiService', ApiService)
  .service('DocumentationService', DocumentationService)
  .service('InstancesService', InstancesService)
  .service('NotificationService', NotificationService)
  .service('PolicyService', PolicyService)
  .service('UserService', UserService)
  .service('ResourceService', ResourceService)
  .service('FetcherService', FetcherService)
  .service('EventsService', EventsService)
  .service('AnalyticsService', AnalyticsService)
  .service('PageSwaggerConfigurationService', PageSwaggerConfigurationService)
  .service('PageSwaggerHttpClientService', PageSwaggerHttpClientService)
  .service('ViewService', ViewService)
  .service('GroupService', GroupService)
  .service('SubscriptionService', SubscriptionService)
  .directive('filecontent', () => new DocumentationDirective())
  .directive('graviteeSidenav', () => new SideNavDirective())
  .directive('graviteePage', () => new PageDirective())
  .directive('noDirtyCheck', () => new FormDirective())
  .directive('autofocus', () => new AutofocusDirective())
  .directive('graviteeRolesAllowed', () => new RoleDirective())
  .directive('graviteeDiff', () => new DiffDirective())
  .directive('graviteeImage', () => new ImageDirective())
  .directive('graviteeDashboardModel', () => new DashboardModelDirective())
  .directive('graviteeAnalyticsApiModel', () => new AnalyticsAPIModelDirective())
  .directive('graviteeAnalyticsApplicationModel', () => new AnalyticsApplicationModelDirective())
  .directive('graviteeEmptyState', () => new EmptyStateDirective())
  .filter('humanDateFilter', function () {
    return function(input) {
      if (!moment().subtract(1, 'weeks').isAfter(input)) {
        return moment(input).fromNow();
      } else {
        return moment(input).format('D MMM. YYYY');
      }
    };
  })
  .filter('apiKeyFilter', function () {
    return function (keys) {
      return keys;
    };
  });
