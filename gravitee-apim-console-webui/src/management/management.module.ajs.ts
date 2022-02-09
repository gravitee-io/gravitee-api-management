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
import { ApisController } from './api/apis.controller';
import ApisStatusDashboardController from '../management/dashboard/apis-status-dashboard/apis-status-dashboard.controller';
import ApiPortalController from '../management/api/portal/general/apiPortal.controller';
import ApiAdminController from '../management/api/apiAdmin.controller';
import ApiAnalyticsController from '../management/api/analytics/overview/analytics.controller';
import ApiMembersController from '../management/api/portal/userGroupAccess/members/members.controller';
import ApiTransferOwnershipController from '../management/api/portal/userGroupAccess/transferOwnership/transferOwnership.controller';
import ApiPoliciesController from '../management/api/design/policies/policies.controller';
import ApiEndpointController from '../management/api/proxy/backend/endpoint/endpointConfiguration.controller';
import ApiEndpointGroupController from '../management/api/proxy/backend/endpoint/group.controller';
import AddPoliciesPathController from '../management/api/design/policies/addPoliciesPath.controller';
import ApiResourcesController from '../management/api/design/resources/resources.controller';
import ApiPathMappingsController from '../management/api/analytics/pathMappings/pathMappings.controller';
import ApiPropertiesController from '../management/api/design/properties/properties.controller';
import ApiEventsController from '../management/api/audit/events/apiEvents.controller';
import ApiHistoryController from '../management/api/audit/history/apiHistory.controller';
import DialogAddPropertyController from '../management/api/design/properties/add-property.dialog.controller';
import DialogAddMemberApiController from '../management/api/portal/userGroupAccess/members/addMemberDialog.controller';
import DialogTransferApiController from '../management/api/portal/userGroupAccess/transferOwnership/transferAPIDialog.controller';
import DialogApiKeyExpirationController from './api/portal/subscriptions/dialog/apikey.expiration.dialog.controller';
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
import DialogApiImportController from '../management/api/portal/general/dialog/apiImportDialog.controller';
import DialogApiExportController from '../management/api/portal/general/dialog/apiExportDialog.controller';
import DialogApiDuplicateController from '../management/api/portal/general/dialog/apiDuplicateDialog.controller';
// Sidenav
import SidenavService from '../components/sidenav/sidenav.service';
import { SidenavComponent } from '../components/sidenav/sidenav.component';
import { SubmenuComponent } from '../components/sidenav/submenu.component';
import { NavbarComponent } from '../components/navbar/navbar.component';
// Api
import ApiCreationComponent from '../management/api/creation/steps/api-creation.component';
import ApiCreationController from '../management/api/creation/steps/api-creation.controller';
import ApiCreationStep1Component from '../management/api/creation/steps/api-creation-step1.component';
import ApiCreationStep2Component from '../management/api/creation/steps/api-creation-step2.component';
import ApiCreationStep3Component from '../management/api/creation/steps/api-creation-step3.component';
import ApiCreationStep4Component from '../management/api/creation/steps/api-creation-step4.component';
import ApiCreationStep5Component from '../management/api/creation/steps/api-creation-step5.component';
import ApiImportComponent from '../components/import/import-api.component';
import NewApiController from '../management/api/creation/newApiPortal.controller';
import DialogConfirmDeploymentController from '../management/api/deploy/confirmDeploymentDialog.controller';
// API Plan
import ApiPlanComponent from '../management/api/api-plan.component';
import ApiEditPlanController from '../management/api/portal/plans/plan/edit-plan.controller';
import ApiEditPlanComponent from '../management/api/portal/plans/plan/edit-plan.component';
import ApiListPlansComponent from '../management/api/portal/plans/list-plans.component';
import ApiListPlansController from '../management/api/portal/plans/list-plans.controller';
import ApiEditPlanWizardGeneralComponent from '../management/api/portal/plans/plan/plan-wizard-general.component';
import ApiEditPlanWizardSecurityComponent from '../management/api/portal/plans/plan/plan-wizard-security.component';
import ApiEditPlanWizardRestrictionsComponent from '../management/api/portal/plans/plan/plan-wizard-restrictions.component';
import ApiEditPlanWizardPoliciesComponent from '../management/api/portal/plans/plan/plan-wizard-policies.component';
// API PrimaryOwner Mode
import ApiPrimaryOwnerModeService from '../services/apiPrimaryOwnerMode.service';
// API Subscription
import ApiSubscriptionsComponent from '../management/api/portal/subscriptions/subscriptions.component';
import ApiSubscriptionComponent from '../management/api/portal/subscriptions/subscription.component';
// Applications
import ApplicationService from '../services/application.service';
import ApplicationTypesService from '../services/applicationTypes.service';
import ApplicationsComponent from './application/applications.component';
import ApplicationsController from './application/applications.controller';

import ApplicationCreationComponent from './application/creation/steps/application-creation.component';
import ApplicationCreationController from './application/creation/steps/application-creation.controller';
import ApplicationCreationStep1Component from './application/creation/steps/application-creation-step1.component';
import ApplicationCreationStep2Component from './application/creation/steps/application-creation-step2.component';
import ApplicationCreationStep2Controller from './application/creation/steps/application-creation-step2.controller';
import ApplicationCreationStep3Component from './application/creation/steps/application-creation-step3.component';
import ApplicationCreationStep4Component from './application/creation/steps/application-creation-step4.component';

import ApplicationComponent from './application/details/application.component';
import ApplicationHeaderComponent from './application/details/header/application-header.component';
import ApplicationGeneralController from './application/details/general/application-general.controller';
import ApplicationGeneralComponent from './application/details/general/application-general.component';
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
// Instances
import InstancesService from '../services/instances.service';
import InstancesController from '../management/instances/instances.controller';
import InstanceHeaderComponent from '../management/instances/details/header/instance-header.component';
import InstanceEnvironmentController from '../management/instances/details/environment/instance-environment.controller';
import InstanceEnvironmentComponent from '../management/instances/details/environment/instance-environment.component';
import InstanceMonitoringComponent from '../management/instances/details/monitoring/instance-monitoring.component';
import InstanceMonitoringController from '../management/instances/details/monitoring/instance-monitoring.controller';
import InstancesComponent from '../management/instances/instances.component';
import InstanceComponent from '../management/instances/details/instance.component';
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
import ApiLogsController from '../management/api/analytics/logs/logs.controller';
import LogsTimeframeComponent from '../components/logs/logs-timeframe.component';
import LogsTimeframeController from '../components/logs/logs-timeframe.controller';
import LogsFiltersComponent from '../components/logs/logs-filters.component';
import LogsFiltersController from '../components/logs/logs-filters.controller';

import { LogComponent } from './api/analytics/logs/log.component';
import ApiLoggingConfigurationController from '../management/api/analytics/logs/logging-configuration.controller';
import DialogConfigureLoggingEditorController from '../management/api/analytics/logs/configure-logging-editor.dialog.controller';
// Others
import ThemeElementDirective from '../components/theme/theme-element.directive';
import EnvironmentService from '../services/environment.service';
import OrganizationService from '../services/organization.service';
import InstallationService from '../services/installation.service';

import ErrorComponent from '../components/error/error.component';
import ErrorController from '../components/error/error.controller';
import IdentityPictureDirective from '../components/identityPicture/identityPicture.directive';
import ImageDirective from '../components/image/image.directive';
import { EventService } from '../services/event.service';
import AnalyticsService from '../services/analytics.service';
import AnalyticsDashboardController from '../management/dashboard/analytics-dashboard/analytics-dashboard.controller';
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
import DialogSubscriptionRejectController from './api/portal/subscriptions/dialog/subscription.reject.dialog.controller';
import DialogSubscriptionAcceptController from './api/portal/subscriptions/dialog/subscription.accept.dialog.controller';
import DialogSubscriptionCreateController from './api/portal/subscriptions/dialog/subscription.create.dialog.controller';
import DialogSubscriptionTransferController from './api/portal/subscriptions/dialog/subscription.transfer.dialog.controller';
import DialogSubscriptionChangeEndDateController from './api/portal/subscriptions/dialog/subscription.change.end.date.dialog.controller';
import DialogSubscriptionRenewController from './api/portal/subscriptions/dialog/subscription.renew.dialog.controller';
import EmptyStateDirective from '../components/emptystate/emptystate.directive';
import DialogPublishPlanController from '../management/api/portal/plans/publishPlanDialog.controller';
import TagsController from '../organization/configuration/tags/tags.controller';
import TagService from '../services/tag.service';
import MetadataController from '../components/metadata/metadata.controller';
import MetadataService from '../services/metadata.service';
import DeleteTagDialogController from '../organization/configuration/tags/delete.tag.dialog.controller';
import DeleteMetadataDialogController from '../components/metadata/dialog/delete.metadata.dialog.controller';
import NewMetadataDialogController from '../components/metadata/dialog/new.metadata.dialog.controller';
import UpdateMetadataDialogController from '../components/metadata/dialog/update.metadata.dialog.controller';
import ChartDirective from '../components/chart/chart.directive';
import UserAvatarDirective from '../components/avatar/user-avatar.directive';
import FileChooserDialogController from '../components/dialog/fileChooserDialog.controller';
import DialogConfirmController from '../components/dialog/confirmDialog.controller';
import DialogConfirmAndValidateController from '../components/dialog/confirmAndValidateDialog.controller';
import DialogDynamicProviderHttpController from '../management/api/design/properties/dynamic-provider-http-dialog.controller';
import TenantsController from '../organization/configuration/tenants/tenants.controller';
import TenantService from '../services/tenant.service';
import DeleteTenantDialogController from '../organization/configuration/tenants/delete.tenant.dialog.controller';
import PoliciesController from '../organization/configuration/policies/policies.controller';

import CategoriesComponent from '../management/configuration/categories/categories.component';
import CategoryComponent from './configuration/categories/category/category.component';
import TenantsComponent from '../organization/configuration/tenants/tenants.component';
import TagsComponent from '../organization/configuration/tags/tags.component';
import MetadataComponent from '../management/configuration/metadata/metadata.component';
import MetadataValidatorDirective from '../components/metadata/metadata.validator.directive';
import PoliciesComponent from '../organization/configuration/policies/policies.component';

import RoleComponent from '../organization/configuration/roles/role/role.components';
import RoleMembersComponent from '../organization/configuration/roles/role/role.members.component';
import RolesComponent from '../organization/configuration/roles/roles.component';
import RoleService from '../services/role.service';
import DialogAddUserRoleController from '../organization/configuration/roles/role/add.user.dialog.controller';

import applicationRouterConfig from './application/applications.route';
import applicationsNotificationsRouterConfig from './application/details/notifications/applications.notifications.settings.route';
import apisRouterConfig from './api/apis.route';
import apisAnalyticsRouterConfig from './api/analytics/apis.analytics.route';
import apisAuditRouterConfig from './api/audit/apis.audit.route';
import apisDesignRouterConfig from './api/design/apis.design.route';
import apisProxyRouterConfig from './api/proxy/apis.proxy.route';
import apisPortalRouterConfig from './api/portal/apis.portal.route';
import apisNotificationsRouterConfig from './api/notifications/apis.notifications.settings.route';
import configurationRouterConfig from './configuration/configuration.route';
import globalNotificationsRouterConfig from './configuration/notifications/global.notifications.settings.route';
// User
import UserService from '../services/user.service';
import UserController from '../user/user.controller';
import UserComponent from '../user/user.component';
import { submenuFilter } from '../components/sidenav/submenu.filter';
// User Tasks
import { TasksComponent } from './tasks/tasks.component';
import { PromotionTaskComponent } from './tasks/promotion/promotion-task.component';
import TaskService from '../services/task.service';
// Portal notifications
import PortalNotificationsComponent from './portalnotifications/portalnotifications.component';
import UserNotificationService from '../services/userNotification.service';
// Notification Settings
import NotificationsComponent from '../components/notifications/notifications.component';
import NotificationSettingsComponent from '../components/notifications/notificationsettings/notificationsettings.component';
import NotificationSettingsService from '../services/notificationSettings.service';
import NotificationTemplatesService from '../services/notificationTemplates.service';
import NotificationTemplatesComponent from '../organization/configuration/notification-templates/notificationTemplates.component';
import NotificationTemplateComponent from '../organization/configuration/notification-templates/notificationTemplate.component';
import NotificationTemplateByTypeComponent from '../organization/configuration/notification-templates/components/notificationTemplateByType.component';
import NotificationTemplatesController from '../organization/configuration/notification-templates/notificationTemplates.controller';
import NotificationTemplateController from '../organization/configuration/notification-templates/notificationTemplate.controller';
import NotificationTemplateByTypeController from '../organization/configuration/notification-templates/components/notificationTemplateByType.controller';
// Documentation
import '../components/documentation/documentation.module';

// Healthcheck
import ApiHealthCheckConfigureController from '../management/api/proxy/backend/healthcheck/healthcheck-configure.controller';
import DialogAssertionInformationController from '../management/api/proxy/backend/healthcheck/healthcheck-assertion-dialog.controller';
import ApiHealthCheckController from '../management/api/proxy/backend/healthcheck/healthcheck.controller';
import ProgressBarComponent from '../components/progressbar/progress-bar.component';
import ApiHealthCheckLogController from '../management/api/proxy/backend/healthcheck/healthcheck-log.controller';
import HealthCheckMetricComponent from '../components/healthcheckmetric/healthcheck-metric.component';
// Ticket
import TicketService from '../services/ticket.service';
import SupportTicketController from '../management/support/ticket.controller';
// Audit
import AuditService from '../services/audit.service';
import AuditController from '../management/audit/audit.controller';
import ApiAuditController from '../management/api/audit/general/audit.controller';
import AuditComponent from '../components/audit/audit.component';
// Configuration
import SettingsComponent from '../management/configuration/settings.component';
import OrganizationSettingsComponent from '../organization/configuration/organization-settings.component';
import ConsoleSettingsService from '../services/consoleSettings.service';
import PortalSettingsService from '../services/portalSettings.service';
import PortalConfigService from '../services/portalConfig.service';
import ApiLoggingComponent from '../management/configuration/api_logging/api_logging.component';
import ApiLoggingController from '../management/configuration/api_logging/api_logging.controller';
// Users
import UsersComponent from '../organization/configuration/users/users.component';
import UserDetailComponent from '../organization/configuration/user/userdetail.component';
import NewUserComponent from '../organization/configuration/user/new/new-user.component';
import DialogAddUserGroupController from '../organization/configuration/user/dialog/addusergroup.dialog.controller';
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
import IdentityProviderComponent from '../organization/configuration/identity/identity-provider.component';
import IdentityProviderController from '../organization/configuration/identity/identity-provider.controller';
import IdentityProviderGoogleComponent from '../organization/configuration/identity/identity-provider-google.component';
import IdentityProviderGitHubComponent from '../organization/configuration/identity/identity-provider-github.component';
import IdentityProviderGraviteeioAmComponent from '../organization/configuration/identity/identity-provider-graviteeio-am.component';
import IdentityProviderOIDCComponent from '../organization/configuration/identity/identity-provider-oidc.component';
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
import ApiProxyController from './api/proxy/apiProxy.controller';
import CockpitComponent from '../organization/configuration/cockpit/cockpit.component';
import ConsoleSettingsComponentAjs from '../organization/configuration/console/console.component';
import PortalSettingsComponent from './configuration/portal/portal.component';
import DialogAddPathMappingController from './api/analytics/pathMappings/modal/add-pathMapping.dialog.controller';
import DialogImportPathMappingController from './api/analytics/pathMappings/modal/import-pathMapping.dialog.controller';

import RouterService from '../services/router.service';

import MessageService from '../services/message.service';
import MessagesComponent from './messages/messages.component';
import apisMessagesRouterConfig from './api/messages/apis.messages.route';

import ApiPortalHeaderComponent from '../management/configuration/api-portal-header/api-portal-header.component';
import ApiHeaderService from '../services/apiHeader.service';

import UpdateApiPortalHeaderDialogController from './configuration/api-portal-header/update.api-portal-header.dialog.controller';
import NewApiPortalHeaderDialogController from './configuration/api-portal-header/new.api-portal-header.dialog.controller';
import Base64Service from '../services/base64.service';
// Alerts
import AlertService from '../services/alert.service';
import AlertsComponent from '../components/alerts/alerts.component';
import AlertComponent from '../components/alerts/alert/alert.component';
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
import EntrypointComponent from '../organization/configuration/tags/entrypoint/entrypoint.component';
import EntrypointController from '../organization/configuration/tags/entrypoint/entrypoint.controller';
import DeleteEntrypointDialogController from '../organization/configuration/tags/entrypoint/delete.entrypoint.dialog.controller';

import TagComponent from '../organization/configuration/tags/tag/tag.component';
import TagController from '../organization/configuration/tags/tag/tag.controller';

import SelectFolderDialogController from '../components/documentation/dialog/selectfolder.controller';
import SelectPageDialogController from '../components/documentation/dialog/selectpage.controller';
// API Response Templates
import ApiResponseTemplatesController from '../management/api/proxy/general/response-templates/response-templates.controller';
import ApiResponseTemplateController from '../management/api/proxy/general/response-templates/response-template.controller';
import ApiResponseTemplateTypeComponent from '../management/api/proxy/general/response-templates/response-template-type.component';
import ApiResponseTemplateComponent from '../management/api/proxy/general/response-templates/response-template.component';
import AnalyticsSettingsComponent from './configuration/analytics/analytics.component';
// Settings - Client Registration
import ClientRegistrationProviderService from '../services/clientRegistrationProvider.service';
import ClientRegistrationProvidersComponent from '../management/configuration/application/registration/client-registration-providers.component';
import ClientRegistrationProviderComponent from '../management/configuration/application/registration/client-registration-provider.component';
import ClientRegistrationProviderController from '../management/configuration/application/registration/client-registration-provider.controller';

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

import DialogReviewController from './api/review/reviewDialog.controller';
import DialogRequestForChangesController from './api/portal/general/dialog/requestForChanges.controller';
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

import ThemeController from './configuration/theme/theme.controller';
import ThemeComponent from './configuration/theme/theme.component';
import ThemeService from '../services/theme.service';

import authenticationConfig from '../authentication/authentication.config';
import NewsletterSubscriptionController from '../user/newsletter/newsletter-subscription.controller';
import CustomUserFieldsComponent from './configuration/custom-user-fields/custom-user-fields.component';
import CustomUserFieldsController from './configuration/custom-user-fields/custom-user-fields.controller';
import CustomUserFieldsService from '../services/custom-user-fields.service';
import NewFieldDialogController from './configuration/custom-user-fields/dialog/new.custom-user-field.dialog.controller';
import DeleteFieldDialogController from './configuration/custom-user-fields/dialog/delete.custom-user-field.dialog.controller';
import UpdateFieldDialogController from './configuration/custom-user-fields/dialog/update.custom-user-field.dialog.controller';
import FlowService from '../services/flow.service';
import ApiKeyValidatedInput from './api/portal/subscriptions/components/apiKeyValidatedInput.component';
import TicketsListController from './support/tickets-list.controller';
import TicketDetailComponent from './support/ticket-detail.component';
import organizationRouterConfig from '../organization/organization.route.ajs';
import SpelService from '../services/spel.service';
import DashboardController from './dashboard/dashboard.controller';
import HomeDashboardController from './dashboard/home-dashboard/home-dashboard.controller';
import AlertsDashboardComponent from '../components/alerts/dashboard/alerts-dashboard.component';
import PlatformAlertsDashboardController from './dashboard/alerts-dashboard/platform-alerts-dashboard.controller';
import WidgetChartCountComponent from '../components/widget/count/widget-chart-count.component';

import { PromotionService } from '../services/promotion.service';

(<any>window).jQuery = jQuery;

import * as angular from 'angular';

const ngInfiniteScroll = require('ng-infinite-scroll');
import ApiAlertsDashboardController from './api/analytics/alerts/api-alerts-dashboard.controller';
import MovedComponent from './configuration/moved/moved.component';

(<any>window).traverse = traverse;

(<any>window).hljs = hljs;

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

require('../libraries/showdown-extension/DocHelper-extension.js');

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
require('showdown-highlightjs-extension');

require('angular-gridster');
require('angular-scroll');
require('diff/dist/diff.min.js');
require('angular-loading-bar');

// Highcharts
const Highcharts = require('highcharts');
window.Highcharts = Highcharts;
require('highcharts/modules/exporting')(Highcharts);
require('highcharts/highcharts-more')(Highcharts);
require('highcharts/modules/solid-gauge')(Highcharts);
require('highcharts/modules/no-data-to-display')(Highcharts);
require('highcharts/modules/map')(Highcharts);

require('@highcharts/map-collection/custom/world');
import { DebugApiService } from '../services/debugApi.service';
import { downgradeComponent } from '@angular/upgrade/static';
import { OrgSettingsGeneralComponent } from '../organization/configuration/console/org-settings-general.component';
import { OrgSettingsUsersComponent } from '../organization/configuration/users/org-settings-users.component';
import { OrgSettingsNewUserComponent } from '../organization/configuration/user/new/org-settings-new-user.component';
import { OrgSettingsIdentityProvidersComponent } from '../organization/configuration/identity-providers/org-settings-identity-providers.component';
import { OrgSettingsIdentityProviderComponent } from '../organization/configuration/identity-provider/org-settings-identity-provider.component';
import { OrgSettingsNotificationTemplatesComponent } from '../organization/configuration/notification-templates/org-settings-notification-templates.component';
import { OrgSettingsCockpitComponent } from '../organization/configuration/cockpit/org-settings-cockpit.component';
import { OrgSettingsNotificationTemplateComponent } from '../organization/configuration/notification-templates/org-settings-notification-template.component';
import { OrgSettingsUserDetailComponent } from '../organization/configuration/user/detail/org-settings-user-detail.component';
import { OrgSettingsPlatformPoliciesComponent } from '../organization/configuration/policies/org-settings-platform-policies.component';
import { OrgSettingsTenantsComponent } from '../organization/configuration/tenants/org-settings-tenants.component';
import { OrgSettingsRolesComponent } from '../organization/configuration/roles/org-settings-roles.component';
import { OrgSettingsTagsComponent } from '../organization/configuration/tags/org-settings-tags.component';
import { OrgSettingsRoleMembersComponent } from '../organization/configuration/roles/org-settings-role-members.component';
import { OrgSettingsRoleComponent } from '../organization/configuration/roles/role/org-settings-role.component';

import { upgradeModule } from '@uirouter/angular-hybrid';
import uiRouter from '@uirouter/angularjs';

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
  'ng-showdown',
  'ngMdIcons',
  'ui.codemirror',
  'md.data.table',
  'ngCookies',
  'dragularModule',
  'readMore',
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

graviteeManagementModule.config((cfpLoadingBarProvider) => {
  'ngInject';
  cfpLoadingBarProvider.includeSpinner = false;
});

graviteeManagementModule.config((localStorageServiceProvider: angular.local.storage.ILocalStorageServiceProvider) => {
  'ngInject';
  localStorageServiceProvider.setPrefix('gravitee');
});
graviteeManagementModule.config(config);
graviteeManagementModule.config(routerConfig);
graviteeManagementModule.config(authenticationConfig);
graviteeManagementModule.config(managementRouterConfig);
graviteeManagementModule.config(organizationRouterConfig);
graviteeManagementModule.config(applicationRouterConfig);
graviteeManagementModule.config(applicationsNotificationsRouterConfig);
graviteeManagementModule.config(apisRouterConfig);
graviteeManagementModule.config(apisPortalRouterConfig);
graviteeManagementModule.config(apisProxyRouterConfig);
graviteeManagementModule.config(apisDesignRouterConfig);
graviteeManagementModule.config(apisAnalyticsRouterConfig);
graviteeManagementModule.config(apisAuditRouterConfig);
graviteeManagementModule.config(apisNotificationsRouterConfig);
graviteeManagementModule.config(apisMessagesRouterConfig);
graviteeManagementModule.config(configurationRouterConfig);
graviteeManagementModule.config(globalNotificationsRouterConfig);
graviteeManagementModule.config(interceptorConfig);
graviteeManagementModule.config(delegatorConfig);
graviteeManagementModule.config(($mdThemingProvider: angular.material.IThemingProvider) => {
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

  $mdThemingProvider.theme('sidenav').backgroundPalette('grey', {
    default: '50',
  });

  $mdThemingProvider.theme('toast-success');
  $mdThemingProvider.theme('toast-error');
});
graviteeManagementModule.config(($showdownProvider) => {
  $showdownProvider.setOption('tables', true);
  $showdownProvider.loadExtension('highlightjs');
  $showdownProvider.loadExtension('prettify');
  $showdownProvider.loadExtension('docHelper');
});
graviteeManagementModule.run(runBlock);
graviteeManagementModule.controller('ApisController', ApisController);
graviteeManagementModule.controller('ApisStatusDashboardController', ApisStatusDashboardController);
graviteeManagementModule.controller('ApiAdminController', ApiAdminController);
graviteeManagementModule.controller('ApiAnalyticsController', ApiAnalyticsController);
graviteeManagementModule.controller('ApiPoliciesController', ApiPoliciesController);
graviteeManagementModule.controller('AddPoliciesPathController', AddPoliciesPathController);
graviteeManagementModule.controller('ApiMembersController', ApiMembersController);
graviteeManagementModule.controller('ApiTransferOwnershipController', ApiTransferOwnershipController);
graviteeManagementModule.controller('ApiPortalController', ApiPortalController);
graviteeManagementModule.controller('ApiProxyController', ApiProxyController);
graviteeManagementModule.controller('ApiHealthCheckController', ApiHealthCheckController);
graviteeManagementModule.controller('ApiEndpointController', ApiEndpointController);
graviteeManagementModule.controller('ApiEndpointGroupController', ApiEndpointGroupController);
graviteeManagementModule.controller('DialogAssertionInformationController', DialogAssertionInformationController);
graviteeManagementModule.controller('ApiPropertiesController', ApiPropertiesController);
graviteeManagementModule.controller('ApiEventsController', ApiEventsController);
graviteeManagementModule.controller('ApiHistoryController', ApiHistoryController);
graviteeManagementModule.controller('ApiResourcesController', ApiResourcesController);
graviteeManagementModule.controller('ApiPathMappingsController', ApiPathMappingsController);
graviteeManagementModule.controller('DialogAddPathMappingController', DialogAddPathMappingController);
graviteeManagementModule.controller('DialogImportPathMappingController', DialogImportPathMappingController);
graviteeManagementModule.controller('DialogAddPropertyController', DialogAddPropertyController);
graviteeManagementModule.controller('DialogAddMemberApiController', DialogAddMemberApiController);
graviteeManagementModule.controller('DialogTransferApiController', DialogTransferApiController);
graviteeManagementModule.controller('DialogApiKeyExpirationController', DialogApiKeyExpirationController);
graviteeManagementModule.controller('UserController', UserController);
graviteeManagementModule.controller('DialogApiImportController', DialogApiImportController);
graviteeManagementModule.controller('DialogApiExportController', DialogApiExportController);
graviteeManagementModule.controller('DialogApiDuplicateController', DialogApiDuplicateController);
graviteeManagementModule.controller('DialogEditPolicyController', DialogEditPolicyController);
graviteeManagementModule.controller('LoginController', LoginController);
graviteeManagementModule.controller('InstancesController', InstancesController);
graviteeManagementModule.controller('InstanceEnvironmentController', InstanceEnvironmentController);
graviteeManagementModule.controller('InstanceMonitoringController', InstanceMonitoringController);
graviteeManagementModule.controller('AnalyticsDashboardController', AnalyticsDashboardController);
graviteeManagementModule.controller('DashboardController', DashboardController);
graviteeManagementModule.controller('HomeDashboardController', HomeDashboardController);
graviteeManagementModule.component('gvAlertDashboard', AlertsDashboardComponent);
graviteeManagementModule.controller('PlatformAlertsDashboardController', PlatformAlertsDashboardController);
graviteeManagementModule.controller('ApiAlertsDashboardController', ApiAlertsDashboardController);
graviteeManagementModule.controller('CategoriesController', CategoriesController);
graviteeManagementModule.controller('CategoryController', CategoryController);
graviteeManagementModule.controller('TenantsController', TenantsController);
graviteeManagementModule.controller('DeleteCategoryDialogController', DeleteCategoryDialogController);
graviteeManagementModule.controller('DeleteAPICategoryDialogController', DeleteAPICategoryDialogController);
graviteeManagementModule.controller('DeleteTenantDialogController', DeleteTenantDialogController);
graviteeManagementModule.component('groups', GroupsComponent);
graviteeManagementModule.component('group', GroupComponent);
graviteeManagementModule.controller('DialogAddGroupMemberController', DialogAddGroupMemberController);
graviteeManagementModule.controller('RegistrationController', RegistrationController);
graviteeManagementModule.controller('ConfirmController', ConfirmController);
graviteeManagementModule.controller('ResetPasswordController', ResetPasswordController);
graviteeManagementModule.controller('NewsletterSubscriptionController', NewsletterSubscriptionController);
graviteeManagementModule.controller('DialogSubscriptionRejectController', DialogSubscriptionRejectController);
graviteeManagementModule.controller('DialogSubscriptionAcceptController', DialogSubscriptionAcceptController);
graviteeManagementModule.controller('DialogSubscriptionCreateController', DialogSubscriptionCreateController);
graviteeManagementModule.controller('DialogSubscriptionTransferController', DialogSubscriptionTransferController);
graviteeManagementModule.controller('DialogSubscriptionChangeEndDateController', DialogSubscriptionChangeEndDateController);
graviteeManagementModule.controller('DialogSubscriptionRenewController', DialogSubscriptionRenewController);
graviteeManagementModule.controller('DialogPublishPlanController', DialogPublishPlanController);
graviteeManagementModule.controller('TagsController', TagsController);
graviteeManagementModule.controller('MetadataController', MetadataController);
graviteeManagementModule.controller('DeleteTagDialogController', DeleteTagDialogController);
graviteeManagementModule.controller('DeleteMetadataDialogController', DeleteMetadataDialogController);
graviteeManagementModule.controller('NewMetadataDialogController', NewMetadataDialogController);
graviteeManagementModule.controller('UpdateMetadataDialogController', UpdateMetadataDialogController);
graviteeManagementModule.controller('DeleteFieldDialogController', DeleteFieldDialogController);
graviteeManagementModule.controller('NewFieldDialogController', NewFieldDialogController);
graviteeManagementModule.controller('UpdateFieldDialogController', UpdateFieldDialogController);
graviteeManagementModule.controller('FileChooserDialogController', FileChooserDialogController);
graviteeManagementModule.controller('DialogConfirmController', DialogConfirmController);
graviteeManagementModule.controller('DialogConfirmDeploymentController', DialogConfirmDeploymentController);
graviteeManagementModule.controller('DialogConfirmAndValidateController', DialogConfirmAndValidateController);
graviteeManagementModule.controller('DialogDynamicProviderHttpController', DialogDynamicProviderHttpController);
graviteeManagementModule.controller('DialogAddUserRoleController', DialogAddUserRoleController);
graviteeManagementModule.controller('SupportTicketController', SupportTicketController);
graviteeManagementModule.controller('TicketsListController', TicketsListController);
graviteeManagementModule.controller('AuditController', AuditController);
graviteeManagementModule.controller('ApiAuditController', ApiAuditController);
graviteeManagementModule.controller('ThemeController', ThemeController);
graviteeManagementModule.controller('CustomUserFieldsController', CustomUserFieldsController);
graviteeManagementModule.controller('TopApisController', TopApisController);
graviteeManagementModule.controller('AddTopApiDialogController', AddTopApiDialogController);
graviteeManagementModule.controller('DeleteTopApiDialogController', DeleteTopApiDialogController);
graviteeManagementModule.controller('SelectFolderDialogController', SelectFolderDialogController);
graviteeManagementModule.controller('SelectPageDialogController', SelectPageDialogController);
graviteeManagementModule.controller('DialogReviewController', DialogReviewController);
graviteeManagementModule.controller('DialogRequestForChangesController', DialogRequestForChangesController);
graviteeManagementModule.controller('PoliciesController', PoliciesController);
graviteeManagementModule.service('ApplicationService', ApplicationService);
graviteeManagementModule.service('ApplicationTypesService', ApplicationTypesService);
graviteeManagementModule.service('ApiService', ApiService);
graviteeManagementModule.service('debugApiService', DebugApiService);
graviteeManagementModule.service('ApiPrimaryOwnerModeService', ApiPrimaryOwnerModeService);
graviteeManagementModule.service('CorsService', CorsService);
graviteeManagementModule.service('DocumentationService', DocumentationService);
graviteeManagementModule.service('InstancesService', InstancesService);
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
graviteeManagementModule.service('ThemeService', ThemeService);
graviteeManagementModule.service('ReCaptchaService', ReCaptchaService);
graviteeManagementModule.service('TokenService', TokenService);
graviteeManagementModule.service('EnvironmentService', EnvironmentService);
graviteeManagementModule.service('OrganizationService', OrganizationService);
graviteeManagementModule.service('InstallationService', InstallationService);
graviteeManagementModule.service('FlowService', FlowService);
graviteeManagementModule.service('SpelService', SpelService);
graviteeManagementModule.service('ConnectorService', ConnectorService);
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
graviteeManagementModule.directive('gvThemeElement', () => ThemeElementDirective);

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

graviteeManagementModule.component('tenants', TenantsComponent);
graviteeManagementModule.directive('ngTenants', downgradeComponent({ component: OrgSettingsTenantsComponent }));

graviteeManagementModule.component('tags', TagsComponent);
graviteeManagementModule.directive('ngOrgSettingsTags', downgradeComponent({ component: OrgSettingsTagsComponent }));
graviteeManagementModule.component('metadata', MetadataComponent);
graviteeManagementModule.component('roles', RolesComponent);
graviteeManagementModule.directive('ngRoles', downgradeComponent({ component: OrgSettingsRolesComponent }));
graviteeManagementModule.component('role', RoleComponent);
graviteeManagementModule.directive('ngOrgSettingsRole', downgradeComponent({ component: OrgSettingsRoleComponent }));
graviteeManagementModule.component('roleMembers', RoleMembersComponent);
graviteeManagementModule.directive('ngRoleMembers', downgradeComponent({ component: OrgSettingsRoleMembersComponent }));
graviteeManagementModule.component('theme', ThemeComponent);
graviteeManagementModule.component('topApis', TopApisComponent);
graviteeManagementModule.component('cockpit', CockpitComponent);
graviteeManagementModule.directive('ngCockpit', downgradeComponent({ component: OrgSettingsCockpitComponent }));
graviteeManagementModule.component('consoleSettings', ConsoleSettingsComponentAjs);
graviteeManagementModule.directive('ngConsoleSettings', downgradeComponent({ component: OrgSettingsGeneralComponent }));
graviteeManagementModule.component('portalSettings', PortalSettingsComponent);
graviteeManagementModule.component('analyticsSettings', AnalyticsSettingsComponent);
graviteeManagementModule.directive('gvMetadataValidator', () => MetadataValidatorDirective);
graviteeManagementModule.component('customUserFields', CustomUserFieldsComponent);
graviteeManagementModule.component('ticketDetail', TicketDetailComponent);
graviteeManagementModule.component('policies', PoliciesComponent);
graviteeManagementModule.directive('ngPlatformPolicies', downgradeComponent({ component: OrgSettingsPlatformPoliciesComponent }));

graviteeManagementModule.component('instances', InstancesComponent);
graviteeManagementModule.component('instance', InstanceComponent);
graviteeManagementModule.component('instanceHeader', InstanceHeaderComponent);
graviteeManagementModule.component('instanceEnvironment', InstanceEnvironmentComponent);
graviteeManagementModule.component('instanceMonitoring', InstanceMonitoringComponent);

graviteeManagementModule.component('apiCreation', ApiCreationComponent);
graviteeManagementModule.controller('ApiCreationController', ApiCreationController);
graviteeManagementModule.controller('NewApiController', NewApiController);
graviteeManagementModule.component('apiCreationStep1', ApiCreationStep1Component);
graviteeManagementModule.component('apiCreationStep2', ApiCreationStep2Component);
graviteeManagementModule.component('apiCreationStep3', ApiCreationStep3Component);
graviteeManagementModule.component('apiCreationStep4', ApiCreationStep4Component);
graviteeManagementModule.component('apiCreationStep5', ApiCreationStep5Component);
graviteeManagementModule.component('gvApiImport', ApiImportComponent);
graviteeManagementModule.component('gvDashboard', DashboardComponent);
graviteeManagementModule.component('gvDashboardFilter', DashboardFilterComponent);
graviteeManagementModule.controller('DashboardFilterController', DashboardFilterController);
graviteeManagementModule.component('gvDashboardTimeframe', DashboardTimeframeComponent);
graviteeManagementModule.controller('DashboardTimeframeController', DashboardTimeframeController);

// Plan
graviteeManagementModule.component('apiPlan', ApiPlanComponent);
graviteeManagementModule.component('editPlan', ApiEditPlanComponent);
graviteeManagementModule.controller('ApiEditPlanController', ApiEditPlanController);
graviteeManagementModule.component('listPlans', ApiListPlansComponent);
graviteeManagementModule.controller('ApiListPlansController', ApiListPlansController);
graviteeManagementModule.component('planWizardGeneral', ApiEditPlanWizardGeneralComponent);
graviteeManagementModule.component('planWizardSecurity', ApiEditPlanWizardSecurityComponent);
graviteeManagementModule.component('planWizardPolicies', ApiEditPlanWizardPoliciesComponent);
graviteeManagementModule.component('planWizardRestrictions', ApiEditPlanWizardRestrictionsComponent);

// API subscriptions
graviteeManagementModule.component('apiKeyValidatedInput', ApiKeyValidatedInput);
graviteeManagementModule.component('apiSubscriptions', ApiSubscriptionsComponent);
graviteeManagementModule.component('apiSubscription', ApiSubscriptionComponent);

graviteeManagementModule.component('applications', ApplicationsComponent);
graviteeManagementModule.component('application', ApplicationComponent);

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
graviteeManagementModule.component('applicationGeneral', ApplicationGeneralComponent);
graviteeManagementModule.component('applicationSubscriptions', ApplicationSubscriptionsComponent);
graviteeManagementModule.component('applicationSubscription', ApplicationSubscriptionComponent);
graviteeManagementModule.component('applicationMembers', ApplicationMembersComponent);
graviteeManagementModule.component('applicationAnalytics', ApplicationAnalyticsComponent);
graviteeManagementModule.component('applicationLogs', ApplicationLogsComponent);
graviteeManagementModule.component('applicationLog', ApplicationLogComponent);
graviteeManagementModule.controller('DialogAddMemberController', DialogAddMemberController);
graviteeManagementModule.controller('ApplicationsController', ApplicationsController);
graviteeManagementModule.controller('ApplicationGeneralController', ApplicationGeneralController);
graviteeManagementModule.controller('ApplicationMembersController', ApplicationMembersController);
graviteeManagementModule.controller('ApplicationSubscriptionsController', ApplicationSubscriptionsController);
graviteeManagementModule.controller('ApplicationAnalyticsController', ApplicationAnalyticsController);
graviteeManagementModule.controller('ApplicationLogsController', ApplicationLogsController);
graviteeManagementModule.controller('DialogTransferApplicationController', DialogTransferApplicationController);

graviteeManagementModule.component('user', UserComponent);

graviteeManagementModule.component('tasks', TasksComponent);
graviteeManagementModule.component('promotionTask', PromotionTaskComponent);

graviteeManagementModule.service('TaskService', TaskService);

graviteeManagementModule.component('portalNotifications', PortalNotificationsComponent);
graviteeManagementModule.service('UserNotificationService', UserNotificationService);
graviteeManagementModule.service('NotificationSettingsService', NotificationSettingsService);
graviteeManagementModule.service('NotificationTemplatesService', NotificationTemplatesService);
graviteeManagementModule.controller('DialogAddNotificationSettingsController', DialogAddNotificationSettingsController);
graviteeManagementModule.component('notificationSettingsComponent', NotificationSettingsComponent);
graviteeManagementModule.component('notificationsComponent', NotificationsComponent);

graviteeManagementModule.component('notificationTemplatesComponent', NotificationTemplatesComponent);
graviteeManagementModule.directive(
  'ngNotificationTemplatesComponent',
  downgradeComponent({ component: OrgSettingsNotificationTemplatesComponent }),
);

graviteeManagementModule.component('notificationTemplateComponent', NotificationTemplateComponent);
graviteeManagementModule.directive(
  'ngNotificationTemplateComponent',
  downgradeComponent({ component: OrgSettingsNotificationTemplateComponent }),
);

graviteeManagementModule.component('gvNotificationTemplateByType', NotificationTemplateByTypeComponent);
graviteeManagementModule.controller('NotificationTemplatesController', NotificationTemplatesController);
graviteeManagementModule.controller('NotificationTemplateController', NotificationTemplateController);
graviteeManagementModule.controller('NotificationTemplateByTypeController', NotificationTemplateByTypeController);

graviteeManagementModule.component('gvSidenav', SidenavComponent);
graviteeManagementModule.component('gvSubmenu', SubmenuComponent);
graviteeManagementModule.component('logout', LogoutComponent);
graviteeManagementModule.component('graviteeNavbar', NavbarComponent);

graviteeManagementModule.filter('currentSubmenus', submenuFilter);
graviteeManagementModule.service('SidenavService', SidenavService);

graviteeManagementModule.controller('ApiLogsController', ApiLogsController);
graviteeManagementModule.component('gvLogsTimeframe', LogsTimeframeComponent);
graviteeManagementModule.controller('LogsTimeframeController', LogsTimeframeController);
graviteeManagementModule.component('log', LogComponent);
graviteeManagementModule.component('gvLogsFilters', LogsFiltersComponent);
graviteeManagementModule.controller('LogsFiltersController', LogsFiltersController);
graviteeManagementModule.controller('ApiLoggingConfigurationController', ApiLoggingConfigurationController);
graviteeManagementModule.controller('DialogConfigureLoggingEditorController', DialogConfigureLoggingEditorController);

graviteeManagementModule.component('gvAudit', AuditComponent);
graviteeManagementModule.component('gvNewsletterReminder', NewsletterReminderComponent);
graviteeManagementModule.component('gvContextualDoc', ContextualDocComponent);
graviteeManagementModule.controller('ContextualDocController', ContextualDocController);

// Healthcheck
graviteeManagementModule.controller('ApiHealthCheckConfigureController', ApiHealthCheckConfigureController);
graviteeManagementModule.controller('ApiHealthCheckLogController', ApiHealthCheckLogController);
graviteeManagementModule.component('progressBar', ProgressBarComponent);
graviteeManagementModule.component('gvHealthcheckMetric', HealthCheckMetricComponent);

// Response Templates
graviteeManagementModule.controller('ApiResponseTemplatesController', ApiResponseTemplatesController);
graviteeManagementModule.controller('ApiResponseTemplateController', ApiResponseTemplateController);
graviteeManagementModule.component('gvResponseTemplateType', ApiResponseTemplateTypeComponent);
graviteeManagementModule.component('gvResponseTemplate', ApiResponseTemplateComponent);

// Configuration
graviteeManagementModule.component('settings', SettingsComponent);
graviteeManagementModule.component('organizationSettings', OrganizationSettingsComponent);
graviteeManagementModule.service('ConsoleSettingsService', ConsoleSettingsService);
graviteeManagementModule.service('PortalSettingsService', PortalSettingsService);
graviteeManagementModule.service('PortalConfigService', PortalConfigService);
graviteeManagementModule.component('apiLogging', ApiLoggingComponent);
graviteeManagementModule.controller('ApiLoggingController', ApiLoggingController);

// Users
graviteeManagementModule.component('users', UsersComponent);
graviteeManagementModule.directive('ngOrgSettingsUsers', downgradeComponent({ component: OrgSettingsUsersComponent }));

graviteeManagementModule.component('userDetail', UserDetailComponent);
graviteeManagementModule.directive('ngOrgSettingsUserDetail', downgradeComponent({ component: OrgSettingsUserDetailComponent }));

graviteeManagementModule.component('newUser', NewUserComponent);
graviteeManagementModule.directive('ngOrgSettingsNewUser', downgradeComponent({ component: OrgSettingsNewUserComponent }));

graviteeManagementModule.controller('DialogAddUserGroupController', DialogAddUserGroupController);

// Router
graviteeManagementModule.service('RouterService', RouterService);

graviteeManagementModule.component('messages', MessagesComponent);

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
graviteeManagementModule.directive(
  'ngOrgSettingsIdentityProviders',
  downgradeComponent({ component: OrgSettingsIdentityProvidersComponent }),
);
graviteeManagementModule.directive(
  'ngOrgSettingsIdentityProvider',
  downgradeComponent({ component: OrgSettingsIdentityProviderComponent }),
);

graviteeManagementModule.component('identityProvider', IdentityProviderComponent);
graviteeManagementModule.component('gvIdentityproviderGraviteeioAm', IdentityProviderGraviteeioAmComponent);
graviteeManagementModule.component('gvIdentityproviderGoogle', IdentityProviderGoogleComponent);
graviteeManagementModule.component('gvIdentityproviderGithub', IdentityProviderGitHubComponent);
graviteeManagementModule.component('gvIdentityproviderOidc', IdentityProviderOIDCComponent);
graviteeManagementModule.controller('IdentityProviderController', IdentityProviderController);
graviteeManagementModule.service('IdentityProviderService', IdentityProviderService);

// Settings: Client Registration
graviteeManagementModule.component('clientRegistrationProviders', ClientRegistrationProvidersComponent);
graviteeManagementModule.component('clientRegistrationProvider', ClientRegistrationProviderComponent);
graviteeManagementModule.controller('ClientRegistrationProviderController', ClientRegistrationProviderController);
graviteeManagementModule.service('ClientRegistrationProviderService', ClientRegistrationProviderService);

// Alerts
graviteeManagementModule.service('AlertService', AlertService);
graviteeManagementModule.component('alertsComponent', AlertsComponent);
graviteeManagementModule.component('alertComponent', AlertComponent);
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
graviteeManagementModule.component('entrypoint', EntrypointComponent);
graviteeManagementModule.controller('EntrypointController', EntrypointController);
graviteeManagementModule.controller('DeleteEntrypointDialogController', DeleteEntrypointDialogController);
graviteeManagementModule.component('tag', TagComponent);
graviteeManagementModule.controller('TagController', TagController);

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
