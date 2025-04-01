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

import { FieldParameter } from '../../../entities/management-api-v2/api/v4/healthCheck';

@Component({
  selector: 'app-health-check-dashboard-v4',
  templateUrl: './api-health-check-dashboard-v4.component.html',
  styleUrl: './api-health-check-dashboard-v4.component.scss',
  standalone: false,
})
export class ApiHealthCheckDashboardV4Component {
  protected readonly FieldParameter = FieldParameter;
}
