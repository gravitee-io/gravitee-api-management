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

import { IHttpPromise } from 'angular';

import { Alert, Scope } from '../entities/alert';

export interface IAlertAnalytics {
  alerts: IAlertTriggerAnalytics[];
  bySeverity: Record<string, number>;
}

export interface IAlertTriggerAnalytics {
  events_count: number;
  id: string;
  severity: string;
  type: string;
}

class AlertService {
  constructor(private $http: ng.IHttpService, private Constants) {
    'ngInject';
  }

  listMetrics(): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/alerts/` + 'metrics');
  }

  getStatus(referenceType?: Scope, referenceId?: string): IHttpPromise<any> {
    return this.$http.get(this.getReferenceURL(referenceType, referenceId) + 'alerts/status');
  }

  listAlerts(referenceType?: Scope, withEventCounts = true, referenceId?: string): angular.IHttpPromise<any> {
    return this.$http.get(`${this.getReferenceURL(referenceType, referenceId)}alerts?event_counts=${withEventCounts}`);
  }

  getAnalytics(from, to, referenceType?: Scope, referenceId?: string): IHttpPromise<IAlertAnalytics> {
    return this.$http.get(`${this.getReferenceURL(referenceType, referenceId)}alerts/analytics?from=${from}&to=${to}`);
  }

  create(alert: Alert): IHttpPromise<any> {
    return this.$http.post(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts', {
      name: alert.name,
      severity: alert.severity,
      source: alert.source,
      description: alert.description,
      type: alert.type,
      enabled: alert.enabled,
      reference_type: Scope[alert.reference_type],
      reference_id: alert.reference_id,
      conditions: alert.conditions,
      notifications: alert.notifications,
      filters: alert.filters,
      projections: alert.projections,
      dampening: alert.dampening,
      template: alert.template,
      event_rules: alert.event_rules,
      notificationPeriods: alert.notificationPeriods,
    });
  }

  update(alert: Alert): IHttpPromise<any> {
    return this.$http.put(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id, {
      id: alert.id,
      name: alert.name,
      severity: alert.severity,
      source: alert.source,
      description: alert.description,
      type: alert.type,
      enabled: alert.enabled,
      reference_type: Scope[alert.reference_type],
      reference_id: alert.reference_id,
      conditions: alert.conditions,
      notifications: alert.notifications,
      filters: alert.filters,
      projections: alert.projections,
      dampening: alert.dampening,
      template: alert.template,
      event_rules: alert.event_rules,
      notificationPeriods: alert.notificationPeriods,
    });
  }

  listAlertEvents(alert: Alert, params?: any): IHttpPromise<any> {
    let url = this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id + '/events?';

    if (params !== undefined) {
      if (params.from !== undefined) {
        url += '&from=' + params.from;
      }

      if (params.to !== undefined) {
        url += '&to=' + params.to;
      }

      if (params.page !== undefined) {
        url += '&page=' + params.page;
      }

      if (params.size !== undefined) {
        url += '&size=' + params.size;
      }
    }

    return this.$http.get(url);
  }

  delete(alert: Alert) {
    return this.$http.delete(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id);
  }

  associate(alert: Alert, type: string): ng.IPromise<any> {
    return this.$http.post(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id + '?type=' + type, {});
  }

  private getReferenceURL(referenceType: Scope, referenceId: string) {
    switch (referenceType) {
      case Scope.API:
        return `${this.Constants.env.baseURL}/apis/` + referenceId + '/';
      case Scope.APPLICATION:
        return `${this.Constants.env.baseURL}/applications/` + referenceId + '/';
      default:
        return `${this.Constants.env.baseURL}/platform/`;
    }
  }
}

export default AlertService;
