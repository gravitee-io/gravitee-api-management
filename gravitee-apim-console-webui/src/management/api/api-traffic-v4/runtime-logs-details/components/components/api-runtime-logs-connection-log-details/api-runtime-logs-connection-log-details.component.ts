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

import { ConnectionLogDetail } from '../../../../../../../entities/management-api-v2';

@Component({
  selector: 'api-runtime-logs-connection-log-details',
  templateUrl: './api-runtime-logs-connection-log-details.component.html',
  styleUrls: ['./api-runtime-logs-connection-log-details.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsConnectionLogDetailsComponent {
  @Input()
  isEntrypoint: boolean;
  @Input()
  connectionLogDetail: ConnectionLogDetail;
  protected readonly Object = Object;
}
