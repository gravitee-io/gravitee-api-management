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

import {IHttpPromise} from "angular";
import {Alert, Scope} from "../entities/alert";

class AlertService {
  private apisURL: string;
  private applicationsURL: string;
  private configurationURL: string;
  private alertsURL: string;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.apisURL = `${Constants.baseURL}apis/`;
    this.applicationsURL = `${Constants.baseURL}applications/`;
    this.configurationURL = `${Constants.baseURL}platform/`;
    this.alertsURL = `${Constants.baseURL}alerts/`;
  }

  listMetrics(): IHttpPromise<any> {
    return this.$http.get(this.alertsURL + 'metrics');
  }

  getStatus(referenceId?: string, referenceType?: Scope): IHttpPromise<any> {
    return this.$http.get(this.getReferenceURL(referenceType, referenceId) + 'alerts/status');
  }

  listAlerts(referenceId?: string, referenceType?: Scope): IHttpPromise<any> {
    return this.$http.get(this.getReferenceURL(referenceType, referenceId) + 'alerts');
  }

  create(alert: Alert): IHttpPromise<any> {
    return this.$http.post(
      this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts', {
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
        dampening: alert.dampening
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
      dampening: alert.dampening
    });
  }

  delete(alert: Alert) {
    return this.$http.delete(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id);
  }

  private getReferenceURL(referenceType: Scope, referenceId: string) {
    switch (referenceType) {
      case Scope.API:
        return this.apisURL + referenceId + '/';
      case Scope.APPLICATION:
        return this.applicationsURL + referenceId + '/';
      default:
        return this.configurationURL;
    }
  }
}

export default AlertService;
