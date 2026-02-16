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
import { ActivatedRoute } from '@angular/router';

import AlertService from '../../../../services/alert.service';
import { Scope } from '../../../../entities/alert';

class ApiAlertsDashboardControllerAjs {
  private hasConfiguredAlerts: boolean;
  private hasAlertingPlugin: boolean;
  apiId: string;
  activatedRoute: ActivatedRoute;

  constructor(
    private readonly AlertService: AlertService,
    private readonly $q: ng.IQService,
  ) {}

  $onInit() {
    this.apiId = this.activatedRoute.snapshot.params.apiId;
    this.$q
      .all({
        configuredAlerts: this.AlertService.listAlerts(Scope.API, false, this.activatedRoute.snapshot.params.apiId).then(
          response => response.data,
        ),
        alertingStatus: this.AlertService.getStatus(Scope.API, this.activatedRoute.snapshot.params.apiId).then(response => response.data),
      })
      .then(({ configuredAlerts, alertingStatus }) => {
        this.hasConfiguredAlerts = configuredAlerts?.length > 0;
        this.hasAlertingPlugin = alertingStatus?.available_plugins > 0;
      });
  }
}
ApiAlertsDashboardControllerAjs.$inject = ['AlertService', '$q'];

export default ApiAlertsDashboardControllerAjs;
