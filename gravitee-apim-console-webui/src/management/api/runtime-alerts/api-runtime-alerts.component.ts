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
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiAlertsService } from '../../../services-ngx/api-alerts.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-runtime-alerts',
  templateUrl: './api-runtime-alerts.component.html',
})
export class ApiRuntimeAlertsComponent {
  public alerts$ = this.apiAlertsService.listAlerts(this.activatedRoute.snapshot.params.apiId, true);
  protected canCreateAlert = this.permissionService.hasAnyMatching(['api-alert-c']);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiAlertsService: ApiAlertsService,
    private readonly permissionService: GioPermissionService,
    private readonly router: Router,
  ) {}

  createAlert() {
    return this.router.navigate(['./new'], { relativeTo: this.activatedRoute });
  }
}
