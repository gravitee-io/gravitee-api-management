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

import { IPromise, IScope } from 'angular';
import { StateService } from '@uirouter/core';
import '@gravitee/ui-components/wc/gv-chart-bar';

import { ITimeframe, TimeframeRanges } from '../../quick-time-range/quick-time-range.component';
import { Alert, Scope } from '../../../entities/alert';
import AlertService, { IAlertTriggerAnalytics } from '../../../services/alert.service';
import UserService from '../../../services/user.service';

class AlertsDashboardComponent implements ng.IComponentController {
  private static INFO_COLOR = '#54a3ff';
  private static WARNING_COLOR = '#FF950D';
  private static CRITICAL_COLOR = '#d73a49';

  private customTimeframe: any;
  private timeframe: ITimeframe;

  private alerts: IAlertTriggerAnalytics[] = [];
  private eventsBySeverity: Record<string, number>;

  private referenceType: string;
  private referenceId: string;
  private hasConfiguredAlerts: boolean;
  private hasAlertingPlugin: boolean;
  private series: IPromise<unknown>;
  private options: any;

  constructor(private $scope: IScope, private AlertService: AlertService, private UserService: UserService, private $state: StateService) {
    'ngInject';
  }

  $onInit() {
    this.timeframe = TimeframeRanges.LAST_MINUTE;

    this.options = {
      name: 'Severity',
      data: [
        {
          name: 'INFO',
          color: AlertsDashboardComponent.INFO_COLOR,
        },
        {
          name: 'WARNING',
          color: AlertsDashboardComponent.WARNING_COLOR,
        },
        {
          name: 'CRITICAL',
          color: AlertsDashboardComponent.CRITICAL_COLOR,
        },
      ],
    };

    if (this.hasAlertingPlugin && this.hasConfiguredAlerts) {
      this.refresh();
    }
  }

  searchAlertAnalytics() {
    const contextualInformationFromReferenceType = this.getContextualInformationFromReferenceType();
    if (contextualInformationFromReferenceType.hasPermission) {
      this.series = this.AlertService.getAnalytics(
        this.customTimeframe.from,
        this.customTimeframe.to,
        contextualInformationFromReferenceType.scope,
        this.referenceId,
      ).then((response) => {
        this.alerts = response.data.alerts;
        this.eventsBySeverity = response.data.bySeverity;

        return {
          values: {
            ...this.eventsBySeverity,
          },
        };
      });
    }
  }

  refresh(timeframe?: ITimeframe) {
    if (timeframe) {
      this.timeframe = timeframe;
    }
    const now = Date.now();

    this.customTimeframe = {
      interval: this.timeframe.interval,
      from: now - this.timeframe.range,
      to: now,
    };

    this.searchAlertAnalytics();
  }

  getContextualInformationFromReferenceType(): {
    scope: Scope;
    alertCreationUiRef: string;
    uiRef: string;
    permission: string;
    hasPermission: boolean;
  } {
    switch (this.referenceType) {
      case 'API':
        return {
          scope: Scope.API,
          alertCreationUiRef: 'management.apis.detail.alerts.alertnew',
          uiRef: 'management.apis.detail.alerts.alert({alertId: alert.id, tab: "history"})',
          permission: 'api-alert-r',
          hasPermission: this.UserService.currentUser?.userApiPermissions.includes('api-alert-r'),
        };
      case 'APPLICATION':
        return {
          scope: Scope.APPLICATION,
          alertCreationUiRef: '',
          uiRef: '',
          permission: 'application-alert-r',
          hasPermission: this.UserService.currentUser?.userApplicationPermissions.includes('application-alert-r'),
        };
      default:
        return {
          scope: Scope.ENVIRONMENT,
          alertCreationUiRef: 'management.settings.alerts.alertnew',
          uiRef: 'management.settings.alerts.alert({alertId: alert.id, tab: "history"})',
          permission: 'environment-alert-r',
          hasPermission: this.UserService.currentUser?.userEnvironmentPermissions.includes('environment-alert-r'),
        };
    }
  }

  goToAlertCreation() {
    this.$state.go(this.getContextualInformationFromReferenceType().alertCreationUiRef);
  }

  isAlertCritical(alert: Alert) {
    return 'CRITICAL' === alert.severity;
  }

  isAlertWarning(alert: Alert) {
    return 'WARNING' === alert.severity;
  }

  isAlertInfo(alert: Alert) {
    return 'INFO' === alert.severity;
  }

  getSeverityColor(alert: Alert) {
    switch (alert.severity) {
      case 'INFO':
        return AlertsDashboardComponent.INFO_COLOR;
      case 'WARNING':
        return AlertsDashboardComponent.WARNING_COLOR;
      case 'CRITICAL':
        return AlertsDashboardComponent.CRITICAL_COLOR;
    }
  }
}

const AlertDashBoardComponent: ng.IComponentOptions = {
  template: require('./alerts-dashboard.html'),
  bindings: {
    referenceType: '<',
    referenceId: '<',
    hasConfiguredAlerts: '<',
    hasAlertingPlugin: '<',
  },
  controller: AlertsDashboardComponent,
};

export default AlertDashBoardComponent;
