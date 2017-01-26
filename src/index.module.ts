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
import angular = require('angular');
import 'angular-aria';
import 'angular-animate';
import 'angular-material';
import 'angular-sanitize';

// require('raml-parser');
// require('api-console/dist/scripts/api-console');
//require('api-console/dist/scripts/api-console-vendor');
require('angular-material-icons');
require('angular-ui-codemirror');
require('angular-material-data-table');
require('angular-cookies');
require('angular-messages');

// Codemirror
import * as CodeMirror from 'codemirror';
(<any>window).CodeMirror = CodeMirror;

require('codemirror/mode/xml/xml.js');
require('codemirror/addon/edit/closetag.js');
require('codemirror/addon/fold/xml-fold.js');
require('codemirror/mode/javascript/javascript.js');
require('codemirror/mode/groovy/groovy.js');
require('codemirror/mode/gfm/gfm.js');
require('codemirror/addon/search/search.js');
require('codemirror/addon/search/searchcursor.js');
require('codemirror/addon/search/jump-to-line.js');
require('codemirror/addon/dialog/dialog.js');
require('codemirror/addon/lint/lint.js');
require('codemirror/addon/lint/json-lint.js');
require('codemirror/addon/lint/yaml-lint.js');
require('codemirror/addon/display/placeholder.js');

require('dragular');
require('v-accordion');
require('angular-schema-form');
require('./libraries/angular-schema-form/boostrap-decorator');
require('./libraries/angular-schema-form/codemirror-decorator');
require('angular-ui-codemirror');
require('ngclipboard');
require('angular-ui-validate');
require('read-more/js/directives/readmore.js');
require('angular-timeline');
require('angular-utf8-base64');
require('ng-file-upload');
require('md-steppers');
require('angular-ui-tree');
require('angular-jwt');
require('ng-showdown');
require('showdown-prettify');
require('../node_modules/angular-swagger-ui/dist/scripts/swagger-ui.js');
require('../node_modules/angular-swagger-ui/dist/scripts/modules/swagger-markdown.min.js');
require('../node_modules/angular-swagger-ui/dist/scripts/modules/swagger-auth.min.js');
require('../node_modules/angular-swagger-ui/dist/scripts/modules/swagger-yaml-parser.min.js');
require('../node_modules/angular-swagger-ui/dist/scripts/modules/swagger-xml-formatter.min.js');
require('../node_modules/angular-swagger-ui/dist/scripts/modules/swagger1-to-swagger2-converter.min.js');
require('angular-gridster');
require('diff/dist/diff.min.js');

// Highcharts

const Highcharts = require('highcharts');
const HighchartsMore = require('../node_modules/highcharts/js/highcharts-more.js');
const SolidGauge = require('../node_modules/highcharts/js/modules/solid-gauge.js');
const NoDataToDisplay = require('../node_modules/highcharts/js/modules/no-data-to-display.js');

HighchartsMore(Highcharts);
SolidGauge(Highcharts);
NoDataToDisplay(Highcharts);

import * as jsyaml from 'js-yaml';
(<any>window).jsyaml = jsyaml;

import * as marked from 'marked';
(<any>window).marked = marked;

import * as moment from 'moment';

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
import DialogAssertionInformationController from './api/admin/healthcheck/healthcheck-assertion-dialog.controller';
import DialogApiPermissionsHelpController from './api/admin/members/api-permissions-dialog.controller';
import ApiPropertiesController from './api/admin/properties/properties.controller';
import SubscriptionsController from './api/admin/subscriptions/subscriptions.controller';
import ApiEventsController from './api/admin/events/apiEvents.controller';
import ApiHistoryController from './api/admin/history/apiHistory.controller';
import DialogAddPropertyController from './api/admin/properties/add-property.dialog.controller';
import DialogAddMemberApiController from './api/admin/members/addMemberDialog.controller';
import DialogTransferApiController from './api/admin/members/transferAPIDialog.controller';
import DialogApiKeyExpirationController from './api/admin/subscriptions/apikey.expiration.dialog.controller';
import DialogEditPolicyController from './api/admin/policies/dialog/policyDialog.controller';
import NotificationService from './services/notification.service';
import DocumentationDirective from './api/admin/documentation/apiDocumentation.directive';
import DocumentationController from './api/admin/documentation/apiDocumentation.controller';
import DocumentationService from './services/apiDocumentation.service';

import PageController from './api/admin/documentation/page/apiPage.controller';
import PolicyService from './services/policy.service';
import ResourceService from './services/resource.service';
import FetcherService from './services/fetcher.service';
import LoginController from './login/login.controller';
import RoleDirective from './components/role/role.directive';
import DiffDirective from './components/diff/diff.directive';
import DialogApiImportController from './api/admin/general/dialog/apiImportDialog.controller';
import DialogApiExportController from './api/admin/general/dialog/apiExportDialog.controller';

// Sidenav
import SidenavService from './components/sidenav/sidenav.service';
import { SidenavComponent } from './components/sidenav/sidenav.component';
import { SubmenuComponent } from './components/sidenav/submenu.component';

//Api
import ApiHeaderComponent from './api/header/api-header.component';
import ApiCreationComponent from './api/admin/creation/steps/api-creation.component';
import ApiCreationController from './api/admin/creation/steps/api-creation.controller';
import ApiCreationStep1Component from './api/admin/creation/steps/api-creation-step1.component';
import ApiCreationStep2Component from './api/admin/creation/steps/api-creation-step2.component';
import ApiCreationStep3Component from './api/admin/creation/steps/api-creation-step3.component';
import ApiCreationStep4Component from './api/admin/creation/steps/api-creation-step4.component';
import ApiCreationStep5Component from './api/admin/creation/steps/api-creation-step5.component';
import ApiPlanComponent from './api/components/api-plan.component';
import ApiPagesComponent from './api/portal/documentation/api-pages.component';
import ApiPageComponent from './api/portal/documentation/api-page.component';

// Applications
import ApplicationService from './services/applications.service';
import ApplicationsComponent from './application/applications.component';
import ApplicationsController from './application/applications.controller';
import ApplicationComponent from './application/details/application.component';
import ApplicationHeaderComponent from './application/details/header/application-header.component';
import ApplicationGeneralController from './application/details/general/application-general.controller';
import ApplicationGeneralComponent from './application/details/general/application-general.component';
import ApplicationMembersController from './application/details/members/application-members.controller';
import ApplicationMembersComponent from './application/details/members/application-members.component';
import ApplicationSubscriptionsController from './application/details/subscriptions/application-subscriptions.controller';
import ApplicationSubscriptionsComponent from './application/details/subscriptions/application-subscriptions.component';
import ApplicationAnalyticsController from './application/details/analytics/application-analytics.controller';
import ApplicationAnalyticsComponent from './application/details/analytics/application-analytics.component';
import DialogApplicationController from './application/dialog/applicationDialog.controller';
import DialogAddMemberController from './application/details/members/addMemberDialog.controller';
import DialogApplicationPermissionsHelpController from './application/details/members/application-permissions-dialog.controller';

// Instances
import InstancesService from './services/instances.service';
import InstancesController from './instances/instances.controller';
import InstanceHeaderComponent from './instances/details/header/instance-header.component';
import InstanceEnvironmentController from './instances/details/environment/instance-environment.controller';
import InstanceEnvironmentComponent from './instances/details/environment/instance-environment.component';
import InstanceMonitoringComponent from './instances/details/monitoring/instance-monitoring.component';
import InstanceMonitoringController from './instances/details/monitoring/instance-monitoring.controller';
import InstancesComponent from './instances/instances.component';
import InstanceComponent from './instances/details/instance.component';

// Analytics / widgets
import WidgetComponent from './components/widget/widget.component';
import WidgetDataTableComponent from './components/widget/widget-data-table.component';
import WidgetChartLineComponent from './components/widget/widget-chart-line.component';
import WidgetChartPieComponent from './components/widget/widget-chart-pie.component';
import DashboardComponent from './components/dashboard/dashboard.component';
import DashboardFilterComponent from './components/dashboard/dashboard-filter.component';
import DashboardFilterController from './components/dashboard/dashboard-filter.controller';
import DashboardTimeframeComponent from './components/dashboard/dashboard-timeframe.component';
import DashboardTimeframeController from './components/dashboard/dashboard-timeframe.controller';

import ImageDirective from './components/image/image.directive';
import EventsService from './services/events.service';
import AnalyticsService from './services/analytics.service';
import DashboardController from './platform/dashboard/dashboard.controller';
import PageSwaggerConfigurationService from './services/pageSwaggerConfiguration.service';
import PageSwaggerHttpClientService from './services/pageSwaggerHttpClient.service';
import ViewsController from './configuration/admin/views/views.controller';
import ViewService from './services/view.service';
import DeleteViewDialogController from './configuration/admin/views/delete.view.dialog.controller';
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
import TagsController from './configuration/admin/tags/tags.controller';
import TagService from './services/tag.service';
import DeleteTagDialogController from './configuration/admin/tags/delete.tag.dialog.controller';
import ChartDirective from './components/chart/chart.directive';
import UserAvatarDirective from './components/avatar/user-avatar.directive';
import DialogConfirmController from './components/dialog/confirmDialog.controller';
import DialogDynamicProviderHttpController from './api/admin/properties/dynamic-provider-http-dialog.controller';
import TenantsController from './configuration/admin/tenants/tenants.controller';
import TenantService from './services/tenant.service';
import DeleteTenantDialogController from './configuration/admin/tenants/delete.tenant.dialog.controller';

import ViewsComponent from './configuration/admin/views/views.component';
import TenantsComponent from './configuration/admin/tenants/tenants.component';
import TagsComponent from './configuration/admin/tags/tags.component';

import apisConfig from './api/apis.route';
import applicationsConfig from './application/applications.route';
import configurationConfig from './configuration/configuration.route';

// User
import UserService from './services/user.service';
import UserController from './user/user.controller';
import UserComponent from './user/user.component';
import { submenuFilter } from './components/sidenav/submenu.filter';

// Documentation
import PageComponent from './components/documentation/page.component';
import PageSwaggerComponent from './components/documentation/page-swagger.component';
import PageRamlComponent from './components/documentation/page-raml.component';
import PageMarkdownComponent from './components/documentation/page-markdown.component';

angular.module('gravitee', ['ui.router', 'ngMaterial', /*'ramlConsoleApp',*/ 'ng-showdown', 'swaggerUi',
  'ngMdIcons', 'ui.codemirror', 'md.data.table', 'ngCookies', 'dragularModule', 'readMore',
  'ngMessages', 'vAccordion', 'schemaForm', 'ngclipboard', 'ui.validate', 'angular-timeline',
  'utf8-base64',  'ngFileUpload', 'md-steppers', 'ui.tree', 'angular-jwt', 'gridster'])
  .config(config)
  .config(routerConfig)
  .config(apisConfig)
  .config(configurationConfig)
  .config(applicationsConfig)
  .config(interceptorConfig)
  .config(delegatorConfig)
  .config(function ($mdThemingProvider: ng.material.IThemingProvider) {
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
  .controller('DialogAddPropertyController', DialogAddPropertyController)
  .controller('DialogAddMemberApiController', DialogAddMemberApiController)
  .controller('DialogTransferApiController', DialogTransferApiController)
  .controller('DialogApiKeyExpirationController', DialogApiKeyExpirationController)
  .controller('UserController', UserController)
  .controller('DocumentationController', DocumentationController)
  .controller('ApplicationsController', ApplicationsController)
  .controller('ApplicationGeneralController', ApplicationGeneralController)
  .controller('ApplicationMembersController', ApplicationMembersController)
  .controller('ApplicationSubscriptionsController', ApplicationSubscriptionsController)
  .controller('ApplicationAnalyticsController', ApplicationAnalyticsController)
  .controller('DialogApplicationController', DialogApplicationController)
  .controller('DialogAddMemberController', DialogAddMemberController)
  .controller('DialogApiImportController', DialogApiImportController)
  .controller('DialogApiExportController', DialogApiExportController)
  .controller('DialogEditPolicyController', DialogEditPolicyController)
  .controller('PageController', PageController)
  .controller('LoginController', LoginController)
  .controller('InstancesController', InstancesController)
  .controller('InstanceEnvironmentController', InstanceEnvironmentController)
  .controller('InstanceMonitoringController', InstanceMonitoringController)
  .controller('DashboardController', DashboardController)
  .controller('ViewsController', ViewsController)
  .controller('TenantsController', TenantsController)
  .controller('DeleteViewDialogController', DeleteViewDialogController)
  .controller('DeleteTenantDialogController', DeleteTenantDialogController)
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
  .controller('TagsController', TagsController)
  .controller('DeleteTagDialogController', DeleteTagDialogController)
  .controller('DialogConfirmController', DialogConfirmController)
  .controller('DialogDynamicProviderHttpController', DialogDynamicProviderHttpController)
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
  .service('TagService', TagService)
  .service('TenantService', TenantService)
  .directive('filecontent', () => DocumentationDirective)
  .directive('noDirtyCheck', () => new FormDirective())
  .directive('autofocus', () => new AutofocusDirective())
  .directive('graviteeRolesAllowed', () => RoleDirective)
  .directive('graviteeDiff', () => DiffDirective)
  .directive('graviteeImage', () => new ImageDirective())
  .directive('graviteeEmptyState', () => new EmptyStateDirective())
  .directive('graviteeChart', () => new ChartDirective())
  .directive('graviteeUserAvatar', () => new UserAvatarDirective())

  .component('gvWidget', WidgetComponent)
  .component('gvWidgetDataTable', WidgetDataTableComponent)
  .component('gvWidgetChartPie', WidgetChartPieComponent)
  .component('gvWidgetChartLine', WidgetChartLineComponent)

  .component('views', ViewsComponent)
  .component('tenants', TenantsComponent)
  .component('tags', TagsComponent)

  .component('instances', InstancesComponent)
  .component('instance', InstanceComponent)
  .component('instanceHeader', InstanceHeaderComponent)
  .component('instanceEnvironment', InstanceEnvironmentComponent)
  .component('instanceMonitoring', InstanceMonitoringComponent)

  .component('applications', ApplicationsComponent)
  .component('application', ApplicationComponent)
  .component('applicationHeader', ApplicationHeaderComponent)
  .component('applicationGeneral', ApplicationGeneralComponent)
  .component('applicationSubscriptions', ApplicationSubscriptionsComponent)
  .component('applicationMembers', ApplicationMembersComponent)
  .component('applicationAnalytics', ApplicationAnalyticsComponent)

  .component('apiHeader', ApiHeaderComponent)
  .component('apiCreation', ApiCreationComponent)
  .controller('ApiCreationController', ApiCreationController)
  .component('apiCreationStep1', ApiCreationStep1Component)
  .component('apiCreationStep2', ApiCreationStep2Component)
  .component('apiCreationStep3', ApiCreationStep3Component)
  .component('apiCreationStep4', ApiCreationStep4Component)
  .component('apiCreationStep5', ApiCreationStep5Component)
  .component('apiPlan', ApiPlanComponent)
  .component('apiPages', ApiPagesComponent)
  .component('apiPage', ApiPageComponent)
  .component('gvDashboard', DashboardComponent)
  .component('gvDashboardFilter', DashboardFilterComponent)
  .controller('DashboardFilterController', DashboardFilterController)
  .component('gvDashboardTimeframe', DashboardTimeframeComponent)
  .controller('DashboardTimeframeController', DashboardTimeframeController)

  .component('user', UserComponent)

  .component('gvPage', PageComponent)
  .component('gvPageMarkdown', PageMarkdownComponent)
  .component('gvPageSwagger', PageSwaggerComponent)
  .component('gvPageRaml', PageRamlComponent)

  .component('gvSidenav', SidenavComponent)
  .component('gvSubmenu', SubmenuComponent)

  .filter('currentSubmenus', submenuFilter)
  .service('SidenavService', SidenavService)

  .filter('humanDateFilter', function () {
    return function(input) {
      if (!moment().subtract(1, 'weeks').isAfter(input)) {
        return moment(input).fromNow();
      } else {
        return moment(input).format('D MMM. YYYY');
      }
    };
  })
  .filter('humanDatetimeFilter', function () {
    return function(input) {
      if (!moment().subtract(1, 'weeks').isAfter(input)) {
        return moment(input).fromNow();
      } else {
        return moment(input).format('D MMM. YYYY HH:mm:ss');
      }
    };
  })
  .filter('apiKeyFilter', function () {
    return function (keys) {
      return keys;
    };
  });

fetchData().then(bootstrapApplication);

function fetchData() {
  let initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
  let $http: ng.IHttpService = initInjector.get('$http');
  let $q: ng.IQService = initInjector.get('$q');

  return $q.all([$http.get('constants.json'), $http.get('build.json')]).then(function (responses: any) {
    angular.module('gravitee').constant('Constants', responses[0].data);
    angular.module('gravitee').constant('Build', responses[1].data);
  });
}

function bootstrapApplication() {
  angular.element(document).ready(function() {
    angular.bootstrap(document, ['gravitee']);
  });
}
