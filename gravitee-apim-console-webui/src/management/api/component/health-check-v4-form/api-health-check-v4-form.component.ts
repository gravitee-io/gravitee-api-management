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

import { Component, Input } from '@angular/core';

import { EndpointGroupHealthCheckFormType } from '../../endpoints-v4/endpoint-group/api-endpoint-group.component';
import { EndpointHealthCheckFormType } from '../../endpoints-v4/endpoint/api-endpoint.component';

@Component({
  selector: 'api-health-check-v4-form',
  templateUrl: './api-health-check-v4-form.component.html',
  styleUrls: ['./api-health-check-v4-form.component.scss'],
  standalone: false,
})
export class ApiHealthCheckV4FormComponent {
  @Input() healthCheckForm: EndpointGroupHealthCheckFormType | EndpointHealthCheckFormType;
  @Input() healthCheckSchema: unknown;

  public static readonly HTTP_HEALTH_CHECK = 'http-health-check';
}
