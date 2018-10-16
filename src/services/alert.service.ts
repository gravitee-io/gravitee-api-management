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
import {Alert} from "../management/components/notifications/alert/alert";

class AlertService {
  private apisURL: string;
  private applicationsURL: string;
  private configurationURL: string;
  private alertsURL: string;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.apisURL = `${Constants.baseURL}apis/`;
    this.applicationsURL = `${Constants.baseURL}applications/`;
    this.configurationURL = `${Constants.baseURL}configuration/`;
    this.alertsURL = `${Constants.baseURL}alerts/`;
  }

  listMetrics(): IHttpPromise<any> {
    return this.$http.get(this.alertsURL + 'metrics');
  }

  listAlerts(referenceId?: string, referenceType?: string): IHttpPromise<any> {
    return this.$http.get(this.getReferenceURL(referenceType, referenceId) + 'alerts');
  }

  create(alert: Alert): IHttpPromise<any> {
    return this.$http.post(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts', alert);
  }

  update(alert: Alert): IHttpPromise<any> {
    delete alert['created_at'];
    delete alert['updated_at'];
    return this.$http.put(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id, alert);
  }

  delete(alert: Alert) {
    return this.$http.delete(this.getReferenceURL(alert.reference_type, alert.reference_id) + 'alerts/' + alert.id);
  }

  private getReferenceURL(referenceType: string, referenceId: string) {
    switch (referenceType) {
      case 'api':
        return this.apisURL + referenceId + '/';
      case 'application':
        return this.applicationsURL + referenceId + '/';
      default:
        return this.configurationURL;
    }
  }
}

export default AlertService;
