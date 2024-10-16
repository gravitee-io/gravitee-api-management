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
import 'angular-local-storage';
import { marked } from 'marked';

import * as traverse from 'traverse';
import * as hljs from 'highlight.js';
// Codemirror
import * as CodeMirror from 'codemirror';
import * as moment from 'moment';
import * as tinycolor from 'tinycolor2';
import FormDirective from '../components/form/form.directive';
import AutofocusDirective from '../components/autofocus/autofocus.directive';
import GvModelDirective from '../libraries/gv-model.directive';
import { ApiService } from '../services/api.service';
import CorsService from '../services/cors.service';
import { ApiV1PoliciesComponentAjs } from './api/design/policies/policies.component.ajs';
import AddPoliciesPathController from '../management/api/design/policies/addPoliciesPath.controller';
import { ApiV1ResourcesComponentAjs } from './api/proxy/resources-v1/resources.component.ajs';
import { ApiV1PropertiesComponentAjs } from './api/proxy/properties-v1/properties.component.ajs';
import { ApiHistoryComponentAjs } from './api/audit/history/apiHistory.component.ajs';
import DialogAddPropertyController from './api/proxy/properties-v1/add-property.dialog.controller';
import DialogEditPolicyController from '../management/api/design/policies/dialog/policyDialog.controller';
import FileContentDirective from '../components/filecontent/filecontent.directive';
import FileLoaderDirective from '../components/fileloader/fileloader.directive';

import { DocumentationService } from '../services/documentation.service';
import NotificationService from '../services/notification.service';

import ConnectorService from '../services/connector.service';
import PolicyService from '../services/policy.service';
import PortalService from '../services/portal.service';
import ResourceService from '../services/resource.service';
import FetcherService from '../services/fetcher.service';
import NotifierService from '../services/notifier.service';
import ServiceDiscoveryService from '../services/serviceDiscovery.service';
import LoginController from '../user/login/login.controller';
import { LogoutComponent } from '../user/logout/logout.component';

import DiffDirective from '../components/diff/diff.directive';
// Api
import ApiCreationV2ComponentAjs from './api/creation-v2/steps/api-creation-v2.component.ajs';
import ApiCreationV2ControllerAjs from './api/creation-v2/steps/api-creation-v2.controller.ajs';
import ApiCreationStep1Component from './api/creation-v2/steps/api-creation-step1.component';
import ApiCreationStep2Component from './api/creation-v2/steps/api-creation-step2.component';
import ApiCreationStep3Component from './api/creation-v2/steps/api-creation-step3.component';
import ApiCreationStep4Component from './api/creation-v2/steps/api-creation-step4.component';
import ApiCreationStep5Component from './api/creation-v2/steps/api-creation-step5.component';
// API Plan
import ApiPlanComponent from '../management/application/components/api-plan/api-plan.component';
// API PrimaryOwner Mode
import ApiPrimaryOwnerModeService from '../services/apiPrimaryOwnerMode.service';
// Applications
import ApplicationService from '../services/application.service';
import ApplicationTypesService from '../services/applicationTypes.service';

import ApplicationCreationComponent from './application/creation/steps/application-creation.component';
import ApplicationCreationController from './application/creation/steps/application-creation.controller';
import ApplicationCreationStep1Component from './application/creation/steps/application-creation-step1.component';
import ApplicationCreationStep2Component from './application/creation/steps/application-creation-step2.component';
import ApplicationCreationStep2Controller from './application/creation/steps/application-creation-step2.controller';
import ApplicationCreationStep3Component from './application/creation/steps/application-creation-step3.component';
import ApplicationCreationStep4Component from './application/creation/steps/application-creation-step4.component';

import ApplicationHeaderComponent from './application/details/header/application-header.component';
import ApplicationGeneralController from './application/details/general/application-general.controller';
import AjsApplicationGeneralComponent from './application/details/general/application-general.component';
import ApplicationMembersController from './application/details/members/application-members.controller';
import ApplicationMembersComponent from './application/details/members/application-members.component';
import ApplicationSubscriptionsController from './application/details/subscriptions/application-subscriptions.controller';
import ApplicationSubscriptionsComponent from './application/details/subscriptions/application-subscriptions.component';
import ApplicationSubscriptionComponent from './application/details/subscriptions/application-subscription.component';
import ApplicationAnalyticsController from './application/details/analytics/application-analytics.controller';
import ApplicationAnalyticsComponent from './application/details/analytics/application-analytics.component';
import ApplicationLogsController from './application/details/logs/application-logs.controller';
import ApplicationLogsComponent from './application/details/logs/application-logs.component';
import ApplicationLogComponent from './application/details/logs/application-log.component';
import DialogAddMemberController from './application/details/members/addMemberDialog.controller';
import DialogTransferApplicationController from './application/details/members/transferApplicationDialog.controller';
// Analytics / widgets
import WidgetComponent from '../components/widget/widget.component';
import WidgetDataTableComponent from '../components/widget/table/widget-data-table.component';
import WidgetChartLineComponent from '../components/widget/line/widget-chart-line.component';
import WidgetChartPieComponent from '../components/widget/pie/widget-chart-pie.component';
import WidgetChartMapComponent from '../components/widget/map/widget-chart-map.component';
import WidgetDataStatsComponent from '../components/widget/stats/widget-data-stats.component';
import WidgetDataTableConfigurationComponent from '../components/widget/table/widget-data-table-configuration.component';
import WidgetChartLineConfigurationComponent from '../components/widget/line/widget-chart-line-configuration.component';
import WidgetChartMapConfigurationComponent from '../components/widget/map/widget-chart-map-configuration.component';
import WidgetChartPieConfigurationComponent from '../components/widget/pie/widget-chart-pie-configuration.component';
import WidgetDataStatsConfigurationComponent from '../components/widget/stats/widget-data-stats-configuration.component';

import DashboardComponent from '../components/dashboard/dashboard.component';
import DashboardFilterComponent from '../components/dashboard/dashboard-filter.component';
import DashboardFilterController from '../components/dashboard/dashboard-filter.controller';
import DashboardTimeframeComponent from '../components/dashboard/dashboard-timeframe.component';
import DashboardTimeframeController from '../components/dashboard/dashboard-timeframe.controller';
import ContextualDocComponent from '../components/contextual/contextual-doc.component';
import ContextualDocController from '../components/contextual/contextual-doc.controller';
// Logs
import { ApiAnalyticsLogsComponentAjs } from './api/analytics/logs/analytics-logs.component.ajs';
import LogsTimeframeComponent from '../components/logs/logs-timeframe.component';
import LogsTimeframeController from '../components/logs/logs-timeframe.controller';
import LogsFiltersComponent from '../components/logs/logs-filters.component';
import LogsFiltersController from '../components/logs/logs-filters.controller';

import { ApiAnalyticsLogComponentAjs } from './api/analytics/logs/analytics-log.component.ajs';
// Others
import EnvironmentService from '../services/environment.service';
import OrganizationService from '../services/organization.service';
import InstallationService from '../services/installation.service';

import ErrorComponent from '../components/error/error.component';
import ErrorController from '../components/error/error.controller';
import IdentityPictureDirective from '../components/identityPicture/identityPicture.directive';
import ImageDirective from '../components/image/image.directive';
import { EventService } from '../services/event.service';
import AnalyticsService from '../services/analytics.service';
import AnalyticsDashboardController from '../management/dashboard-ajs/analytics-dashboard/analytics-dashboard.controller';
import PlatformLogsController from '../management/platform/logs/platform-logs.controller';
import PlatformLogsComponent from '../management/platform/logs/platform-logs.component';
import PlatformLogComponent from '../management/platform/logs/platform-log.component';

import CategoriesController from '../management/configuration/categories/categories.controller';
import CategoryController from './configuration/categories/category/category.controller';
import CategoryService from '../services/category.service';
import DeleteCategoryDialogController from '../management/configuration/categories/delete.category.dialog.controller';
import DeleteAPICategoryDialogController from './configuration/categories/category/delete-api-category.dialog.controller';
import DialogAddGroupMemberController from './configuration/groups/group/addMemberDialog.controller';
import RegistrationController from '../user/registration/registration.controller';
import ConfirmController from '../user/registration/confirm/confirm.controller';
import ResetPasswordController from '../user/resetPassword/resetPassword.controller';
import SubscriptionService from '../services/subscription.service';
import EmptyStateDirective from '../components/emptystate/emptystate.directive';
import TagService from '../services/tag.service';
import MetadataController from '../components/metadata/metadata.controller';
import MetadataService from '../services/metadata.service';
import DeleteMetadataDialogController from '../components/metadata/dialog/delete.metadata.dialog.controller';
import NewMetadataDialogController from '../components/metadata/dialog/new.metadata.dialog.controller';
import UpdateMetadataDialogController from '../components/metadata/dialog/update.metadata.dialog.controller';
import ChartDirective from '../components/chart/chart.directive';
import UserAvatarDirective from '../components/avatar/user-avatar.directive';
import FileChooserDialogController from '../components/dialog/fileChooserDialog.controller';
import DialogConfirmController from '../components/dialog/confirmDialog.controller';
import DialogConfirmAndValidateController from '../components/dialog/confirmAndValidateDialog.controller';
import DialogDynamicProviderHttpController from './api/proxy/properties-v1/dynamic-provider-http-dialog.controller';
import TenantService from '../services/tenant.service';

import CategoriesComponent from '../management/configuration/categories/categories.component';
import CategoryComponent from './configuration/categories/category/category.component';
import MetadataValidatorDirective from '../components/metadata/metadata.validator.directive';

import RoleService from '../services/role.service';

import applicationRouterConfig from './application/applications.route';
import configurationRouterConfig from './configuration/configuration.route';
import globalNotificationsRouterConfig from './configuration/notifications/global.notifications.settings.route';
// User
import UserService from '../services/user.service';
import UserController from '../user/user.controller';
import UserComponent from '../user/user.component';
import { TasksComponent } from './tasks/tasks.component';
// Notification Settings
import NotificationsComponentAjs from '../components/notifications/notifications.component.ajs';
import NotificationSettingsService from '../services/notificationSettings.service';
import NotificationTemplatesService from '../services/notificationTemplates.service';

// Documentation
import '../components/documentation/documentation.module.ajs';

// Healthcheck
import ProgressBarComponent from '../components/progressbar/progress-bar.component';
import HealthCheckMetricComponent from '../components/healthcheckmetric/healthcheck-metric.component';
import { ApiHealthcheckDashboardComponentAjs } from './api/proxy/health-check-dashboard/healthcheck-dashboard.component.ajs';
import { ApiHealthcheckLogComponentAjs } from './api/proxy/health-check-dashboard/healthcheck-log.component.ajs';

// Ticket
import TicketService from '../services/ticket.service';
import SupportTicketController from '../management/support/ticket.controller';
// Audit
import AuditService from '../services/audit.service';
import { ApiAuditComponentAjs } from './api/audit/general/audit.component.ajs';
import AuditComponent from '../components/audit/audit.component';
// Configuration
import SettingsComponent from '../management/configuration/settings.component';
import ConsoleSettingsService from '../services/consoleSettings.service';
import PortalSettingsService from '../services/portalSettings.service';
import PortalConfigService from '../services/portalConfig.service';
// Groups
import GroupsComponent from '../management/configuration/groups/groups.component';
import GroupComponent from './configuration/groups/group/group.component';
import GroupService from '../services/group.service';
// Dictionaries
import DictionaryService from '../services/dictionary.service';
import DictionariesComponent from '../management/configuration/dictionaries/dictionaries.component';
import DictionariesController from '../management/configuration/dictionaries/dictionaries.controller';
import DictionaryComponent from '../management/configuration/dictionaries/dictionary.component';
import DictionaryController from '../management/configuration/dictionaries/dictionary.controller';
import DialogDictionaryAddPropertyController from '../management/configuration/dictionaries/add-property.dialog.controller';
// Settings - Identity providers
import IdentityProvidersComponent from '../components/identityProviders/identity-providers.component';
import IdentityProviderService from '../services/identityProvider.service';
// Others
import StringService from '../services/string.service';
import AuthenticationService from '../services/authentication.service';

import config from './management.config';
import routerConfig from '../index.route';
import managementRouterConfig from './management.route';
import interceptorConfig from './management.interceptor';
import delegatorConfig from './management.delegator';
import runBlock from './management.run';

import { permission, uiPermission } from 'angular-permission';

import DialogAddNotificationSettingsController from '../components/notifications/notificationsettings/addnotificationsettings.dialog.controller';

import TopApisController from './configuration/top-apis/top-apis.controller';
import TopApiService from '../services/top-api.service';
import TopApisComponent from '../management/configuration/top-apis/top-apis.component';
import AddTopApiDialogController from '../management/configuration/top-apis/dialog/add.top-api.dialog.controller';
import DeleteTopApiDialogController from '../management/configuration/top-apis/dialog/delete.top-api.dialog.controller';
import PortalSettingsComponent from './configuration/portal/portal.component';

import RouterService from '../services/router.service';

import MessageService from '../services/message.service';
import { MessagesComponent } from './messages/messages.component';

import ApiPortalHeaderComponent from '../management/configuration/api-portal-header/api-portal-header.component';
import ApiHeaderService from '../services/apiHeader.service';

import UpdateApiPortalHeaderDialogController from './configuration/api-portal-header/update.api-portal-header.dialog.controller';
import NewApiPortalHeaderDialogController from './configuration/api-portal-header/new.api-portal-header.dialog.controller';
import Base64Service from '../services/base64.service';
// Alerts
import AlertService from '../services/alert.service';
import AlertsComponentAjs from '../components/alerts/alerts.component.ajs';
import AlertComponentAjs from '../components/alerts/alert/alert.component.ajs';
import AlertNotificationsComponent from '../components/alerts/alert/notifications/alert-notifications';
import AlertNotificationComponent from '../components/alerts/alert/notifications/alert-notification';
import AlertHistoryComponent from '../components/alerts/alert/history/alert-history.component';
import AlertTriggerDampeningComponent from '../components/alerts/alert/triggers/trigger-dampening.component';
import AlertTriggerWindowComponent from '../components/alerts/alert/triggers/trigger-window.component';
import AlertTriggerFiltersComponent from '../components/alerts/alert/triggers/trigger-filters.component';
import AlertTriggerFilterComponent from '../components/alerts/alert/triggers/trigger-filter.component';
import AlertTriggerConditionComponent from '../components/alerts/alert/triggers/trigger-condition.component';
import AlertTriggerConditionThresholdComponent from '../components/alerts/alert/triggers/conditions/trigger-condition-threshold.component';
import AlertTriggerConditionThresholdRangeComponent from '../components/alerts/alert/triggers/conditions/trigger-condition-threshold-range.component';
import AlertTriggerConditionStringComponent from '../components/alerts/alert/triggers/conditions/trigger-condition-string.component';
import AlertTriggerConditionCompareComponent from '../components/alerts/alert/triggers/conditions/trigger-condition-compare.component';
import AlertTriggerMetricsSimpleConditionComponent from '../components/alerts/alert/triggers/trigger-metrics-simple-condition.component';
import AlertTriggerMetricsAggregationComponent from '../components/alerts/alert/triggers/trigger-metrics-aggregation.component';
import AlertTriggerMissingDataComponent from '../components/alerts/alert/triggers/trigger-missing-data.component';
import AlertTriggerMetricsRateComponent from '../components/alerts/alert/triggers/trigger-metrics-rate.component';
import AlertTriggerApiHealthCheckEndpointStatusChangedComponent from '../components/alerts/alert/triggers/trigger-api-hc-endpoint-status-changed.component';
import AlertTriggerNodeLifecycleChangedComponent from '../components/alerts/alert/triggers/trigger-node-lifecycle-changed.component';
import AlertTriggerNodeHealthcheckComponent from '../components/alerts/alert/triggers/trigger-node-healthcheck.component';
import AlertTriggerApplicationQuotaComponent from '../components/alerts/alert/triggers/trigger-application-quota.component';
import AlertTriggerProjectionsComponent from '../components/alerts/alert/triggers/projections/trigger-projections.component';
import AlertTriggerProjectionComponent from '../components/alerts/alert/triggers/projections/trigger-projection.component';
import AlertTriggerTimeframesComponent from '../components/alerts/alert/triggers/trigger-timeframe.component';

import CircularPercentageComponent from '../components/circularPercentage/circularPercentage.component';
import CircularPercentageController from '../components/circularPercentage/circularPercentage.controller';

import EntrypointService from '../services/entrypoint.service';

import SelectFolderDialogController from '../components/documentation/dialog/selectfolder.controller';
import SelectPageDialogController from '../components/documentation/dialog/selectpage.controller';
import AnalyticsSettingsComponent from './configuration/analytics/analytics.component';
// Settings - Client Registration
import { ClientRegistrationProvidersComponent } from './configuration/client-registration-providers/client-registration-providers.component';

import DashboardService from '../services/dashboard.service';
import AnalyticsDashboardComponent from './configuration/analytics/dashboard/dashboard.components';
// Tokens
import TokenService from '../services/token.service';
import DialogGenerateTokenController from '../user/token/generateTokenDialog.controller';
// Newsletter
import NewsletterReminderComponent from '../components/newsletter-subcription/newsletter-reminder.component';
// Quick Time Range
import QuickTimeRangeComponent from '../components/quick-time-range/quick-time-range.component';
import QuickTimeRangeController from '../components/quick-time-range/quick-time-range.controller';

// User-Autocomplete
import UserAutocompleteComponent from '../components/user-autocomplete/user-autocomplete.component';
import UserAutocompleteController from '../components/user-autocomplete/user-autocomplete.controller';

import ApplicationSubscribeComponent from './application/details/subscribe/application-subscribe.component';
import ApplicationSubscribeController from './application/details/subscribe/application-subscribe.controller';
import ApiKeyModeChoiceDialogController from '../components/dialog/apiKeyMode/api-key-mode-choice-dialog.controller';

import QualityRuleService from '../services/qualityRule.service';
import ApiQualityRulesComponent from '../management/configuration/api-quality-rules/api-quality-rules.component';
import ApiQualityRuleComponent from '../management/configuration/api-quality-rules/api-quality-rule/api-quality-rule.component';
import ApiQualityRuleController from '../management/configuration/api-quality-rules/api-quality-rule/api-quality-rule.controller';
import DeleteApiQualityRuleDialogController from '../management/configuration/api-quality-rules/api-quality-rule/delete-api-quality-rule.dialog.controller';
import DialogQueryFilterInformationController from './configuration/analytics/dashboard/query-filter-information.dialog.controller';

import ReCaptchaService from '../services/reCaptcha.service';

import PortalThemeController from './configuration/portal-theme/portalTheme.controller';
import PortalThemeComponent from './configuration/portal-theme/portalTheme.component';
import PortalThemeService from '../services/portalTheme.service';

import authenticationConfig from '../authentication/authentication.config';
import NewsletterSubscriptionController from '../user/newsletter/newsletter-subscription.controller';
import CustomUserFieldsComponent from './configuration/custom-user-fields/custom-user-fields.component';
import CustomUserFieldsController from './configuration/custom-user-fields/custom-user-fields.controller';
import CustomUserFieldsService from '../services/custom-user-fields.service';
import NewFieldDialogController from './configuration/custom-user-fields/dialog/new.custom-user-field.dialog.controller';
import DeleteFieldDialogController from './configuration/custom-user-fields/dialog/delete.custom-user-field.dialog.controller';
import UpdateFieldDialogController from './configuration/custom-user-fields/dialog/update.custom-user-field.dialog.controller';
import FlowService from '../services/flow.service';
import TicketsListController from './support/tickets-list.controller';
import TicketDetailComponent from './support/ticket-detail.component';
import SpelService from '../services/spel.service';
import AlertsDashboardComponent from '../components/alerts/dashboard/alerts-dashboard.component';
import WidgetChartCountComponent from '../components/widget/count/widget-chart-count.component';

import { PromotionService } from '../services/promotion.service';

(<any>window).jQuery = jQuery;

import * as angular from 'angular';

const ngInfiniteScroll = require('ng-infinite-scroll');
import { ApiAlertsDashboardComponentAjs } from './api/analytics/alerts/api-alerts-dashboard.component.ajs';
import MovedComponent from './configuration/moved/moved.component';

(<any>window).traverse = traverse;

(<any>window).hljs = hljs;
marked.setOptions({
  highlight: (code) => hljs.highlightAuto(code).value,
});

(<any>window).CodeMirror = CodeMirror;

require('satellizer');
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
require('angular-cookies');
require('angular-messages');

require('dragular');
require('v-accordion');

require('angular-schema-form');
require('../libraries/angular-schema-form/boostrap-decorator');
require('../libraries/angular-schema-form/codemirror-decorator');
require('../libraries/angular-ui-codemirror/ui-codemirror');

require('ngclipboard');
require('angular-ui-validate');
require('angular-timeline');
require('angular-utf8-base64');
require('ng-file-upload');
require('md-steppers');
require('angular-ui-tree');
require('angular-jwt');

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
import { DebugApiService } from '../services/debugApi.service';
import { downgradeComponent, downgradeInjectable } from '@angular/upgrade/static';
import DialogTransferOwnershipController from './configuration/groups/group/transferOwnershipDialog.controller';
import { CockpitService } from '../services-ngx/cockpit.service';

import { upgradeModule } from '@uirouter/angular-hybrid';
import uiRouter from '@uirouter/angularjs';
import { GioBannerComponent } from '@gravitee/ui-particles-angular';
import { GioPendoService } from '@gravitee/ui-analytics';
import ApplicationSubscriptionsListComponent from '../management/application/details/subscriptions/application-subscriptions-list.component';
import ApplicationSubscriptionsListController from '../management/application/details/subscriptions/application-subscriptions-list.controller';
import ApiKeysComponent from '../management/api-key/api-keys.component';
import ApiKeysController from '../management/api-key/api-keys.controller';
import { EnvAuditComponent } from './audit/env-audit.component';
import { EnvApplicationListComponent } from './application/list/env-application-list.component';
import { GioSideNavComponent } from '../components/gio-side-nav/gio-side-nav.component';
import { GioTopNavComponent } from '../components/gio-top-nav/gio-top-nav.component';
import { SettingsNavigationComponent } from './configuration/settings-navigation/settings-navigation.component';
import { ApplicationNavigationComponent } from './application/details/application-navigation/application-navigation.component';
import { IfMatchEtagInterceptor } from '../shared/interceptors/if-match-etag.interceptor';
import SearchAndSelectComponent from '../components/search-and-select/search-and-select.component';
import { SearchAndSelectController } from '../components/search-and-select/search-and-select.controller';
import AlertTabsController from '../components/alerts/alertTabs/alert-tabs-component';
import AlertsActivityController from '../components/alerts/activity/alerts-activity.controller';
import { ApiV2Service } from '../services-ngx/api-v2.service';
import { OrgNavigationComponent } from '../organization/configuration/navigation/org-navigation.component';
import { ClientRegistrationProviderComponent } from './configuration/client-registration-providers/client-registration-provider/client-registration-provider.component';
import { ApiLoggingComponent } from './configuration/api-logging/api-logging.component';
import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';
import { ApiAnalyticsOverviewComponentAjs } from './api/analytics/overview/analytics-overview.component.ajs';
import { ApplicationNotificationSettingsListComponent } from './application/details/notifications/notification-settings/notification-settings-list/application-notification-settings-list.component';
import { ApplicationNotificationSettingsDetailsComponent } from './application/details/notifications/notification-settings/notification-settings-details/application-notification-settings-details.component';
import { EnvironmentNotificationSettingsListComponent } from './configuration/notifications/notification-settings/notification-settings-list/environment-notification-settings-list.component';
import { EnvironmentNotificationSettingsDetailsComponent } from './configuration/notifications/notification-settings/notification-settings-details/environment-notification-settings-details.component';
import { EnvironmentMetadataComponent } from './configuration/metadata/environment-metadata.component';
import { ApplicationMetadataComponent } from './application/details/metadata/application-metadata.component';
import { ApplicationGeneralComponent } from './application/details/general/general-ng/application-general.component';
import { GroupV2Service } from '../services-ngx/group-v2.service';

(<any>window).moment = moment;
require('angular-moment-picker');

(<any>window).tinycolor = tinycolor;
require('md-color-picker');

angular.module('gravitee-management', [
  'angular-loading-bar',
  uiRouter,
  upgradeModule.name,
  permission,
  uiPermission,
  'ngMaterial',
  'ngMdIcons',
  'ui.codemirror',
  'md.data.table',
  'ngCookies',
  'dragularModule',
  'ngMessages',
  'vAccordion',
  'schemaForm',
  'ngclipboard',
  'ui.validate',
  'angular-timeline',
  'utf8-base64',
  'ngFileUpload',
  'md-steppers',
  'ui.tree',
  'angular-jwt',
  'gridster',
  'ngAnimate',
  'LocalStorageModule',
  'satellizer',
  ngInfiniteScroll,
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

const localStorageConfig = (localStorageServiceProvider) => {
  localStorageServiceProvider.setPrefix('gravitee');
};
localStorageConfig.$inject = ['localStorageServiceProvider'];
graviteeManagementModule.config(localStorageConfig);

graviteeManagementModule.config(config);
graviteeManagementModule.config(routerConfig);
graviteeManagementModule.config(authenticationConfig);
graviteeManagementModule.config(managementRouterConfig);
graviteeManagementModule.config(applicationRouterConfig);
graviteeManagementModule.config(configurationRouterConfig);
graviteeManagementModule.config(globalNotificationsRouterConfig);
graviteeManagementModule.config(interceptorConfig);
graviteeManagementModule.config(delegatorConfig);

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
graviteeManagementModule.run(runBlock);

// New Navigation components
graviteeManagementModule.directive('gioSideNav', downgradeComponent({ component: GioSideNavComponent }));
graviteeManagementModule.directive('gioTopNav', downgradeComponent({ component: GioTopNavComponent }));
graviteeManagementModule.directive('settingsNavigation', downgradeComponent({ component: SettingsNavigationComponent }));
graviteeManagementModule.directive('applicationNavigation', downgradeComponent({ component: ApplicationNavigationComponent }));

// Pendo Analytics
graviteeManagementModule.factory('ngGioPendoService', downgradeInjectable(GioPendoService));

graviteeManagementModule.component('apiAnalyticsOverviewComponentAjs', ApiAnalyticsOverviewComponentAjs);
graviteeManagementModule.component('apiV1PoliciesComponentAjs', ApiV1PoliciesComponentAjs);
graviteeManagementModule.controller('AddPoliciesPathController', AddPoliciesPathController);
graviteeManagementModule.component('apiHealthcheckDashboardComponentAjs', ApiHealthcheckDashboardComponentAjs);

graviteeManagementModule.component('apiV1PropertiesComponentAjs', ApiV1PropertiesComponentAjs);
graviteeManagementModule.component('apiHistoryComponentAjs', ApiHistoryComponentAjs);
graviteeManagementModule.component('apiV1ResourcesComponentAjs', ApiV1ResourcesComponentAjs);
graviteeManagementModule.controller('DialogAddPropertyController', DialogAddPropertyController);
graviteeManagementModule.controller('UserController', UserController);
graviteeManagementModule.controller('DialogEditPolicyController', DialogEditPolicyController);
graviteeManagementModule.controller('LoginController', LoginController);
graviteeManagementModule.controller('AnalyticsDashboardController', AnalyticsDashboardController);
graviteeManagementModule.component('gvAlertDashboard', AlertsDashboardComponent);
graviteeManagementModule.controller('AlertsActivityController', AlertsActivityController);
graviteeManagementModule.component('apiAlertsDashboardComponentAjs', ApiAlertsDashboardComponentAjs);
graviteeManagementModule.controller('CategoriesController', CategoriesController);
graviteeManagementModule.controller('CategoryController', CategoryController);
graviteeManagementModule.controller('DeleteCategoryDialogController', DeleteCategoryDialogController);
graviteeManagementModule.controller('DeleteAPICategoryDialogController', DeleteAPICategoryDialogController);
graviteeManagementModule.component('groups', GroupsComponent);
graviteeManagementModule.component('group', GroupComponent);
graviteeManagementModule.controller('DialogAddGroupMemberController', DialogAddGroupMemberController);
graviteeManagementModule.controller('DialogTransferOwnershipController', DialogTransferOwnershipController);
graviteeManagementModule.controller('RegistrationController', RegistrationController);
graviteeManagementModule.controller('ConfirmController', ConfirmController);
graviteeManagementModule.controller('ResetPasswordController', ResetPasswordController);
graviteeManagementModule.controller('NewsletterSubscriptionController', NewsletterSubscriptionController);
graviteeManagementModule.controller('MetadataController', MetadataController);
graviteeManagementModule.controller('DeleteMetadataDialogController', DeleteMetadataDialogController);
graviteeManagementModule.controller('NewMetadataDialogController', NewMetadataDialogController);
graviteeManagementModule.controller('UpdateMetadataDialogController', UpdateMetadataDialogController);
graviteeManagementModule.controller('DeleteFieldDialogController', DeleteFieldDialogController);
graviteeManagementModule.controller('NewFieldDialogController', NewFieldDialogController);
graviteeManagementModule.controller('UpdateFieldDialogController', UpdateFieldDialogController);
graviteeManagementModule.controller('FileChooserDialogController', FileChooserDialogController);
graviteeManagementModule.controller('DialogConfirmController', DialogConfirmController);
graviteeManagementModule.controller('DialogConfirmAndValidateController', DialogConfirmAndValidateController);
graviteeManagementModule.controller('DialogDynamicProviderHttpController', DialogDynamicProviderHttpController);
graviteeManagementModule.controller('SupportTicketController', SupportTicketController);
graviteeManagementModule.controller('TicketsListController', TicketsListController);
graviteeManagementModule.directive('ngEnvAudit', downgradeComponent({ component: EnvAuditComponent }));
graviteeManagementModule.component('apiAuditComponentAjs', ApiAuditComponentAjs);
graviteeManagementModule.controller('PortalThemeController', PortalThemeController);
graviteeManagementModule.controller('CustomUserFieldsController', CustomUserFieldsController);
graviteeManagementModule.controller('TopApisController', TopApisController);
graviteeManagementModule.controller('AddTopApiDialogController', AddTopApiDialogController);
graviteeManagementModule.controller('DeleteTopApiDialogController', DeleteTopApiDialogController);
graviteeManagementModule.controller('SelectFolderDialogController', SelectFolderDialogController);
graviteeManagementModule.controller('SelectPageDialogController', SelectPageDialogController);
graviteeManagementModule.service('ApplicationService', ApplicationService);
graviteeManagementModule.service('ApplicationTypesService', ApplicationTypesService);
graviteeManagementModule.service('ApiService', ApiService);
graviteeManagementModule.service('debugApiService', DebugApiService);
graviteeManagementModule.service('ApiPrimaryOwnerModeService', ApiPrimaryOwnerModeService);
graviteeManagementModule.service('CorsService', CorsService);
graviteeManagementModule.service('DocumentationService', DocumentationService);
graviteeManagementModule.service('NotificationService', NotificationService);
graviteeManagementModule.service('PolicyService', PolicyService);
graviteeManagementModule.service('NotifierService', NotifierService);
graviteeManagementModule.service('UserService', UserService);
graviteeManagementModule.service('Base64Service', Base64Service);
graviteeManagementModule.service('ResourceService', ResourceService);
graviteeManagementModule.service('FetcherService', FetcherService);
graviteeManagementModule.service('ServiceDiscoveryService', ServiceDiscoveryService);
graviteeManagementModule.service('eventService', EventService);
graviteeManagementModule.service('AnalyticsService', AnalyticsService);
graviteeManagementModule.service('CategoryService', CategoryService);
graviteeManagementModule.service('GroupService', GroupService);
graviteeManagementModule.service('SubscriptionService', SubscriptionService);
graviteeManagementModule.service('TagService', TagService);
graviteeManagementModule.service('MetadataService', MetadataService);
graviteeManagementModule.service('CustomUserFieldsService', CustomUserFieldsService);
graviteeManagementModule.service('TenantService', TenantService);
graviteeManagementModule.service('StringService', StringService);
graviteeManagementModule.service('AuthenticationService', AuthenticationService);
graviteeManagementModule.service('RoleService', RoleService);
graviteeManagementModule.service('TicketService', TicketService);
graviteeManagementModule.service('AuditService', AuditService);
graviteeManagementModule.service('TopApiService', TopApiService);
graviteeManagementModule.service('MessageService', MessageService);
graviteeManagementModule.service('PortalService', PortalService);
graviteeManagementModule.service('PortalThemeService', PortalThemeService);
graviteeManagementModule.service('ReCaptchaService', ReCaptchaService);
graviteeManagementModule.service('TokenService', TokenService);
graviteeManagementModule.service('EnvironmentService', EnvironmentService);
graviteeManagementModule.service('OrganizationService', OrganizationService);
graviteeManagementModule.service('InstallationService', InstallationService);
graviteeManagementModule.service('FlowService', FlowService);
graviteeManagementModule.service('SpelService', SpelService);
graviteeManagementModule.service('ConnectorService', ConnectorService);
graviteeManagementModule.factory('ngApiV2Service', downgradeInjectable(ApiV2Service));
graviteeManagementModule.factory('ngGioPermissionService', downgradeInjectable(GioPermissionService));
graviteeManagementModule.factory('ngGroupV2Service', downgradeInjectable(GroupV2Service));

graviteeManagementModule.controller('DialogGenerateTokenController', DialogGenerateTokenController);

graviteeManagementModule.directive('filecontent', () => FileContentDirective);
graviteeManagementModule.directive('fileloader', () => FileLoaderDirective);
graviteeManagementModule.directive('noDirtyCheck', () => new FormDirective());
graviteeManagementModule.directive('autofocus', () => new AutofocusDirective());
graviteeManagementModule.directive('graviteeDiff', () => DiffDirective);
graviteeManagementModule.directive('graviteeIdentityPicture', () => new IdentityPictureDirective());
graviteeManagementModule.directive('gvModel', () => new GvModelDirective());
graviteeManagementModule.directive('graviteeImage', () => new ImageDirective());
graviteeManagementModule.directive('graviteeEmptyState', () => new EmptyStateDirective());
graviteeManagementModule.directive('graviteeChart', () => new ChartDirective());
graviteeManagementModule.directive('graviteeUserAvatar', () => new UserAvatarDirective());

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
graviteeManagementModule.component('gvError', ErrorComponent);
graviteeManagementModule.controller('ErrorController', ErrorController);

graviteeManagementModule.component('categories', CategoriesComponent);
graviteeManagementModule.component('category', CategoryComponent);
graviteeManagementModule.component('moved', MovedComponent);

graviteeManagementModule.directive('ngEnvironmentMetadata', downgradeComponent({ component: EnvironmentMetadataComponent }));
graviteeManagementModule.component('theme', PortalThemeComponent);
graviteeManagementModule.component('topApis', TopApisComponent);
graviteeManagementModule.factory('ngCockpitService', downgradeInjectable(CockpitService));

graviteeManagementModule.component('portalSettings', PortalSettingsComponent);
graviteeManagementModule.component('analyticsSettings', AnalyticsSettingsComponent);
graviteeManagementModule.directive('gvMetadataValidator', () => MetadataValidatorDirective);
graviteeManagementModule.component('customUserFields', CustomUserFieldsComponent);
graviteeManagementModule.component('ticketDetail', TicketDetailComponent);

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

graviteeManagementModule.directive('ngBanner', downgradeComponent({ component: GioBannerComponent }));

graviteeManagementModule.directive('ngEnvApplicationList', downgradeComponent({ component: EnvApplicationListComponent }));

graviteeManagementModule.component('applicationSubscribe', ApplicationSubscribeComponent);
graviteeManagementModule.controller('ApplicationSubscribeController', ApplicationSubscribeController);
graviteeManagementModule.controller('ApiKeyModeChoiceDialogController', ApiKeyModeChoiceDialogController);

graviteeManagementModule.component('createApplication', ApplicationCreationComponent);
graviteeManagementModule.controller('ApplicationCreationController', ApplicationCreationController);
graviteeManagementModule.component('applicationCreationStep1', ApplicationCreationStep1Component);
graviteeManagementModule.component('applicationCreationStep2', ApplicationCreationStep2Component);
graviteeManagementModule.controller('ApplicationCreationStep2Controller', ApplicationCreationStep2Controller);
graviteeManagementModule.component('applicationCreationStep3', ApplicationCreationStep3Component);
graviteeManagementModule.component('applicationCreationStep4', ApplicationCreationStep4Component);

graviteeManagementModule.component('applicationHeader', ApplicationHeaderComponent);
graviteeManagementModule.component('applicationGeneral', AjsApplicationGeneralComponent);
graviteeManagementModule.component('applicationSubscriptions', ApplicationSubscriptionsComponent);
graviteeManagementModule.component('applicationSubscription', ApplicationSubscriptionComponent);
graviteeManagementModule.component('applicationSubscriptionsList', ApplicationSubscriptionsListComponent);
graviteeManagementModule.component('applicationMembers', ApplicationMembersComponent);
graviteeManagementModule.component('applicationAnalytics', ApplicationAnalyticsComponent);
graviteeManagementModule.component('applicationLogs', ApplicationLogsComponent);
graviteeManagementModule.component('applicationLog', ApplicationLogComponent);
graviteeManagementModule.controller('DialogAddMemberController', DialogAddMemberController);
graviteeManagementModule.controller('ApplicationGeneralController', ApplicationGeneralController);
graviteeManagementModule.controller('ApplicationMembersController', ApplicationMembersController);
graviteeManagementModule.controller('ApplicationSubscriptionsController', ApplicationSubscriptionsController);
graviteeManagementModule.controller('ApplicationSubscriptionsListController', ApplicationSubscriptionsListController);
graviteeManagementModule.controller('ApplicationAnalyticsController', ApplicationAnalyticsController);
graviteeManagementModule.controller('ApplicationLogsController', ApplicationLogsController);
graviteeManagementModule.controller('DialogTransferApplicationController', DialogTransferApplicationController);
graviteeManagementModule.component('apiPlan', ApiPlanComponent);

graviteeManagementModule.component('user', UserComponent);

graviteeManagementModule.directive('tasks', downgradeComponent({ component: TasksComponent }));

graviteeManagementModule.service('NotificationSettingsService', NotificationSettingsService);
graviteeManagementModule.service('NotificationTemplatesService', NotificationTemplatesService);
graviteeManagementModule.controller('DialogAddNotificationSettingsController', DialogAddNotificationSettingsController);
graviteeManagementModule.component('notificationsComponentAjs', NotificationsComponentAjs);

graviteeManagementModule.directive(
  'environmentNotificationSettingsList',
  downgradeComponent({ component: EnvironmentNotificationSettingsListComponent }),
);
graviteeManagementModule.directive(
  'environmentNotificationSettingsDetails',
  downgradeComponent({ component: EnvironmentNotificationSettingsDetailsComponent }),
);

graviteeManagementModule.directive(
  'applicationNotificationSettingsList',
  downgradeComponent({ component: ApplicationNotificationSettingsListComponent }),
);
graviteeManagementModule.directive(
  'applicationNotificationSettingsDetails',
  downgradeComponent({ component: ApplicationNotificationSettingsDetailsComponent }),
);
graviteeManagementModule.directive('ngApplicationMetadata', downgradeComponent({ component: ApplicationMetadataComponent }));
graviteeManagementModule.directive('ngApplicationGeneralNg', downgradeComponent({ component: ApplicationGeneralComponent }));

graviteeManagementModule.component('logout', LogoutComponent);

graviteeManagementModule.component('apiAnalyticsLogsComponentAjs', ApiAnalyticsLogsComponentAjs);
graviteeManagementModule.component('gvLogsTimeframe', LogsTimeframeComponent);
graviteeManagementModule.controller('LogsTimeframeController', LogsTimeframeController);
graviteeManagementModule.component('apiAnalyticsLogComponentAjs', ApiAnalyticsLogComponentAjs);
graviteeManagementModule.component('gvLogsFilters', LogsFiltersComponent);
graviteeManagementModule.controller('LogsFiltersController', LogsFiltersController);
graviteeManagementModule.component('gvSearchAndSelect', SearchAndSelectComponent);
graviteeManagementModule.controller('SearchAndSelectController', SearchAndSelectController);

graviteeManagementModule.component('gvAudit', AuditComponent);
graviteeManagementModule.component('gvNewsletterReminder', NewsletterReminderComponent);
graviteeManagementModule.component('gvContextualDoc', ContextualDocComponent);
graviteeManagementModule.controller('ContextualDocController', ContextualDocController);

// Healthcheck
graviteeManagementModule.component('apiHealthcheckLogComponentAjs', ApiHealthcheckLogComponentAjs);
graviteeManagementModule.component('progressBar', ProgressBarComponent);
graviteeManagementModule.component('gvHealthcheckMetric', HealthCheckMetricComponent);

// Configuration
graviteeManagementModule.component('settings', SettingsComponent);
graviteeManagementModule.directive('orgNavigation', downgradeComponent({ component: OrgNavigationComponent }));
graviteeManagementModule.service('ConsoleSettingsService', ConsoleSettingsService);
graviteeManagementModule.service('PortalSettingsService', PortalSettingsService);
graviteeManagementModule.service('PortalConfigService', PortalConfigService);

// Router
graviteeManagementModule.service('RouterService', RouterService);

graviteeManagementModule.directive('ngMessages', downgradeComponent({ component: MessagesComponent }));

// Dictionaries
graviteeManagementModule.service('DictionaryService', DictionaryService);
graviteeManagementModule.component('dictionaries', DictionariesComponent);
graviteeManagementModule.component('dictionary', DictionaryComponent);
graviteeManagementModule.controller('DictionariesController', DictionariesController);
graviteeManagementModule.controller('DictionaryController', DictionaryController);
graviteeManagementModule.controller('DialogDictionaryAddPropertyController', DialogDictionaryAddPropertyController);

// ApiHeader
graviteeManagementModule.component('configApiPortalHeader', ApiPortalHeaderComponent);
graviteeManagementModule.service('ApiHeaderService', ApiHeaderService);
graviteeManagementModule.controller('NewApiPortalHeaderDialogController', NewApiPortalHeaderDialogController);
graviteeManagementModule.controller('UpdateApiPortalHeaderDialogController', UpdateApiPortalHeaderDialogController);

graviteeManagementModule.component('configApiQuality', ApiQualityRulesComponent);
graviteeManagementModule.component('qualityRule', ApiQualityRuleComponent);
graviteeManagementModule.controller('ApiQualityRuleController', ApiQualityRuleController);
graviteeManagementModule.controller('DeleteApiQualityRuleDialogController', DeleteApiQualityRuleDialogController);
graviteeManagementModule.service('QualityRuleService', QualityRuleService);

// Settings: Identity provider
graviteeManagementModule.component('identityProviders', IdentityProvidersComponent);

graviteeManagementModule.service('IdentityProviderService', IdentityProviderService);

// Settings: Client Registration
graviteeManagementModule.directive(
  'ngClientRegistrationProviders',
  downgradeComponent({ component: ClientRegistrationProvidersComponent }),
);
graviteeManagementModule.directive('ngClientRegistrationProvider', downgradeComponent({ component: ClientRegistrationProviderComponent }));

// Settings: API Logging
graviteeManagementModule.directive('ngApiLogging', downgradeComponent({ component: ApiLoggingComponent }));
// Alerts
graviteeManagementModule.service('AlertService', AlertService);
graviteeManagementModule.controller('AlertTabsController', AlertTabsController);
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

// CircularPercentageComponent
graviteeManagementModule.component('circularPercentage', CircularPercentageComponent);
graviteeManagementModule.controller('CircularPercentageController', CircularPercentageController);

graviteeManagementModule.service('EntrypointService', EntrypointService);

graviteeManagementModule.service('DashboardService', DashboardService);
graviteeManagementModule.component('dashboard', AnalyticsDashboardComponent);
graviteeManagementModule.controller('DialogQueryFilterInformationController', DialogQueryFilterInformationController);

// Platform Analytics
graviteeManagementModule.component('platformLogs', PlatformLogsComponent);
graviteeManagementModule.component('platformLog', PlatformLogComponent);
graviteeManagementModule.controller('PlatformLogsController', PlatformLogsController);

// User-Autocomplete
graviteeManagementModule.component('gvUserAutocomplete', UserAutocompleteComponent);
graviteeManagementModule.controller('UserAutocompleteController', UserAutocompleteController);

// Quick Time range
graviteeManagementModule.component('gvQuickTimeRange', QuickTimeRangeComponent);
graviteeManagementModule.controller('QuickTimeRangeController', QuickTimeRangeController);

// Promotions
graviteeManagementModule.service('promotionService', PromotionService);

graviteeManagementModule.factory('ngIfMatchEtagInterceptor', downgradeInjectable(IfMatchEtagInterceptor));

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
