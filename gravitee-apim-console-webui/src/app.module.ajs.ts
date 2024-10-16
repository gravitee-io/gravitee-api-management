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
/* eslint-disable @typescript-eslint/no-var-requires, import/order */
import * as jQuery from 'jquery';
import 'angular-aria';
import 'angular-animate';
import 'angular-material';
import 'angular-sanitize';
import { marked } from 'marked';

import * as hljs from 'highlight.js';
// Codemirror
import * as CodeMirror from 'codemirror';
import moment from 'moment';
import * as tinycolor from 'tinycolor2';
import AutofocusDirective from './components/autofocus/autofocus.directive';
import GvModelDirective from './libraries/gv-model.directive';
import { ApiService } from './services/api.service';
import CorsService from './services/cors.service';
import { ApiV1PoliciesComponentAjs } from './management/api/policy-studio-v1/policies/policies.component.ajs';
import AddPoliciesPathController from './management/api/policy-studio-v1/policies/addPoliciesPath.controller';
import { ApiV1ResourcesComponentAjs } from './management/api/resources-v1/resources.component.ajs';
import { ApiV1PropertiesComponentAjs } from './management/api/properties-v1/properties.component.ajs';
import { ApiHistoryComponentAjs } from './management/api/audit/history/apiHistory.component.ajs';
import DialogAddPropertyController from './management/api/properties-v1/add-property.dialog.controller';
import DialogEditPolicyController from './management/api/policy-studio-v1/policies/dialog/policyDialog.controller';
import FileContentDirective from './components/filecontent/filecontent.directive';
import FileLoaderDirective from './components/dialog/fileloader/fileloader.directive';

import { DocumentationService } from './services/documentation.service';
import NotificationService from './services/notification.service';

import PolicyService from './services/policy.service';
import ResourceService from './services/resource.service';
import FetcherService from './services/fetcher.service';
import NotifierService from './services/notifier.service';

import DiffDirective from './management/api/audit/history/diff/diff.directive';
// Api
import ApiCreationV2ComponentAjs from './management/api/creation-v2/steps/api-creation-v2.component.ajs';
import ApiCreationV2ControllerAjs from './management/api/creation-v2/steps/api-creation-v2.controller.ajs';
import ApiCreationStep1Component from './management/api/creation-v2/steps/api-creation-step1.component';
import ApiCreationStep2Component from './management/api/creation-v2/steps/api-creation-step2.component';
import ApiCreationStep3Component from './management/api/creation-v2/steps/api-creation-step3.component';
import ApiCreationStep4Component from './management/api/creation-v2/steps/api-creation-step4.component';
import ApiCreationStep5Component from './management/api/creation-v2/steps/api-creation-step5.component';
// API Plan
import ApiPlanComponent from './management/application/components/api-plan/api-plan.component';
// API PrimaryOwner Mode
import ApiPrimaryOwnerModeService from './services/apiPrimaryOwnerMode.service';
// Applications
import ApplicationService from './services/application.service';
import ApplicationTypesService from './services/applicationTypes.service';

import ApplicationCreationComponentAjs from './management/application/creation/steps/application-creation.component.ajs';
import ApplicationCreationController from './management/application/creation/steps/application-creation.controller';
import ApplicationCreationStep1Component from './management/application/creation/steps/application-creation-step1.component';
import ApplicationCreationStep2Component from './management/application/creation/steps/application-creation-step2.component';
import ApplicationCreationStep2Controller from './management/application/creation/steps/application-creation-step2.controller';
import ApplicationCreationStep3Component from './management/application/creation/steps/application-creation-step3.component';
import ApplicationCreationStep4Component from './management/application/creation/steps/application-creation-step4.component';

import ApplicationHeaderComponent from './management/application/details/header/application-header.component';
import ApplicationSubscriptionComponentAjs from './management/application/details/subscriptions/application-subscription.component.ajs';
import ApplicationAnalyticsController from './management/application/details/analytics/application-analytics.controller';
import ApplicationAnalyticsComponentAjs from './management/application/details/analytics/application-analytics.component.ajs';
import ApplicationLogsController from './management/application/details/logs/application-logs.controller';
import ApplicationLogsComponentAjs from './management/application/details/logs/application-logs.component.ajs';
import ApplicationLogComponentAjs from './management/application/details/logs/application-log.component.ajs';
// Analytics / widgets
import WidgetComponent from './components/dashboard/widget/widget.component';
import WidgetDataTableComponent from './components/dashboard/widget/table/widget-data-table.component';
import WidgetChartLineComponent from './components/dashboard/widget/line/widget-chart-line.component';
import WidgetChartPieComponent from './components/dashboard/widget/pie/widget-chart-pie.component';
import WidgetChartMapComponent from './components/dashboard/widget/map/widget-chart-map.component';
import WidgetDataStatsComponent from './components/dashboard/widget/stats/widget-data-stats.component';
import WidgetDataTableConfigurationComponent from './components/dashboard/widget/table/widget-data-table-configuration.component';
import WidgetChartLineConfigurationComponent from './components/dashboard/widget/line/widget-chart-line-configuration.component';
import WidgetChartMapConfigurationComponent from './components/dashboard/widget/map/widget-chart-map-configuration.component';
import WidgetChartPieConfigurationComponent from './components/dashboard/widget/pie/widget-chart-pie-configuration.component';
import WidgetDataStatsConfigurationComponent from './components/dashboard/widget/stats/widget-data-stats-configuration.component';

import DashboardComponent from './components/dashboard/dashboard.component';
import DashboardFilterComponent from './components/dashboard/dashboard-filter.component';
import DashboardFilterController from './components/dashboard/dashboard-filter.controller';
import DashboardTimeframeComponent from './components/dashboard/dashboard-timeframe.component';
import DashboardTimeframeController from './components/dashboard/dashboard-timeframe.controller';
import ContextualDocComponentAjs from './components/contextual/contextual-doc.component.ajs';
import ContextualDocController from './components/contextual/contextual-doc.controller';
// Logs
import { ApiAnalyticsLogsComponentAjs } from './management/api/analytics/logs/analytics-logs.component.ajs';
import LogsTimeframeComponent from './components/logs/logs-timeframe.component';
import LogsTimeframeController from './components/logs/logs-timeframe.controller';
import LogsFiltersComponent from './components/logs/logs-filters.component';
import LogsFiltersController from './components/logs/logs-filters.controller';

import { ApiAnalyticsLogComponentAjs } from './management/api/analytics/logs/analytics-log.component.ajs';
// Others
import EnvironmentService from './services/environment.service';

import ErrorController from './components/documentation/error/error.controller';
import IdentityPictureDirective from './components/identityPicture/identityPicture.directive';
import ImageDirective from './components/image/image.directive';
import { EventService } from './services/event.service';
import AnalyticsService from './services/analytics.service';
import PlatformLogsController from './management/analytics/logs/platform-logs.controller';
import PlatformLogsComponentAjs from './management/analytics/logs/platform-logs.component.ajs';
import PlatformLogComponentAjs from './management/analytics/logs/platform-log.component.ajs';

import CategoryService from './services/category.service';
import DialogAddGroupMemberController from './management/settings/groups/group/addMemberDialog.controller';
import SubscriptionService from './services/subscription.service';
import EmptyStateDirective from './components/emptystate/emptystate.directive';
import TagService from './services/tag.service';
import ChartDirective from './management/api/health-check-dashboard/chart/chart.directive';
import FileChooserDialogController from './components/dialog/fileChooserDialog.controller';
import DialogConfirmController from './components/dialog/confirmDialog.controller';
import DialogConfirmAndValidateController from './components/dialog/confirmAndValidateDialog.controller';
import DialogDynamicProviderHttpController from './management/api/properties-v1/dynamic-provider-http-dialog.controller';
import TenantService from './services/tenant.service';

import RoleService from './services/role.service';

// User
import UserService from './services/user.service';
import UserController from './user/my-accout/user.controller';
import UserComponentAjs from './user/my-accout/user.component.ajs';

// Documentation
import './components/documentation/documentation.module.ajs';

// Healthcheck
import ProgressBarComponent from './management/api/health-check-dashboard/progressbar/progress-bar.component';
import HealthCheckMetricComponent from './management/api/health-check-dashboard/healthcheckmetric/healthcheck-metric.component';
import { ApiHealthcheckDashboardComponentAjs } from './management/api/health-check-dashboard/healthcheck-dashboard.component.ajs';
import { ApiHealthcheckLogComponentAjs } from './management/api/health-check-dashboard/healthcheck-log.component.ajs';

// Ticket
import TicketService from './services/ticket.service';
// Audit
import AuditService from './services/audit.service';
import { ApiAuditComponentAjs } from './management/api/audit/general/audit.component.ajs';
import AuditComponent from './components/audit/audit.component';
// Configuration
import PortalSettingsService from './services/portalSettings.service';
// Groups
import GroupsComponentAjs from './management/settings/groups/groups.component.ajs';
import GroupComponentAjs from './management/settings/groups/group/group.component.ajs';
import GroupService from './services/group.service';
// Dictionaries
import DictionaryService from './services/dictionary.service';
import DictionariesComponentAjs from './management/settings/dictionaries/dictionaries.component.ajs';
import DictionariesController from './management/settings/dictionaries/dictionaries.controller';
import DictionaryComponentAjs from './management/settings/dictionaries/dictionary.component.ajs';
import DictionaryController from './management/settings/dictionaries/dictionary.controller';
import DialogDictionaryAddPropertyController from './management/settings/dictionaries/add-property.dialog.controller';
// Settings - Identity providers
// Others
import StringService from './services/string.service';

import interceptorConfig from './app.interceptor.ajs';

import { permission } from 'angular-permission';

// Alerts
import AlertService from './services/alert.service';
import AlertsComponentAjs from './components/alerts/alerts.component.ajs';
import AlertComponentAjs from './components/alerts/alert/alert.component.ajs';
import AlertNotificationsComponent from './components/alerts/alert/notifications/alert-notifications';
import AlertNotificationComponent from './components/alerts/alert/notifications/alert-notification';
import AlertHistoryComponent from './components/alerts/alert/history/alert-history.component';
import AlertTriggerDampeningComponent from './components/alerts/alert/triggers/trigger-dampening.component';
import AlertTriggerWindowComponent from './components/alerts/alert/triggers/trigger-window.component';
import AlertTriggerFiltersComponent from './components/alerts/alert/triggers/trigger-filters.component';
import AlertTriggerFilterComponent from './components/alerts/alert/triggers/trigger-filter.component';
import AlertTriggerConditionComponent from './components/alerts/alert/triggers/trigger-condition.component';
import AlertTriggerConditionThresholdComponent from './components/alerts/alert/triggers/conditions/trigger-condition-threshold.component';
import AlertTriggerConditionThresholdRangeComponent from './components/alerts/alert/triggers/conditions/trigger-condition-threshold-range.component';
import AlertTriggerConditionStringComponent from './components/alerts/alert/triggers/conditions/trigger-condition-string.component';
import AlertTriggerConditionCompareComponent from './components/alerts/alert/triggers/conditions/trigger-condition-compare.component';
import AlertTriggerMetricsSimpleConditionComponent from './components/alerts/alert/triggers/trigger-metrics-simple-condition.component';
import AlertTriggerMetricsAggregationComponent from './components/alerts/alert/triggers/trigger-metrics-aggregation.component';
import AlertTriggerMissingDataComponent from './components/alerts/alert/triggers/trigger-missing-data.component';
import AlertTriggerMetricsRateComponent from './components/alerts/alert/triggers/trigger-metrics-rate.component';
import AlertTriggerApiHealthCheckEndpointStatusChangedComponent from './components/alerts/alert/triggers/trigger-api-hc-endpoint-status-changed.component';
import AlertTriggerNodeLifecycleChangedComponent from './components/alerts/alert/triggers/trigger-node-lifecycle-changed.component';
import AlertTriggerNodeHealthcheckComponent from './components/alerts/alert/triggers/trigger-node-healthcheck.component';
import AlertTriggerApplicationQuotaComponent from './components/alerts/alert/triggers/trigger-application-quota.component';
import AlertTriggerProjectionsComponent from './components/alerts/alert/triggers/projections/trigger-projections.component';
import AlertTriggerProjectionComponent from './components/alerts/alert/triggers/projections/trigger-projection.component';
import AlertTriggerTimeframesComponent from './components/alerts/alert/triggers/trigger-timeframe.component';

import SelectFolderDialogController from './components/documentation/dialog/selectfolder.controller';
import SelectPageDialogController from './components/documentation/dialog/selectpage.controller';
// Settings - Client Registration
import DashboardService from './services/dashboard.service';
import SettingsAnalyticsDashboardComponentAjs from './management/settings/analytics/dashboard/settings-analytics-dashboard.components.ajs';
// Tokens
import TokenService from './services/token.service';
import DialogGenerateTokenController from './user/my-accout/token/generateTokenDialog.controller';
// Quick Time Range
import QuickTimeRangeComponent from './components/alerts/quick-time-range/quick-time-range.component';
import QuickTimeRangeController from './components/alerts/quick-time-range/quick-time-range.controller';

// User-Autocomplete
import UserAutocompleteComponent from './components/user-autocomplete/user-autocomplete.component';
import UserAutocompleteController from './components/user-autocomplete/user-autocomplete.controller';

import ApplicationSubscribeComponentAjs from './management/application/details/subscribe/application-subscribe.component.ajs';
import ApplicationSubscribeController from './management/application/details/subscribe/application-subscribe.controller';
import ApiKeyModeChoiceDialogController from './components/dialog/apiKeyMode/api-key-mode-choice-dialog.controller';

import DialogQueryFilterInformationController from './management/settings/analytics/dashboard/query-filter-information.dialog.controller';

import PortalThemeController from './management/settings/portal-theme/portalTheme.controller';
import PortalThemeComponentAjs from './management/settings/portal-theme/portalTheme.component.ajs';
import PortalThemeService from './services/portalTheme.service';

import CustomUserFieldsService from './services/custom-user-fields.service';

import FlowService from './services/flow.service';
import AlertsDashboardComponent from './components/alerts/dashboard/alerts-dashboard.component';
import WidgetChartCountComponent from './components/dashboard/widget/count/widget-chart-count.component';
import * as angular from 'angular';

import { ApiAlertsDashboardComponentAjs } from './management/api/analytics/alerts/api-alerts-dashboard.component.ajs';

import { markedHighlight } from 'marked-highlight';
import { downgradeInjectable } from '@angular/upgrade/static';
import DialogTransferOwnershipController from './management/settings/groups/group/transferOwnershipDialog.controller';
import ApiKeysComponent from './management/application/components/api-key/api-keys.component';
import ApiKeysController from './management/application/components/api-key/api-keys.controller';
import { IfMatchEtagInterceptor } from './shared/interceptors/if-match-etag.interceptor';
import SearchAndSelectComponent from './components/logs/search-and-select/search-and-select.component';
import { SearchAndSelectController } from './components/logs/search-and-select/search-and-select.controller';
import AlertsActivityComponentAjs from './management/alerts/activity/alerts-activity.component.ajs';
import { ApiV2Service } from './services-ngx/api-v2.service';
import { GioPermissionService } from './shared/components/gio-permission/gio-permission.service';
import { ApiAnalyticsOverviewComponentAjs } from './management/api/analytics/overview/analytics-overview.component.ajs';
import { Router } from '@angular/router';
import SettingsAnalyticsComponentAjs from './management/settings/analytics/settings-analytics.component.ajs';
import AnalyticsDashboardComponentAjs from './management/analytics/analytics-dashboard/analytics-dashboard.component.ajs';
import { GroupV2Service } from './services-ngx/group-v2.service';

(<any>window).jQuery = jQuery;

(<any>window).hljs = hljs;

marked.use(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, language) {
      const validLanguage = hljs.getLanguage(language) ? language : 'plaintext';
      return hljs.highlight(validLanguage, code).value;
    },
  }),
);

(<any>window).CodeMirror = CodeMirror;

require('angular-highlightjs');

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

(<any>window).resolveUrl = function (url) {
  return url.startsWith('http') ? url : (<any>window).location.origin + url;
};

require('angular-material-icons');
require('angular-material-data-table');
require('angular-messages');

require('dragular');
require('v-accordion');

require('angular-schema-form');
require('./libraries/angular-schema-form/boostrap-decorator');
require('./libraries/angular-schema-form/codemirror-decorator');
require('./libraries/angular-ui-codemirror/ui-codemirror');

require('ngclipboard');
require('angular-ui-validate');
require('angular-timeline');
require('ng-file-upload');
require('md-steppers');
require('angular-ui-tree');

require('angular-gridster');
require('angular-scroll');
require('diff/dist/diff.min.js');
require('angular-loading-bar');

// Highcharts
const Highcharts = require('highcharts');
window.Highcharts = Highcharts;
require('highcharts/highcharts-more')(Highcharts);
require('highcharts/modules/solid-gauge')(Highcharts);
require('highcharts/modules/no-data-to-display')(Highcharts);
require('highcharts/modules/map')(Highcharts);

require('@highcharts/map-collection/custom/world');

(<any>window).moment = moment;
require('angular-moment-picker');

(<any>window).tinycolor = tinycolor;
require('md-color-picker');

angular.module('gravitee-management', [
  'angular-loading-bar',
  permission,
  'ngMaterial',
  'ngMdIcons',
  'ui.codemirror',
  'md.data.table',
  'dragularModule',
  'ngMessages',
  'vAccordion',
  'schemaForm',
  'ngclipboard',
  'ui.validate',
  'angular-timeline',
  'ngFileUpload',
  'md-steppers',
  'ui.tree',
  'gridster',
  'ngAnimate',
  'moment-picker',
  'mdColorPicker',
  'gravitee-component-documentation',
]);

const graviteeManagementModule = angular.module('gravitee-management');

const includeSpinnerConfig = (cfpLoadingBarProvider) => {
  cfpLoadingBarProvider.includeSpinner = false;
};
includeSpinnerConfig.$inject = ['cfpLoadingBarProvider'];
graviteeManagementModule.config(includeSpinnerConfig);

graviteeManagementModule.config(interceptorConfig);

// Hack to disable location provider. Now we only use angular
const disableAjsLocationProvider = ($provide) => {
  $provide.decorator('$browser', [
    '$delegate',
    ($delegate) => {
      $delegate.onUrlChange = () => null;
      $delegate.url = () => '';

      return $delegate;
    },
  ]);
};
disableAjsLocationProvider.$inject = ['$provide'];
graviteeManagementModule.config(disableAjsLocationProvider);

const themeConfig = ($mdThemingProvider: angular.material.IThemingProvider) => {
  $mdThemingProvider.definePalette('gravitee', {
    '0': '1b1d3c',
    '50': '1b1d3c',
    '100': '1b1d3c',
    '200': '1b1d3c',
    '300': '1b1d3c',
    '400': '1b1d3c',
    '500': '1b1d3c',
    '600': '1b1d3c',
    '700': '1b1d3c',
    '800': '1b1d3c',
    '900': '1b1d3c',
    A100: '1b1d3c',
    A200: '1b1d3c',
    A400: '1b1d3c',
    A700: '1b1d3c',
    contrastDefaultColor: 'light',
  });

  $mdThemingProvider.definePalette('graviteeWarn', {
    '0': 'be1818',
    '50': 'be1818',
    '100': 'be1818',
    '200': 'be1818',
    '300': 'be1818',
    '400': 'be1818',
    '500': 'be1818',
    '600': 'be1818',
    '700': 'be1818',
    '800': 'be1818',
    '900': 'be1818',
    A100: 'be1818',
    A200: 'be1818',
    A400: 'be1818',
    A700: 'be1818',
    contrastDefaultColor: 'light',
  });

  $mdThemingProvider.theme('default').primaryPalette('gravitee').accentPalette('gravitee').warnPalette('graviteeWarn');

  $mdThemingProvider.theme('toast-success');
  $mdThemingProvider.theme('toast-error');
};
themeConfig.$inject = ['$mdThemingProvider'];
graviteeManagementModule.config(themeConfig);
// graviteeManagementModule.run(runBlock);

// New Navigation components

graviteeManagementModule.component('apiAnalyticsOverviewComponentAjs', ApiAnalyticsOverviewComponentAjs);
graviteeManagementModule.component('apiV1PoliciesComponentAjs', ApiV1PoliciesComponentAjs);
graviteeManagementModule.controller('AddPoliciesPathController', AddPoliciesPathController);
graviteeManagementModule.directive('apiHealthcheckDashboardChart', () => new ChartDirective());
graviteeManagementModule.component('gvHealthcheckMetric', HealthCheckMetricComponent);
graviteeManagementModule.component('apiHealthcheckDashboardComponentAjs', ApiHealthcheckDashboardComponentAjs);

graviteeManagementModule.component('apiV1PropertiesComponentAjs', ApiV1PropertiesComponentAjs);
graviteeManagementModule.directive('apiHistoryDiff', () => DiffDirective);
graviteeManagementModule.component('apiHistoryComponentAjs', ApiHistoryComponentAjs);
graviteeManagementModule.component('apiV1ResourcesComponentAjs', ApiV1ResourcesComponentAjs);
graviteeManagementModule.controller('DialogAddPropertyController', DialogAddPropertyController);
graviteeManagementModule.controller('UserController', UserController);
graviteeManagementModule.controller('DialogEditPolicyController', DialogEditPolicyController);
graviteeManagementModule.component('analyticsDashboardComponentAjs', AnalyticsDashboardComponentAjs);
graviteeManagementModule.component('gvAlertDashboard', AlertsDashboardComponent);
graviteeManagementModule.component('alertsActivityComponentAjs', AlertsActivityComponentAjs);
graviteeManagementModule.component('apiAlertsDashboardComponentAjs', ApiAlertsDashboardComponentAjs);
graviteeManagementModule.component('settingsGroupsAjs', GroupsComponentAjs);
graviteeManagementModule.component('settingsGroupEditAjs', GroupComponentAjs);
graviteeManagementModule.controller('DialogAddGroupMemberController', DialogAddGroupMemberController);
graviteeManagementModule.controller('DialogTransferOwnershipController', DialogTransferOwnershipController);
graviteeManagementModule.controller('FileChooserDialogController', FileChooserDialogController);
graviteeManagementModule.controller('DialogConfirmController', DialogConfirmController);
graviteeManagementModule.controller('DialogConfirmAndValidateController', DialogConfirmAndValidateController);
graviteeManagementModule.controller('DialogDynamicProviderHttpController', DialogDynamicProviderHttpController);
graviteeManagementModule.component('apiAuditComponentAjs', ApiAuditComponentAjs);
graviteeManagementModule.controller('PortalThemeController', PortalThemeController);
graviteeManagementModule.controller('SelectFolderDialogController', SelectFolderDialogController);
graviteeManagementModule.controller('SelectPageDialogController', SelectPageDialogController);
graviteeManagementModule.service('ApplicationService', ApplicationService);
graviteeManagementModule.service('ApplicationTypesService', ApplicationTypesService);
graviteeManagementModule.service('ApiService', ApiService);
graviteeManagementModule.service('ApiPrimaryOwnerModeService', ApiPrimaryOwnerModeService);
graviteeManagementModule.service('CorsService', CorsService);
graviteeManagementModule.service('DocumentationService', DocumentationService);
graviteeManagementModule.service('NotificationService', NotificationService);
graviteeManagementModule.service('PolicyService', PolicyService);
graviteeManagementModule.service('NotifierService', NotifierService);
graviteeManagementModule.service('UserService', UserService);
graviteeManagementModule.service('ResourceService', ResourceService);
graviteeManagementModule.service('FetcherService', FetcherService);
graviteeManagementModule.service('eventService', EventService);
graviteeManagementModule.service('AnalyticsService', AnalyticsService);
graviteeManagementModule.service('CategoryService', CategoryService);
graviteeManagementModule.service('GroupService', GroupService);
graviteeManagementModule.service('SubscriptionService', SubscriptionService);
graviteeManagementModule.service('TagService', TagService);
graviteeManagementModule.service('CustomUserFieldsService', CustomUserFieldsService);
graviteeManagementModule.service('TenantService', TenantService);
graviteeManagementModule.service('StringService', StringService);
graviteeManagementModule.service('RoleService', RoleService);
graviteeManagementModule.service('TicketService', TicketService);
graviteeManagementModule.service('AuditService', AuditService);
graviteeManagementModule.service('PortalThemeService', PortalThemeService);
graviteeManagementModule.service('TokenService', TokenService);
graviteeManagementModule.service('EnvironmentService', EnvironmentService);
graviteeManagementModule.service('FlowService', FlowService);
graviteeManagementModule.factory('ngApiV2Service', downgradeInjectable(ApiV2Service));
graviteeManagementModule.factory('ngGioPermissionService', downgradeInjectable(GioPermissionService));
graviteeManagementModule.factory('ngGroupV2Service', downgradeInjectable(GroupV2Service));

graviteeManagementModule.controller('DialogGenerateTokenController', DialogGenerateTokenController);

graviteeManagementModule.directive('filecontent', () => FileContentDirective);
graviteeManagementModule.directive('fileloader', () => FileLoaderDirective);
graviteeManagementModule.directive('autofocus', () => new AutofocusDirective());
graviteeManagementModule.directive('graviteeIdentityPicture', () => new IdentityPictureDirective());
graviteeManagementModule.directive('gvModel', () => new GvModelDirective());
graviteeManagementModule.directive('graviteeImage', () => new ImageDirective());
graviteeManagementModule.directive('graviteeEmptyState', () => new EmptyStateDirective());

graviteeManagementModule.component('gvWidget', WidgetComponent);
graviteeManagementModule.component('gvWidgetDataTable', WidgetDataTableComponent);
graviteeManagementModule.component('gvWidgetDataStats', WidgetDataStatsComponent);
graviteeManagementModule.component('gvWidgetChartPie', WidgetChartPieComponent);
graviteeManagementModule.component('gvWidgetChartLine', WidgetChartLineComponent);
graviteeManagementModule.component('gvWidgetChartMap', WidgetChartMapComponent);
graviteeManagementModule.component('gvWidgetChartCount', WidgetChartCountComponent);
graviteeManagementModule.component('gvWidgetDataTableConfiguration', WidgetDataTableConfigurationComponent);
graviteeManagementModule.component('gvWidgetDataLineConfiguration', WidgetChartLineConfigurationComponent);
graviteeManagementModule.component('gvWidgetDataMapConfiguration', WidgetChartMapConfigurationComponent);
graviteeManagementModule.component('gvWidgetDataPieConfiguration', WidgetChartPieConfigurationComponent);
graviteeManagementModule.component('gvWidgetDataStatsConfiguration', WidgetDataStatsConfigurationComponent);
graviteeManagementModule.controller('ErrorController', ErrorController);

graviteeManagementModule.component('settingsThemeAjs', PortalThemeComponentAjs);

graviteeManagementModule.component('settingsAnalyticsAjs', SettingsAnalyticsComponentAjs);

graviteeManagementModule.component('apiCreationV2ComponentAjs', ApiCreationV2ComponentAjs);
graviteeManagementModule.controller('ApiCreationV2AjsController', ApiCreationV2ControllerAjs);
graviteeManagementModule.component('apiCreationStep1', ApiCreationStep1Component);
graviteeManagementModule.component('apiCreationStep2', ApiCreationStep2Component);
graviteeManagementModule.component('apiCreationStep3', ApiCreationStep3Component);
graviteeManagementModule.component('apiCreationStep4', ApiCreationStep4Component);
graviteeManagementModule.component('apiCreationStep5', ApiCreationStep5Component);
graviteeManagementModule.component('gvDashboard', DashboardComponent);
graviteeManagementModule.component('gvDashboardFilter', DashboardFilterComponent);
graviteeManagementModule.controller('DashboardFilterController', DashboardFilterController);
graviteeManagementModule.component('gvDashboardTimeframe', DashboardTimeframeComponent);
graviteeManagementModule.controller('DashboardTimeframeController', DashboardTimeframeController);

// API subscriptions
graviteeManagementModule.component('apiKeys', ApiKeysComponent);
graviteeManagementModule.controller('ApiKeysController', ApiKeysController);

graviteeManagementModule.component('applicationSubscribe', ApplicationSubscribeComponentAjs);
graviteeManagementModule.controller('ApplicationSubscribeController', ApplicationSubscribeController);
graviteeManagementModule.controller('ApiKeyModeChoiceDialogController', ApiKeyModeChoiceDialogController);

graviteeManagementModule.component('createApplication', ApplicationCreationComponentAjs);
graviteeManagementModule.controller('ApplicationCreationController', ApplicationCreationController);
graviteeManagementModule.component('applicationCreationStep1', ApplicationCreationStep1Component);
graviteeManagementModule.component('applicationCreationStep2', ApplicationCreationStep2Component);
graviteeManagementModule.controller('ApplicationCreationStep2Controller', ApplicationCreationStep2Controller);
graviteeManagementModule.component('applicationCreationStep3', ApplicationCreationStep3Component);
graviteeManagementModule.component('applicationCreationStep4', ApplicationCreationStep4Component);

graviteeManagementModule.component('applicationHeader', ApplicationHeaderComponent);
graviteeManagementModule.component('applicationSubscription', ApplicationSubscriptionComponentAjs);
graviteeManagementModule.component('applicationAnalytics', ApplicationAnalyticsComponentAjs);
graviteeManagementModule.component('applicationLogs', ApplicationLogsComponentAjs);
graviteeManagementModule.component('applicationLog', ApplicationLogComponentAjs);
graviteeManagementModule.controller('ApplicationAnalyticsController', ApplicationAnalyticsController);
graviteeManagementModule.controller('ApplicationLogsController', ApplicationLogsController);
graviteeManagementModule.component('apiPlan', ApiPlanComponent);

graviteeManagementModule.component('user', UserComponentAjs);

graviteeManagementModule.component('apiAnalyticsLogsComponentAjs', ApiAnalyticsLogsComponentAjs);
graviteeManagementModule.component('gvLogsTimeframe', LogsTimeframeComponent);
graviteeManagementModule.controller('LogsTimeframeController', LogsTimeframeController);
graviteeManagementModule.component('apiAnalyticsLogComponentAjs', ApiAnalyticsLogComponentAjs);
graviteeManagementModule.component('gvLogsFilters', LogsFiltersComponent);
graviteeManagementModule.controller('LogsFiltersController', LogsFiltersController);
graviteeManagementModule.component('gvSearchAndSelect', SearchAndSelectComponent);
graviteeManagementModule.controller('SearchAndSelectController', SearchAndSelectController);

graviteeManagementModule.component('gvAudit', AuditComponent);
graviteeManagementModule.component('gvContextualDoc', ContextualDocComponentAjs);
graviteeManagementModule.controller('ContextualDocController', ContextualDocController);

// Healthcheck
graviteeManagementModule.component('apiHealthcheckLogComponentAjs', ApiHealthcheckLogComponentAjs);
graviteeManagementModule.component('progressBar', ProgressBarComponent);

// Configuration
graviteeManagementModule.service('PortalSettingsService', PortalSettingsService);

// Dictionaries
graviteeManagementModule.service('DictionaryService', DictionaryService);
graviteeManagementModule.component('settingsDictionariesAjs', DictionariesComponentAjs);
graviteeManagementModule.component('settingsDictionaryAjs', DictionaryComponentAjs);
graviteeManagementModule.controller('DictionariesController', DictionariesController);
graviteeManagementModule.controller('DictionaryController', DictionaryController);
graviteeManagementModule.controller('DialogDictionaryAddPropertyController', DialogDictionaryAddPropertyController);

// Alerts
graviteeManagementModule.service('AlertService', AlertService);
graviteeManagementModule.component('alertsComponentAjs', AlertsComponentAjs);
graviteeManagementModule.component('alertComponentAjs', AlertComponentAjs);
graviteeManagementModule.component('gvAlertNotification', AlertNotificationComponent);
graviteeManagementModule.component('gvAlertNotifications', AlertNotificationsComponent);
graviteeManagementModule.component('gvAlertHistory', AlertHistoryComponent);
graviteeManagementModule.component('gvAlertTriggerWindow', AlertTriggerWindowComponent);
graviteeManagementModule.component('gvAlertTriggerDampening', AlertTriggerDampeningComponent);
graviteeManagementModule.component('gvAlertTriggerCondition', AlertTriggerConditionComponent);
graviteeManagementModule.component('gvAlertTriggerFilters', AlertTriggerFiltersComponent);
graviteeManagementModule.component('gvAlertTriggerFilter', AlertTriggerFilterComponent);
graviteeManagementModule.component('gvAlertTriggerConditionThreshold', AlertTriggerConditionThresholdComponent);
graviteeManagementModule.component('gvAlertTriggerConditionThresholdRange', AlertTriggerConditionThresholdRangeComponent);
graviteeManagementModule.component('gvAlertTriggerConditionString', AlertTriggerConditionStringComponent);
graviteeManagementModule.component('gvAlertTriggerConditionCompare', AlertTriggerConditionCompareComponent);
graviteeManagementModule.component('gvAlertTriggerMetricsSimpleCondition', AlertTriggerMetricsSimpleConditionComponent);
graviteeManagementModule.component('gvAlertTriggerMetricsAggregation', AlertTriggerMetricsAggregationComponent);
graviteeManagementModule.component('gvAlertTriggerMissingData', AlertTriggerMissingDataComponent);
graviteeManagementModule.component('gvAlertTriggerMetricsRate', AlertTriggerMetricsRateComponent);
graviteeManagementModule.component('gvAlertTriggerApiHealthCheckStatusChanged', AlertTriggerApiHealthCheckEndpointStatusChangedComponent);
graviteeManagementModule.component('gvAlertTriggerNodeLifecycleChanged', AlertTriggerNodeLifecycleChangedComponent);
graviteeManagementModule.component('gvAlertTriggerNodeHealthcheck', AlertTriggerNodeHealthcheckComponent);
graviteeManagementModule.component('gvAlertTriggerApplicationQuota', AlertTriggerApplicationQuotaComponent);
graviteeManagementModule.component('gvAlertTriggerProjections', AlertTriggerProjectionsComponent);
graviteeManagementModule.component('gvAlertTriggerProjection', AlertTriggerProjectionComponent);
graviteeManagementModule.component('gvAlertTriggerTimeframe', AlertTriggerTimeframesComponent);

graviteeManagementModule.service('DashboardService', DashboardService);
graviteeManagementModule.component('settingsAnalyticsDashboardAjs', SettingsAnalyticsDashboardComponentAjs);
graviteeManagementModule.controller('DialogQueryFilterInformationController', DialogQueryFilterInformationController);

// Platform Analytics
graviteeManagementModule.component('platformLogsComponentAjs', PlatformLogsComponentAjs);
graviteeManagementModule.component('platformLogComponentAjs', PlatformLogComponentAjs);
graviteeManagementModule.controller('PlatformLogsController', PlatformLogsController);

// User-Autocomplete
graviteeManagementModule.component('gvUserAutocomplete', UserAutocompleteComponent);
graviteeManagementModule.controller('UserAutocompleteController', UserAutocompleteController);

// Quick Time range
graviteeManagementModule.component('gvQuickTimeRange', QuickTimeRangeComponent);
graviteeManagementModule.controller('QuickTimeRangeController', QuickTimeRangeController);

// Promotions

graviteeManagementModule.factory('ngIfMatchEtagInterceptor', downgradeInjectable(IfMatchEtagInterceptor));
graviteeManagementModule.factory('ngRouter', downgradeInjectable(Router));
graviteeManagementModule.filter('humanDateFilter', () => {
  return function (input) {
    if (input) {
      if (!moment().subtract(1, 'weeks').isAfter(input)) {
        return moment(input).fromNow();
      } else {
        return moment(input).format('ll');
      }
    }
  };
});
graviteeManagementModule.filter('humanDatetimeFilter', () => {
  return function (input) {
    if (input) {
      if (!moment().subtract(1, 'weeks').isAfter(input)) {
        return moment(input).fromNow();
      } else {
        return moment(input).format('D MMM YYYY HH:mm:ss');
      }
    }
  };
});
graviteeManagementModule.filter('datetimeFilter', () => {
  return function (input) {
    if (input) {
      return moment(input).format('D MMM YYYY HH:mm:ss');
    }
  };
});
graviteeManagementModule.filter('apiKeyFilter', () => {
  return function (keys) {
    return keys;
  };
});
graviteeManagementModule.filter('floor', () => {
  return function (input) {
    return Math.floor(input);
  };
});
