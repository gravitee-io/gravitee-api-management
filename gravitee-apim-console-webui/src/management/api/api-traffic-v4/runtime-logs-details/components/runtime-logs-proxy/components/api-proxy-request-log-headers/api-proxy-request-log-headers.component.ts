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
import { Component, computed, input, InputSignal } from '@angular/core';

@Component({
  selector: 'api-proxy-request-log-headers',
  templateUrl: './api-proxy-request-log-headers.component.html',
  styleUrls: ['./api-proxy-request-log-headers.component.scss'],
})
export class ApiProxyRequestLogHeadersComponent {
  headers: InputSignal<Record<string, string[]>> = input.required<Record<string, string[]>>();
  formattedHeaders = computed(() => {
    const headersValue = this.headers();
    if (!headersValue) return [];
    return Object.entries(headersValue).map(([key, values]) => ({
      key,
      value: (values ?? []).join(','),
    }));
  });
}
