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
import { Component, input } from '@angular/core';

@Component({
  selector: 'env-logs-details-row',
  templateUrl: './env-logs-details-row.component.html',
  styleUrl: './env-logs-details-row.component.scss',
  standalone: true,
})
export class EnvLogsDetailsRowComponent {
  label = input.required<string>();
  dataTestId = input<string>();
}
