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
import { ActivatedRoute, Router } from '@angular/router';
import '@gravitee/ui-components/wc/gv-chart-bar';

import { ITimeframe, TimeframeRanges } from '../quick-time-range/quick-time-range.component';
import { Alert, Scope } from '../../../entities/alert';
import AlertService, { IAlertTriggerAnalytics } from '../../../services/alert.service';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';

class AlertsDashboardComponent implements ng.IComponentController {
  private static INFO_COLOR = '#54a3ff';
  private static WARNING_COLOR = '#FF950D';
  private static CRITICAL_COLOR = '#d73a49';

  private customTimeframe: any;
  private timeframe: ITimeframe = TimeframeRanges.LAST_MINUTE;

  private alerts: IAlertTriggerAnalytics[] = [];
  private eventsBySeverity: Record<string, number>;

  private referenceType: string;
  private referenceId: string;
  private hasConfiguredAlerts: boolean;
  private hasAlertingPlugin: boolean;
  private series: IPromise<unknown>;
  private options: any;
  private activatedRoute: ActivatedRoute;

  constructor(
    private $scope: IScope,
    private AlertService: AlertService,
    private UserService: UserService,
    private ngRouter: Router,
    private Constants: Constants,
  ) {}

  $onInit() {
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
  }

  $onChanges() {
    if (this.hasAlertingPlugin && this.hasConfiguredAlerts) {
      this.refresh(this.timeframe);
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

  refresh(timeframe: ITimeframe) {
    this.timeframe = timeframe;
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
    alertCreationRouteSegment: string;
    alertEditRouteSegment: string;
    permission: string;
    hasPermission: boolean;
  } {
    switch (this.referenceType) {
      case 'API':
        return {
          scope: Scope.API,
          alertCreationRouteSegment: '../alerts/new',
          alertEditRouteSegment: '../alerts',
          permission: 'api-alert-r',
          hasPermission: this.UserService.currentUser?.userApiPermissions.includes('api-alert-r'),
        };
      case 'APPLICATION':
        return {
          scope: Scope.APPLICATION,
          alertCreationRouteSegment: '',
          alertEditRouteSegment: '',
          permission: 'application-alert-r',
          hasPermission: this.UserService.currentUser?.userApplicationPermissions.includes('application-alert-r'),
        };
      default:
        return {
          scope: Scope.ENVIRONMENT,
          alertCreationRouteSegment: '../list/new',
          alertEditRouteSegment: '../list',
          permission: 'environment-alert-r',
          hasPermission: this.UserService.currentUser?.userEnvironmentPermissions.includes('environment-alert-r'),
        };
    }
  }

  goToAlertCreation() {
    this.ngRouter.navigate([this.getContextualInformationFromReferenceType().alertCreationRouteSegment], {
      relativeTo: this.activatedRoute,
    });
  }

  goToHistory(alert: Alert) {
    this.ngRouter.navigate([this.getContextualInformationFromReferenceType().alertEditRouteSegment, alert.id], {
      relativeTo: this.activatedRoute,
    });
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
  template: require('html-loader!./alerts-dashboard.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    referenceType: '<',
    referenceId: '<',
    hasConfiguredAlerts: '<',
    hasAlertingPlugin: '<',
    activatedRoute: '<',
  },
  controller: AlertsDashboardComponent,
};
AlertsDashboardComponent.$inject = ['$scope', 'AlertService', 'UserService', 'ngRouter', 'Constants'];
export default AlertDashBoardComponent;
