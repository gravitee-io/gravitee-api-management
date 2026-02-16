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
import { Scope as AlertScope } from '../../../entities/alert';
import AlertService from '../../../services/alert.service';

class AlertsActivityController {
  private hasConfiguredAlerts: boolean;
  private hasAlertingPlugin: boolean;
  private isLoaded = false;

  constructor(private AlertService: AlertService) {}

  $onInit() {
    Promise.all([
      this.AlertService.listAlerts(AlertScope.ENVIRONMENT, false).then(response => response.data),
      this.AlertService.getStatus(AlertScope.ENVIRONMENT).then(response => response.data),
    ]).then(([configuredAlerts, alertingStatus]) => {
      this.hasConfiguredAlerts = configuredAlerts?.length > 0;
      this.hasAlertingPlugin = alertingStatus?.available_plugins > 0;
      this.isLoaded = true;
    });
  }
}
AlertsActivityController.$inject = ['AlertService'];

const AlertsActivityComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
  },
  controller: AlertsActivityController,
  template: require('html-loader!./alerts-activity.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
};

export default AlertsActivityComponentAjs;
